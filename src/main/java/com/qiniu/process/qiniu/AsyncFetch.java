package com.qiniu.process.qiniu;

import com.qiniu.common.Constants;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Client;
import com.qiniu.http.Response;
import com.qiniu.interfaces.IFileChecker;
import com.qiniu.process.Base;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.util.*;

import java.io.IOException;
import java.util.Map;

public class AsyncFetch extends Base<Map<String, String>> {

    private String protocol;
    private String domain;
    private String urlIndex;
    private String addPrefix;
    private String rmPrefix;
    private boolean hasCustomArgs;
    private String host;
    private String md5Index;
    private String callbackUrl;
    private String callbackBody;
    private String callbackBodyType;
    private String callbackHost;
    private int fileType;
    private boolean ignoreSameKey;
    private Configuration configuration;
    private Auth auth;
    private Client client;
    private static String requestUrl;
//    private BucketManager bucketManager;

    public AsyncFetch(String accessKey, String secretKey, Configuration configuration, String bucket, String protocol,
                      String domain, String urlIndex, String addPrefix, String rmPrefix) throws IOException {
        super("asyncfetch", accessKey, secretKey, bucket);
        this.auth = Auth.create(accessKey, secretKey);
        this.client = new Client(configuration.clone());
        CloudApiUtils.checkQiniu(accessKey, secretKey, configuration, bucket);
//        this.bucketManager = new BucketManager(Auth.create(accessKey, secretKey), configuration);
//        CloudApiUtils.checkQiniu(bucketManager, bucket);
        set(configuration, protocol, domain, urlIndex, addPrefix, rmPrefix);
    }

    public AsyncFetch(String accessKey, String secretKey, Configuration configuration, String bucket, String protocol,
                      String domain, String urlIndex, String addPrefix, String rmPrefix, String savePath, int saveIndex)
            throws IOException {
        super("asyncfetch", accessKey, secretKey, bucket, savePath, saveIndex);
        this.auth = Auth.create(accessKey, secretKey);
        this.client = new Client(configuration.clone());
        CloudApiUtils.checkQiniu(accessKey, secretKey, configuration, bucket);
//        this.bucketManager = new BucketManager(Auth.create(accessKey, secretKey), configuration);
//        CloudApiUtils.checkQiniu(bucketManager, bucket);
        set(configuration, protocol, domain, urlIndex, addPrefix, rmPrefix);
    }

    public AsyncFetch(String accessKey, String secretKey, Configuration configuration, String bucket, String protocol,
                      String domain, String urlIndex, String addPrefix, String rmPrefix, String savePath)
            throws IOException {
        this(accessKey, secretKey, configuration, bucket, protocol, domain, urlIndex, addPrefix, rmPrefix, savePath, 0);
    }

    private void set(Configuration configuration, String protocol, String domain, String urlIndex, String addPrefix,
                     String rmPrefix) throws IOException {
        this.configuration = configuration;
        if (domain == null || "".equals(domain)) {
            if (urlIndex == null || "".equals(urlIndex)) {
                throw new IOException("please set one of domain and url-index.");
            } else {
                this.urlIndex = urlIndex;
            }
        } else {
            this.protocol = protocol == null || !protocol.matches("(http|https)") ? "http" : protocol;
            RequestUtils.lookUpFirstIpFromHost(domain);
            this.domain = domain;
            this.urlIndex = "url";
        }
        this.addPrefix = addPrefix == null ? "" : addPrefix;
        this.rmPrefix = rmPrefix;
        requestUrl = configuration.apiHost(auth.accessKey, bucket) + "/sisyphus/fetch";
    }

    public void setFetchArgs(String host, String md5Index, String callbackUrl, String callbackBody,
                             String callbackBodyType, String callbackHost, int fileType, boolean ignoreSameKey) {
        this.host = host;
        this.md5Index = md5Index;
        this.callbackUrl = callbackUrl;
        this.callbackBody = callbackBody;
        this.callbackBodyType = callbackBodyType;
        this.callbackHost = callbackHost;
        this.fileType = fileType;
        this.ignoreSameKey = ignoreSameKey;
        this.hasCustomArgs = true;
    }

    @Override
    public AsyncFetch clone() throws CloneNotSupportedException {
        AsyncFetch asyncFetch = (AsyncFetch)super.clone();
        asyncFetch.auth = Auth.create(accessId, secretKey);
        asyncFetch.client = new Client(configuration.clone());
//        asyncFetch.bucketManager = new BucketManager(Auth.create(accessId, secretKey), configuration);
        return asyncFetch;
    }

    @Override
    protected IFileChecker fileCheckerInstance() {
        return "stat".equals(checkType) ? CloudApiUtils.fileCheckerInstance(new BucketManager(auth, configuration), bucket)
                : key -> null;
    }

    public Response asyncFetch(String url, String key, String md5, String etag) throws QiniuException {
        StringMap stringMap = new StringMap().put("url", url).put("bucket", bucket)
                .putNotNull("key", key).putNotEmpty("etag", etag);
        if (hasCustomArgs) {
            stringMap.putNotEmpty("host", host).putNotNull("md5", md5)
                    .putNotNull("callbackurl", callbackUrl).putNotNull("callbackbody", callbackBody)
                    .putNotNull("callbackbodytype", callbackBodyType).putNotNull("callbackhost", callbackHost)
                    .putNotNull("file_type", fileType).putNotNull("ignore_same_key", ignoreSameKey);
        }
//        return bucketManager.asyncFetch(url, bucket, stringMap);
        byte[] bodyByte = Json.encode(stringMap).getBytes(Constants.UTF_8);
        return client.post(requestUrl, bodyByte, auth.authorizationV2(requestUrl, "POST", bodyByte, Client.JsonMime),
                Client.JsonMime);
    }

    @Override
    protected String resultInfo(Map<String, String> line) {
        return domain == null ? line.get(urlIndex) : line.get("key");
    }

    @Override
    protected String singleResult(Map<String, String> line) throws Exception {
        String url;
        String key = line.get("key"); // 原始的认为正确的 key，用来拼接 URL 时需要保持不变
        if (domain == null) {
            url = line.get(urlIndex);
            if (key != null) key = String.join("", addPrefix, FileUtils.rmPrefix(rmPrefix, key));
            else key = String.join("", addPrefix, FileUtils.rmPrefix(rmPrefix, URLUtils.getKey(url)));
        } else {
            if (key == null) throw new IOException("key is not exists or empty in " + line);
            url = String.join("", protocol, "://", domain, "/", key.replace("\\?", "%3f"));
            key = String.join("", addPrefix, FileUtils.rmPrefix(rmPrefix, key)); // 目标文件名
        }
        String etag = line.get("etag");
        if (etag == null || "".equals(etag)) etag = line.get("hash");
//        String check = iFileChecker.check(key);
        if (iFileChecker.check(key) != null) throw new IOException("file exists");
        Response response = asyncFetch(url, key, line.get(md5Index), etag);
        return String.join("\t", key, url, String.valueOf(response.statusCode), HttpRespUtils.getResult(response));
    }

    @Override
    public void closeResource() {
        super.closeResource();
        protocol = null;
        domain = null;
        urlIndex = null;
        addPrefix = null;
        rmPrefix = null;
        host = null;
        md5Index = null;
        callbackUrl = null;
        callbackBody = null;
        callbackBodyType = null;
        callbackHost = null;
        configuration = null;
        auth = null;
        client = null;
//        bucketManager = null;
    }
}
