package com.qiniu.datasource;

import com.qiniu.interfaces.*;
import com.qiniu.persistence.FileSaveMapper;
import com.qiniu.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static com.qiniu.entry.CommonParams.lineFormats;

public abstract class DatasourceActor {

    protected static final File errorLogFile = new File(String.join(".", LogUtils.getLogPath(LogUtils.QSUITS), LogUtils.ERROR));
    protected static final File infoLogFile = new File(String.join(".", LogUtils.getLogPath(LogUtils.QSUITS), LogUtils.INFO));
    protected static final File procedureLogFile = new File(String.join(".", LogUtils.getLogPath(LogUtils.PROCEDURE), LogUtils.LOG_EXT));
    protected static final Logger rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    protected static final Logger errorLogger = LoggerFactory.getLogger(LogUtils.ERROR);
    protected static final Logger infoLogger = LoggerFactory.getLogger(LogUtils.INFO);
    protected static final Logger procedureLogger = LoggerFactory.getLogger(LogUtils.PROCEDURE);

    protected int unitLen;
    protected int threads;
    protected int retryTimes = 5;
    protected boolean saveTotal;
    protected String savePath;
    protected String saveFormat;
    protected String saveSeparator;
    protected Map<String, String> indexMap;
    protected List<String> rmFields;
    protected List<String> fields;
    protected ExecutorService executorPool; // 线程池
    protected ConcurrentMap<String, IResultOutput> saverMap;
    protected ConcurrentMap<String, ILineProcess> processorMap;
    protected boolean stopped;
    protected int countInterval;
    protected AtomicLong statistics;
    protected ConcurrentMap<String, String> progressMap;
    protected String breakpointFileName;
    protected FileSaveMapper breakpointSaver;

    public DatasourceActor(int unitLen, int threads) throws IOException {
        if (unitLen <= 1) throw new IOException("unitLen must bigger than 1.");
        this.unitLen = unitLen;
        this.threads = threads;
        saverMap = new ConcurrentHashMap<>(threads);
        processorMap = new ConcurrentHashMap<>(threads);
        countInterval = 300;
        statistics = new AtomicLong(0);
        progressMap = new ConcurrentHashMap<>(threads);
    }

    public void setSaveOptions(boolean saveTotal, String savePath, String format, String separator, List<String> rmFields)
            throws IOException {
        this.saveTotal = saveTotal;
        if (this.savePath != null && !"".equals(this.savePath)) {
            breakpointSaver.closeWriters();
            File file = new File(breakpointFileName + ".json");
            boolean success = file.delete();
            if (!success) success = file.delete();
        }
        this.savePath = savePath;
        this.saveFormat = format;
        if (!lineFormats.contains(saveFormat)) throw new IOException("please check your format for map to string.");
        this.saveSeparator = separator;
        this.rmFields = rmFields;
        if (rmFields != null && rmFields.size() > 0) {
            this.fields = ConvertingUtils.getFields(fields, rmFields);
        }
        String path = new File(savePath).getCanonicalPath();
        breakpointSaver = new FileSaveMapper(new File(path).getParent());
        breakpointSaver.setAppend(false);
        breakpointSaver.setFileExt(".json");
        breakpointFileName = path.substring(path.lastIndexOf(FileUtils.pathSeparator) + 1);
        breakpointSaver.addWriter(breakpointFileName);
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes < 1 ? 5 : retryTimes;
    }

    void recordLister(String key, String record) {
        progressMap.put(key, record);
        try { FileUtils.createIfNotExists(procedureLogFile); } catch (IOException ignored) {}
        procedureLogger.info("{}-|-{}", key, record);
    }

    void removeRecordedPrefix(String prefix) {
        procedureLogger.info("{}-|-", prefix);
        progressMap.remove(prefix);
    }

    void refreshRecordAndStatistics() {
        rootLogger.info("finished count: {}.", statistics.get());
        if (procedureLogFile.length() > 536870912) { // 超过 512M 就处理一次
            try {
                breakpointSaver.clear(breakpointFileName);
//                StringBuilder record = new StringBuilder("{\"");
//                progressMap.forEach((key, m) -> record.append(key).append("\":").append(m).append(",\""));
//                record.delete(record.length() - 2, record.length()).append("}");
//                breakpointSaver.writeToKey(breakpointFileName, record.toString().replace("\\", "\\\\"), true);
                breakpointSaver.writeToKey(breakpointFileName, JsonUtils.toJsonWithoutUrlEscape(progressMap), true);
                procedureLogFile.delete();
            } catch (IOException e) {
                errorLogger.info("record breakpoint failed", e);
            }
        }
    }

    void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            int i = 0;
            while (i < millis) i++;
        }
    }

    void endAction() throws IOException {
        ILineProcess processor;
        for (Map.Entry<String, IResultOutput> saverEntry : saverMap.entrySet()) {
            saverEntry.getValue().closeWriters();
            processor = processorMap.get(saverEntry.getKey());
            if (processor != null) processor.cancel();
        }
        String record = "{}";
        if (progressMap.size() > 0) {
//            StringBuilder re = new StringBuilder("{\"");
//            progressMap.forEach((key, m) -> re.append(key).append("\":").append(m).append(",\""));
//            re.delete(re.length() - 2, re.length()).append("}");
//            record = re.toString().replace("\\", "\\\\");
            record = JsonUtils.toJsonWithoutUrlEscape(progressMap);
            breakpointSaver.writeToKey(breakpointFileName, record, true);
            breakpointSaver.closeWriters();
            rootLogger.info("please check the lines breakpoint in {}.json, it can be used for one more time reading " +
                    "remained lines", breakpointFileName);
        } else {
            breakpointSaver.closeWriters();
            File file = new File(breakpointFileName + ".json");
            if (!file.delete()) file.delete();
        }
        procedureLogger.info(record);
    }

    void showdownHook() {
        SignalHandler handler = signal -> {
            rootLogger.info(signal.toString());
            try {
                stopped = true;
                endAction();
            } catch (IOException e) {
                rootLogger.error("showdown error", e);
            }
            System.exit(0);
        };
        try { // 设置 INT 信号 (Ctrl + C 中断执行) 交给指定的信号处理器处理，废掉系统自带的功能
            Signal.handle(new Signal("INT"), handler); } catch (Exception ignored) {}
        try { Signal.handle(new Signal("TERM"), handler); } catch (Exception ignored) {}
        try { Signal.handle(new Signal("USR1"), handler); } catch (Exception ignored) {}
        try { Signal.handle(new Signal("USR2"), handler); } catch (Exception ignored) {}
    }

    protected abstract void export() throws Exception;

    final Object object = new Object();
    LocalDateTime pauseDateTime = LocalDateTime.MAX;

    public void export(LocalDateTime startTime, long pauseDelay, long duration) throws Exception {
        if (startTime != null) {
            Clock clock = Clock.systemDefaultZone();
            LocalDateTime now = LocalDateTime.now(clock);
            if (startTime.minusWeeks(1).isAfter(now)) {
                throw new Exception("startTime is not allowed to exceed next week");
            }
            while (now.isBefore(startTime)) {
                System.out.printf("\r%s", LocalDateTime.now(clock).toString().substring(0, 19));
                sleep(1000);
                now = LocalDateTime.now(clock);
            }
        }
        if (duration <= 0 || pauseDelay < 0) {
            export();
        } else if (duration > 84600 || duration < 1800) {
            throw new Exception("duration can not be bigger than 23.5 hours or smaller than 0.5 hours.");
        } else {
            pauseDateTime = LocalDateTime.now().plusSeconds(pauseDelay);
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    synchronized (object) {
                        object.notifyAll();
                    }
                    pauseDateTime = LocalDateTime.now().plusSeconds(86400 - duration);
//                    pauseDateTime = LocalDateTime.now().plusSeconds(20 - duration);
                }
            }, (pauseDelay + duration) * 1000, 86400000);
//            }, (pauseDelay + duration) * 1000, 20000);
            export();
            timer.cancel();
        }
    }
}
