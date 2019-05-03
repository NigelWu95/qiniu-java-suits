package com.qiniu.datasource;

import com.qiniu.common.QiniuException;
import com.qiniu.entry.CommonParams;
import com.qiniu.persistence.FileSaveMapper;
import com.qiniu.convert.MapToString;
import com.qiniu.convert.LineToMap;
import com.qiniu.interfaces.ILineProcess;
import com.qiniu.interfaces.ITypeConvert;
import com.qiniu.util.FileNameUtils;
import com.qiniu.util.HttpResponseUtils;
import com.qiniu.util.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileInput implements IDataSource<LocalFileReader, FileSaveMapper> {

    private String filePath;
    private String parseType;
    private String separator;
    private String rmKeyPrefix;
    private Map<String, String> indexMap;
    private int unitLen;
    private int threads;
    private int retryTimes = 5;
    private String savePath;
    private boolean saveTotal;
    private String saveFormat;
    private String saveSeparator;
    private List<String> rmFields;
    private ExecutorService executorPool; // 线程池
    private AtomicBoolean exitBool; // 多线程的原子操作 bool 值
    private ILineProcess<Map<String, String>> processor; // 定义的资源处理器

    public FileInput(String filePath, String parseType, String separator, String rmKeyPrefix, Map<String, String> indexMap,
                     int unitLen, int threads) {
        this.filePath = filePath;
        this.parseType = parseType;
        this.separator = separator;
        this.rmKeyPrefix = rmKeyPrefix;
        this.indexMap = indexMap;
        this.unitLen = unitLen;
        this.threads = threads;
        this.saveTotal = false; // 默认全记录不保存
    }

    @Override
    public String getSourceName() {
        return "local";
    }

    // 不调用则各参数使用默认值
    @Override
    public void setSaveOptions(String savePath, boolean saveTotal, String format, String separator, List<String> rmFields) {
        this.savePath = savePath;
        this.saveTotal = saveTotal;
        this.saveFormat = format;
        this.saveSeparator = separator;
        this.rmFields = rmFields;
    }

    @Override
    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

    // 通过 commonParams 来更新基本参数
    @Override
    public void updateSettings(CommonParams commonParams) {
        this.filePath = commonParams.getPath();
        this.parseType = commonParams.getParse();
        this.separator = commonParams.getSeparator();
        this.indexMap = commonParams.getIndexMap();
        this.unitLen = commonParams.getUnitLen();
        this.threads = commonParams.getThreads();
        this.retryTimes = commonParams.getRetryTimes();
        this.savePath = commonParams.getSavePath();
        this.saveTotal = commonParams.getSaveTotal();
        this.saveFormat = commonParams.getSaveFormat();
        this.saveSeparator = commonParams.getSaveSeparator();
        this.rmFields = commonParams.getRmFields();
    }

    @Override
    public void setProcessor(ILineProcess<Map<String, String>> processor) {
        this.processor = processor;
    }

    public void export(LocalFileReader reader, FileSaveMapper fileSaver, ILineProcess<Map<String, String>> processor)
            throws IOException {
        ITypeConvert<String, Map<String, String>> typeConverter = new LineToMap(parseType, separator, rmKeyPrefix, indexMap);
        ITypeConvert<Map<String, String>, String> writeTypeConverter = new MapToString(saveFormat, saveSeparator, rmFields);
        List<String> srcList = new ArrayList<>();
        List<Map<String, String>> infoMapList;
        List<String> writeList;
        String line = "";
        int retry;
        while (line != null) {
            retry = retryTimes + 1;
            while (retry > 0) {
                try {
                    // 避免文件过大，行数过多，使用 lines() 的 stream 方式直接转换可能会导致内存泄漏，故使用 readLine() 的方式
                    line = reader.readLine();
                    retry = 0;
                } catch (IOException e) {
                    retry--;
                    if (retry == 0) throw e;
                }
            }
            if (line != null) srcList.add(line);
            if (srcList.size() >= unitLen || (line == null && srcList.size() > 0)) {
                infoMapList = typeConverter.convertToVList(srcList);
                if (typeConverter.errorSize() > 0)
                    fileSaver.writeError(String.join("\n", typeConverter.consumeErrors()), false);
                if (saveTotal) {
                    writeList = writeTypeConverter.convertToVList(infoMapList);
                    if (writeList.size() > 0) fileSaver.writeSuccess(String.join("\n", writeList), false);
                    if (writeTypeConverter.errorSize() > 0)
                        fileSaver.writeError(String.join("\n", writeTypeConverter.consumeErrors()), false);
                }
                // 如果抛出异常需要检测下异常是否是可继续的异常，如果是程序可继续的异常，忽略当前异常保持数据源读取过程继续进行
                try {
                    if (processor != null) processor.processLine(infoMapList);
                } catch (QiniuException e) {
                    // 这里其实逻辑上没有做重试次数的限制，因为返回的 retry 始终大于等于 -1，所以不是必须抛出的异常则会跳过，process 本身会
                    // 保存失败的记录，除非是 process 出现 599 状态码才会抛出异常
                    retry = HttpResponseUtils.checkException(e, 1);
                    if (retry == -2) throw e;
                }
                srcList.clear();
            }
        }
    }

    public void execInThread(LocalFileReader reader, FileSaveMapper recordSaver, int order) throws Exception {
        // 如果是第一个线程直接使用初始的 processor 对象，否则使用 clone 的 processor 对象，多线程情况下不要直接使用传入的 processor，
        // 因为对其关闭会造成 clone 的对象无法进行结果持久化的写入
        ILineProcess<Map<String, String>> lineProcessor = processor == null ? null : processor.clone();
        // 持久化结果标识信息
        String newOrder = String.valueOf(order);
        FileSaveMapper fileSaveMapper = new FileSaveMapper(savePath, getSourceName(), newOrder);
        executorPool.execute(() -> {
            try {
                String record = "order " + newOrder + ": " + reader.getName();
                recordSaver.writeKeyFile(".result", record + "\treading...", true);
                export(reader, fileSaveMapper, lineProcessor);
                record += "\tsuccessfully done";
                System.out.println(record);
                recordSaver.writeKeyFile(".result", record, true);
                fileSaveMapper.closeWriters();
                if (lineProcessor != null) lineProcessor.closeResource();
                reader.close();
            } catch (Exception e) {
                try {
                    System.out.println("order " + newOrder + ": " + reader.getName() + "\tnextLine:" + reader.readLine());
                } catch (IOException io) {
                    io.printStackTrace();
                }
                recordSaver.closeWriters();
                fileSaveMapper.closeWriters();
                if (lineProcessor != null) lineProcessor.closeResource();
                SystemUtils.exit(exitBool, e);
            }
        });
    }

    @Override
    public void export() throws Exception {
        List<LocalFileReader> localFileReaders = new ArrayList<>();
        File sourceFile = new File(FileNameUtils.realPathWithUserHome(filePath));
        if (sourceFile.isDirectory()) {
            File[] fs = sourceFile.listFiles();
            if (fs == null) throw new IOException("The current path you gave may be incorrect: " + filePath);
            for(File f : fs) {
                if (!f.isDirectory() && f.getName().endsWith(".txt")) {
                    localFileReaders.add(new LocalFileReader(f.getAbsoluteFile().getPath()));
                }
            }
        } else {
            if (filePath.endsWith(".txt")) {
                localFileReaders.add(new LocalFileReader(filePath));
            } else {
                throw new IOException("please provide the .txt file. The current path you gave is: " + filePath);
            }
        }
        if (localFileReaders.size() == 0) throw new IOException("please provide the .txt file int the directory. The current" +
                " path you gave is: " + filePath);

        int filesCount = localFileReaders.size();
        int runningThreads = filesCount < threads ? filesCount : threads;
        String info = "read objects from file(s): " + filePath + (processor == null ? "" : " and " + processor.getProcessName());
        System.out.println(info + " running...");
        FileSaveMapper recordSaver = new FileSaveMapper(savePath);
        exitBool = new AtomicBoolean(false);
        try {
            executorPool = Executors.newFixedThreadPool(runningThreads);
            int order = 1;
            for (LocalFileReader localFileReader : localFileReaders) {
                execInThread(localFileReader, recordSaver, order++);
            }
            executorPool.shutdown();
            while (!executorPool.isTerminated()) Thread.sleep(1000);
            recordSaver.closeWriters();
            System.out.println(info + " finished");
        } catch (Throwable throwable) {
            SystemUtils.exit(exitBool, throwable);
        }
    }
}
