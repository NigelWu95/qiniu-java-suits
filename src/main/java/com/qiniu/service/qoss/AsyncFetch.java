package com.qiniu.service.qoss;

import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager.*;
import com.qiniu.service.interfaces.ILineProcess;
import com.qiniu.storage.Configuration;
import com.qiniu.util.Auth;
import com.qiniu.util.HttpResponseUtils;
import com.qiniu.util.RequestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AsyncFetch extends OperationBase implements ILineProcess<Map<String, String>>, Cloneable {

    private String domain;
    private String protocol;
    private String urlIndex;
    private String md5Index;
    private Auth srcAuth;
    private boolean keepKey;
    private String keyPrefix;
//    private M3U8Manager m3u8Manager;
    private boolean hasCustomArgs;
    private String host;
    private String callbackUrl;
    private String callbackBody;
    private String callbackBodyType;
    private String callbackHost;
    private int fileType;
    private boolean ignoreSameKey;

    public AsyncFetch(Auth auth, Configuration configuration, String bucket, String domain, String protocol, Auth srcAuth,
                      boolean keepKey, String keyPrefix, String urlIndex, String resultPath, int resultIndex)
            throws IOException {
        super(auth, configuration, bucket, "asyncfetch", resultPath, resultIndex);
        setBatch(false);
        if (urlIndex== null || "".equals(urlIndex)) {
            this.urlIndex = null;
            if (domain == null || "".equals(domain)) throw new IOException("please set one of domain and urlIndex.");
            else {
                RequestUtils.checkHost(domain);
                this.domain = domain;
                this.protocol = protocol == null || !protocol.matches("(http|https)") ? "http" : protocol;
            }
        } else this.urlIndex = urlIndex;
        this.srcAuth = srcAuth;
        this.keepKey = keepKey;
        this.keyPrefix = keyPrefix;
//        this.m3u8Manager = new M3U8Manager();
    }

    public AsyncFetch(Auth auth, Configuration configuration, String bucket, String domain, String protocol, Auth srcAuth,
                      boolean keepKey, String keyPrefix, String urlIndex, String resultPath)
            throws IOException {
        this(auth, configuration, bucket, domain, protocol, srcAuth, keepKey, keyPrefix, urlIndex, resultPath, 0);
    }

    public void setFetchArgs(String md5Index, String host, String callbackUrl, String callbackBody, String callbackBodyType,
                             String callbackHost, int fileType, boolean ignoreSameKey) {
        this.md5Index = md5Index == null ? "" : md5Index;
        this.host = host;
        this.callbackUrl = callbackUrl;
        this.callbackBody = callbackBody;
        this.callbackBodyType = callbackBodyType;
        this.callbackHost = callbackHost;
        this.fileType = fileType;
        this.ignoreSameKey = ignoreSameKey;
        this.hasCustomArgs = true;
    }

    private Response fetch(String url, String key, String md5, String etag) throws QiniuException {
        if (srcAuth != null) url = srcAuth.privateDownloadUrl(url);
        return hasCustomArgs ?
                bucketManager.asynFetch(url, bucket, key, md5, etag, callbackUrl, callbackBody, callbackBodyType,
                        callbackHost, String.valueOf(fileType)) :
                bucketManager.asynFetch(url, bucket, key);
    }

    protected String processLine(Map<String, String> line) throws QiniuException {
        String url;
        String key;
        if (urlIndex != null) {
            url = line.get(urlIndex);
            key = url.split("(https?://[^\\s/]+\\.[^\\s/.]{1,3}/)|(\\?.+)")[1];
        } else  {
            url = protocol + "://" + domain + "/" + line.get("key");
            key = line.get("key");
        }
        Response response = fetch(url, keepKey ? keyPrefix + key : null, line.get(md5Index), line.get("hash"));
        return response.statusCode + "\t" + HttpResponseUtils.getResult(response);
    }

    synchronized protected BatchOperations getOperations(List<Map<String, String>> fileInfoList){
        return new BatchOperations();
    }
}
