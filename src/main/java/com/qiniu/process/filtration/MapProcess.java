package com.qiniu.process.filtration;

import com.qiniu.common.QiniuException;
import com.qiniu.persistence.FileSaveMapper;
import com.qiniu.convert.MapToString;
import com.qiniu.interfaces.ILineFilter;
import com.qiniu.interfaces.ILineProcess;
import com.qiniu.interfaces.ITypeConvert;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapProcess implements ILineProcess<Map<String, String>>, Cloneable {

    private String processName;
    private ILineFilter<Map<String, String>> filter;
    private ILineProcess<Map<String, String>> nextProcessor;
    private String savePath;
    private String saveFormat;
    private String saveSeparator;
    private List<String> rmFields;
    private int saveIndex;
    private FileSaveMapper fileSaveMapper;
    private ITypeConvert<Map<String, String>, String> typeConverter;

    public MapProcess(BaseFieldsFilter filter, SeniorChecker checker, String savePath,
                      String saveFormat, String saveSeparator, List<String> rmFields, int saveIndex)
            throws Exception {
        this.processName = "filter";
        this.filter = newFilter(filter, checker);
        this.savePath = savePath;
        this.saveFormat = saveFormat;
        this.saveSeparator = saveSeparator;
        this.rmFields = rmFields;
        this.saveIndex = saveIndex;
        this.fileSaveMapper = new FileSaveMapper(savePath, processName, String.valueOf(saveIndex));
        this.typeConverter = new MapToString(this.saveFormat, this.saveSeparator, rmFields);
    }

    public MapProcess(BaseFieldsFilter filter, SeniorChecker checker, String savePath, String saveFormat,
                      String saveSeparator, List<String> rmFields) throws Exception {
        this(filter, checker, savePath, saveFormat, saveSeparator, rmFields, 0);
    }

    private ILineFilter<Map<String, String>> newFilter(BaseFieldsFilter filter, SeniorChecker checker)
            throws NoSuchMethodException {
        List<Method> filterMethods = new ArrayList<Method>() {{
            if (filter.checkKeyPrefix()) add(filter.getClass().getMethod("filterKeyPrefix", Map.class));
            if (filter.checkKeySuffix()) add(filter.getClass().getMethod("filterKeySuffix", Map.class));
            if (filter.checkKeyInner()) add(filter.getClass().getMethod("filterKeyInner", Map.class));
            if (filter.checkKeyRegex()) add(filter.getClass().getMethod("filterKeyRegex", Map.class));
            if (filter.checkPutTime()) add(filter.getClass().getMethod("filterPutTime", Map.class));
            if (filter.checkMimeType()) add(filter.getClass().getMethod("filterMimeType", Map.class));
            if (filter.checkType()) add(filter.getClass().getMethod("filterType", Map.class));
            if (filter.checkStatus()) add(filter.getClass().getMethod("filterStatus", Map.class));
            if (filter.checkAntiKeyPrefix()) add(filter.getClass().getMethod("filterAntiKeyPrefix", Map.class));
            if (filter.checkAntiKeySuffix()) add(filter.getClass().getMethod("filterAntiKeySuffix", Map.class));
            if (filter.checkAntiKeyInner()) add(filter.getClass().getMethod("filterAntiKeyInner", Map.class));
            if (filter.checkAntiKeyRegex()) add(filter.getClass().getMethod("filterAntiKeyRegex", Map.class));
            if (filter.checkAntiMimeType()) add(filter.getClass().getMethod("filterAntiMimeType", Map.class));
        }};
        List<Method> checkMethods = new ArrayList<Method>() {{
            if ("ext-mime".equals(checker.getCheckName()))
                add(checker.getClass().getMethod("checkMimeType", Map.class));
        }};

        return line -> {
            boolean result;
            for (Method method : filterMethods) {
                result = (boolean) method.invoke(filter, line);
                if (!result) return false;
            }
            for (Method method : checkMethods) {
                result = (boolean) method.invoke(checker, line);
                if (!result) return false;
            }
            return true;
        };
    }

    public String getProcessName() {
        return this.processName;
    }

    public void setNextProcessor(ILineProcess<Map<String, String>> nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    public MapProcess clone() throws CloneNotSupportedException {
        MapProcess mapProcess = (MapProcess)super.clone();
        try {
            mapProcess.fileSaveMapper = new FileSaveMapper(savePath, processName, String.valueOf(++saveIndex));
            mapProcess.typeConverter = new MapToString(saveFormat, saveSeparator, rmFields);
            if (nextProcessor != null) {
                mapProcess.nextProcessor = nextProcessor.clone();
            }
        } catch (IOException e) {
            throw new CloneNotSupportedException(e.getMessage() + ", init writer failed.");
        }
        return mapProcess;
    }

    public void processLine(List<Map<String, String>> list) throws IOException {
        if (list == null || list.size() == 0) return;
        List<Map<String, String>> filterList = new ArrayList<>();
        for (Map<String, String> line : list) {
            try {
                if (filter.doFilter(line)) filterList.add(line);
            } catch (Exception e) {
                throw new QiniuException(e, e.getMessage());
            }
        }
        // 默认在不进行进一步处理的情况下直接保存结果，如果需要进一步处理则不保存过滤的结果。
        if (nextProcessor == null) {
            List<String> writeList = typeConverter.convertToVList(filterList);
            if (writeList.size() > 0) fileSaveMapper.writeSuccess(String.join("\n", writeList), false);
            if (typeConverter.errorSize() > 0)
                fileSaveMapper.writeError(String.join("\n", typeConverter.consumeErrors()), false);
        } else {
            nextProcessor.processLine(filterList);
        }
    }

    public void closeResource() {
        fileSaveMapper.closeWriters();
        if (nextProcessor != null) nextProcessor.closeResource();
    }
}
