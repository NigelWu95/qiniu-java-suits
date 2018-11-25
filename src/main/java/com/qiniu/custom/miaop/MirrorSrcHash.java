package com.qiniu.custom.miaop;

import com.qiniu.common.FileMap;
import com.qiniu.common.QiniuException;
import com.qiniu.service.interfaces.IQossProcess;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.util.HttpResponseUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MirrorSrcHash implements IQossProcess, Cloneable {

    private String domain;
    private OkHttpClient httpClient = new OkHttpClient();
    private String processName;
    private int retryCount = 3;
    protected String resultFileDir;
    private FileMap fileMap;

    private void initBaseParams(String domain) {
        this.processName = "hash";
        this.domain = domain;
    }

    public MirrorSrcHash(String domain, String resultFileDir) {
        initBaseParams(domain);
        this.resultFileDir = resultFileDir;
        this.httpClient = new OkHttpClient();
        this.fileMap = new FileMap();
    }

    public MirrorSrcHash(String domain, String resultFileDir, int resultFileIndex) throws IOException {
        this(domain, resultFileDir);
        this.fileMap.initWriter(resultFileDir, processName, resultFileIndex);
    }

    public MirrorSrcHash getNewInstance(int resultFileIndex) throws CloneNotSupportedException {
        MirrorSrcHash mirrorSrcHash = (MirrorSrcHash)super.clone();
        mirrorSrcHash.httpClient = new OkHttpClient();
        mirrorSrcHash.fileMap = new FileMap();
        try {
            mirrorSrcHash.fileMap.initWriter(resultFileDir, processName, resultFileIndex);
        } catch (IOException e) {
            throw new CloneNotSupportedException("init writer failed.");
        }
        return mirrorSrcHash;
    }

    public void setBatch(boolean batch) {}

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getProcessName() {
        return this.processName;
    }

    public String getInfo() {
        return domain;
    }

    private String getMd5(String url) throws QiniuException {

        Request.Builder requestBuilder = new Request.Builder().head().url(url);
        okhttp3.Response res;
        try {
            res = httpClient.newCall(requestBuilder.build()).execute();
        } catch (IOException e) {
            throw new QiniuException(e);
        }
        String md5 = res.header("Content-MD5");
        res.close();
        return md5;
    }

    public String singleWithRetry(FileInfo fileInfo, int retryCount) throws QiniuException {

        String result = "";
        try {
            result = getMd5("http://" + domain + "/" + fileInfo.key);
        } catch (QiniuException e1) {
            HttpResponseUtils.checkRetryCount(e1, retryCount);
            while (retryCount > 0) {
                try {
                    result = getMd5("http://" + domain + "/" + fileInfo.key);
                    retryCount = 0;
                } catch (QiniuException e2) {
                    retryCount = HttpResponseUtils.getNextRetryCount(e2, retryCount);
                }
            }
        }
        return result;
    }

    public void processFile(List<FileInfo> fileInfoList) throws QiniuException {

        fileInfoList = fileInfoList == null ? null : fileInfoList.parallelStream()
                .filter(Objects::nonNull).collect(Collectors.toList());
        if (fileInfoList == null || fileInfoList.size() == 0) return;
        List<String> resultList = new ArrayList<>();
        for (FileInfo fileInfo : fileInfoList) {
            try {
                String result = singleWithRetry(fileInfo, retryCount);
                if (result != null && !"".equals(result)) resultList.add(fileInfo.key + "\t" + result);
                else throw new QiniuException(null, "empty hash");
            } catch (QiniuException e) {
                HttpResponseUtils.processException(e, fileMap, processName, getInfo() + "\t" + fileInfo.key);
            }
        }
        if (resultList.size() > 0) fileMap.writeSuccess(String.join("\n", resultList));
    }

    public void closeResource() {
        fileMap.closeWriter();
    }
}
