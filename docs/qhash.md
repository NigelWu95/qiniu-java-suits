# 资源查询 qhash

## 简介
对空间中的资源查询 qhash。参考：[七牛资源 hash 值查询](https://developer.qiniu.com/dora/manual/1297/file-hash-value-qhash)  
1. **操作需要指定数据源，默认表示从七牛空间列举文件执行操作，如非默认或需更多条件，请先[配置数据源](datasource.md)**  
2. 单次查询一个文件请参考[ single 操作](single.md)  
3. 交互式操作随时输入 key 进行查询请参考[ interactive 操作](interactive.md)  

## 配置
```
process=qhash 
protocol=
domain=
indexes=
url-index=
algorithm=
```  
|参数名|参数值及类型 | 含义|  
|-----|-------|-----|  
|process=qhash| 查询qhash时设置为qhash| 表示查询资源 hash 值操作|  
|protocol| http/https| 使用 http 还是 https 访问资源进行抓取（默认 http）|  
|domain| 域名字符串| 用于拼接文件名生成链接的域名（七牛存储空间域名可以使用[ domainsfrom 命令查询](domainsofbucket.md)），当指定 url-index 时无需设置|  
|indexes|字符串| 设置输入行中 key 字段的下标（有默认值），参考[数据源 indexes 设置](datasource.md#1-公共参数)|  
|url-index| 字符串| 通过 url 操作时需要设置的 url 索引（下标），未设置任何索引时根据 parse 类型默认为 0 或 "url"|  
|algorithm| md5/sha1| 查询 qhash 使用的算法,默认为 md5|  

### 关于 url-index
当 parse=tab/csv 时 [xx-]index(ex) 设置的下标必须为整数。url-index 表示输入行中存在 url 形式的源文件地址，未设置的情况下则默认从 key 字段
加上 domain 的方式访问源文件地址，key 下标用 indexes 参数设置，参见[ indexes 索引](datasource.md#关于-indexes-索引)。  

### 命令行方式
```
-process=qhash -protocol= -domain= -algorithm=md5 
```
