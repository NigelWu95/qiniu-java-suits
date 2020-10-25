package com.qiniu.entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.qiniu.config.JsonFile;
import com.qiniu.config.ParamsConfig;
import com.qiniu.convert.LineToMap;
import com.qiniu.interfaces.IEntryParam;
import com.qiniu.interfaces.ITypeConvert;
import com.qiniu.process.filtration.BaseFilter;
import com.qiniu.process.filtration.SeniorFilter;
import com.qiniu.util.*;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Base64;

public class CommonParams {

    private IEntryParam entryParam;
    private int connectTimeout;
    private int readTimeout;
    private int requestTimeout;
    private boolean httpsForConfigEnabled;
    private String path;
    private String source;
    private boolean isStorageSource;
    private Map<String, String> accountMap;
    private String account;
    private String qiniuAccessKey;
    private String qiniuSecretKey;
    private String tencentSecretId;
    private String tencentSecretKey;
    private String aliyunAccessId;
    private String aliyunAccessSecret;
    private String upyunUsername;
    private String upyunPassword;
    private String s3AccessId;
    private String s3SecretKey;
    private String huaweiAccessId;
    private String huaweiSecretKey;
    private String baiduAccessId;
    private String baiduSecretKey;
    private String bucket;
    private String logFilepath;
    private Map<String, Map<String, String>> pathConfigMap;
    private List<String> antiPrefixes;
    private boolean prefixLeft;
    private boolean prefixRight;
    private String parse;
    private String separator;
    private boolean keepDir;
    private String addKeyPrefix;
    private String rmKeyPrefix;
    private String process;
    private String privateType;
    private String regionName;
    private BaseFilter<Map<String, String>> baseFilter;
    private SeniorFilter<Map<String, String>> seniorFilter;
    private Map<String, String> indexMap;
    private int unitLen;
    private int threads;
    private int batchSize;
    private int retryTimes;
    private boolean saveTotal;
    private String savePath;
    private String saveTag;
    private String saveFormat;
    private String saveSeparator;
    private List<String> rmFields;
    private Map<String, String> mapLine;
    private List<JsonObject> pfopConfigs;
    private Base64.Decoder decoder = Base64.getDecoder();
    private LocalDateTime startDateTime;
    private long pauseDelay;
    private long pauseDuration;
    private boolean isSelfUpload; // 表示读取的文件路径本身，而不是对文本内容做解析，用作目录下文件直接上传等操作

    public static Set<String> lineFormats = new HashSet<String>(){{
        add("csv");
        add("tab");
        add("json");
        add("yaml");
    }};

    private void accountInit() throws IOException {
        try {
            accountMap = ParamsUtils.toParamsMap(AccountUtils.accountPath);
        } catch (FileNotFoundException ignored) {
            accountMap = new HashMap<>();
        }
        account = entryParam.getValue("a", null);
        if (account == null) {
            if (entryParam.getValue("default", "false").equals("true")) {
                account = accountMap.get("account");
                if (account == null) throw new IOException("no default account.");
            }
        }
    }

    public CommonParams() throws IOException {
        try {
            accountMap = ParamsUtils.toParamsMap(AccountUtils.accountPath);
        } catch (FileNotFoundException ignored) {
            accountMap = new HashMap<>();
        }
        account = accountMap.get("account");
        if (account != null) {
            qiniuAccessKey = accountMap.get(account + "-qiniu-id");
            qiniuSecretKey = accountMap.get(account + "-qiniu-secret");
            if (qiniuAccessKey != null && qiniuSecretKey != null) {
                qiniuAccessKey = new String(decoder.decode(qiniuAccessKey.substring(8)));
                qiniuSecretKey = new String(decoder.decode(qiniuSecretKey.substring(8)));
            }
            tencentSecretId = accountMap.get(account + "-tencent-id");
            tencentSecretKey = accountMap.get(account + "-tencent-secret");
            if (tencentSecretId != null && tencentSecretKey != null) {
                tencentSecretId = new String(decoder.decode(tencentSecretId.substring(8)));
                tencentSecretKey = new String(decoder.decode(tencentSecretKey.substring(8)));
            }
            aliyunAccessId = accountMap.get(account + "-aliyun-id");
            aliyunAccessSecret = accountMap.get(account + "-aliyun-secret");
            if (aliyunAccessId != null && aliyunAccessSecret != null) {
                aliyunAccessId = new String(decoder.decode(aliyunAccessId.substring(8)));
                aliyunAccessSecret = new String(decoder.decode(aliyunAccessSecret.substring(8)));
            }
            upyunUsername = accountMap.get(account + "-upyun-id");
            upyunPassword = accountMap.get(account + "-upyun-secret");
            if (upyunUsername != null && upyunPassword != null) {
                upyunUsername = new String(decoder.decode(upyunUsername.substring(8)));
                upyunPassword = new String(decoder.decode(upyunPassword.substring(8)));
            }
            s3AccessId = accountMap.get(account + "-s3-id");
            s3SecretKey = accountMap.get(account + "-s3-secret");
            if (s3AccessId != null && s3SecretKey != null) {
                s3AccessId = new String(decoder.decode(s3AccessId.substring(8)));
                s3SecretKey = new String(decoder.decode(s3SecretKey.substring(8)));
            }
            huaweiAccessId = accountMap.get(account + "-huawei-id");
            huaweiSecretKey = accountMap.get(account + "-huawei-secret");
            if (huaweiAccessId != null && huaweiSecretKey != null) {
                huaweiAccessId = new String(decoder.decode(huaweiAccessId.substring(8)));
                huaweiSecretKey = new String(decoder.decode(huaweiSecretKey.substring(8)));
            }
            baiduAccessId = accountMap.get(account + "-baidu-id");
            baiduSecretKey = accountMap.get(account + "-baidu-secret");
            if (baiduAccessId != null && baiduSecretKey != null) {
                baiduAccessId = new String(decoder.decode(baiduAccessId.substring(8)));
                baiduSecretKey = new String(decoder.decode(baiduSecretKey.substring(8)));
            }
        }
    }

    /**
     * 从入口中解析出程序运行所需要的参数，参数解析需要一定的顺序，因为部分参数会依赖前面参数解析的结果
     * @param entryParam 配置参数入口
     * @throws IOException 获取一些参数失败时抛出的异常
     */
    public CommonParams(IEntryParam entryParam) throws Exception {
        this.entryParam = entryParam;
        setTimeout();
        path = entryParam.getValue("path", "");
        setSource();
        setHttpsConfigEnabled();
        accountInit();
        logFilepath = entryParam.getValue("log", null);
        if (isStorageSource) {
            setAuthKey();
            setBucket();
            String prefixes = entryParam.getValue("prefixes", null);
            setPathConfigMap(entryParam.getValue("prefix-config", ""), prefixes, true, true);
            setPrefixLeft(entryParam.getValue("prefix-left", "false").trim());
            setPrefixRight(entryParam.getValue("prefix-right", "false").trim());
        } else {
            setParse();
            setSeparator();
            setKeepDir();
            addKeyPrefix = entryParam.getValue("add-keyPrefix", null);
            rmKeyPrefix = entryParam.getValue("rm-keyPrefix", null);
            String uris = entryParam.getValue("uris", null);
            setPathConfigMap(entryParam.getValue("uri-config", ""), uris, false, false);
        }
        antiPrefixes = Arrays.asList(ParamsUtils.escapeSplit(entryParam.getValue("anti-prefixes", "")));
        setProcess();
        setPrivateType();
        regionName = entryParam.getValue("region", "").trim().toLowerCase();
        setBaseFilter();
        setSeniorFilter();
        setIndexMap();
        checkFilterForProcess();
        setUnitLen();
        setThreads();
        setBatchSize();
        setRetryTimes();
        setSaveTotal();
        setSavePath();
        saveTag = entryParam.getValue("save-tag", "").trim();
        saveFormat = entryParam.getValue("save-format", "tab").trim();
        if (!lineFormats.contains(saveFormat)) {
            throw new IOException("unsupported format: \"" + saveFormat + "\", please set the it in: " + lineFormats);
        }
        setSaveSeparator();
        setRmFields();
        setPfopConfigs();
        setStartAndPause();
    }

    public CommonParams(Map<String, String> paramsMap) throws Exception {
        this.entryParam = new ParamsConfig(paramsMap);
        setTimeout();
        source = "terminal";
        setHttpsConfigEnabled();
        accountInit();
        setParse();
        setSeparator();
        addKeyPrefix = entryParam.getValue("add-keyPrefix", null);
        rmKeyPrefix = entryParam.getValue("rm-keyPrefix", null);
        setProcess();
        setPrivateType();
        regionName = entryParam.getValue("region", "").trim().toLowerCase();
        setIndexMap();
        setRetryTimes();
        String line = entryParam.getValue("line", null);
        boolean fromLine = line != null && !"".equals(line);
//        if ((entryParam.getValue("indexes", null) != null || indexMap.size() > 1) && !fromLine && !"qupload".equals(process)) {
//            throw new IOException("you have set parameter for line index but no line data to parse, please set \"-line=<data>\".");
//        }
        if (fromLine) {
            ITypeConvert<String, Map<String, String>> converter = new LineToMap(parse, separator, addKeyPrefix, rmKeyPrefix, indexMap);
            mapLine = converter.convertToV(line);
            fromLine = "domainsofbucket".equals(process) ? mapLine.containsKey("bucket") : mapLine.containsKey("key");
        } else {
            mapLine = new HashMap<>();
        }
        switch (process) {
            case "delete":
            case "status":
            case "lifecycle":
            case "type":
            case "mirror":
            case "restorear":
            case "metadata":
                if (!fromLine) mapLine.put("key", entryParam.getValue("key", entryParam.getParamsMap().containsKey("key") ? "" : null));
                break;
            case "copy":
            case "move":
            case "rename":
                if (!fromLine) mapLine.put("key", entryParam.getValue("key", entryParam.getParamsMap().containsKey("key") ? "" : null));
                if (entryParam.getParamsMap().containsKey("to-key")) {
                    indexMap.put("toKey", "toKey");
                    mapLine.put("toKey", entryParam.getValue("to-key", ""));
                }
                break;
            case "stat":
                if (!fromLine) mapLine.put("key", entryParam.getValue("key", entryParam.getParamsMap().containsKey("key") ? "" : null));
                saveFormat = entryParam.getValue("save-format", "tab").trim();
                ParamsUtils.checked(saveFormat, "save-format", "(csv|tab|json)");
                setSaveSeparator();
                break;
            case "mime":
                if (!fromLine) mapLine.put("key", entryParam.getValue("key", entryParam.getParamsMap().containsKey("key") ? "" : null));
                String mime = entryParam.getValue("mime", "").trim();
                if (!"".equals(mime)) {
                    indexMap.put("mime", "mime");
                    mapLine.put("mime", mime);
                }
                break;
            case "download":
            case "fetch":
            case "asyncfetch":
            case "avinfo":
            case "qhash":
            case "privateurl":
            case "exportts":
            // 这几个数据源的私有签名都是采用 bucket + key + endpoint(region) 的方式来签算
//            case "tenprivate":
//            case "aliprivate":
//            case "s3private":
//            case "awsprivate":
//            case "huaweiprivate":
//            case "baiduprivate":
            case "imagecensor":
            case "videocensor":
            case "cdnrefresh":
            case "cdnprefetch":
            case "refreshquery":
            case "prefetchquery":
            case "syncupload":
                String url = entryParam.getValue("url", "").trim();
                if (!"".equals(url)) {
                    indexMap.put("url", "url");
                    mapLine.put("url", url);
                    mapLine.put("key", entryParam.getValue("key", null));
                } else if (!fromLine) {
                    entryParam.getValue("domain");
                    mapLine.put("key", entryParam.getValue("key"));
                }
                break;
            case "pfop":
                if (!fromLine) mapLine.put("key", entryParam.getValue("key"));
                String fops = entryParam.getValue("fops", "").trim();
                if (!"".equals(fops)) {
                    indexMap.put("fops", "fops");
                    mapLine.put("fops", fops);
                }
                setPfopConfigs();
                break;
            case "pfopcmd":
                if (!fromLine) mapLine.put("key", entryParam.getValue("key"));
                String avinfo = entryParam.getValue("avinfo", "").trim();
                if (!"".equals(avinfo)) {
                    indexMap.put("avinfo", "avinfo");
                    mapLine.put("avinfo", avinfo);
                }
                setPfopConfigs();
                break;
            case "pfopresult":
            case "censorresult":
                String id = entryParam.getValue("id", "").trim();
                if (!"".equals(id)) {
                    indexMap.put("id", "id");
                    mapLine.put("id", id);
                }
                break;
            case "qupload":
                String key = entryParam.getValue("key", entryParam.getParamsMap().containsKey("key") ? "" : null);
                if (!fromLine) mapLine.put("key", key);
                String filepath = entryParam.getValue("filepath", entryParam.getValue("path", ""));
                if (!"".equals(filepath)) {
                    indexMap.put("filepath", "filepath");
                    mapLine.put("filepath", filepath);
                } else if (key == null || "".equals(key)) {
                    throw new IOException("filepath and key shouldn't all be empty, file must be found with them.");
                }
                break;
            case "domainsofbucket": if (!fromLine) mapLine.put("bucket", entryParam.getValue("bucket")); break;
            default: if (!fromLine) mapLine.put("key", entryParam.getValue("key")); break;
        }
        if (mapLine.size() <= 0) {
            if (fromLine) {
                throw new IOException("please check your line or indexes settings because there are no target data.");
            } else {
                throw new IOException("if you have set line indexes, please set \"-line=<data>\".");
            }
        }
    }

    private void setTimeout() {
        connectTimeout = Integer.parseInt(entryParam.getValue("connect-timeout", "60").trim());
        readTimeout = Integer.parseInt(entryParam.getValue("read-timeout", "120").trim());
        requestTimeout = Integer.parseInt(entryParam.getValue("request-timeout", "60").trim());
    }

    private void setSource() throws IOException {
        if (entryParam.getValue("interactive", "").trim().equals("true")) {
            source = "terminal";
            return;
        }
        if ("".equals(path)) {
            try {
                source = entryParam.getValue("source-type").trim();
            } catch (IOException e1) {
                try {
                    source = entryParam.getValue("source").trim();
                } catch (IOException e2) {
                    if ("domainsofbucket".equals(process)) source = "local";
                    else source = "qiniu";
                }
            }
            // list 和 file 方式是兼容老的数据源参数，list 默认表示从七牛进行列举，file 表示从本地读取文件
            if ("list".equals(source)) source = "qiniu";
            else if ("file".equals(source)) source = "local";
            else if ("aws".equals(source)) source = "s3";
            else if (!source.matches("(local|qiniu|tencent|aliyun|upyun|s3|huawei|baidu)")) {
                throw new IOException("the datasource: " + source + " is supported.");
            }
        } else if (path.startsWith("qiniu://")) {
            source = "qiniu";
            bucket = path.substring(8);
        } else if (path.startsWith("tencent://")) {
            source = "tencent";
            bucket = path.substring(10);
        } else if (path.startsWith("aliyun://")) {
            source = "aliyun";
            bucket = path.substring(9);
        } else if (path.startsWith("upyun://")) {
            source = "upyun";
            bucket = path.substring(8);
        } else if (path.startsWith("aws://")) {
            source = "s3";
            bucket = path.substring(6);
        } else if (path.startsWith("s3://")) {
            source = "s3";
            bucket = path.substring(5);
        } else if (path.startsWith("huawei://")) {
            source = "huawei";
            bucket = path.substring(9);
        } else if (path.startsWith("baidu://")) {
            source = "baidu";
            bucket = path.substring(8);
        } else {
            source = "local";
        }
        isStorageSource = CloudApiUtils.isStorageSource(source);
        if (isStorageSource && "domainsofbucket".equals(process)) throw new IOException("domainsofbucket doesn't support source: " + source);
    }

    private void setHttpsConfigEnabled() throws IOException {
        String enabled = entryParam.getValue("config-https", "").trim();
        if ("".equals(enabled)) {
            if ("qiniu".equals(source) || "huawei".equals(source)) httpsForConfigEnabled = true;
        } else {
            ParamsUtils.checked(enabled, "config-https", "(true|false)");
            httpsForConfigEnabled = Boolean.parseBoolean(enabled);
        }
    }

    private void setParse() throws IOException {
        parse = entryParam.getValue("parse", "tab").trim();
        ParamsUtils.checked(parse, "parse", "(csv|tab|json|object|file)");
    }

    private void setSeparator() {
        String separator = entryParam.getValue("separator", null);
        if (separator == null || separator.isEmpty()) {
            if ("terminal".equals(source)) this.separator = " ";
            else if ("tab".equals(parse) || "file".equals(parse)) this.separator = "\t";
            else if ("csv".equals(parse)) this.separator = ",";
            else this.separator = " ";
        } else {
            this.separator = separator;
        }
    }

    private void setKeepDir() throws IOException {
        String keepDir = entryParam.getValue("keep-dir", "false");
        ParamsUtils.checked(keepDir, "keep-dir", "(true|false)");
        this.keepDir = Boolean.parseBoolean(keepDir);
    }

    private void setQiniuAuthKey() throws IOException {
        if (account == null) {
            qiniuAccessKey = entryParam.getValue("ak").trim();
            qiniuSecretKey = entryParam.getValue("sk").trim();
        } else {
            // 如果同时设置了 ak、sk，则覆盖从 account 中获取的密钥
            qiniuAccessKey = entryParam.getValue("ak", null);
            if (qiniuAccessKey == null) {
                qiniuAccessKey = accountMap.get(account + "-qiniu-id");
                qiniuSecretKey = accountMap.get(account + "-qiniu-secret");
                if (qiniuAccessKey == null || qiniuSecretKey == null) throw new IOException("no account: " + account);
                qiniuAccessKey = new String(decoder.decode(qiniuAccessKey.substring(8)));
                qiniuSecretKey = new String(decoder.decode(qiniuSecretKey.substring(8)));
            } else {
                qiniuAccessKey = qiniuAccessKey.trim();
                qiniuSecretKey = entryParam.getValue("sk").trim();
            }
        }
    }

    private void setTencentAuthKey() throws IOException {
        if (account == null) {
            tencentSecretId = entryParam.getValue("ten-id").trim();
            tencentSecretKey = entryParam.getValue("ten-secret").trim();
        } else {
            tencentSecretId = entryParam.getValue("ten-id", null);
            if (tencentSecretId == null) {
                tencentSecretId = accountMap.get(account + "-tencent-id");
                tencentSecretKey = accountMap.get(account + "-tencent-secret");
                if (tencentSecretId == null || tencentSecretKey == null) throw new IOException("no account: " + account);
                tencentSecretId = new String(decoder.decode(tencentSecretId.substring(8)));
                tencentSecretKey = new String(decoder.decode(tencentSecretKey.substring(8)));
            } else {
                tencentSecretId = tencentSecretId.trim();
                tencentSecretKey = entryParam.getValue("ten-secret").trim();
            }
        }
    }

    private void setAliyunAuthKey() throws IOException {
        if (account == null) {
            aliyunAccessId = entryParam.getValue("ali-id").trim();
            aliyunAccessSecret = entryParam.getValue("ali-secret").trim();
        } else {
            aliyunAccessId = entryParam.getValue("ali-id", null);
            if (aliyunAccessId == null) {
                aliyunAccessId = accountMap.get(account + "-aliyun-id");
                aliyunAccessSecret = accountMap.get(account + "-aliyun-secret");
                if (aliyunAccessId == null || aliyunAccessSecret == null) throw new IOException("no account: " + account);
                aliyunAccessId = new String(decoder.decode(aliyunAccessId.substring(8)));
                aliyunAccessSecret = new String(decoder.decode(aliyunAccessSecret.substring(8)));
            } else {
                aliyunAccessId = aliyunAccessId.trim();
                aliyunAccessSecret = entryParam.getValue("ali-secret").trim();
            }
        }
    }

    private void setUpyunAuthKey() throws IOException {
        if (account == null) {
            upyunUsername = entryParam.getValue("up-id").trim();
            upyunPassword = entryParam.getValue("up-secret").trim();
        } else {
            upyunUsername = entryParam.getValue("up-id", null);
            if (upyunUsername == null) {
                upyunUsername = accountMap.get(account + "-upyun-id");
                upyunPassword = accountMap.get(account + "-upyun-secret");
                if (upyunUsername == null || upyunPassword == null) throw new IOException("no account: " + account);
                upyunUsername = new String(decoder.decode(upyunUsername.substring(8)));
                upyunPassword = new String(decoder.decode(upyunPassword.substring(8)));
            } else {
                upyunUsername = upyunUsername.trim();
                upyunPassword = entryParam.getValue("up-secret").trim();
            }
        }
    }

    private void setS3AuthKey() throws IOException {
        if (account == null) {
            s3AccessId = entryParam.getValue("s3-id").trim();
            s3SecretKey = entryParam.getValue("s3-secret").trim();
        } else {
            s3AccessId = entryParam.getValue("s3-id", null);
            if (s3AccessId == null) {
                s3AccessId = accountMap.get(account + "-s3-id");
                s3SecretKey = accountMap.get(account + "-s3-secret");
                if (s3AccessId == null || s3SecretKey == null) throw new IOException("no account: " + account);
                s3AccessId = new String(decoder.decode(s3AccessId.substring(8)));
                s3SecretKey = new String(decoder.decode(s3SecretKey.substring(8)));
            } else {
                s3AccessId = s3AccessId.trim();
                s3SecretKey = entryParam.getValue("s3-secret").trim();
            }
        }
    }

    private void setHuaweiAuthKey() throws IOException {
        if (account == null) {
            huaweiAccessId = entryParam.getValue("hua-id").trim();
            huaweiSecretKey = entryParam.getValue("hua-secret").trim();
        } else {
            huaweiAccessId = entryParam.getValue("hua-id", null);
            if (huaweiAccessId == null) {
                huaweiAccessId = accountMap.get(account + "-huawei-id");
                huaweiSecretKey = accountMap.get(account + "-huawei-secret");
                if (huaweiAccessId == null || huaweiSecretKey == null) throw new IOException("no account: " + account);
                huaweiAccessId = new String(decoder.decode(huaweiAccessId.substring(8)));
                huaweiSecretKey = new String(decoder.decode(huaweiSecretKey.substring(8)));
            } else {
                huaweiAccessId = huaweiAccessId.trim();
                huaweiSecretKey = entryParam.getValue("hua-secret", huaweiSecretKey).trim();
            }
        }
    }

    private void setBaiduAuthKey() throws IOException {
        if (account == null) {
            baiduAccessId = entryParam.getValue("bai-id").trim();
            baiduSecretKey = entryParam.getValue("bai-secret").trim();
        } else {
            baiduAccessId = entryParam.getValue("bai-id", null);
            if (baiduAccessId == null) {
                baiduAccessId = accountMap.get(account + "-baidu-id");
                baiduSecretKey = accountMap.get(account + "-baidu-secret");
                if (baiduAccessId == null || baiduSecretKey == null) throw new IOException("no account: " + account);
                baiduAccessId = new String(decoder.decode(baiduAccessId.substring(8)));
                baiduSecretKey = new String(decoder.decode(baiduSecretKey.substring(8)));
            } else {
                baiduAccessId = baiduAccessId.trim();
                baiduSecretKey = entryParam.getValue("bai-secret", baiduSecretKey).trim();
            }
        }
    }

    private void setAuthKey() throws IOException {
        if ("qiniu".equals(source)) {
            setQiniuAuthKey();
        } else if ("tencent".equals(source)) {
            setTencentAuthKey();
        } else if ("aliyun".equals(source)) {
            setAliyunAuthKey();
        } else if ("upyun".equals(source)) {
            setUpyunAuthKey();
        } else if ("s3".equals(source)) {
            setS3AuthKey();
        } else if ("huawei".equals(source)) {
            setHuaweiAuthKey();
        } else if ("baidu".equals(source)) {
            setBaiduAuthKey();
        } else {
            if (account == null) {
                qiniuAccessKey = entryParam.getValue("ak", "").trim();
                qiniuSecretKey = entryParam.getValue("sk", "").trim();
            } else {
                qiniuAccessKey = accountMap.get(account + "-qiniu-id");
                qiniuSecretKey = accountMap.get(account + "-qiniu-secret");
            }
        }
    }

    /**
     * 支持从路径方式上解析出 bucket，如果主动设置 bucket 则替换路径中的值
     * @throws IOException 解析 bucket 参数失败抛出异常
     */
    private void setBucket() throws IOException {
        if (bucket == null || "".equals(bucket)) {
            if (path.startsWith("qiniu://")) bucket = path.substring(8);
            else if (path.startsWith("tencent://")) bucket = path.substring(10);
            else if (path.startsWith("aliyun://")) bucket = path.substring(9);
            else if (path.startsWith("upyun://")) bucket = path.substring(8);
            else if (path.startsWith("s3://")) bucket = path.substring(5);
            else if (path.startsWith("aws://")) bucket = path.substring(6);
            else if (path.startsWith("huawei://")) bucket = path.substring(9);
            else if (path.startsWith("baidu://")) bucket = path.substring(8);
            else bucket = entryParam.getValue("bucket").trim();
        } else {
            bucket = entryParam.getValue("bucket", bucket).trim();
        }
    }

    private void setProcess() throws Exception {
        process = entryParam.getValue("process", "").trim();
        if (!process.isEmpty() && isStorageSource && !ProcessUtils.supportStorageSource(process)) {
            throw new IOException("the process: " + process + " don't support getting source line from list.");
        }
        if (ProcessUtils.needQiniuAuth(process)) {
            setQiniuAuthKey();
        } else if (ProcessUtils.needTencentAuth(process)) {
            setTencentAuthKey();
        } else if (ProcessUtils.needAliyunAuth(process)) {
            setAliyunAuthKey();
        } else if (ProcessUtils.needAwsS3Auth(process)) {
            setS3AuthKey();
        } else if (ProcessUtils.needHuaweiAuth(process)) {
            setHuaweiAuthKey();
        } else if (ProcessUtils.needBaiduAuth(process)) {
            setBaiduAuthKey();
        }
        if (ProcessUtils.needBucket(process)) {
            if (bucket == null || "".equals(bucket)) bucket = entryParam.getValue("bucket").trim();
            else bucket = entryParam.getValue("bucket", bucket).trim();
        }
        if ("qupload".equals(process) && "file".equals(entryParam.getValue("parse", "file")) && !"terminal".equals(source)) {
            isSelfUpload = true;
            parse = "file"; // 修正 parse 的默认值
            String directories = entryParam.getValue("directories", null);
            setPathConfigMap(entryParam.getValue("directory-config", ""), directories, false, true);
        }
    }

    private void setPrivateType() throws IOException {
        privateType = entryParam.getValue("private", "").trim();
        if ("".equals(privateType) || !ProcessUtils.canPrivateToNext(process)) return;
        switch (privateType) {
            case "qiniu":
                if (isStorageSource) {
                    if (!"qiniu".equals(source)) {
                        throw new IOException("the privateType: " + privateType + " can not match source: " + source);
                    }
                } else {
                    setQiniuAuthKey();
                }
                break;
            case "tencent":
                if (isStorageSource) {
                    if (!"tencent".equals(source)) {
                        throw new IOException("the privateType: " + privateType + " can not match source: " + source);
                    }
                } else {
                    setTencentAuthKey();
                }
                break;
            case "aliyun":
                if (isStorageSource) {
                    if (!"aliyun".equals(source)) {
                        throw new IOException("the privateType: " + privateType + " can not match source: " + source);
                    }
                } else {
                    setAliyunAuthKey();
                }
                break;
            case "aws":
            case "s3":
                if (isStorageSource) {
                    if (!"s3".equals(source)) {
                        throw new IOException("the privateType: " + privateType + " can not match source: " + source);
                    }
                } else {
                    setS3AuthKey();
                }
                break;
            case "huawei":
                if (isStorageSource) {
                    if (!"huawei".equals(source)) {
                        throw new IOException("the privateType: " + privateType + " can not match source: " + source);
                    }
                } else {
                    setHuaweiAuthKey();
                }
                break;
            case "baidu":
                if (isStorageSource) {
                    if (!"baidu".equals(source)) {
                        throw new IOException("the privateType: " + privateType + " can not match source: " + source);
                    }
                } else {
                    setBaiduAuthKey();
                }
                break;
            default: throw new IOException("unsupported private-type: " + privateType);
        }
    }

    private void fromProcedureLog(String logFile, boolean withMarker, boolean withEnd) throws IOException {
        String lastLine = FileUtils.lastLineOfFile(logFile);
        if (lastLine != null && !"".equals(lastLine)) {
            try {
                parseConfigMapFromJson(JsonUtils.toJsonObject(lastLine), withMarker, withEnd);
            } catch (Exception e) {
                File file = new File(logFile);
                FileReader fileReader = new FileReader(file);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                int index;
                String line;
                String value;
                Map<String, String> map = new HashMap<>();
                while ((line = bufferedReader.readLine()) != null) {
                    index = line.indexOf("-|-");
                    if (index < 0) {
                        try {
                            parseConfigMapFromJson(JsonUtils.toJsonObject(line), withMarker, withEnd);
                            return;
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    } else {
                        map.put(line.substring(0, index), line.substring(index));
                    }
                }
                Map<String, String> configMap;
                for (String key : map.keySet()) {
                    value = map.get(key);
                    if (!"".equals(value)) {
                        try {
                            configMap = JsonUtils.fromJson(value, map.getClass());
                        } catch (Exception e1) {
                            e1.printStackTrace();
                            continue;
                        }
                        pathConfigMap.put(key, configMap);
                    }
                }
                try {
                    bufferedReader.close();
                    fileReader.close();
                } catch (IOException ioe) {
                    bufferedReader = null;
                    fileReader = null;
                }
            }
        }
    }

    private void parseConfigMapFromJson(JsonObject jsonObject, boolean withMarker, boolean withEnd) throws IOException {
        JsonObject jsonCfg;
        JsonElement markerElement;
        JsonElement startElement;
        JsonElement endElement;
        for (String key : jsonObject.keySet()) {
            Map<String, String> startAndEnd = new HashMap<>();
//                if ("".equals(prefix)) throw new IOException("prefix (prefixes config's element key) can't be empty.");
            JsonElement json = jsonObject.get(key);
            if (json == null || json instanceof JsonNull) {
                pathConfigMap.put(key, startAndEnd);
                continue;
            }
//            if (withMarker || withEnd) {
                if (!(json instanceof JsonObject)) throw new IOException("the value of key: " + key + " must be json.");
                jsonCfg = json.getAsJsonObject();
                if (withMarker) {
                    markerElement = jsonCfg.get("marker");
                    if (markerElement != null && !(markerElement instanceof JsonNull)) {
                        startAndEnd.put("marker", markerElement.getAsString());
                    }
                }
                startElement = jsonCfg.get("start");
                if (startElement != null && !(startElement instanceof JsonNull)) {
                    startAndEnd.put("start", startElement.getAsString());
                }
                if (withEnd) {
                    endElement = jsonCfg.get("end");
                    if (endElement != null && !(endElement instanceof JsonNull)) {
                        startAndEnd.put("end", endElement.getAsString());
                    }
                }
//            } else {
//                startAndEnd.put("start", json.getAsString());
//            }
            pathConfigMap.put(key, startAndEnd);
        }
    }

    private void setPathConfigMap(String jsonConfigPath, String subPaths, boolean withMarker, boolean withEnd) throws Exception {
        pathConfigMap = new HashMap<>();
        if (logFilepath == null || "".equals(logFilepath)) {
            if (jsonConfigPath != null && !"".equals(jsonConfigPath)) {
                JsonFile jsonFile = new JsonFile(jsonConfigPath);
                parseConfigMapFromJson(jsonFile.getJsonObject(), withMarker, withEnd);
            } else if (subPaths != null && !"".equals(subPaths)) {
                String[] subPathList = ParamsUtils.escapeSplit(subPaths);
                for (String subPath : subPathList) pathConfigMap.put(subPath, new HashMap<>());
            }
        } else {
            if (jsonConfigPath != null && !"".equals(jsonConfigPath)) {
                throw new IOException("log and uris can not be used together, please remove prefixes/files/directories if you want use breakpoint with log.");
            } else if (subPaths != null && !"".equals(subPaths)) {
                throw new IOException("log and json config can not be used together, please remove config path if you want use breakpoint with log.");
            } else {
                fromProcedureLog(logFilepath, withMarker, withEnd);
            }
        }
    }

    private void setPrefixLeft(String prefixLeft) throws IOException {
        ParamsUtils.checked(prefixLeft, "prefix-left", "(true|false)");
        this.prefixLeft = Boolean.parseBoolean(prefixLeft);
    }

    private void setPrefixRight(String prefixRight) throws IOException {
        ParamsUtils.checked(prefixRight, "prefix-right", "(true|false)");
        this.prefixRight = Boolean.parseBoolean(prefixRight);
    }

    public String[] splitDateScale(String dateScale) throws IOException {
        String[] scale;
        if (dateScale != null && !"".equals(dateScale)) {
            // 设置的 dateScale 格式应该为 [yyyy-MM-dd HH:mm:ss,yyyy-MM-dd HH:mm:ss]
            if (dateScale.startsWith("[") && dateScale.endsWith("]")) {
                scale = dateScale.substring(1, dateScale.length() - 1).split(",");
            } else if (dateScale.startsWith("[") || dateScale.endsWith("]")) {
                throw new IOException("please check your date scale, set it as \"[<date1>,<date2>]\".");
            } else {
                scale = dateScale.split(",");
            }
        } else {
            scale = new String[]{null, null};
        }
        if (scale.length <= 1) {
            throw new IOException("please set start and end date, if no start please set is as \"[0,<date>]\", or " +
                    "no end please set it as \"[<date>,now/max]\"");
        }
        return scale;
    }

    public LocalDateTime checkedDatetime(String datetime) throws Exception {
        LocalDateTime dateTime;
        if (datetime == null) {
            return null;
        } else {
            datetime = datetime.trim();
        }
        if (datetime.matches("(|0)")) {
            dateTime = LocalDateTime.MIN;
        } else if (datetime.equals("now")) {
            dateTime = LocalDateTime.now();
        } else if (datetime.equals("max")) {
            dateTime = LocalDateTime.MAX;
        } else if (datetime.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
            dateTime = LocalDateTime.parse(datetime.replace(" ", "T"));
        } else if (datetime.matches("\\d{4}-\\d{2}-\\d{2}")) {
            dateTime = LocalDateTime.parse(datetime + "T00:00:00");
        } else {
            throw new IOException("please check your datetime string format, set it as \"yyyy-MM-dd HH:mm:ss\".");
        }
        return dateTime;
    }

    private void setBaseFilter() throws Exception {
        String keyPrefix = entryParam.getValue("f-prefix", "");
        String keySuffix = entryParam.getValue("f-suffix", "");
        String keyInner = entryParam.getValue("f-inner", "");
        String keyRegex = entryParam.getValue("f-regex", "");
        String mimeType = entryParam.getValue("f-mime", "").trim();
        String antiKeyPrefix = entryParam.getValue("f-anti-prefix", "");
        String antiKeySuffix = entryParam.getValue("f-anti-suffix", "");
        String antiKeyInner = entryParam.getValue("f-anti-inner", "");
        String antiKeyRegex = entryParam.getValue("f-anti-regex", "");
        String antiMimeType = entryParam.getValue("f-anti-mime", "").trim();
        String[] dateScale = splitDateScale(entryParam.getValue("f-date-scale", "").trim());
        LocalDateTime putTimeMin = checkedDatetime(dateScale[0]);
        LocalDateTime putTimeMax = checkedDatetime(dateScale[1]);
        if (putTimeMin != null && putTimeMax != null && putTimeMax.compareTo(putTimeMin) <= 0) {
            throw new IOException("please set date scale to make first as start date, second as end date, <date1> " +
                    "should earlier then <date2>.");
        }
        String type = entryParam.getValue("f-type", "").trim();
        String status = entryParam.getValue("f-status", "").trim();
        if (!"".equals(status)) ParamsUtils.checked(status, "f-status", "[01]");

        List<String> keyPrefixList = Arrays.asList(ParamsUtils.escapeSplit(keyPrefix));
        List<String> keySuffixList = Arrays.asList(ParamsUtils.escapeSplit(keySuffix));
        List<String> keyInnerList = Arrays.asList(ParamsUtils.escapeSplit(keyInner));
        List<String> keyRegexList = Arrays.asList(ParamsUtils.escapeSplit(keyRegex));
        List<String> mimeTypeList = Arrays.asList(ParamsUtils.escapeSplit(mimeType));
        List<String> antiKeyPrefixList = Arrays.asList(ParamsUtils.escapeSplit(antiKeyPrefix));
        List<String> antiKeySuffixList = Arrays.asList(ParamsUtils.escapeSplit(antiKeySuffix));
        List<String> antiKeyInnerList = Arrays.asList(ParamsUtils.escapeSplit(antiKeyInner));
        List<String> antiKeyRegexList = Arrays.asList(ParamsUtils.escapeSplit(antiKeyRegex));
        List<String> antiMimeTypeList = Arrays.asList(ParamsUtils.escapeSplit(antiMimeType));
        List<String> typeList = Arrays.asList(ParamsUtils.escapeSplit(type));

        try {
            baseFilter = new BaseFilter<Map<String, String>>(keyPrefixList, keySuffixList, keyInnerList, keyRegexList,
                    antiKeyPrefixList, antiKeySuffixList, antiKeyInnerList, antiKeyRegexList, mimeTypeList, antiMimeTypeList,
                    putTimeMin, putTimeMax, typeList, status) {
                @Override
                protected String valueFrom(Map<String, String> item, String key) {
                    return item.get(key);
                }
            };
        } catch (IOException e) {
            baseFilter = null;
        }
    }

    private void setSeniorFilter() throws IOException {
        String checkType = entryParam.getValue("f-check", "").trim();
        ParamsUtils.checked(checkType, "f-check", "(|ext-mime)");
        String checkConfig = entryParam.getValue("f-check-config", "");
        String checkRewrite = entryParam.getValue("f-check-rewrite", "false").trim();
        ParamsUtils.checked(checkRewrite, "f-check-rewrite", "(true|false)");
        try {
            seniorFilter = new SeniorFilter<Map<String, String>>(checkType, checkConfig, Boolean.parseBoolean(checkRewrite)) {
                @Override
                protected String valueFrom(Map<String, String> item, String key) {
                    return item.get(key);
                }
            };
        } catch (Exception e) {
            seniorFilter = null;
        }
    }

    private void setIndex(String index, String indexName) throws IOException {
        if (indexMap.containsKey(index)) {
            throw new IOException("index: " + index + " is already used by \"" + indexMap.get(index) + "-index=" + index + "\"");
        }
        if (index != null && !"".equals(index) && !"-1".equals(index)) {
            if ("tab".equals(parse) || "csv".equals(parse)) {
                if (index.matches("\\d+")) {
                    indexMap.put(index, indexName);
                } else {
                    throw new IOException("incorrect " + indexName + "-index: " + index + ", it should be a number.");
                }
            } else if (parse == null || "json".equals(parse) || "".equals(parse)
                    || "object".equals(parse) || "file".equals(parse)) {
                indexMap.put(index, indexName);
            } else {
                throw new IOException("the parse type: " + parse + " is unsupported now.");
            }
        }
    }

    private void setIndexes(List<String> keys, String indexes, boolean fieldIndex) throws IOException {
        if (indexes.startsWith("pre-")) {
            String num = indexes.substring(4);
            if (num.matches("\\d+")) {
                int number = Integer.parseInt(num);
                if (number < 0) {
                    throw new IOException("pre size can not be smaller than zero.");
                } else if (keys.size() >= number) {
                    for (int i = 0; i < number; i++) setIndex(fieldIndex ? keys.get(i) : String.valueOf(i), keys.get(i));
                } else {
                    throw new IOException("the indexes are out of default fields' size, default fields are: " + keys);
                }
            } else {
                throw new IOException("\"pre-indexes\" must use a number like \"indexes=pre-3\"");
            }
        } else if (indexes.startsWith("[") && indexes.endsWith("]")) {
            indexes = indexes.substring(1, indexes.length() - 1);
            String[] indexList = ParamsUtils.escapeSplit(indexes, false);
            for (int i = 0; i < indexList.length; i++) {
                if (indexList[i].matches(".+:.+")) {
                    String[] keyIndex = ParamsUtils.escapeSplit(indexList[i], ':');
                    if (keyIndex.length != 2) throw new IOException("incorrect key:index pattern: " + indexList[i]);
                    setIndex(keyIndex[1], keyIndex[0]);
                } else {
                    if (i >= keys.size()) {
                        throw new IOException("the indexes are out of default fields' size, default fields are: " + keys);
                    }
                    setIndex(indexList[i], keys.get(i));
                }
            }
        } else if (indexes.startsWith("[") || indexes.endsWith("]")) {
            throw new IOException("please check your indexes, set it as \"[key1:index1,key2:index2,...]\".");
        } else if (!"".equals(indexes)) {
            String[] indexList = ParamsUtils.escapeSplit(indexes);
            for (int i = 0; i < indexList.length; i++) {
                if ("timestamp".equals(indexList[i])) {
                    setIndex(indexList[i], "timestamp");
                    keys.add(i, "timestamp");
                } else {
                    if (i >= keys.size()) {
                        throw new IOException("the indexes are out of default fields' size, default fields are: " + keys);
                    }
                    setIndex(indexList[i], keys.get(i));
                }
            }
        }
    }

    private void setIndexMap() throws IOException {
        indexMap = new HashMap<>();
        boolean fieldIndex = parse == null || "json".equals(parse) || "".equals(parse) || "object".equals(parse) || "file".equals(parse);
        if ("domainsofbucket".equals(process)) {
            indexMap.put(fieldIndex ? "bucket" : "0", "bucket");
            return;
        }
        int fieldsMode = 0;
        List<String> keys = new ArrayList<>(9);
        String indexes = entryParam.getValue("indexes", "").trim();
        boolean useDefault = "".equals(indexes);
        boolean zeroDefault = false;
        if (isSelfUpload || "file".equals(parse)) { // 自上传和导出文件信息都是 local source，需要定义单独的默认 keys
            if (isStorageSource) throw new IOException("self upload only support local file source.");
            fieldsMode = 1; // file 的 parse 方式，字段类型为 field，所以顺序无所谓，mime 和 etag 涉及计算，所以将优先级放在后面
            keys.add("key");
            keys.add("parent");
            keys.add("size");
            keys.add("datetime");
            keys.add("mime");
            keys.add("etag");
            if (useDefault) {
                saveFormat = entryParam.getValue("save-format", "tab").trim();
                if ("yaml".equals(saveFormat)) indexes = "pre-2";
                else indexes = "pre-1";
            }
//            else if (!indexes.startsWith("pre-")) {
//                throw new IOException("upload from path only support \"pre-indexes\" like \"indexes=pre-3\".");
//            }
            setIndexes(keys, indexes, fieldIndex);
            setIndex(entryParam.getValue("filepath-index", "filepath").trim(), "filepath");
        } else { // 存储数据源的 keys 定义
            keys.addAll(ConvertingUtils.defaultFileFields);
            if ("upyun".equals(source)) {
                fieldsMode = 1;
                keys.remove(ConvertingUtils.defaultEtagField);
                keys.remove(ConvertingUtils.defaultTypeField);
                keys.remove(ConvertingUtils.defaultStatusField);
                keys.remove(ConvertingUtils.defaultMd5Field);
                keys.remove(ConvertingUtils.defaultOwnerField);
            } else if ("huawei".equals(source)) {
                fieldsMode = 2;
                keys.remove(ConvertingUtils.defaultStatusField);
            } else if (isStorageSource && !"qiniu".equals(source)) {
                fieldsMode = 3;
                keys.remove(ConvertingUtils.defaultMimeField);
                keys.remove(ConvertingUtils.defaultStatusField);
                keys.remove(ConvertingUtils.defaultMd5Field);
            }
            if (useDefault && isStorageSource)  {
                for (String key : keys) indexMap.put(key, key);
            } else {
                setIndexes(keys, indexes, fieldIndex);
            }
            if (ProcessUtils.needFilepath(process) || "file".equals(parse)) {
                // 由于 filepath 可能依据 parent 和文件名生成，故列表第一列亦可能为文件名，所以要确保没有设置 parent 才能给默认的 filepath-index=0
                String filepathIndex = entryParam.getValue("filepath-index", "").trim();
                if ("".equals(filepathIndex)) {
                    zeroDefault = true;
                    if (entryParam.getValue("parent-path", null) == null) {
                        indexMap.put(fieldIndex ? "filepath" : "0", "filepath");
                    } else {
                        indexMap.put(fieldIndex ? "key" : "0", "key");
                    }
                } else {
                    indexMap.put(filepathIndex, "filepath");
                }
            } else if (ProcessUtils.needUrl(process)) {
                // 由于 url 可能依据 domain 和文件名生成，故列表第一列亦可能为文件名，所以要确保没有设置 domain 才能给默认的 url-index=0
                String urlIndex = entryParam.getValue("url-index", "").trim();
                if ("".equals(urlIndex)) {
                    zeroDefault = true;
                    if (entryParam.getValue("domain", null) == null) {
                        indexMap.put(fieldIndex ? "url" : "0", "url");
                    } else {
                        indexMap.put(fieldIndex ? "key" : "0", "key");
                    }
                } else {
                    indexMap.put(urlIndex, "url");
                }
            } else if (ProcessUtils.needId(process)) {
                String idIndex = entryParam.getValue("id-index", "").trim();
                if ("".equals(idIndex)) {
                    zeroDefault = true;
                    indexMap.put(fieldIndex ? "id" : "0", "id");
                } else {
                    indexMap.put(idIndex, "id");
                }
            } else {
                indexMap.put(fieldIndex ? "key" : "0", "key");
                if (ProcessUtils.needToKey(process))
                    // move/copy/rename 等操作不设置默认 toKey，因为大部分情况是增加或删除前缀，需要优先考虑，查看 processor 的实现
                    setIndex(entryParam.getValue("toKey-index", "").trim(), "toKey");
                if (ProcessUtils.needFops(process))
                    setIndex(entryParam.getValue("fops-index", fieldIndex ? "fops" : "1").trim(), "fops");
                if (ProcessUtils.needAvinfo(process))
                    setIndex(entryParam.getValue("avinfo-index", fieldIndex ? "avinfo" : "1").trim(), "avinfo");
            }
        }

        if (baseFilter != null) {
            if (baseFilter.checkKeyCon() && !indexMap.containsValue("key")) {
                if (useDefault) {
                    if (zeroDefault) indexMap.put(fieldIndex ? "key" : "0", "key");
                    else setIndex(fieldIndex ? "key" : "0", "key");
                } else {
                    throw new IOException("f-[x] about key filter for file key must get the key's index in indexes settings.");
                }
            }
            if (baseFilter.checkDatetimeCon() && !indexMap.containsValue("datetime")) {
                if (useDefault) {
                    indexMap.put(fieldIndex ? "datetime" : "3", "datetime");
                } else {
                    throw new IOException("f-date-scale filter must get the datetime's index in indexes settings.");
                }
            }
            if (baseFilter.checkMimeTypeCon() && !indexMap.containsValue("mime")) {
                if (useDefault) {
                    if (fieldsMode != 3) {
                        indexMap.put(fieldIndex ? "mime" : "4", "mime");
                    }
                } else {
                    throw new IOException("f-mime filter must get the mime's index in indexes settings.");
                }
            }
            if (baseFilter.checkTypeCon() && !indexMap.containsValue("type")) {
                if (useDefault) {
                    if (fieldsMode != 1) {
                        indexMap.put(fieldIndex ? "type" : "5", "type");
                    }
                } else {
                    throw new IOException("f-type filter must get the type's index in indexes settings.");
                }
            }
            if (baseFilter.checkStatusCon() && !indexMap.containsValue("status")) {
                if (useDefault) {
                    if (fieldsMode == 0) {
                        indexMap.put(fieldIndex ? "status" : "6", "status");
                    }
                } else {
                    throw new IOException("f-status filter must get the status's index in indexes settings.");
                }
            }
        }
        if (seniorFilter != null) {
            if (seniorFilter.checkExtMime()) {
                if (!indexMap.containsValue("key")) {
                    if (useDefault) {
                        if (zeroDefault) indexMap.put(fieldIndex ? "key" : "0", "key");
                        else setIndex(fieldIndex ? "key" : "0", "key");
                    } else {
                        throw new IOException("f-check=ext-mime filter must get the key's index in indexes settings.");
                    }
                }
                if (!indexMap.containsValue("mime")) {
                    if (useDefault) {
                        if (fieldsMode != 3) {
                            indexMap.put(fieldIndex ? "mime" : "4", "mime");
                        }
                    } else {
                        throw new IOException("f-check=ext-mime filter must get the mime's index in indexes settings.");
                    }
                }
            }
        }
    }

    private void checkFilterForProcess() throws IOException {
        if ((baseFilter == null || !baseFilter.checkMimeTypeCon()) && indexMap.containsValue("mime")) {
            if ("imagecensor".equals(process)) {
                throw new IOException("please set \"f-mime\" like \"f-mime=image/\" for \"process=" + process
                        + "\", and recommend you to set \"f-strict-error\" as true to record unmatched lines.");
            } else if ("videocensor".equals(process) || "avinfo".equals(process)) {
                throw new IOException("please set \"f-mime\" like \"f-mime=video/\" for \"process=" + process
                        + "\", and recommend you to set \"f-strict-error\" as true to record unmatched lines.");
            }
        }
        if ("type".equals(process) && (baseFilter == null || !baseFilter.checkTypeCon()) && indexMap.containsValue("type")) {
            throw new IOException("please set \"f-type\" like \"f-type=0\" for \"process=type\" if you want to set target "
                    + "files \"type=1\", or \"type=0\" with \"f-type=1\", and recommend you to set "
                    + "\"f-strict-error=true\" to record unmatched lines.");
        }
        if ("status".equals(process) && (baseFilter == null || !baseFilter.checkStatusCon()) && indexMap.containsValue("status")) {
            throw new IOException("please set \"f-status\" like \"f-status=0\" for \"process=status\" if you want to set "
                    + "target files \"status=1\", or \"status=0\" with \"f-status=1\", and recommend you to set "
                    + "\"f-strict-error=true\" to record unmatched lines.");
        }
    }

    private void setUnitLen() throws IOException {
        String unitLen = entryParam.getValue("unit-len", "-1").trim();
        if (unitLen.startsWith("-")) {
            if (isSelfUpload) this.unitLen = 20;
            else if ("qiniu".equals(source)) this.unitLen = 10000;
            else this.unitLen = 1000;
        } else {
            ParamsUtils.checked(unitLen, "unit-len", "\\d+");
            this.unitLen = Integer.parseInt(unitLen);
            if (isSelfUpload && this.unitLen > 100) {
                throw new IOException("file upload shouldn't have too big unit-len, suggest to set unit-len smaller than 100.");
            }
        }
    }

    private void setThreads() throws IOException {
        // 刷新预取操作存在 qps 限制，因此不支持自定义线程数
        if ("cdnrefresh".equals(process) || "cdnprefetch".equals(process)) {
            this.threads = 1;
        } else {
            String threads = entryParam.getValue("threads", "50").trim();
            ParamsUtils.checked(threads, "threads", "[1-9]\\d*");
            this.threads = Integer.parseInt(threads);
        }
    }

    private void setBatchSize() throws IOException {
        String batchSize = entryParam.getValue("batch-size", "-1").trim();
        if (batchSize.startsWith("-")) {
            if (ProcessUtils.canBatch(process)) {
                if ("cdnrefresh".equals(process)) {
                    if ("true".equals(entryParam.getValue("is-dir", "false").trim())) this.batchSize = 10;
                    else this.batchSize = 30;
                } else if ("cdnprefetch".equals(process)) {
                    this.batchSize = 30;
                } else if ("stat".equals(process) || "refreshquery".equals(process) || "prefetchquery".equals(process)) {
                    this.batchSize = 100;
                } else {
                    this.batchSize = 1000;
                }
            } else {
                this.batchSize = 0;
            }
        } else {
            ParamsUtils.checked(batchSize, "batch-size", "\\d+");
            this.batchSize = Integer.parseInt(batchSize);
            if ("cdnrefresh".equals(process)) {
                if ("true".equals(entryParam.getValue("is-dir", "false").trim()) && this.batchSize > 10) {
                    throw new IOException("cdn url refresh for dir can not use batchSize more than 10.");
                } else if (this.batchSize > 60) {
                    throw new IOException("cdn url refresh can not use batchSize more than 60.");
                }
            } else if ("cdnprefetch".equals(process) && this.batchSize > 60) {
                throw new IOException("cdn url prefetch can not use batchSize more than 60.");
            } else if (this.batchSize > 100 && ("refreshquery".equals(process) || "prefetchquery".equals(process))) {
                throw new IOException("cdn refresh or prefetch query can not use batchSize more than 100.");
            }
        }
    }

    private void setRetryTimes() throws IOException {
        String retryTimes = entryParam.getValue("retry-times", "5").trim();
        ParamsUtils.checked(retryTimes, "retry-times", "\\d+");
        this.retryTimes = Integer.parseInt(retryTimes);
    }

    private void setSaveTotal() throws IOException {
        String saveTotal = entryParam.getValue("save-total", "").trim();
        if ("".equals(saveTotal)) {
            if ((process != null && !"".equals(process))) {
                this.saveTotal = "delete".equals(process);
            } else {
                this.saveTotal = baseFilter == null && seniorFilter == null;
            }
//            if (isStorageSource) {
//                this.saveTotal = true;
//（2）云存储数据源时如果无 process 则 saveTotal 默认为 true，如果存在 process 则 saveTotal 默认为 false。
//                if (process == null || "".equals(process)) {
//                    this.saveTotal = true;
//                } else {
//                    if (baseFilter != null || seniorFilter != null) this.saveTotal = true;
//                    else this.saveTotal = false;
//                }
//            } else {
//                if (isSelfUpload) { // 自上传时将上传路径的路径等信息做下保存
//                    this.saveTotal = true;
//                }
//                else
//                if ((process != null && !"".equals(process)) || baseFilter != null || seniorFilter != null) {
//                    this.saveTotal = false;
//                } else {
//                    this.saveTotal = true;
//                }
//            }
        } else {
            ParamsUtils.checked(saveTotal, "save-total", "(true|false)");
            this.saveTotal = Boolean.parseBoolean(saveTotal);
        }
    }

    private void setSavePath() throws IOException {
        savePath = entryParam.getValue("save-path", "").trim();
        if ("".equals(savePath)) {
            savePath = "local".equals(source) ? (path.endsWith(FileUtils.pathSeparator) ?
                    path.substring(0, path.length() - 1) : path) + "-result" : (bucket == null ? "" : bucket);
            savePath = savePath.substring(savePath.lastIndexOf(FileUtils.pathSeparator) + 1);
        } else {
            savePath = FileUtils.convertToRealPath(savePath);
        }
        if (CloudApiUtils.isFileSource(source) && FileUtils.convertToRealPath(path).equals(FileUtils.convertToRealPath(savePath))) {
            throw new IOException(String.format("the save-path \"%s\" can not be same as path.", savePath));
        } else {
            File file = new File(savePath);
            File[] files = file.listFiles();
            boolean isOk;
            if (files != null && files.length > 0) {
                if ("".equals(process)) {
                    isOk = Arrays.asList(files).parallelStream().anyMatch(f -> f.getName().startsWith(source));
                } else {
                    isOk = Arrays.asList(files).parallelStream().anyMatch(f -> f.getName().startsWith(source) || f.getName().startsWith(process));
                }
                if (isOk) {
                    if (pathConfigMap == null || pathConfigMap.size() <= 0) {
                        throw new IOException(String.format("please change the save-path \"%s\", " +
                                "because there are remained files from last job, not to cover them.", savePath));
                    }
                }
//                取消目录非空校验，因为结果文件的前缀也可以区分
//                else {
//                    throw new IOException(String.format("please change save-path \"%s\" because it's not empty.", savePath));
//                }
            }
        }
    }

    private void setSaveSeparator() {
        String separator = entryParam.getValue("save-separator", "");
        if (separator == null || separator.isEmpty()) {
            if ("tab".equals(saveFormat)) this.saveSeparator = "\t";
            else if ("csv".equals(saveFormat)) this.saveSeparator = ",";
            else this.saveSeparator = " ";
        } else {
            this.saveSeparator = separator;
        }
    }

    private void setRmFields() throws IOException {
        String param = entryParam.getValue("rm-fields", "").trim();
        if ("".equals(param)) {
            rmFields = null;
        } else {
            String[] fields = ParamsUtils.escapeSplit(param);
            rmFields = new ArrayList<>(fields.length);
            Collections.addAll(rmFields, fields);
        }
    }

    private void setPfopConfigs() throws IOException {
        String cmd = entryParam.getValue("cmd", "").trim();
        if (!"".equals(cmd)) {
            JsonObject pfopJson = new JsonObject();
            pfopJson.addProperty("cmd", cmd);
            String saveas = entryParam.getValue("saveas");
            pfopJson.addProperty("saveas", saveas);
            String scale = entryParam.getValue("scale", "").trim();
            if ("pfopcmd".equals(process) && !"".equals(scale)) {
                if (!scale.matches("\\[.*]")) throw new IOException("correct \"scale\" parameter should " +
                        "like \"[num1,num2]\"");
                String[] scales = scale.substring(1, scale.length() - 1).split(",");
                JsonArray jsonArray = new JsonArray();
                if (scales.length > 1) {
                    jsonArray.add(scales[0]);
                    jsonArray.add(scales[1]);
                } else {
                    jsonArray.add(Integer.parseInt(scales[0]));
                    jsonArray.add(Integer.MAX_VALUE);
                }
                pfopJson.add("scale", jsonArray);
            }
            pfopConfigs = new ArrayList<JsonObject>(){{
                add(pfopJson);
            }};
        }
    }

    private void setStartAndPause() throws Exception {
        String startTime = entryParam.getValue("start-time", null);
        if (startTime != null) startDateTime = checkedDatetime(startTime);
        String delay = entryParam.getValue("pause-delay", null);
        if (startTime != null) {
            ParamsUtils.checked(delay, "pause-delay", "\\d+");
            pauseDelay = Long.parseLong(delay);
        }
        String duration = entryParam.getValue("pause-duration", null);
        if (startTime != null) {
            ParamsUtils.checked(duration, "pause-duration", "\\d+");
            pauseDuration = Long.parseLong(duration);
        }
    }

    public void setEntryParam(IEntryParam entryParam) {
        this.entryParam = entryParam;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public void setHttpsForConfigEnabled(boolean httpsForConfigEnabled) {
        this.httpsForConfigEnabled = httpsForConfigEnabled;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setQiniuAccessKey(String qiniuAccessKey) {
        this.qiniuAccessKey = qiniuAccessKey;
    }

    public void setQiniuSecretKey(String qiniuSecretKey) {
        this.qiniuSecretKey = qiniuSecretKey;
    }

    public void setTencentSecretId(String tencentSecretId) {
        this.tencentSecretId = tencentSecretId;
    }

    public void setTencentSecretKey(String tencentSecretKey) {
        this.tencentSecretKey = tencentSecretKey;
    }

    public void setAliyunAccessId(String aliyunAccessId) {
        this.aliyunAccessId = aliyunAccessId;
    }

    public void setAliyunAccessSecret(String aliyunAccessSecret) {
        this.aliyunAccessSecret = aliyunAccessSecret;
    }

    public void setUpyunUsername(String upyunUsername) {
        this.upyunUsername = upyunUsername;
    }

    public void setUpyunPassword(String upyunPassword) {
        this.upyunPassword = upyunPassword;
    }

    public void setS3AccessId(String s3AccessId) {
        this.s3AccessId = s3AccessId;
    }

    public void setS3SecretKey(String s3SecretKey) {
        this.s3SecretKey = s3SecretKey;
    }

    public void setHuaweiAccessId(String huaweiAccessId) {
        this.huaweiAccessId = huaweiAccessId;
    }

    public void setHuaweiSecretKey(String huaweiSecretKey) {
        this.huaweiSecretKey = huaweiSecretKey;
    }

    public void setBaiduAccessId(String baiduAccessId) {
        this.baiduAccessId = baiduAccessId;
    }

    public void setBaiduSecretKey(String baiduSecretKey) {
        this.baiduSecretKey = baiduSecretKey;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public void setPathConfigMap(Map<String, Map<String, String>> pathConfigMap) {
        this.pathConfigMap = pathConfigMap;
    }

    public void setAntiPrefixes(List<String> antiPrefixes) {
        this.antiPrefixes = antiPrefixes;
    }

    public void setPrefixLeft(boolean prefixLeft) {
        this.prefixLeft = prefixLeft;
    }

    public void setPrefixRight(boolean prefixRight) {
        this.prefixRight = prefixRight;
    }

    public void setParse(String parse) {
        this.parse = parse;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public void setKeepDir(boolean keepDir) {
        this.keepDir = keepDir;
    }

    public void setAddKeyPrefix(String addKeyPrefix) {
        this.addKeyPrefix = addKeyPrefix;
    }

    public void setRmKeyPrefix(String rmKeyPrefix) {
        this.rmKeyPrefix = rmKeyPrefix;
    }

    public void setProcess(String process) {
        this.process = process;
    }

    public void setPrivateType(String privateType) {
        this.privateType = privateType;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public void setBaseFilter(BaseFilter<Map<String, String>> baseFilter) {
        this.baseFilter = baseFilter;
    }

    public void setSeniorFilter(SeniorFilter<Map<String, String>> seniorFilter) {
        this.seniorFilter = seniorFilter;
    }

    public void setIndexMap(HashMap<String, String> indexMap) {
        this.indexMap = indexMap;
    }

    public void setUnitLen(int unitLen) {
        this.unitLen = unitLen;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

    public void setSaveTotal(boolean saveTotal) {
        this.saveTotal = saveTotal;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public void setSaveTag(String saveTag) {
        this.saveTag = saveTag;
    }

    public void setSaveFormat(String saveFormat) {
        this.saveFormat = saveFormat;
    }

    public void setRmFields(List<String> rmFields) {
        this.rmFields = rmFields;
    }

    public void setMapLine(Map<String, String> mapLine) {
        this.mapLine = mapLine;
    }

    public void setPfopConfigs(List<JsonObject> pfopConfigs) {
        this.pfopConfigs = pfopConfigs;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public void setPauseDelay(long pauseDelay) {
        this.pauseDelay = pauseDelay;
    }

    public void setPauseDuration(long pauseDuration) {
        this.pauseDuration = pauseDuration;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public int getRequestTimeout() {
        return requestTimeout;
    }

    public boolean isHttpsForConfigEnabled() {
        return httpsForConfigEnabled;
    }

    public String getPath() {
        return path;
    }

    public String getSource() {
        return source;
    }

    public String getQiniuAccessKey() {
        return qiniuAccessKey;
    }

    public String getQiniuSecretKey() {
        return qiniuSecretKey;
    }

    public String getTencentSecretId() {
        return tencentSecretId;
    }

    public String getTencentSecretKey() {
        return tencentSecretKey;
    }

    public String getAliyunAccessId() {
        return aliyunAccessId;
    }

    public String getAliyunAccessSecret() {
        return aliyunAccessSecret;
    }

    public String getUpyunUsername() {
        return upyunUsername;
    }

    public String getUpyunPassword() {
        return upyunPassword;
    }

    public String getS3AccessId() {
        return s3AccessId;
    }

    public String getS3SecretKey() {
        return s3SecretKey;
    }

    public String getHuaweiAccessId() {
        return huaweiAccessId;
    }

    public String getHuaweiSecretKey() {
        return huaweiSecretKey;
    }

    public String getBaiduAccessId() {
        return baiduAccessId;
    }

    public String getBaiduSecretKey() {
        return baiduSecretKey;
    }

    public String getBucket() {
        return bucket;
    }

    public Map<String, Map<String, String>> getPathConfigMap() {
        return pathConfigMap;
    }

    public List<String> getAntiPrefixes() {
        return antiPrefixes;
    }

    public boolean getPrefixLeft() {
        return prefixLeft;
    }

    public boolean getPrefixRight() {
        return prefixRight;
    }

    public String getParse() {
        return parse;
    }

    public String getSeparator() {
        return separator;
    }

    public boolean getKeepDir() {
        return keepDir;
    }

    public String getAddKeyPrefix() {
        return addKeyPrefix;
    }

    public String getRmKeyPrefix() {
        return rmKeyPrefix;
    }

    public String getProcess() {
        return process;
    }

    public String getPrivateType() {
        return privateType;
    }

    public String getRegionName() {
        return regionName;
    }

    public BaseFilter<Map<String, String>> getBaseFilter() {
        return baseFilter;
    }

    public SeniorFilter<Map<String, String>> getSeniorFilter() {
        return seniorFilter;
    }

    public Map<String, String> getIndexMap() {
        return indexMap;
    }

    public int getUnitLen() {
        return unitLen;
    }

    public int getThreads() {
        return threads;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public Boolean getSaveTotal() {
        return saveTotal;
    }

    public String getSavePath() {
        return savePath;
    }

    public String getSaveTag() {
        return saveTag;
    }

    public String getSaveFormat() {
        return saveFormat;
    }

    public String getSaveSeparator() {
        return saveSeparator;
    }

    public List<String> getRmFields() {
        return rmFields;
    }

    public Map<String, String> getMapLine() {
        return mapLine;
    }

    public List<JsonObject> getPfopConfigs() {
        return pfopConfigs;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public long getPauseDelay() {
        return pauseDelay;
    }

    public long getPauseDuration() {
        return pauseDuration;
    }

    public boolean isSelfUpload() {
        return isSelfUpload;
    }
}
