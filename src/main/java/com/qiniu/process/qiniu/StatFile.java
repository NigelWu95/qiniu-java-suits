package com.qiniu.process.qiniu;

import com.google.gson.*;
import com.qiniu.convert.JsonObjectPair;
import com.qiniu.convert.StringBuilderPair;
import com.qiniu.http.Response;
import com.qiniu.interfaces.IStringFormat;
import com.qiniu.process.Base;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.BucketManager.*;
import com.qiniu.storage.Configuration;
import com.qiniu.util.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatFile extends Base<Map<String, String>> {

    private String format;
    private String separator;
    private List<String> rmFields;
    private List<String> statJsonFields;
    private IStringFormat<JsonObject> stringFormatter;
    private BatchOperations batchOperations;
    private List<Map<String, String>> lines;
    private Configuration configuration;
    private BucketManager bucketManager;

    public StatFile(String accessKey, String secretKey, Configuration configuration, String bucket, String format,
                    String separator, List<String> rmFields) throws IOException {
        super("stat", accessKey, secretKey, bucket);
        set(configuration, rmFields, format, separator);
        this.bucketManager = new BucketManager(Auth.create(accessKey, secretKey), configuration);
        CloudApiUtils.checkQiniu(bucketManager, bucket);
    }

    public StatFile(String accessKey, String secretKey, Configuration configuration, String bucket, String savePath,
                    String format, String separator, List<String> rmFields, int saveIndex) throws IOException {
        super("stat", accessKey, secretKey, bucket, savePath, saveIndex);
        set(configuration, rmFields, format, separator);
        this.batchSize = 100;
        this.batchOperations = new BatchOperations();
        this.lines = new ArrayList<>();
        this.bucketManager = new BucketManager(Auth.create(accessKey, secretKey), configuration);
        CloudApiUtils.checkQiniu(bucketManager, bucket);
    }

    public StatFile(String accessKey, String secretKey, Configuration configuration, String bucket, String savePath,
                    String format, String separator, List<String> rmFields) throws IOException {
        this(accessKey, secretKey, configuration, bucket, savePath, format, separator, rmFields, 0);
    }

    private void set(Configuration configuration, List<String> rmFields, String format, String separator) throws IOException {
        this.configuration = configuration;
        this.format = format;
        if ("csv".equals(format) || "tab".equals(format)) {
            this.separator = "csv".equals(format) ? "," : separator;
        } else if (!"json".equals(this.format)) {
            throw new IOException("please check your format for converting result string.");
        }
        stringFormatter = getNewStatJsonFormatter(rmFields);
    }

    private IStringFormat<JsonObject> getNewStatJsonFormatter(List<String> rmFields) {
        IStringFormat<JsonObject> stringFormatter;
        if (statJsonFields == null) statJsonFields = ConvertingUtils.getFields(ConvertingUtils.statFileFields, rmFields);
        if ("json".equals(format)) {
            stringFormatter = line -> ConvertingUtils.toPair(line, statJsonFields, new JsonObjectPair()).toString();
        } else {
            stringFormatter = line -> ConvertingUtils.toPair(line, statJsonFields, new StringBuilderPair(separator));
        }
        this.rmFields = rmFields;
        return stringFormatter;
    }

    @Override
    public StatFile clone() throws CloneNotSupportedException {
        StatFile statFile = (StatFile)super.clone();
        statFile.bucketManager = new BucketManager(Auth.create(accessId, secretKey), configuration);
        statFile.batchOperations = new BatchOperations();
        statFile.lines = new ArrayList<>();
        statFile.stringFormatter = getNewStatJsonFormatter(rmFields);
        return statFile;
    }

    @Override
    protected String resultInfo(Map<String, String> line) {
        return line.get("key");
    }

    @Override
    protected List<Map<String, String>> putBatchOperations(List<Map<String, String>> processList) throws IOException {
        batchOperations.clearOps();
        lines.clear();
        String key;
        for (Map<String, String> map : processList) {
            key = map.get("key");
            if (key != null) {
                lines.add(map);
                batchOperations.addStatOps(bucket, key);
            } else {
                fileSaveMapper.writeError("key is not exists or empty in " + map, false);
            }
        }
        return lines;
    }

    @Override
    public String batchResult(List<Map<String, String>> lineList) throws IOException {
        return HttpRespUtils.getResult(bucketManager.batch(batchOperations));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected List<Map<String, String>> parseBatchResult(List<Map<String, String>> processList, String result) throws Exception {
        if (result == null || "".equals(result)) throw new IOException("not valid json.");
        List<Map<String, String>> retryList = null;
        JsonArray jsonArray = new Gson().fromJson(result, JsonArray.class);
        JsonObject jsonObject;
        JsonObject data;
        for (int j = 0; j < processList.size(); j++) {
            if (j < jsonArray.size()) {
                jsonObject = jsonArray.get(j).getAsJsonObject();
                if (!(jsonObject.get("data") instanceof JsonNull) && jsonObject.get("data") instanceof JsonObject) {
                    data = jsonObject.get("data").getAsJsonObject();
                } else {
                    fileSaveMapper.writeError(String.join("\t",
                            processList.get(j).get("key"), jsonObject.toString()), false);
                    continue;
                }
                switch (HttpRespUtils.checkStatusCode(jsonObject.get("code").getAsInt())) {
                    case 1:
                        data.addProperty("key", processList.get(j).get("key"));
                        fileSaveMapper.writeSuccess(stringFormatter.toFormatString(data), false);
                        break;
                    case 0:
                        if (retryList == null) retryList = new ArrayList<>();
                        retryList.add(processList.get(j)); // 放回重试列表
                        break;
                    case -1:
                        fileSaveMapper.writeError(String.join("\t",
                                processList.get(j).get("key"), jsonObject.toString()), false);
                        break;
                }
            } else {
                fileSaveMapper.writeError(String.join("\t",
                        processList.get(j).get("key"), "empty stat result"), false);
            }
        }
        return retryList;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected String singleResult(Map<String, String> line) throws Exception {
        String key = line.get("key");
        if (key == null) throw new IOException("key is not exists or empty in " + line);
        Response response = bucketManager.statResponse(bucket, key);
        JsonObject statJson = JsonUtils.toJsonObject(response.bodyString());
        statJson.addProperty("key", key);
        response.close();
        return stringFormatter.toFormatString(statJson);
    }

    @Override
    public void closeResource() {
        super.closeResource();
        format = null;
        separator = null;
        stringFormatter = null;
        batchOperations = null;
        lines = null;
        configuration = null;
        bucketManager = null;
    }
}
