package com.fxzt.vidora.task;

import com.fxzt.vidora.service.CleanupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UploadCleanupTask {

    @Autowired
    private CleanupService cleanupService;

    // 定时清理（每天凌晨3点）
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanStaleUploads() {
        try {
            int cleaned = cleanupService.cleanStaleUploads(24);
            log.info("清理未完成上传任务完成，共处理 {} 条", cleaned);
        } catch (Exception e) {
            log.error("清理未完成上传任务失败", e);
        }
    }
}
