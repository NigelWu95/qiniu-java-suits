package com.qiniu.datasource;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.COSObjectSummary;
import com.qiniu.common.SuitsException;
import com.qiniu.convert.Converter;
import com.qiniu.convert.JsonObjectPair;
import com.qiniu.convert.StringBuilderPair;
import com.qiniu.convert.StringMapPair;
import com.qiniu.interfaces.IStorageLister;
import com.qiniu.interfaces.IStringFormat;
import com.qiniu.interfaces.ITypeConvert;
import com.qiniu.persistence.FileSaveMapper;
import com.qiniu.interfaces.IResultOutput;
import com.qiniu.util.CloudApiUtils;
import com.qiniu.util.ConvertingUtils;

import java.io.IOException;
import java.util.*;

public class TenCosContainer extends CloudStorageContainer<COSObjectSummary, Map<String, String>> {

//    private String secretId;
//    private String secretKey;
    private COSCredentials credentials;
    private ClientConfig clientConfig;

    public TenCosContainer(String secretId, String secretKey, ClientConfig clientConfig, String bucket,
                           Map<String, Map<String, String>> prefixesMap, List<String> antiPrefixes, boolean prefixLeft,
                           boolean prefixRight, Map<String, String> indexMap, List<String> fields, int unitLen,
                           int threads) throws IOException {
        super(bucket, prefixesMap, antiPrefixes, prefixLeft, prefixRight, indexMap, fields, unitLen, threads);
//        this.secretId = secretId;
//        this.secretKey = secretKey;
        this.credentials = new BasicCOSCredentials(secretId, secretKey);
        this.clientConfig = clientConfig;
        TenLister tenLister = new TenLister(new COSClient(credentials, clientConfig),
                bucket, null, null, null, 1);
        tenLister.close();
        tenLister = null;
        COSObjectSummary test = new COSObjectSummary();
        test.setKey("test");
        ConvertingUtils.toPair(test, indexMap, new StringMapPair());
    }

    @Override
    public String getSourceName() {
        return "tencent";
    }

    @Override
    protected ITypeConvert<COSObjectSummary, Map<String, String>> getNewConverter() {
        return new Converter<COSObjectSummary, Map<String, String>>() {
            @Override
            public Map<String, String> convertToV(COSObjectSummary line) throws IOException {
                return ConvertingUtils.toPair(line, indexMap, new StringMapPair());
            }
        };
    }

    @Override
    protected ITypeConvert<COSObjectSummary, String> getNewStringConverter() {
        IStringFormat<COSObjectSummary> stringFormatter;
        if ("json".equals(saveFormat)) {
            stringFormatter = line -> ConvertingUtils.toPair(line, fields, new JsonObjectPair()).toString();
        } else if ("yaml".equals(saveFormat)) {
            stringFormatter = line -> ConvertingUtils.toStringWithIndent(line, fields);
        } else {
            stringFormatter = line -> ConvertingUtils.toPair(line, fields, new StringBuilderPair(saveSeparator));
        }
        return new Converter<COSObjectSummary, String>() {
            @Override
            public String convertToV(COSObjectSummary line) throws IOException {
                return stringFormatter.toFormatString(line);
            }
        };
    }

    @Override
    protected IResultOutput getNewResultSaver(String order) throws IOException {
        return order != null ? new FileSaveMapper(savePath, getSourceName(), order) : new FileSaveMapper(savePath);
    }

    @Override
    protected IStorageLister<COSObjectSummary> getLister(String prefix, String marker, String start, String end, int unitLen) throws SuitsException {
        if (marker == null || "".equals(marker)) marker = CloudApiUtils.getTenCosMarker(start);
        return new TenLister(new COSClient(credentials, clientConfig), bucket, prefix, marker, end, unitLen);
    }
}
