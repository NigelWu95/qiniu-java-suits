package com.qiniu.process.qiniu;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.qiniu.interfaces.ICdnApplier;
import com.qiniu.process.Base;
import com.qiniu.storage.Configuration;
import com.qiniu.util.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CdnUrlQuery extends Base<Map<String, String>> {

    private String protocol;
    private String domain;
    private String urlIndex;
    private boolean prefetch;
    private List<String> batches;
    private List<Map<String, String>> lines;
    private ICdnApplier cdnApplier;

    public CdnUrlQuery(String accessKey, String secretKey, Configuration configuration, String protocol, String domain,
                       String urlIndex, boolean prefetch) throws IOException {
        super(prefetch ? "prefetchquery" : "refreshquery", accessKey, secretKey, null);
        Auth auth = Auth.create(accessKey, secretKey);
        CdnHelper cdnHelper = new CdnHelper(auth, configuration.clone());
        this.cdnApplier = prefetch ? cdnHelper::queryPrefetch : cdnHelper::queryRefresh;
        CloudApiUtils.checkQiniu(auth);
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
        this.prefetch = prefetch;
    }

    public CdnUrlQuery(String accessKey, String secretKey, Configuration configuration, String protocol, String domain,
                       String urlIndex, boolean prefetch, String savePath, int saveIndex) throws IOException {
        super(prefetch ? "prefetchquery" : "refreshquery", accessKey, secretKey, null, savePath, saveIndex);
        this.batchSize = 30;
        this.batches = new ArrayList<>(30);
        this.lines = new ArrayList<>();
        Auth auth = Auth.create(accessKey, secretKey);
        CdnHelper cdnHelper = new CdnHelper(auth, configuration.clone());
        this.cdnApplier = prefetch ? cdnHelper::queryPrefetch : cdnHelper::queryRefresh;
        CloudApiUtils.checkQiniu(auth);
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
        this.prefetch = prefetch;
        this.fileSaveMapper.preAddWriter("processing");
    }

    public CdnUrlQuery(String accessKey, String secretKey, Configuration configuration, String protocol, String domain,
                       String urlIndex, boolean prefetch, String savePath) throws IOException {
        this(accessKey, secretKey, configuration, protocol, domain, urlIndex, prefetch, savePath, 0);
    }

    @Override
    public CdnUrlQuery clone() throws CloneNotSupportedException {
        CdnUrlQuery cdnUrlQuery = (CdnUrlQuery)super.clone();
        cdnUrlQuery.lines = new ArrayList<>();
        CdnHelper cdnHelper = new CdnHelper(Auth.create(accessId, secretKey));
        cdnUrlQuery.cdnApplier = prefetch ? cdnHelper::queryPrefetch : cdnHelper::queryRefresh;
        if (cdnUrlQuery.fileSaveMapper != null) {
            cdnUrlQuery.fileSaveMapper.preAddWriter("processing");
        }
        return cdnUrlQuery;
//        if (!autoIncrease) saveIndex.addAndGet(1);
//        return this;
    }

    @Override
    protected String resultInfo(Map<String, String> line) {
        String key = line.get("key");
        return key == null ? line.get(urlIndex) : String.join("\t", key, line.get(urlIndex));
    }

    @Override
    protected synchronized List<Map<String, String>> putBatchOperations(List<Map<String, String>> processList) throws IOException {
        batches.clear();
        lines.clear();
        String key;
        String url;
        for (Map<String, String> line : processList) {
            key = line.get("key");
            url = line.get(urlIndex);
            if (url == null || "".equals(url)) {
                if (key == null) {
                    fileSaveMapper.writeError("key and url are not exist or empty in " + line, false);
                } else {
                    url = String.join("", protocol, "://", domain, "/", key.replace("\\?", "%3f"));
                    line.put(urlIndex, url);
                    lines.add(line);
                    batches.add(url);
                }
            } else {
                lines.add(line);
                batches.add(url);
            }
        }
        return lines;
    }

    @Override
    protected String batchResult(List<Map<String, String>> lineList) throws IOException {
        String[] urls = new String[batches.size()];
        return HttpRespUtils.getResult(cdnApplier.apply(batches.toArray(urls)));
    }

    @Override
    protected List<Map<String, String>> parseBatchResult(List<Map<String, String>> processList, String result) throws Exception {
        if (result == null || "".equals(result)) throw new IOException("not valid refresh response.");
        JsonObject refreshResult = JsonUtils.toJsonObject(result);
        int code = refreshResult.get("code").getAsInt();
        if (code == 200) {
            JsonArray jsonArray = new JsonArray();
            JsonElement jsonElement = refreshResult.get("items");
            if (!(jsonElement instanceof JsonNull)) jsonArray = jsonElement.getAsJsonArray();
            if (jsonArray.size() > 0) {
                JsonObject item;
                String state;
                for (int i = 0; i < jsonArray.size(); i++) {
                    item = jsonArray.get(i).getAsJsonObject();
                    state = item.get("state").getAsString();
                    if ("success".equals(state)) {
                        fileSaveMapper.writeSuccess(item.toString(), false);
                    } else if ("processing".equals(state)) {
                        fileSaveMapper.writeToKey("processing", item.toString(), false);
                    } else {
                        fileSaveMapper.writeError(item.toString(), false);
                    }
                }
            }
        } else {
            fileSaveMapper.writeError(String.join("\n", processList.stream().map(this::resultInfo)
                    .collect(Collectors.toList())) + "\t" + code + refreshResult.get("error").getAsString(), false);
        }
        refreshResult = null;
        return null;
    }

    @Override
    protected String singleResult(Map<String, String> line) throws IOException {
        String key = line.get("key");
        String url = line.get(urlIndex);
        if (url == null || "".equals(url)) {
            if (key == null) throw new IOException("key is not exists or empty in " + line);
            url = String.join("", protocol, "://", domain, "/", key.replace("\\?", "%3f"));
        }
        String[] urls = new String[]{url};
        return key == null ? HttpRespUtils.getResult(cdnApplier.apply(urls)) :
                String.join("\t", key, HttpRespUtils.getResult(cdnApplier.apply(urls)));
    }

    @Override
    public void closeResource() {
        super.closeResource();
        protocol = null;
        domain = null;
        urlIndex = null;
        batches = null;
        lines = null;
        cdnApplier = null;
        saveIndex = null;
    }
}
