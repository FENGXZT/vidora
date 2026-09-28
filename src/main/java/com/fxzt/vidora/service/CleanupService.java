package com.fxzt.vidora.service;

public interface CleanupService {

    /**
     * 清理超过 hours 小时未完成的上传
     * @param hours 小时数
     */
    int cleanStaleUploads(int hours);
}
