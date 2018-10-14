package com.qiniu.service.oss;

import com.qiniu.common.QiniuAuth;
import com.qiniu.common.QiniuBucketManager;
import com.qiniu.common.QiniuBucketManager.*;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.util.HttpResponseUtils;

import java.util.ArrayList;

public abstract class OperationBase {

    protected QiniuAuth auth;
    protected Configuration configuration;
    protected QiniuBucketManager bucketManager;
    protected volatile BatchOperations batchOperations;

    public OperationBase(QiniuAuth auth, Configuration configuration) {
        this.auth = auth;
        this.configuration = configuration;
        this.bucketManager = new QiniuBucketManager(auth, configuration);
        this.batchOperations = new BatchOperations();
    }

    public OperationBase clone() throws CloneNotSupportedException {
        OperationBase operationBase = (OperationBase)super.clone();
        operationBase.bucketManager = new QiniuBucketManager(auth, configuration);
        operationBase.batchOperations = new BatchOperations();
        return operationBase;
    }

    synchronized public ArrayList<String> getBatchOps() {
        return batchOperations.getOps();
    }

    synchronized public Response batchWithRetry(int retryCount) throws QiniuException {
        Response response = null;

        try {
            response = bucketManager.batch(batchOperations);
        } catch (QiniuException e1) {
            HttpResponseUtils.checkRetryCount(e1, retryCount);
            while (retryCount > 0) {
                try {
                    System.out.println("batch operations failed: " + e1.error() + ", last "
                            + retryCount + " times retry...");
                    response = bucketManager.batch(batchOperations);
                    retryCount = 0;
                } catch (QiniuException e2) {
                    retryCount = HttpResponseUtils.getNextRetryCount(e2, retryCount);
                }
            }
        }

        return response;
    }

    synchronized public String batchCheckRun(int retryCount) throws QiniuException {
        if (!batchOperations.getOps().isEmpty()) {
            Response response = batchWithRetry(retryCount);
            String responseBody = response.bodyString();
            int statusCode = response.statusCode;
            String reqId = response.reqId;
            response.close();
            batchOperations.clearOps();

            return statusCode + "\t" + reqId + "\t" + responseBody;
        } else {
            return "";
        }
    }

    public void closeBucketManager() {
        if (bucketManager != null)
            bucketManager.closeResponse();
    }
}