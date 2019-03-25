package com.qiniu.util;

import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;

public class HttpResponseUtils {

    /**
     * 处理异常结果，提取异常信息进行判断或者在需要抛出异常时记录具体错误描述
     * @param e 需要处理的 QiniuException 异常
     * @param retry 当前重试次数
     * @return 返回重试次数，返回 -2 表示传入的异常为空，返回 -1 表示该异常应该抛出，返回 0 表示该异常可以记录并跳过，返回 1 表示可以进行重试
     */
    public static int checkException(QiniuException e, int retry) {
        if (e != null) {
            if (e.response != null) {
                if (e.code() == 631 || retry <= 0) {
                    return -1;
                } else if (e.code() == 478 || e.code() == 404 || e.code() == 612) {
                    // 478 状态码表示镜像源返回了非 200 的状态码，避免因为该异常导致程序中断先处理该异常
                    return 0;
                } else if (e.response.needRetry()) {
                    // 631 状态码表示空间不存在，则不需要重试直接走抛出异常方式
                    e.response.close();
                } else {
                    return -1;
                }
            } else {
                return 0;
            }
        } else {
            return -2;
        }

        // 处理一次异常返回的重试次数应该少一次
        return retry - 1;
    }

    /**
     * 将 Response 对象转换成为结果字符串
     * @param response 得到的 Response 对象
     * @return Response body 转换的 String 对象
     * @throws QiniuException Response 非正常响应的情况下抛出的异常
     */
    public static String getResult(Response response) throws QiniuException {
        if (response == null) throw new QiniuException(new Exception("empty response"));
        if (response.statusCode != 200 && response.statusCode != 298) throw new QiniuException(response);
        String responseBody = response.bodyString();
        response.close();
        return responseBody;
    }

    /**
     * 将 Response 对象转换成为 json 格式结果字符串
     * @param response 得到的 Response 对象
     * @return Response body 转换的 String 对象，用 json 格式记录，包括 status code
     * @throws QiniuException Response 非正常响应的情况下抛出的异常
     */
    public static String responseJson(Response response) throws QiniuException {
        String result = getResult(response);
        return "{\"code\":" + response.statusCode + ",\"message\":\"" + result + "\"}";
    }
}
