package com.qiniu.util;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qiniu.config.PropertiesFile;
import com.qiniu.sdk.FileItem;
import org.junit.Test;

import java.io.IOException;

public class CloudApiUtilsTest {

    @Test
    public void testGetQiniuRegion() throws IOException {
        PropertiesFile propertiesFile = new PropertiesFile("resources/.application.properties");
        String secretId = propertiesFile.getValue("ak");
        String secretKey = propertiesFile.getValue("sk");
        String bucket = propertiesFile.getValue("bucket");
        String region = CloudApiUtils.getQiniuQosRegion(secretId, secretKey, bucket);
        System.out.println(bucket + "\t" + region);
    }

    @Test
    public void testGetTenCosRegion() throws IOException {
        LogUtils.getLogPath(LogUtils.QSUITS);
        PropertiesFile propertiesFile = new PropertiesFile("resources/.tencent.properties");
        String secretId = propertiesFile.getValue("ten-id");
        String secretKey = propertiesFile.getValue("ten-secret");
        String bucket = propertiesFile.getValue("bucket");
        String region = CloudApiUtils.getTenCosRegion(secretId, secretKey, bucket);
        System.out.println(bucket + "\t" + region);
    }

    @Test
    public void testGetHuaweiObsRegion() throws IOException {
        LogUtils.getLogPath(LogUtils.QSUITS);
        PropertiesFile propertiesFile = new PropertiesFile("resources/.huawei.properties");
        String secretId = propertiesFile.getValue("hua-id");
        String secretKey = propertiesFile.getValue("hua-secret");
        String bucket = propertiesFile.getValue("bucket");
        String region = CloudApiUtils.getHuaweiObsRegion(secretId, secretKey, bucket);
        System.out.println(bucket + "\t" + region);
    }

    @Test
    public void testGetS3Region() throws IOException {
        PropertiesFile propertiesFile = new PropertiesFile("resources/.s3.properties");
        String accessId = propertiesFile.getValue("s3-id");
        String secretKey = propertiesFile.getValue("s3-secret");
        String bucket = propertiesFile.getValue("bucket");
        System.out.println(bucket + "\t" + CloudApiUtils.getS3Region(accessId, secretKey, bucket + "1"));
        System.out.println(bucket + "\t" + CloudApiUtils.getS3Region(accessId, secretKey, bucket + "2"));
        System.out.println(bucket + "\t" + CloudApiUtils.getS3Region(accessId, secretKey, bucket + "3"));
    }

    @Test
    public void testGetAliOssRegion() throws IOException {
        PropertiesFile propertiesFile = new PropertiesFile("resources/.ali.properties");
        String accessKeyId = propertiesFile.getValue("ali-id");
        String accessKeySecret = propertiesFile.getValue("ali-secret");
        String bucket = propertiesFile.getValue("bucket");
        System.out.println(bucket + "\t" + CloudApiUtils.getAliOssRegion(accessKeyId, accessKeySecret, bucket));
        System.out.println(bucket + "\t" + CloudApiUtils.getAliOssRegion(accessKeyId, accessKeySecret, bucket + "2"));
        System.out.println(bucket + "\t" + CloudApiUtils.getAliOssRegion(accessKeyId, accessKeySecret, bucket + "3"));
    }

    @Test
    public void testGetBaiduBosRegion() throws IOException {
        PropertiesFile propertiesFile = new PropertiesFile("resources/.application.properties");
        String accessKeyId = propertiesFile.getValue("bai-id");
        String secretKey = propertiesFile.getValue("bai-secret");
        String bucket = propertiesFile.getValue("bucket");
        bucket = "nigel-test";
        System.out.println(CloudApiUtils.getBaiduBosRegion(accessKeyId, secretKey, bucket));
    }

    @Test
    public void testGetUpYunMarker() {
        String bucket = "squirrel";
        String name1 = "wordSplit/xml/20161220/FF8080815919A151015919D7DC8F0036";
        FileItem fileItem1 = new FileItem();
        fileItem1.key = name1;
        fileItem1.attribute = "folder";
        System.out.println(CloudApiUtils.getUpYunYosMarker(bucket, fileItem1));
        String name2 = "wordSplit/xml/20161220/FF8080815919A15101591AFE37C603F7/4028965B591534B501591BBEC0E8049A.txt";
        FileItem fileItem2 = new FileItem();
        fileItem2.key = name2;
        System.out.println(CloudApiUtils.getUpYunYosMarker(bucket, fileItem2));
    }

    @Test
    public void testDecodeUpYunMarker() {
        String marker1 = "c3F1aXJyZWwvfndvcmRTcGxpdC9+eG1sL34yMDE2MTIyMC9AfkZGODA4MDgxNTkxOUExNTEwMTU5MTlEN0RDOEYwMDM2";
        String marker2 = "c3F1aXJyZWwvfndvcmRTcGxpdC9+eG1sL34yMDE2MTIyMC9+RkY4MDgwODE1OTE5QTE1MTAxNTkxQUZFMzdDNjAzRjcvQCM0MDI4OTY1QjU5MTUzNEI1MDE1OTFCQkVDMEU4MDQ5QS50eHQ=";
        System.out.println(CloudApiUtils.decodeUpYunYosMarker(marker1));
        System.out.println(CloudApiUtils.decodeUpYunYosMarker(marker2));
    }

    @Test
    public void testCheck() throws IOException {
        PropertiesFile propertiesFile = new PropertiesFile("resources/.application.properties");
        String accessKeyId = propertiesFile.getValue("ten-id");
        String accessKeySecret = propertiesFile.getValue("ten-secret");
        COSClient cosClient = new COSClient(new BasicCOSCredentials(accessKeyId, accessKeySecret), new ClientConfig());
        CloudApiUtils.checkTencent(cosClient);
    }
}