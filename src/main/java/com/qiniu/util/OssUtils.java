package com.qiniu.util;

import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.model.OSSObjectSummary;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.COSObjectSummary;
import com.qiniu.common.Constants;
import com.qiniu.common.Zone;
import com.qiniu.sdk.FileItem;
import com.qiniu.storage.model.FileInfo;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Base64.*;

public class OssUtils {

    public static Encoder encoder = java.util.Base64.getEncoder();
    public static Decoder decoder = java.util.Base64.getDecoder();

    public static Map<String, Integer> aliStatus = new HashMap<String, Integer>(){{
        put("UnknownHost", 400); // 错误的 region 等
        put("AccessDenied", 403);// 拒绝访问
        put("BucketAlreadyExists", 409);// 存储空间已经存在
        put("BucketNotEmpty", 409);// 存储空间非空
        put("EntityTooLarge", 400);// 实体过大
        put("EntityTooSmall", 400);// 实体过小
        put("FileGroupTooLarge", 400);// 文件组过大
        put("FilePartNotExist", 400);// 文件分片不存在
        put("FilePartStale", 400);// 文件分片过时
        put("InvalidArgument", 400);// 参数格式错误
        put("InvalidAccessKeyId", 403);// AccessKeyId不存在
        put("InvalidBucketName", 400);// 无效的存储空间名称
        put("InvalidDigest", 400);// 无效的摘要
        put("InvalidObjectName", 400);// 无效的文件名称
        put("InvalidPart", 400);// 无效的分片
        put("InvalidPartOrder", 400);// 无效的分片顺序
        put("InvalidTargetBucketForLogging", 400);// Logging操作中有无效的目标存储空间
        put("InternalError", 500);// OSS内部错误
        put("MalformedXML", 400);// XML格式非法
        put("MethodNotAllowed", 405);// 不支持的方法
        put("MissingArgument", 411);// 缺少参数
        put("MissingContentLength", 411);// 缺少内容长度
        put("NoSuchBucket", 404);// 存储空间不存在
        put("NoSuchKey", 404);// 文件不存在
        put("NoSuchUpload", 404);// 分片上传ID不存在
        put("NotImplemented", 501);// 无法处理的方法
        put("PreconditionFailed", 412);// 预处理错误
        put("RequestTimeTooSkewed", 403);// 客户端本地时间和OSS服务器时间相差超过15分钟
        put("RequestTimeout", 400);// 请求超时
        put("SignatureDoesNotMatch", 403);// 签名错误
        put("InvalidEncryptionAlgorithmError", 400);// 指定的熵编码加密算法错误
    }};

    public static Map<String, Integer> netStatus = new HashMap<String, Integer>(){{
        put("AccessDenied", 403); // Forbidden   权限错误，拒绝访问
        put("BadDigest", 400); // Bad Request 提供的 MD5 值与服务器收到的二进制内容不匹配
        put("BucketAlreadyExist", 409); // Conflict    创建桶时，桶名已存在
        put("BucketAlreadyOwnedByYou", 409); // Conflict    创建桶时，该桶已经属于你，重复创建了
        put("BucketNotEmpty", 409); // Conflict    尝试删的桶非空
        put("EntityTooSmall", 400); // Bad Request 提交的请求小于允许的对象的最小值
        put("EntityTooLarge", 400); // Bad Request 提交的请求大于允许的对象的最大值
        put("IllegalVersioningConfigurationException", 400); // Bad Request 版本号配置无效
        put("IncompleteBody", 400); // Bad Request 上传的数据量小于 HTTP 头中的 Content-Length
        put("InternalError", 500); // Internal Server Error   服务器内部错误，请重试
        put("InvalidAccessKeyId", 403); // Forbidden   AccessKey 找不到匹配的记录
        put("InvalidArgument", 400); // Bad Request 无效参数
        put("InvalidBucketName", 400); // Bad Request 无效桶名称
        put("InvalidDigest", 400); // Bad Request 不是有效的 Content-MD5
        put("InvalidPart", 400); // Bad Request 无效的上传块
        put("InvalidPartOrder", 400); // Bad Request 上传块的顺序有错误
        put("InvalidRange", 416); // Requested Range Not Satisfiable 请求的 Range 不合法
        put("InvalidRequest", 400); // Bad Request 非法请求
        put("InvalidStorageClass", 400); // Bad Request 无效的存储级别
        put("KeyTooLong", 400); // Bad Request Object Key 长度太长
        put("MalformedXML", 400); // Bad Request XML 格式错误
        put("MetadataTooLarge", 400); // Bad Request 元数据过大
        put("MethodNotAllowed", 405); // Method Not Allowed  请求的 HTTP Method 不允许访问
        put("MissingContentLength", 411); // Length Required 缺少 HTTP Header Content-Length
        put("MissingRequestBodyError", 400); // Bad Request 缺少请求体
        put("NoSuchBucket", 404); // Not Found   请求的桶不存在
        put("NoSuchKey", 404); // Not Found   没有这个 key
        put("NoSuchUpload", 404); // Not Found   对应的分块上传不存在
        put("NoSuchVersion", 400); // Bad Request 没有这个版本号
        put("NotImplemented", 501); // Not Implemented 该项功能尚未实现
        put("RequestTimeout", 400); // Bad Request 请求超时
        put("RequestTimeTooSkewed", 403); // Forbidden   请求时间戳和服务器时间戳差距过大
        put("SignatureDoesNotMatch", 403); // Forbidden   请求的签名与服务器计算的签名不符
        put("ServiceUnavailable", 503); // Service Unavailable 服务不可用
        put("TooManyBuckets", 400); // Bad Request 创建了过多的桶
    }};

    public static int AliStatusCode(String error, int Default) {
        return aliStatus.getOrDefault(error, Default);
    }

    public static int NetStatusCode(String error, int Default) {
        return aliStatus.getOrDefault(error, Default);
    }

    public static String getQiniuMarker(FileInfo fileInfo) {
        return getQiniuMarker(fileInfo.key);
    }

    public static String getAliOssMarker(OSSObjectSummary summary) {
        return summary.getKey();
    }

    public static String getTenCosMarker(COSObjectSummary summary) {
        return summary.getKey();
    }

    public static String getUpYunMarker(String bucket, FileItem fileItem) {
        return new String(encoder.encode((bucket + (fileItem.size == 0 ? "/@~" : "/@#") + fileItem.key).getBytes()));
    }

    public static String getQiniuMarker(String key) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("c", 0);
        jsonObject.addProperty("k", key);
        return Base64.encodeToString(JsonUtils.toJson(jsonObject).getBytes(Constants.UTF_8),
                Base64.URL_SAFE | Base64.NO_WRAP);
    }

    public static String getAliOssMarker(String key) {
        return key;
    }

    public static String getTenCosMarker(String key) {
        return key;
    }

//    public static String getUpYunMarker(String bucket, String key) {
//        return new String(encoder.encode((bucket + "/@#" + key).getBytes()));
//    }

    public static String decodeQiniuMarker(String marker) {
        String decodedMarker = new String(Base64.decode(marker, Base64.URL_SAFE | Base64.NO_WRAP));
        JsonObject jsonObject = new JsonParser().parse(decodedMarker).getAsJsonObject();
        return jsonObject.get("k").getAsString();
    }

    public static String decodeAliOssMarker(String marker) {
        return marker;
    }

    public static String decodeTenCosMarker(String marker) {
        return marker;
    }

    public static String decodeUpYunMarker(String bucket, String marker) {
        String keyString = new String(decoder.decode(marker));
        return keyString.substring(bucket.length() + 3);
    }

    public static Zone getQiniuRegion(String regionName) {
        if (regionName == null) return Zone.autoZone();
        switch (regionName) {
            case "z0":
            case "huadong": return Zone.huadong();
            case "z1":
            case "huabei": return Zone.huabei();
            case "z2":
            case "huanan": return Zone.huanan();
            case "na0":
            case "beimei": return Zone.beimei();
            case "as0":
            case "xinjiapo": return Zone.xinjiapo();
            case "qvm-z0":
            case "qvm-huadong": return Zone.qvmHuadong();
            case "qvm-z1":
            case "qvm-huabei": return Zone.qvmHuabei();
            default: return Zone.autoZone();
        }
    }

    public static String getAliOssRegion(String accessKeyId, String accessKeySecret, String bucket) throws IOException {
        CredentialsProvider credentialsProvider = new DefaultCredentialProvider(accessKeyId, accessKeySecret);
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        OSSClient ossClient = new OSSClient("oss-cn-shanghai.aliyuncs.com", credentialsProvider, clientConfiguration);
        try {
            return ossClient.getBucketLocation(bucket);
        } catch (OSSException | ClientException e) {
            throw new IOException(e.getMessage(), e);
        }
//        OSSClient ossClient = new OSSClient("oss.aliyuncs.com", credentialsProvider, clientConfiguration);
//        // 阿里 oss sdk listBuckets 能迭代列举出所有空间
//        List<com.aliyun.oss.model.Bucket> list = ossClient.listBuckets();
//        for (com.aliyun.oss.model.Bucket eachBucket : list) {
//            if (eachBucket.getName().equals(bucket)) return eachBucket.getLocation();
//        }
//        throw new IOException("can not find this bucket.");
    }

    public static String getTenCosRegion(String secretId, String secretKey, String bucket) throws IOException {
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig();
        COSClient cosClient = new COSClient(cred, clientConfig);
        // 腾讯 cos sdk listBuckets 不进行分页列举，账号空间个数上限为 200，可一次性列举完
        List<com.qcloud.cos.model.Bucket> list = cosClient.listBuckets();
        for (com.qcloud.cos.model.Bucket eachBucket : list) {
            if (eachBucket.getName().equals(bucket)) return eachBucket.getLocation();
        }
        throw new IOException("can not find this bucket.");
    }

    private static final String lineSeparator = System.getProperty("line.separator");

    public static String upYunSign(String method, String date, String path, String userName, String password, String md5)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        String sp = "&";
        sb.append(method);
        sb.append(sp);
        sb.append(path);

        sb.append(sp);
        sb.append(date);

        if (md5 != null && md5.length() > 0) {
            sb.append(sp);
            sb.append(md5);
        }
        String raw = sb.toString().trim();
        byte[] hmac;
        try {
            hmac = EncryptUtils.calculateRFC2104HMACRaw(password, raw);
        } catch (Exception e) {
            throw new IOException("calculate SHA1 wrong.");
        }

        if (hmac != null) {
            return "UPYUN " + userName + ":" + EncryptUtils.encodeLines(hmac, 0, hmac.length, 76,
                    lineSeparator).trim();
        }

        return null;
    }
}
