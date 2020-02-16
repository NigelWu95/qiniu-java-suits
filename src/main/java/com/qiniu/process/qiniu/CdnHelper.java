package com.qiniu.process.qiniu;

import com.qiniu.common.Constants;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Client;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.util.Auth;
import com.qiniu.util.Json;

import java.util.HashMap;
import java.util.Map;

public class CdnHelper {

    private final Auth auth;
    private final Client client;
    private static final String refreshUrl = "http://fusion.qiniuapi.com/v2/tune/refresh";
    private static final String prefetchUrl = "http://fusion.qiniuapi.com/v2/tune/prefetch";
    private static final String refreshQueryUrl = "http://fusion.qiniuapi.com/v2/tune/refresh/list";
    private static final String prefetchQueryUrl = "http://fusion.qiniuapi.com/v2/tune/prefetch/list";

    public CdnHelper(Auth auth) {
        this.auth = auth;
        this.client = new Client();
    }

    public CdnHelper(Auth auth, Configuration configuration) {
        this.auth = auth;
        this.client = new Client(configuration);
    }

    public Response refresh(String[] urls, String[] dirs) throws QiniuException {
        Map<String, String[]> req = new HashMap<>();
        if (urls != null) req.put("urls", urls);
        if (dirs != null) req.put("dirs", dirs);
        return UOperationForUrls(refreshUrl, req);
    }

    public Response queryRefresh(String[] urls) throws QiniuException {
        Map<String, String[]> req = new HashMap<>();
        req.put("urls", urls);
        return UOperationForUrls(refreshQueryUrl, req);
    }

    public Response prefetch(String[] urls) throws QiniuException {
        Map<String, String[]> req = new HashMap<>();
        req.put("urls", urls);
        return UOperationForUrls(prefetchUrl, req);
    }

    public Response queryPrefetch(String[] urls) throws QiniuException {
        Map<String, String[]> req = new HashMap<>();
        req.put("urls", urls);
        return UOperationForUrls(prefetchQueryUrl, req);
    }

    private Response UOperationForUrls(String apiUrl, Map<String, String[]> req) throws QiniuException {
        byte[] body = Json.encode(req).getBytes(Constants.UTF_8);
        return client.post(apiUrl, body, auth.authorizationV2(apiUrl, "POST", body, Client.JsonMime), Client.JsonMime);
    }
}
