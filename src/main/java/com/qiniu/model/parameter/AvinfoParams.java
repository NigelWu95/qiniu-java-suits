package com.qiniu.model.parameter;

import com.qiniu.service.interfaces.IEntryParam;

public class AvinfoParams extends QossParams {

    private String domain;
    private String https;
    private String needSign;

    public AvinfoParams(IEntryParam entryParam) throws Exception {
        super(entryParam);
        this.domain = entryParam.getParamValue("domain");
        try { this.https = entryParam.getParamValue("https"); } catch (Exception e) { https = ""; }
        try { this.needSign = entryParam.getParamValue("private"); } catch (Exception e) { needSign = ""; }
    }

    public String getDomain() {
        return domain;
    }

    public boolean getHttps() {
        if (https.matches("(true|false)")) {
            return Boolean.valueOf(https);
        } else {
            return false;
        }
    }

    public boolean getNeedSign() {
        if (needSign.matches("(true|false)")) {
            return Boolean.valueOf(needSign);
        } else {
            return false;
        }
    }
}
