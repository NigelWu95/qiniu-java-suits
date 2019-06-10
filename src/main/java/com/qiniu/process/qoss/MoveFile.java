package com.qiniu.process.qoss;

import com.qiniu.process.Base;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.BucketManager.*;
import com.qiniu.storage.Configuration;
import com.qiniu.util.Auth;
import com.qiniu.util.FileUtils;
import com.qiniu.util.HttpRespUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class MoveFile extends Base<Map<String, String>> {

    private String toBucket;
    private String toKeyIndex;
    private String addPrefix;
    private String rmPrefix;
    private BatchOperations batchOperations;
    private Configuration configuration;
    private BucketManager bucketManager;

    public MoveFile(String accessKey, String secretKey, Configuration configuration, String bucket, String toBucket,
                    String toKeyIndex, String addPrefix, boolean forceIfOnlyPrefix, String rmPrefix) throws IOException {
        // 目标 bucket 为空时规定为 rename 操作
        super(toBucket == null || "".equals(toBucket) ? "rename" : "move", accessKey, secretKey, bucket);
        set(configuration, toBucket, toKeyIndex, addPrefix, forceIfOnlyPrefix, rmPrefix);
        this.bucketManager = new BucketManager(Auth.create(accessKey, secretKey), configuration.clone());
    }

    public MoveFile(String accessKey, String secretKey, Configuration configuration, String bucket, String toBucket,
                    String toKeyIndex, String addPrefix, boolean forceIfOnlyPrefix, String rmPrefix, String savePath,
                    int saveIndex) throws IOException {
        // 目标 bucket 为空时规定为 rename 操作
        super(toBucket == null || "".equals(toBucket) ? "rename" : "move", accessKey, secretKey, bucket,
                savePath, saveIndex);
        set(configuration, toBucket, toKeyIndex, addPrefix, forceIfOnlyPrefix, rmPrefix);
        this.batchSize = 1000;
        this.batchOperations = new BatchOperations();
        this.bucketManager = new BucketManager(Auth.create(accessKey, secretKey), configuration.clone());
    }

    public MoveFile(String accessKey, String secretKey, Configuration configuration, String bucket, String toBucket,
                    String toKeyIndex, String keyPrefix, boolean forceIfOnlyPrefix, String rmPrefix, String savePath)
            throws IOException {
        this(accessKey, secretKey, configuration, bucket, toBucket, toKeyIndex, keyPrefix, forceIfOnlyPrefix, rmPrefix,
                savePath, 0);
    }

    private void set(Configuration configuration, String toBucket, String toKeyIndex, String addPrefix,
                     boolean forceIfOnlyPrefix, String rmPrefix) throws IOException {
        this.configuration = configuration;
        this.toBucket = toBucket;
        if (toKeyIndex == null || "".equals(toKeyIndex)) {
            this.toKeyIndex = "key";
            if (toBucket == null || "".equals(toBucket)) {
                // rename 操作时未设置 new-key 的条件判断
                if (forceIfOnlyPrefix) {
                    if (addPrefix == null || "".equals(addPrefix))
                        throw new IOException("although prefix-force is true, but the add-prefix is empty.");
                } else {
                    throw new IOException("there is no to-key index, if you only want to add prefix for renaming, " +
                            "please set the \"prefix-force\" as true.");
                }
            }
        } else {
            this.toKeyIndex = toKeyIndex;
        }
        this.addPrefix = addPrefix == null ? "" : addPrefix;
        this.rmPrefix = rmPrefix == null ? "" : rmPrefix;
    }

    public void updateToBucket(String toBucket) {
        this.toBucket = toBucket;
    }

    public void updateToKeyIndex(String toKeyIndex) {
        this.toKeyIndex = toKeyIndex;
    }

    public void updateAddPrefix(String addPrefix) {
        this.addPrefix = addPrefix;
    }

    public void updateRmPrefix(String rmPrefix) {
        this.rmPrefix = rmPrefix;
    }

    public MoveFile clone() throws CloneNotSupportedException {
        MoveFile moveFile = (MoveFile)super.clone();
        moveFile.bucketManager = new BucketManager(Auth.create(authKey1, authKey2), configuration.clone());
        if (batchSize > 1) moveFile.batchOperations = new BatchOperations();
        return moveFile;
    }

    @Override
    public String resultInfo(Map<String, String> line) {
        return line.get("key") + "\t" + line.get("to-key");
    }

    @Override
    public boolean validCheck(Map<String, String> line) {
        if (line.get("key") == null) return false;
        try {
            String toKey = FileUtils.rmPrefix(rmPrefix, line.get(toKeyIndex));
            line.put("to-key", addPrefix + toKey);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    synchronized public String batchResult(List<Map<String, String>> lineList) throws IOException {
        batchOperations.clearOps();
        lineList.forEach(line -> {
            if (toBucket == null || "".equals(toBucket)) {
                batchOperations.addRenameOp(bucket, line.get("key"), line.get("to-key"));
            } else {
                batchOperations.addMoveOp(bucket, line.get("key"), toBucket, line.get("to-key"));
            }
        });
        return HttpRespUtils.getResult(bucketManager.batch(batchOperations));
    }

    @Override
    public String singleResult(Map<String, String> line) throws IOException {
        String key = line.get("key");
        String toKey = line.get("to-key");
        if (toBucket == null || "".equals(toBucket)) {
            return key + "\t" + toKey + "\t" + HttpRespUtils.getResult(bucketManager.rename(bucket, key, toKey));
        } else {
            return key + "\t" + toKey + "\t" + HttpRespUtils.getResult(bucketManager.move(bucket, key, toBucket, toKey));
        }
    }
}
