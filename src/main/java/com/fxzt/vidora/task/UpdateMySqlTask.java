package com.fxzt.vidora.task;

import com.fxzt.vidora.exception.BusinessException;
import com.fxzt.vidora.service.UpdateMySqlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UpdateMySqlTask {
    @Autowired
    private UpdateMySqlService updateMySqlService;

    @Scheduled(fixedDelay = 30_000)
    public void updateViewCount(){
        try {
            updateMySqlService.updateViewCount();
        } catch (Exception e) {
            throw new BusinessException("更新播放量失败：" + e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 60_000)
    public void updateLikeCount(){
        try {
            updateMySqlService.updateLikeCount();
        } catch (Exception e) {
            throw new BusinessException("更新点赞数失败：" + e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void updateLikeCountFullAmount() {
        try {
            updateMySqlService.updateLikeCountFullAmount();
        } catch (Exception e) {
            throw new BusinessException("全量更新点赞数失败：" + e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 60_000)
    public void updateFavoriteCount() {
        try { updateMySqlService.updateFavoriteCount(); }
        catch (Exception e) { throw new BusinessException("更新收藏数失败：" + e.getMessage()); }
    }

    @Scheduled(cron = "0 5 * * * *")
    public void updateFavoriteCountFullAmount() {
        try { updateMySqlService.updateFavoriteCountFullAmount(); }
        catch (Exception e) { throw new BusinessException("全量更新收藏数失败：" + e.getMessage()); }
    }

    @Scheduled(fixedDelay = 300_000)
    public void recomputeHotScore(){
        try {
            updateMySqlService.recomputeHotScore();
        }catch (Exception e) { log.error("重新计算热门视频分数失败：" + e.getMessage()); }
    }
}
