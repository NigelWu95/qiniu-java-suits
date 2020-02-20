package com.qiniu.process.qiniu;

import com.qiniu.model.qdora.Item;
import com.qiniu.model.qdora.PfopResult;
import com.qiniu.process.Base;
import com.qiniu.storage.Configuration;
import com.qiniu.util.JsonUtils;

import java.io.IOException;
import java.util.Map;

public class QueryPfopResult extends Base<Map<String, String>> {

    private String protocol;
    private String pidIndex;
    private Configuration configuration;
    private MediaManager mediaManager;

    public QueryPfopResult(Configuration configuration, String protocol, String pIdIndex) throws IOException {
        super("pfopresult", "", "", null);
        set(configuration, protocol, pIdIndex);
        this.mediaManager = new MediaManager(configuration, protocol);
    }

    public QueryPfopResult(Configuration configuration, String protocol, String pIdIndex, String savePath, int saveIndex)
            throws IOException {
        super("pfopresult", "", "", null, savePath, saveIndex);
        set(configuration, protocol, pIdIndex);
        this.mediaManager = new MediaManager(configuration, protocol);
        this.fileSaveMapper.preAddWriter("waiting");
        this.fileSaveMapper.preAddWriter("notify_failed");
    }

    public QueryPfopResult(Configuration configuration, String protocol, String pIdIndex, String savePath) throws IOException {
        this(configuration, protocol, pIdIndex, savePath, 0);
    }

    private void set(Configuration configuration, String protocol, String pidIndex) throws IOException {
        this.configuration = configuration;
        this.protocol = protocol == null || !protocol.matches("(http|https)") ? "http" : protocol;
        if (pidIndex == null || "".equals(pidIndex)) throw new IOException("please set the id-index.");
        else this.pidIndex = pidIndex;
    }

    @Override
    public QueryPfopResult clone() throws CloneNotSupportedException {
        QueryPfopResult pfopResult = (QueryPfopResult)super.clone();
        pfopResult.mediaManager = new MediaManager(configuration, protocol);
        if (pfopResult.fileSaveMapper != null) {
            pfopResult.fileSaveMapper.preAddWriter("waiting");
            pfopResult.fileSaveMapper.preAddWriter("notify_failed");
        }
        return pfopResult;
    }

    @Override
    protected String resultInfo(Map<String, String> line) {
        StringBuilder ret = new StringBuilder();
        for (String key : line.keySet()) ret.append(line.get(key)).append("\t");
        return ret.deleteCharAt(ret.length() - 1).toString();
    }

    @Override
    protected void parseSingleResult(Map<String, String> line, String result) throws Exception {
        if (result != null && !"".equals(result)) {
            PfopResult pfopResult = JsonUtils.fromJson(result, PfopResult.class);
            // 可能有多条转码指令
            for (Item item : pfopResult.items) {
                StringBuilder ret = new StringBuilder();
                for (String key : line.keySet()) ret.append(line.get(key)).append("\t");
                if (item.code == 0) {
                    ret.append(pfopResult.inputBucket).append("\t").append(pfopResult.inputKey).append("\t");
                    ret.append(JsonUtils.toJsonWithoutUrlEscape(item));
                    fileSaveMapper.writeSuccess(ret.toString(), false);
                } else if (item.code == 3) {
                    ret.append(pfopResult.inputBucket).append("\t").append(pfopResult.inputKey).append("\t");
                    ret.append(JsonUtils.toJsonWithoutUrlEscape(item));
                    fileSaveMapper.writeError(ret.toString(), false);
                } else if (item.code == 4) {
                    ret.append(pfopResult.inputBucket).append("\t").append(pfopResult.inputKey).append("\t");
                    ret.append(JsonUtils.toJsonWithoutUrlEscape(item));
                    fileSaveMapper.writeToKey("notify_failed", ret.toString(), false);
                } else {
                    fileSaveMapper.writeToKey("waiting", ret.deleteCharAt(ret.length() - 1).toString(), false);
                }
            }
        } else {
            throw new IOException("only has empty_result");
        }
    }

    @Override
    protected String singleResult(Map<String, String> line) throws IOException {
        String pid = line.get(pidIndex);
        if (pid == null || pid.isEmpty()) throw new IOException("id is not exists or empty in " + line);
        return mediaManager.getPfopResultBodyById(pid);
    }

    @Override
    public void closeResource() {
        super.closeResource();
        configuration = null;
        mediaManager = null;
        protocol = null;
        pidIndex = null;
    }
}
