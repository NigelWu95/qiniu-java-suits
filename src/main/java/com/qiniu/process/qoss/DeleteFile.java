package com.qiniu.process.qoss;

import com.qiniu.common.QiniuException;
import com.qiniu.process.Base;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.BucketManager.*;
import com.qiniu.storage.Configuration;
import com.qiniu.util.Auth;
import com.qiniu.util.HttpResponseUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class DeleteFile extends Base<Map<String, String>> {

    private BatchOperations batchOperations;
    private BucketManager bucketManager;

    public DeleteFile(String accessKey, String secretKey, Configuration configuration, String bucket, String savePath,
                      int saveIndex) throws IOException {
        super("delete", accessKey, secretKey, configuration, bucket, savePath, saveIndex);
        this.batchSize = 1000;
        this.batchOperations = new BatchOperations();
        this.bucketManager = new BucketManager(Auth.create(accessKey, secretKey), configuration.clone());
    }

    public void updateDelete(String bucket) {
        this.bucket = bucket;
    }

    public DeleteFile(String accessKey, String secretKey, Configuration configuration, String bucket, String savePath)
            throws IOException {
        this(accessKey, secretKey, configuration, bucket, savePath, 0);
    }

    public DeleteFile clone() throws CloneNotSupportedException {
        DeleteFile deleteFile = (DeleteFile)super.clone();
        deleteFile.bucketManager = new BucketManager(Auth.create(accessKey, secretKey), configuration.clone());
        if (batchSize > 1) deleteFile.batchOperations = new BatchOperations();
        return deleteFile;
    }

    @Override
    protected String resultInfo(Map<String, String> line) {
        return line.get("key");
    }

    @Override
    protected boolean validCheck(Map<String, String> line) {
        return line.get("key") == null;
    }

    @Override
    synchronized protected String batchResult(List<Map<String, String>> lineList) throws QiniuException {
        batchOperations.clearOps();
        lineList.forEach(line -> batchOperations.addDeleteOp(bucket, line.get("key")));
        return HttpResponseUtils.getResult(bucketManager.batch(batchOperations));
    }

    @Override
    protected String singleResult(Map<String, String> line) throws QiniuException {
        return HttpResponseUtils.getResult(bucketManager.delete(bucket, line.get("key")));
    }
}
