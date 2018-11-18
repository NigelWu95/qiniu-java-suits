package com.qiniu.service.oss;

import com.qiniu.common.FileReaderAndWriterMap;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.sdk.BucketManager.*;
import com.qiniu.service.interfaces.IOssFileProcess;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.storage.model.StorageType;
import com.qiniu.util.Auth;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class ChangeType extends OperationBase implements IOssFileProcess, Cloneable {

    private int type;

    private void initOwnParams(int type) {
        this.type = type;
    }

    public ChangeType(Auth auth, Configuration configuration, String bucket, int type, String resultFileDir,
                      String processName, int resultFileIndex) throws IOException {
        super(auth, configuration, bucket, resultFileDir, processName, resultFileIndex);
        initOwnParams(type);
    }

    public ChangeType(Auth auth, Configuration configuration, String bucket, int type, String resultFileDir,
                      String processName) {
        super(auth, configuration, bucket, resultFileDir, processName);
        initOwnParams(type);
    }

    public ChangeType getNewInstance(int resultFileIndex) throws CloneNotSupportedException {
        ChangeType changeType = (ChangeType)super.clone();
        changeType.fileReaderAndWriterMap = new FileReaderAndWriterMap();
        try {
            changeType.fileReaderAndWriterMap.initWriter(resultFileDir, processName, resultFileIndex);
        } catch (IOException e) {
            e.printStackTrace();
            throw new CloneNotSupportedException();
        }
        return changeType;
    }

    protected Response getResponse(FileInfo fileInfo) throws QiniuException {
        StorageType storageType = type == 0 ? StorageType.COMMON : StorageType.INFREQUENCY;
        return bucketManager.changeType(bucket, fileInfo.key, storageType);
    }

    synchronized protected BatchOperations getOperations(List<FileInfo> fileInfoList){
        List<String> keyList = fileInfoList.stream().map(fileInfo -> fileInfo.key).collect(Collectors.toList());
        return batchOperations.addChangeTypeOps(bucket, type == 0 ? StorageType.COMMON : StorageType.INFREQUENCY,
                keyList.toArray(new String[]{}));
    }

    protected String getInfo() {
        return bucket + "\t" + type;
    }
}
