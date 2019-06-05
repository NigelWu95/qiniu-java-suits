# 资源异步抓取

## 简介
对文件列表进行异步抓取保存到目标空间。参考：[七牛异步第三方资源抓取](https://developer.qiniu.com/kodo/api/4097/asynch-fetch)  

## 配置文件选项
**操作需指定数据源，请先[配置数据源](../docs/datasource.md)**  

### 功能配置参数
```
process=asyncfetch
ak=
sk=
to-bucket=
domain=
protocol=
url-index=
add-prefix=
rm-prefix=
host=
indexes=
callback-url=
callback-body=
callback-body-type=
callback-host=
file-type=
ignore-same-key=
```  
|参数名|参数值及类型 | 含义|  
|-----|-------|-----|  
|process| 异步抓取时设置为asyncfetch | 表示异步 fetch 操作|  
|ak、sk|长度 40 的字符串|七牛账号的ak、sk，通过七牛控制台个人中心获取，当数据源为 qiniu 时无需再设置|  
|to-bucket|字符串| 保存抓取结果的空间名|  
|domain| 域名字符串| 当数据源数据的资源为文件名列表时，需要设置进行访问的域名，当数据源为云存储且指定 url-index 时无需设置|  
|protocol| http/https| 使用 http 还是 https 访问资源进行抓取（默认 http）|  
|url-index| 字符串| 通过 url 操作时需要设置的 url 索引（下标），需要手动指定才会进行解析|  
|add-prefix| 字符串| 表示为保存的文件名添加指定前缀|  
|rm-prefix| 字符串| 表示将得到的目标文件名去除存在的指定前缀后再作为保存的文件名|  
|host| host 字符串|（暂未启用）抓取源资源时指定 host|  
|md5-index| 字符串| 资源 md5 值索引（下标），需要手动指定才会进行解析|  
|callback-url| 公网可访问的 url 字符串| 设置回调地址|  
|callback-body| body 字符串| 设置回调 body|  
|callback-body-type| body-type 字符串| 设置回调 body 类型|  
|callback-host| host 字符串| 设置回调 host |  
|file-type| 0/1| 文件的存储类型|  
|ignore-same-key| true/false|（暂未启用）为 false 时表示覆盖同名文件，为 true 表示不覆盖|  

### 关于 url-index 和 md5-index
当使用 file 源且 parse=tab/csv 时下标必须为整数。url-index 表示输入行含 url 形式的源文件地址，未设置的情况下则使用 key 字段加上 domain 的
方式访问源文件地址，key 下标用 indexes 参数设置，md5-index 为需要进行 md5 校验时输入 md5 值的字段下标，不设置则无效。  

## 命令行参数方式
```
-process=asyncfetch -ak= -sk= -to-bucket= -add-prefix= -domain= -protocol= -host= -callback-url= -callback-body= -callback-body-type= -callback-host= -file-type= -ignore-same-key=
```

