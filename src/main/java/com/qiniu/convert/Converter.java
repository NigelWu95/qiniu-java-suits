package com.qiniu.convert;

import com.qiniu.interfaces.ITypeConvert;
import com.qiniu.util.JsonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class Converter<E, T> implements ITypeConvert<E, T> {

    private List<String> errorList = new ArrayList<>();

    public abstract T convertToV(E line) throws IOException;

    @Override
    public List<T> convertToVList(List<E> lineList) {
        if (lineList == null) return new ArrayList<>();
        List<T> mapList = new ArrayList<>(lineList.size());
        for (E line : lineList) {
            try {
                mapList.add(convertToV(line));
            } catch (Exception e) {
                if (line instanceof String) {
                    errorList.add(String.join("\t", String.valueOf(line), "convert error", e.getMessage()));
                } else {
                    errorList.add(String.join("\t", JsonUtils.toJson(line), "convert error", e.getMessage()));
                }
            }
        }
        return mapList;
    }

    @Override
    public int errorSize() {
        return errorList.size();
    }

    @Override
    public String errorLines() {
        try {
            return String.join("\n", errorList);
        } finally {
            errorList.clear();
        }
    }
}
