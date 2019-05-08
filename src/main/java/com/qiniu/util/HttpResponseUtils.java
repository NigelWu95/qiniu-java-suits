package com.qiniu.util;

import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;

public final class HttpResponseUtils {

    /**
     * 判断 process 产生（不适用于 datasource 读取产生的异常）的异常结果，返回后续处理标志
     * @param e 需要处理的 QiniuException 异常
     * @param times 此次处理失败前的重试次数，如果已经为小于 1 的话则说明没有重试机会
     * @return 返回重试次数，返回 -2 表示该异常应该抛出，返回 -1 表示重试次数已用尽，可以记录为待重试信息，返回 0 表示该异常应该记录并跳过，返
     * 回大于 0 表示可以进行重试
     */
    public static int checkException(QiniuException e, int times) {
        // 处理一次异常返回后的重试次数应该减少一次，并且可用于后续判断是否有重试的必要
        times--;
        if (e.response != null) {
            int code = e.code();
            e.response.close();
            if (times <= 0) {
                if (code == 599) return -2; // 如果经过重试之后响应的是 599 状态码则抛出异常
                else return -1;
            }
            // 429 和 573 为请求过多的状态码，可以进行重试
            else if (code < 0 || code == 406 || code == 429 || (code >= 500 && code < 600 && code != 579)) {
                return times;
            } else if ((e.code() >= 400 && e.code() <= 499) || (e.code() >= 612 && e.code() <= 614) || e.code() == 579) {
                // 避免因为某些可忽略的状态码导致程序中断故先处理该异常
                return 0;
            } else { // 如 631 状态码表示空间不存在，则不需要重试抛出异常
                return -2;
            }
        } else {
            if (e.error() != null || e.getMessage() != null) {
                return 0;
            } else if (times <= 0) {
                return -1;
            } else {
                return times; // 请求超时等情况下可能异常中的 response 为空，需要重试
            }
        }
    }

    // 检查七牛 API 请求返回的状态码，返回 1 表示成功，返回 0 表示需要重试，返回 -1 表示可以记录错误
    public static int checkStatusCode(int code) {
        if (code == 200) {
            return 1;
        } else if (code <= 0 || code == 406 || code == 429 || (code >= 500 && code < 600 && code != 579)) {
            return 0;
        } else {
            return -1;
        }
    }

    /**
     * 将 Response 对象转换成为结果字符串
     * @param response 得到的 Response 对象
     * @return Response body 转换的 String 对象
     * @throws QiniuException Response 非正常响应的情况下抛出的异常
     */
    public static String getResult(Response response) throws QiniuException {
        if (response == null) throw new QiniuException(null, "empty response");
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
