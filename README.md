[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.qiniu/qsuits/badge.svg)](https://search.maven.org/artifact/com.qiniu/qsuits/2.20/jar)
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

# qiniu-suits (qsuits)
云存储 API (base-qiniu)套件（可以工具形式使用：在 release 页面[下载最新 jar 包](https://github.com/NigelWu95/qiniu-suits-java/releases)，
x.xx-thin 为降配版，适用于 4C8G 及以下的机器），能够**并发列举**云存储空间的大量资源列表，同时支持对资源列表并发进行批量处理，主要包括对七牛云存
储资源进行增/删/改/查/迁移/转码等。基于 Java 编写，可基于 JDK（8 及以上）环境在命令行或 IDE 等情况下运行。  

### **高级功能列表（所有操作均支持批量并发处理）：**
- [x] 云存储(**阿里云/腾讯云/七牛云等**)大量文件高效[并发列举](docs/datasource.md#3-list-云存储列举)，支持指定前缀、开始及结束文件名(或前缀)或 marker 等参数  
- [x] 资源文件[过滤](docs/filter.md)，按照日期范围、文件名(前缀、后缀、包含)、mime 类型等字段正向及反向筛选目标文件  
- [x] 检查云存储资源文件后缀名 ext 和 mime-type 类型是否匹配 [check](docs/filter.md#特殊特征匹配过滤-f-check[-x])，过滤异常文件列表  
- [x] 修改空间资源的存储类型（低频/标准）[type 配置](docs/type.md)  
- [x] 修改空间资源的状态（启用/禁用）[status 配置](docs/status.md)  
- [x] 修改空间资源的生命周期 [lifecycle 配置](docs/lifecycle.md)  
- [x] 删除空间资源 [delete 配置](docs/delete.md)  
- [x] 复制资源到指定空间 [copy 配置](docs/copy.md)  
- [x] 移动资源到指定空间 [move 配置](docs/move.md)  
- [x] 对指定空间的资源进行重命名 [rename 配置](docs/rename.md)  
- [x] 异步抓取资源到指定空间 [asyncfetch 配置](docs/asyncfetch.md)  
- [x] 对空间资源执行 pfop 请求 [pfop 配置](docs/pfop.md)  
- [x] 通过 persistentId 查询 pfop 的结果 [pfopresult 配置](docs/pfopresult.md)  
- [x] 查询空间资源的元信息 [stat 配置](docs/stat.md)  
- [x] 对设置了镜像源的空间资源进行镜像更新 [mirror 配置](docs/mirror.md)  
- [x] 查询空间资源的视频元信息 [avinfo 配置](docs/avinfo.md)  
- [x] 查询资源的 qhash [qhash 配置](docs/qhash.md)  
- [x] 对私有空间资源进行私有签名 [privateurl 配置](docs/privateurl.md)  
- [x] 根据音视频资源的 avinfo 信息来生成转码指令 [pfopcmd 配置](docs/pfopcmd.md)  
- [x] 对 m3u8 的资源进行读取导出其中的 ts 文件列表 [exportts 配置](docs/exportts.md)  

### 1 程序运行过程  
读取[数据源](docs/datasource.md) => [选择[过滤器](docs/filter.md)] => [指定数据[处理过程](#5-处理过程) =>] [结果持久化](docs/resultsave.md)  

### 2 运行方式  
#### 关于版本  
使用时建议直接使用最新版本或者更新到最新版本，x.xx 版为常规版本，x.xx-thin 为降配版，常规版对机器配置要求比较高，降配版可在偏低配置的机器上运行，
通常 4C8G 以下用降配版。  
**1. 程序依赖 jar**  
引入 jar 包（[下载 jar 包](https://search.maven.org/search?q=a:qsuits)或者 [使用 maven 仓库](https://mvnrepository.com/artifact/com.qiniu/qsuits)，
**maven 仓库中的 jar 包不支持命令行运行，如需命令行工具** jar 包，请至 [Release](https://github.com/NigelWu95/qiniu-suits-java/releases)
页面下载），可以重写或新增 processor 接口实现类进行自定义功能，maven:
```
<dependency>
  <groupId>com.qiniu</groupId>
  <artifactId>qsuits</artifactId>
  <version>x.xx</version>
</dependency>
```  
x.xx 替换成最新版本号，最新版本见 [Release](https://github.com/NigelWu95/qiniu-suits-java/releases)  
**2. 命令行运行 jar**  
在 [Release](https://github.com/NigelWu95/qiniu-suits-java/releases) 页面下载最新 jar 包，使用命令行参数 [-config=<filepath>] 指定
配置文件路径，运行命令形如：
```
java -jar qsuits-x.x.jar -config=config.txt
```  
配置文件中可设置形如\<属性名\>=\<属性值\>，每行一个参数：  
```
source=qiniu
bucket=
ak=
sk=
```  
**备注1**：可以通过默认路径的配置文件来设置参数值，默认配置文件路径为 `resources/application.config` 或 `resources/.application.config`，
两个文件存在任意一个均可作为配置文件来设置参数，此时则不需要通过 `-config=` 指定配置文件路径。  
**备注2**：直接使用命令行传入参数（较繁琐），不使用配置文件的情况下全部所需参数可以完全从命令行指定，形式为：**`-<key>=<value>`**，**请务必在参
数前加上 -**，如  
```
java -jar qsuits-x.x.jar [-source=qiniu] -bucket=<path> -ak=<ak> -sk=<sk>
```  

### 3 数据源
数据源分为几大类型：云存储列举(list)、文件内容读取(file)，可以通过 **source=** 来指定数据源，例如:  
`source=qiniu` 表示从七牛存储空间列举出资源列表  
`source=local` 表示从本地文件按行读取资源列表  
如果使用 `source` 参数则需要显式指定 `bucket` 或者 `path` 参数来指明具体的数据源地址  
**在 v2.11 以上版本，取消了设置 source 参数的强制性，如果不显式指定 source 则根据 path 参数来自动判断：  
`path=qiniu://<bucket>` 表示从七牛存储空间列举出资源列表  
`path=tencent://<bucket>` 表示从腾讯存储空间列举出资源列表  
`path=../<file-path>` 表示从本地文件中读取资源列表  
当无 source 和 path 路径进行判断时则默认认为从七牛空间进行列举**，配置文件示例可参考 [配置模板](resources/application.config)  
#### list 云存储列举  
支持从不同的云存储上列举出空间文件，默认线程数(threads 参数)为 30，1 亿以内文件可以不增加线程，通常云存储空间列举的必须参数包括密钥、空间名(通过
path 或 bucket 设置)及空间所在区域(通过 region 设置)：  

|list 源|             密钥和 region 字段         |                  对应关系和描述               |  
|------|---------------------------------------|---------------------------------------------|  
|qiniu|`ak=`<br>`sk=`<br>`region=z0/z1/z2/...`|密钥对应七牛云账号的 AccessKey 和 SecretKey<br>region(可不设置)使用简称，参考[七牛 Region](https://developer.qiniu.com/kodo/manual/1671/region-endpoint)|  
|tencent|`ten-id=`<br>`ten-secret=`<br>`region=ap-beijing/...`| 密钥对应腾讯云账号的 SecretId 和 SecretKey<br>region(必须设置)使用简称，参考[腾讯 Region](https://cloud.tencent.com/document/product/436/6224)|  
|aliyun|`ali-id=`<br>`ali-secret=`<br>`region=oss-cn-hangzhou/...`| 密钥对应阿里云账号的 AccessKeyId 和 AccessKeySecret<br>region(必须设置)使用简称，参考[阿里 Region](https://help.aliyun.com/document_detail/31837.html)|  
#### file 文件内容读取  
文件内容为资源列表，可按行读取输入文件的内容获取资源列表，文件行解析参数如下：  
`parse=tab/json` 表示输入行的格式  
`separator=\t` 表示输入行的格式分隔符（非 json 时可能需要）  
**数据源更多参数配置和详细解释及可能涉及的高级用法见：[数据源配置](docs/datasource.md)**  

### 4 过滤器功能
从数据源输入的数据通常可能存在过滤需求，如过滤指定规则的文件名、过滤时间点或者过滤存储类型等，可通过配置选项设置一些过滤条件，目前支持两种过滤条件：
1.**基本字段过滤**和2.**特殊特征匹配过滤**  
#### 基本字段过滤  
根据设置的字段条件进行筛选，多个条件时需同时满足才保留，若存在记录不包该字段信息时则正向规则下不保留，反正规则下保留，字段包含：  
`f-prefix=` 表示**选择**文件名符合该前缀的文件  
`f-suffix=` 表示**选择**文件名符合该后缀的文件  
`f-inner=` 表示**选择**文件名包含该部分字符的文件  
`f-regex=` 表示**选择**文件名符合该正则表达式的文件，所填内容必须为正则表达式  
`f-mime=` 表示**选择**符合该 mime 类型的文件  
`f-type=` 表示**选择**符合该存储类型的文件, 为 0（标准存储） 或 1（低频存储）  
`f-status=` 表示**选择**符合该存储状态的文件, 为 0（启用） 或 1（禁用）  
`f-date-scale` 设置过滤的时间范围，格式为 [\<date1\>,\<date2\>]，\<date\> 格式为：2018-08-01 00:00:00，[特殊规则](#f-date-scale)  
`f-anti-prefix=` 表示**排除**文件名符合该前缀的文件  
`f-anti-suffix=` 表示**排除**文件名符合该后缀的文件  
`f-anti-inner=` 表示**排除**文件名包含该部分字符的文件  
`f-anti-regex=` 表示**排除**文件名符合该正则表达式的文件，所填内容必须为正则表达式  
`f-anti-mime=` 表示**排除**该 mime 类型的文件  

#### 特殊字符
特殊字符包括: `, \ =` 如有参数值本身包含特殊字符需要进行转义：`\, \\ \=`  

#### f-date-scale
<date> 中的 00:00:00 为默认值可省略，无起始时间则可填 [0,\<date2\>]，结束时间支持 now 和 max，分别表示到当前时间为结束或无结束时间。  

#### 特殊特征匹配过滤 f-check[-x]  
根据资源的字段关系选择某个特征下的文件，目前支持 "ext-mime" 检查，程序内置的默认特征配置见：[check 默认配置](resources/check.json)，运行
参数选项如下：  
`f-check=ext-mime` 表示进行**后缀名 ext **和**mimeType**（即 content-type）匹配性检查，不符合规范的疑似异常文件将被筛选出来  
`f-check-config` 自定义资源字段规范对应关系列表的配置文件，格式为 json，自定义规范配置 key 字段必填，其元素类型为列表 [], 否则无效，如
"ext-mime" 配置时后缀名和 mimeType 用 ":" 组合成字符串成为一组对应关系，写法如下：  
```
{
  "ext-mime": [
    "mp5:video/mp5"
  ]
}
```  
配置举例：[check-config 配置](resources/check-config.json)  
`f-check-rewrite` 是否覆盖默认的特征配置，为 false（默认）表示将自定义的规范对应关系列表和默认的列表进行叠加，否则程序内置的规范对应关系将失效，
只检查自定义的规范列表。  
设置了过滤条件的情况下，后续的处理过程会选择满足过滤条件的记录来进行，或者对于数据源的输入进行过滤后的记录可以直接持久化保存结果，如通过 qiniu 源获
取文件列表过滤后进行保存，可设置 save-total=true/false 来选择是否将列举到的完整记录进行保存。  
filter 详细配置可见[filter 配置说明](docs/filter.md)  

### 5 处理过程
处理过程表示对由数据源输入的每一条记录进行处理，所有处理结果保存在 save-path 路径下，具体处理过程由处理类型参数指定，如 **process=type/status
/lifecycle/copy** (命令行方式则指定为 **-process=xxx**) 等，同时 process 操作支持设置公共参数：  
`retry-times=` 操作失败（可重试的异常情况下，如请求超时）需要进行的重试次数，默认为 5 次  
`batch-size=` 支持 batch 操作时设置的一次批量操作的文件个数（支持 batch 操作：type/status/lifecycle/delete/copy/move/rename/stat，
其他操作请勿设置 batchSize 或者设置为 0），当响应结果较多 429/573 状态码时需要降低 batch-size，或者直接使用非 batch 方式：batch-size=0/1  
**处理操作类型：**  
`process=type` 表示修改空间资源的存储类型（低频/标准）[type 配置](docs/type.md)  
`process=status` 表示修改空间资源的状态（启用/禁用）[status 配置](docs/status.md)  
`process=lifecycle` 表示修改空间资源的生命周期 [lifecycle 配置](docs/lifecycle.md)  
`process=delete` 表示删除空间资源 [delete 配置](docs/delete.md)  
`process=copy` 表示复制资源到指定空间 [copy 配置](docs/copy.md)  
`process=move` 表示移动资源到指定空间 [move 配置](docs/move.md)  
`process=rename` 表示对指定空间的资源进行重命名 [rename 配置](docs/rename.md)  
`process=asyncfetch` 表示异步抓取资源到指定空间 [asyncfetch 配置](docs/asyncfetch.md)  
`process=pfop` 表示对空间资源执行 pfop 请求 [pfop 配置](docs/pfop.md)  
`process=pfopresult` 表示通过 persistentId 查询 pfop 的结果 [pfopresult 配置](docs/pfopresult.md)  
`process=stat` 表示查询空间资源的元信息 [stat 配置](docs/stat.md)  
`process=mirror` 表示对设置了镜像源的空间资源进行镜像更新 [mirror 配置](docs/mirror.md)  
`process=avinfo` 表示查询空间资源的视频元信息 [avinfo 配置](docs/avinfo.md)  
`process=qhash` 表示查询资源的 qhash [qhash 配置](docs/qhash.md)  
`process=privateurl` 表示对私有空间资源进行私有签名 [privateurl 配置](docs/privateurl.md)  
`process=pfopcmd` 表示根据音视频资源的 avinfo 信息来生成转码指令 [pfopcmd 配置](docs/pfopcmd.md)  
`process=exportts` 表示对 m3u8 的资源进行读取导出其中的 ts 文件列表 [exportts 配置](docs/exportts.md)  

### 6 结果持久化
对数据源输出（列举）结果进行持久化操作（目前支持写入到本地文件），持久化选项：  
`save-path=` 表示保存结果的文件路径  
`save-format=` 结果保存格式（json/tab），默认为 tab  
`save-separator=` 结果保存分隔符，结合 save-format=tab 默认使用 "\t" 分隔  
`save-total=` 是否保存数据源的完整输出结果，用于在设置过滤器的情况下选择是否保留原始数据，如 bucket 的 list 操作需要在列举出结果之后再针对字段
进行过滤，save-total=true 则表示保存列举出来的完整数据，而过滤的结果会单独保存，如果只需要过滤之后的数据，则设置 save-total=false。
**默认情况：**  
（1）本地文件数据源时默认如果存在 process 或者 filter 设置则为 false，反之则为 true（说明可能是单纯格式转换）。  
（2）云存储数据源时如果无 process 则为 true，如果存在 process 且包含 filter 设置时为 false，既存在 process 同时包含 filter 设置时为 true。  
（3）默认保存结果的路径（save-path）使用 <bucket>（云存储数据源情况下）名称或者 <path>-result 来创建目录  

**--** 所有持久化参数均为可选参数，未设置的情况下保留所有字段：key,hash,fsize,putTime,mimeType,type,status,md5,endUser，可通过rm-fields
选择去除某些字段，每一行信息以 json 格式保存在 save-path 路径下。详细配置说明见 [持久化配置](docs/resultsave.md)。  
**--** 持数据源久化结果的文件名为 "\<source-name\>\_success_\<order\>.txt"：  
（1）qiniu 存储数据源 =》 "qiniu_success_\<order\>.txt"  
（2）local 源 =》 "local_success_\<order\>.txt"  
如果设置了过滤选项或者处理过程，则过滤到的结果文件名为 "filter_success/error_\<order\>.txt"，process 过程保存的结果为文件为 
"\<process\>_success/error_\<order\>.txt"。  
**--** process 结果的文件名为：<process>_success/error_\<order\>.txt 及 <process>_need_retry_\<order\>.txt，error 的结果表明无法成功
处理，可能需要确认所有错误数据和原因，need_retry 的结果为需要重试的记录，包含错误信息。  

### 7 超时设置
多数数据源或者操作涉及网络请求，因此提供超时时间设置，默认的超时时间一般能够满足要求，特殊需要的情况下可以修改各超时时间：  
`connect-timeout=60` 网络连接超时时间，程序默认 60s  
`read-timeout=120` socket 读取超时时间，程序默认 120s  
`request-timeout=60` 网络请求超时时间，程序默认 60s  

### 补充
1. 命令行方式与配置文件方式不可同时使用，指定 -config=<path> 或使用默认配置配置文件路径时，需要将所有参数设置在配置文件中。
2. 一般情况下，命令行输出异常信息如 socket timeout 超时为正常现象，如：
```
list prefix:<prefix> retrying...
...
java.net.SocketTimeoutException: timeout
```
程序会自动重试，如果比较频繁则可以修改[超时配置](#7-超时设置)重新运行程序，超过重试次数或者其他非预期异常发生时程序会退出，可以将异常信息反馈在 
[ISSUE列表](https://github.com/NigelWu95/qiniu-suits-java/issues) 中。  
3. 常见错误信息：  
（1）java.lang.OutOfMemoryError: GC overhead limit exceeded  
表示内存中加载了过多的资源导致 java 的 gc 内存溢出，需要关闭程序重新运行，降低线程数 threads 或者 unit-len。  
（2）java.lang.OutOfMemoryError: unable to create new native thread   
与（1）类似，内存溢出导致无法继续创建更多线程或对象。  
（3）java.lang.UnsupportedClassVersionError: Unsupported major.minor version ...  
请使用 java 8 或以上版本的 jdk（jre） 环境来运行该程序。  