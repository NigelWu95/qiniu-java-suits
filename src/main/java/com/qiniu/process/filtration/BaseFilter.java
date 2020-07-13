package com.qiniu.process.filtration;

import com.qiniu.util.ConvertingUtils;
import com.qiniu.util.DatetimeUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public abstract class BaseFilter<T> {

    private List<String> keyPrefix;
    private List<String> keySuffix;
    private List<String> keyInner;
    private List<String> keyRegex;
    private LocalDateTime datetimeMin;
    private LocalDateTime datetimeMax;
    private List<String> mimeType;
    private List<String> typeList;
    private String status;
    private List<String> antiKeyPrefix;
    private List<String> antiKeySuffix;
    private List<String> antiKeyInner;
    private List<String> antiKeyRegex;
    private List<String> antiMimeType;

    public BaseFilter(List<String> keyPrefix, List<String> keySuffix, List<String> keyInner, List<String> keyRegex,
                      List<String> antiKeyPrefix, List<String> antiKeySuffix, List<String> antiKeyInner,
                      List<String> antiKeyRegex, List<String> mimeType, List<String> antiMimeType, LocalDateTime putTimeMin,
                      LocalDateTime putTimeMax, List<String> typeList, String status) throws IOException {
        this.keyPrefix = keyPrefix;
        this.keySuffix = keySuffix;
        this.keyInner = keyInner;
        this.keyRegex = keyRegex;
        this.antiKeyPrefix = antiKeyPrefix;
        this.antiKeySuffix = antiKeySuffix;
        this.antiKeyInner = antiKeyInner;
        this.antiKeyRegex = antiKeyRegex;
        this.mimeType = mimeType;
        this.antiMimeType = antiMimeType;
        this.datetimeMin = putTimeMin;
        this.datetimeMax = putTimeMax;
        this.typeList = typeList;
        this.status = status == null ? "" : status;
        if (!checkKeyCon() && !checkMimeTypeCon() && !checkDatetimeCon() && !checkTypeCon() && !checkStatusCon())
            throw new IOException("all conditions are invalid.");
    }

    private boolean checkList(List<String> list) {
        return list != null && list.size() != 0;
    }

    public boolean checkKeyCon() {
        return checkList(keyPrefix) || checkList(keySuffix) || checkList(keyInner) || checkList(keyRegex) ||
                checkList(antiKeyPrefix) || checkList(antiKeySuffix) || checkList(antiKeyInner) || checkList(antiKeyRegex);
    }

    public boolean checkMimeTypeCon() {
        return checkList(mimeType) || checkList(antiMimeType);
    }

    public boolean checkDatetimeCon() {
        return datetimeMin != null && datetimeMax != null && datetimeMax.compareTo(datetimeMin) > 0;
    }

    public boolean checkTypeCon() {
        return checkList(typeList);
    }

    public boolean checkStatusCon() {
        return status != null && !"".equals(status);
    }

    public boolean filterKey(T item) {
        try {
            if (item == null) return false;
            String key = valueFrom(item, "key");
            if (checkList(keyPrefix) && keyPrefix.stream().noneMatch(key::startsWith)) return false;
            if (checkList(keySuffix) && keySuffix.stream().noneMatch(key::endsWith)) return false;
            if (checkList(keyInner) && keyInner.stream().noneMatch(key::contains)) return false;
            if (checkList(keyRegex) && keyRegex.stream().noneMatch(key::matches)) return false;
            if (checkList(antiKeyPrefix) && antiKeyPrefix.stream().anyMatch(key::startsWith)) return false;
            if (checkList(antiKeySuffix) && antiKeySuffix.stream().anyMatch(key::endsWith)) return false;
            if (checkList(antiKeyInner) && antiKeyInner.stream().anyMatch(key::contains)) return false;
            return !checkList(antiKeyRegex) || antiKeyRegex.stream().noneMatch(key::matches);
        } catch (Exception e) {
            return true;
        }
    }

    public boolean filterMimeType(T item) {
        try {
            if (item == null) return false;
            String mType = valueFrom(item, ConvertingUtils.defaultMimeField);
            if (mType == null) mType = valueFrom(item, "mimeType");
            if (mType == null) mType = valueFrom(item, "contentType");
            if (checkList(mimeType) && mimeType.stream().noneMatch(mType::contains)) return false;
            return !checkList(antiMimeType) || antiMimeType.stream().noneMatch(mType::contains);
        } catch (Exception e) {
            return true;
        }
    }

    public boolean filterDatetime(T item) {
        try {
            if (item == null) return false;
            LocalDateTime localDateTime;
            String datetime = valueFrom(item, ConvertingUtils.defaultDatetimeField);
            if (datetime == null) {
                String timestamp = valueFrom(item, "timestamp");
                if (timestamp == null) {
                    timestamp = valueFrom(item, "putTime");
                }
                if (timestamp == null) {
                    datetime = valueFrom(item, "lastModified");
                } else {
                    long accuracy = (long) Math.pow(10, (timestamp.length() - 10));
                    datetime = DatetimeUtils.stringOf(Long.parseLong(timestamp), accuracy);
                }
            }
            localDateTime = LocalDateTime.parse(datetime);
            return localDateTime.compareTo(datetimeMax) <= 0 && localDateTime.compareTo(datetimeMin) >= 0;
        } catch (Exception e) {
            return true;
        }
    }

    public boolean filterType(T item) {
        try {
            if (item == null) return false;
            String type = valueFrom(item, ConvertingUtils.defaultTypeField);
            for (String s : typeList) if (type.equals(s)) return true;
            return false;
        } catch (NullPointerException e) {
            return true;
        }
    }

    public boolean filterStatus(T item) {
        try {
            if (item == null) return false;
            return valueFrom(item, ConvertingUtils.defaultStatusField).equals(status);
        } catch (NullPointerException e) {
            return true;
        }
    }

    protected abstract String valueFrom(T item, String key);
}
