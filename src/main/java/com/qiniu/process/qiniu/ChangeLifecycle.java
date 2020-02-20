package com.qiniu.process.qiniu;

import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.process.Base;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.BucketManager.*;
import com.qiniu.storage.Configuration;
import com.qiniu.util.Auth;
import com.qiniu.util.HttpRespUtils;
import com.qiniu.util.CloudApiUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChangeLifecycle extends Base<Map<String, String>> {

    private int days;
    private BatchOperations batchOperations;
    private List<Map<String, String>> lines;
    private Configuration configuration;
    private BucketManager bucketManager;

    public ChangeLifecycle(String accessKey, String secretKey, Configuration configuration, String bucket, int days)
            throws IOException{
        super("lifecycle", accessKey, secretKey, bucket);
        this.days = days;
        this.configuration = configuration;
        this.bucketManager = new BucketManager(Auth.create(accessKey, secretKey), configuration);
        CloudApiUtils.checkQiniu(bucketManager, bucket);
    }

    public ChangeLifecycle(String accessKey, String secretKey, Configuration configuration, String bucket, int days,
                           String savePath, int saveIndex) throws IOException {
        super("lifecycle", accessKey, secretKey, bucket, savePath, saveIndex);
        this.days = days;
        this.batchSize = 1000;
        this.batchOperations = new BatchOperations();
        this.lines = new ArrayList<>(1000);
        this.configuration = configuration;
        this.bucketManager = new BucketManager(Auth.create(accessKey, secretKey), configuration);
        CloudApiUtils.checkQiniu(bucketManager, bucket);
    }

    public ChangeLifecycle(String accessKey, String secretKey, Configuration configuration, String bucket, int days,
                           String savePath) throws IOException {
        this(accessKey, secretKey, configuration, bucket, days, savePath, 0);
    }

    @Override
    public ChangeLifecycle clone() throws CloneNotSupportedException {
        ChangeLifecycle changeLifecycle = (ChangeLifecycle)super.clone();
        changeLifecycle.bucketManager = new BucketManager(Auth.create(accessId, secretKey), configuration);
        if (fileSaveMapper != null) {
            changeLifecycle.batchOperations = new BatchOperations();
            changeLifecycle.lines = new ArrayList<>(batchSize);
        }
        return changeLifecycle;
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
                batchOperations.addDeleteAfterDaysOps(bucket, days, key);
            } else {
                fileSaveMapper.writeError("key is not exists or empty in " + map, false);
            }
        }
        return lines;
    }

    @Override
    protected String batchResult(List<Map<String, String>> lineList) throws IOException {
        return HttpRespUtils.getResult(bucketManager.batch(batchOperations));
    }

    @Override
    protected String singleResult(Map<String, String> line) throws IOException {
        String key = line.get("key");
        if (key == null) throw new IOException("key is not exists or empty in " + line);
        Response response = bucketManager.deleteAfterDays(bucket, key, days);
        if (response.statusCode != 200) throw new QiniuException(response);
        response.close();
        return String.join("\t", key, "200");
    }

    @Override
    public void closeResource() {
        super.closeResource();
        batchOperations = null;
        lines = null;
        configuration = null;
        bucketManager = null;
    }
}
