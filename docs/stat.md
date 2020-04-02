# 资源查询元信息

## 简介
对空间中的资源查询 stat 信息。参考：[七牛资源元信息查询](https://developer.qiniu.com/kodo/api/1308/stat)  
1. **操作需要指定数据源，默认表示从七牛空间列举文件执行操作，如非默认或需更多条件，请先[配置数据源](datasource.md)**  
2. 支持通过 `-a=<account-name>`/`-d` 使用已设置的账号，则不需要再直接设置密钥，参考：[账号设置](../README.md#账号设置)  
3. 单次查询一个文件请参考[ single 操作](single.md)  
4. 交互式操作随时输入 key 进行查询请参考[ interactive 操作](interactive.md)  

## 配置
> config.txt
```
path=
process=stat
ak=
sk=
bucket=
indexes=
```  
|参数名|参数值及类型 | 含义|  
|-----|-------|-----|  
|process=stat| 查询资源元信息时设置为stat| 表示查询 stat 信息操作|  
|ak、sk|长度40的字符串|七牛账号的ak、sk，通过七牛控制台个人中心获取，当数据源为 qiniu 时无需再设置|  
|bucket| 字符串| 操作的资源原空间，当数据源为 qiniu 时无需再设置|  
|indexes|字符串| 设置输入行中 key 字段的下标（有默认值），参考[数据源 indexes 设置](datasource.md#1-公共参数)|  

运行参数：`-config=config.txt`

### 命令行方式
```
-path= -process=stat -ak= -sk= -bucket=
```

## 备注
stat 操作是 file 源下的操作，从 every line of file 的 key 索引（indexes 参数的第一个索引值，默认为 0）获取文件名，当使用 file 源且 parse=tab/csv 时下标必须为整数。
