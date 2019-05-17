package com.qiniu.process.qdora;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.qiniu.common.QiniuException;
import com.qiniu.config.JsonFile;
import com.qiniu.model.qdora.Avinfo;
import com.qiniu.model.qdora.VideoStream;
import com.qiniu.process.Base;
import com.qiniu.util.JsonConvertUtils;
import com.qiniu.util.PfopUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PfopCommand extends Base<Map<String, String>> {

    private boolean hasDuration;
    private boolean hasSize;
    private String avinfoIndex;
    private ArrayList<JsonObject> pfopConfigs;
    private MediaManager mediaManager;

    public PfopCommand(String avinfoIndex, String jsonPath, boolean hasDuration, boolean hasSize, String savePath,
                       int saveIndex) throws IOException {
        super("pfopcmd", "", "", null, null, savePath, saveIndex);
        set(avinfoIndex, jsonPath, hasDuration, hasSize);
        this.mediaManager = new MediaManager();
    }

    public void updateCommand(String avinfoIndex, String jsonPath, boolean hasDuration, boolean hasSize)
            throws IOException {
        set(avinfoIndex, jsonPath, hasDuration, hasSize);
    }

    private void set(String avinfoIndex, String jsonPath, boolean hasDuration, boolean hasSize) throws IOException {
        if (avinfoIndex == null || "".equals(avinfoIndex)) throw new IOException("please set the avinfoIndex.");
        else this.avinfoIndex = avinfoIndex;
        this.pfopConfigs = new ArrayList<>();
        JsonFile jsonFile = new JsonFile(jsonPath);
        for (String key : jsonFile.getKeys()) {
            JsonObject jsonObject = PfopUtils.checkPfopJson(jsonFile.getElement(key).getAsJsonObject(), true);
            jsonObject.addProperty("name", key);
            this.pfopConfigs.add(jsonObject);
        }
        this.hasDuration = hasDuration;
        this.hasSize = hasSize;
    }

    public PfopCommand(String avinfoIndex, String jsonPath, boolean hasDuration, boolean hasSize, String savePath)
            throws IOException {
        this(avinfoIndex, jsonPath, hasDuration, hasSize, savePath, 0);
    }

    @SuppressWarnings("unchecked")
    public PfopCommand clone() throws CloneNotSupportedException {
        PfopCommand pfopCommand = (PfopCommand)super.clone();
        pfopCommand.mediaManager = new MediaManager();
        pfopCommand.pfopConfigs = (ArrayList<JsonObject>) pfopConfigs.clone();
        return pfopCommand;
    }

    @Override
    public String resultInfo(Map<String, String> line) {
        return line.get("key") + "\t" + line.get(avinfoIndex);
    }

    @Override
    public boolean validCheck(Map<String, String> line) {
        return line.get("key") != null;
    }

    @Override
    public void parseSingleResult(Map<String, String> line, String result) throws IOException {
        fileSaveMapper.writeSuccess(result, false);
    }

    @Override
    public String singleResult(Map<String, String> line) throws QiniuException {
        String key;
        String info;
        Avinfo avinfo;
        StringBuilder other = new StringBuilder();
        VideoStream videoStream;
        List<Integer> scale;
        List<String> resultList = new ArrayList<>();
        for (JsonObject pfopConfig : pfopConfigs) {
            scale = JsonConvertUtils.fromJsonArray(pfopConfig.get("scale").getAsJsonArray(), new TypeToken<List<Integer>>(){});
            key = line.get("key");
            info = line.get(avinfoIndex);
            try {
                if (info == null || "".equals(info)) throw new IOException("avinfo is empty.");
                avinfo = mediaManager.getAvinfoByJson(info);
                if (hasDuration) other.append("\t").append(Double.valueOf(avinfo.getFormat().duration));
                if (hasSize) other.append("\t").append(Long.valueOf(avinfo.getFormat().size));
                videoStream = avinfo.getVideoStream();
                if (videoStream == null) throw new Exception("videoStream is null");
                if (scale.get(0) < videoStream.width && videoStream.width <= scale.get(1)) {
                    resultList.add(key + "\t" + PfopUtils.generateFopCmd(key, pfopConfig) + other.toString());
                }
            } catch (Exception e) {
                throw new QiniuException(e, e.getMessage());
            }
        }
        return String.join("\n", resultList);
    }
}
