package com.qiniu.process.other;

import com.qiniu.model.qdora.VideoTS;
import com.qiniu.process.Base;
import com.qiniu.storage.Configuration;
import com.qiniu.util.RequestUtils;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

public class ExportTS extends Base<Map<String, String>> {

    private String protocol;
    private String domain;
    private String urlIndex;
    private Configuration configuration;
    private M3U8Manager m3U8Manager;

    public ExportTS(Configuration configuration, String protocol, String domain, String urlIndex) throws IOException {
        super("exportts", "", "", null);
        set(configuration, protocol, domain, urlIndex);
        this.m3U8Manager = new M3U8Manager(configuration.clone(), protocol);
    }

    public ExportTS(Configuration configuration, String protocol, String domain, String urlIndex, String savePath,
                    int saveIndex) throws IOException {
        super("exportts", "", "", null, savePath, saveIndex);
        set(configuration, protocol, domain, urlIndex);
        this.m3U8Manager = new M3U8Manager(configuration.clone(), protocol);
    }

    public ExportTS(Configuration configuration, String protocol, String domain, String urlIndex, String savePath)
            throws IOException {
        this(configuration, protocol, domain, urlIndex, savePath, 0);
    }

    private void set(Configuration configuration, String protocol, String domain, String urlIndex) throws IOException {
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
    }

    @Override
    public ExportTS clone() throws CloneNotSupportedException {
        ExportTS exportTS = (ExportTS)super.clone();
        exportTS.m3U8Manager = new M3U8Manager(configuration.clone(), protocol);
        return exportTS;
    }

    @Override
    protected String resultInfo(Map<String, String> line) {
        return domain == null ? line.get(urlIndex) : line.get("key");
    }

    @Override
    protected String singleResult(Map<String, String> line) throws Exception {
        String url;
        if (domain == null) {
            url = line.get(urlIndex);
        } else {
            String key = line.get("key");
            if (key == null) throw new IOException("key is not exists or empty in " + line);
            url = String.join("", protocol, "://", domain, "/", key.replace("\\?", "%3f"));
        }
        return String.join("\n", m3U8Manager.getVideoTSListByUrl(url).stream()
                .map(VideoTS::toString).collect(Collectors.toList()));
    }

    @Override
    public void closeResource() {
        super.closeResource();
        protocol = null;
        domain = null;
        urlIndex = null;
        configuration = null;
        m3U8Manager = null;
    }
}
