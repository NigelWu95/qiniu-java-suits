package com.qiniu.service.datasource;

import com.qiniu.persistence.FileMap;
import com.qiniu.service.convert.InfoMapToString;
import com.qiniu.service.convert.LineToInfoMap;
import com.qiniu.service.interfaces.ILineProcess;
import com.qiniu.service.interfaces.ITypeConvert;
import com.qiniu.util.ExecutorsUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class FileInput {

    private String parseType;
    private String separator;
    private Map<String, String> infoIndexMap;
    private int unitLen;
    private String resultPath;
    private boolean saveTotal;
    private String resultFormat;
    private String resultSeparator;
    private List<String> resultFields;

    public FileInput(String parseType, String separator, Map<String, String> infoIndexMap, int unitLen,
                     String resultPath) {
        this.parseType = parseType;
        this.separator = separator;
        this.infoIndexMap = infoIndexMap;
        this.unitLen = unitLen;
        this.resultPath = resultPath;
        this.saveTotal = false;
    }

    public void setResultSaveOptions(String format, String separator, List<String> fields) {
        this.saveTotal = true;
        this.resultFormat = format;
        this.resultSeparator = separator;
        this.resultFields = fields;
    }

    public void traverseByReader(String resultTag, BufferedReader bufferedReader, ILineProcess processor) {
        FileMap fileMap = new FileMap(resultPath, "fileinput", resultTag);
        processor.setResultTag(resultTag);
        try {
            ITypeConvert<String, Map<String, String>> typeConverter = new LineToInfoMap(parseType, separator, infoIndexMap);
            ITypeConvert<Map<String, String>, String> writeTypeConverter = null;
            if (saveTotal) {
                writeTypeConverter = new InfoMapToString(resultFormat, resultSeparator, resultFields);
                fileMap.initDefaultWriters();
            }
            List<String> srcList = new ArrayList<>();
            String line;
            boolean goon = true;
            while (goon) {
                // 避免文件过大，行数过多，使用 lines() 的 stream 方式直接转换可能会导致内存泄漏，故使用 readLine() 的方式
                line = bufferedReader.readLine();
                if (line == null) goon = false;
                else srcList.add(line);
                if (srcList.size() >= unitLen || line == null) {
                    List<Map<String, String>> infoMapList = typeConverter.convertToVList(srcList);
                    List<String> writeList;
                    if (typeConverter.getErrorList().size() > 0) fileMap.writeError(String.join("\n",
                            typeConverter.getErrorList()));
                    if (saveTotal) {
                        writeList = writeTypeConverter.convertToVList(infoMapList);
                        if (writeList.size() > 0) fileMap.writeSuccess(String.join("\n", writeList));
                        if (writeTypeConverter.getErrorList().size() > 0)
                            fileMap.writeError(String.join("\n", writeTypeConverter.getErrorList()));
                    }
                    int size = infoMapList.size() / unitLen + 1;
                    for (int j = 0; j < size; j++) {
                        List<Map<String, String>> processList = infoMapList.subList(unitLen * j,
                                j == size - 1 ? infoMapList.size() : unitLen * (j + 1));
                        if (processor != null) processor.processLine(processList);
                    }
                    srcList = new ArrayList<>();
                }
            }
            bufferedReader.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (processor != null) processor.closeResource();
        }
    }

    public void process(int maxThreads, String filePath, ILineProcess<Map<String, String>> processor) throws Exception {
        FileMap fileMap = new FileMap();
        File sourceFile = new File(filePath);
        try {
            if (sourceFile.isDirectory()) {
                fileMap.initReaders(filePath);
            } else {
                fileMap.initReader(sourceFile.getParent(), sourceFile.getName());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Set<Entry<String, BufferedReader>> readerEntrySet = fileMap.getReaderMap().entrySet();
        int listSize = readerEntrySet.size();
        int runningThreads = listSize < maxThreads ? listSize : maxThreads;
        String info = "read files" + (processor == null ? "" : " and " + processor.getProcessName());
        System.out.println(info + " concurrently running with " + runningThreads + " threads ...");
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable);
            thread.setUncaughtExceptionHandler((t, e) -> System.out.println(t.getName() + "\t" + e.getMessage()));
            return thread;
        };
        ExecutorService executorPool = Executors.newFixedThreadPool(runningThreads, threadFactory);
        int i = 0;
        for (Entry<String, BufferedReader> readerEntry : readerEntrySet) {
            ILineProcess lineProcessor = processor == null ? null : i == 0 ? processor : processor.clone();
            executorPool.execute(() -> traverseByReader(readerEntry.getKey(), readerEntry.getValue(), lineProcessor));
            i++;
        }
        executorPool.shutdown();
        ExecutorsUtils.waitForShutdown(executorPool, info);
        fileMap.closeReader();
    }
}
