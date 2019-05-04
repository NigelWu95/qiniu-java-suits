package com.qiniu.convert;

import com.aliyun.oss.model.OSSObjectSummary;
import com.qiniu.interfaces.IStringFormat;
import com.qiniu.util.LineUtils;

import java.io.IOException;
import java.util.List;

public class OSSObjToString extends Converter<OSSObjectSummary, String> {

    private IStringFormat<OSSObjectSummary> stringFormatter;

    public OSSObjToString(String format, String separator, List<String> rmFields) throws IOException {
        // 将 file info 的字段逐一进行获取是为了控制输出字段的顺序
        if ("json".equals(format)) {
            stringFormatter = line -> LineUtils.toFormatString(line, rmFields);
        } else if ("csv".equals(format)) {
            stringFormatter = line -> LineUtils.toFormatString(line, ",", rmFields);
        } else if ("tab".equals(format)) {
            stringFormatter = line -> LineUtils.toFormatString(line, separator, rmFields);
        } else {
            throw new IOException("please check your format for map to string.");
        }
    }

    @Override
    public String convertToV(OSSObjectSummary line) throws IOException {
        return stringFormatter.toFormatString(line);
    }
}
