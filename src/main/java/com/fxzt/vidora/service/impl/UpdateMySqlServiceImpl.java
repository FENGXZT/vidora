package com.fxzt.vidora.service.impl;

import com.fxzt.vidora.exception.BusinessException;
import com.fxzt.vidora.mapper.VideoFavoriteMapper;
import com.fxzt.vidora.mapper.VideoLikeMapper;
import com.fxzt.vidora.mapper.VideoMapper;
import com.fxzt.vidora.service.UpdateMySqlService;
import com.fxzt.vidora.util.CacheHelper;
import com.fxzt.vidora.util.RedisKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
public class UpdateMySqlServiceImpl implements UpdateMySqlService {

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private CacheHelper cacheHelper;

    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private VideoLikeMapper videoLikeMapper;
    @Autowired
    private VideoFavoriteMapper videoFavoriteMapper;

    @Override
    public void updateViewCount() {
        String keys = RedisKeys.videoPlayCount();
        try {
            ScanOptions options = ScanOptions.scanOptions()
                    .match(keys)
                    .count(200)
                    .build();

            try (Cursor<String> cursor = redis.scan(options)) {
                while (cursor.hasNext()) {
                    String key = cursor.next();
                    try {
                        flushOne(key);
                    } catch (Exception e) {
                        log.error("更新播放量失败，key={}", key, e);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("更新播放量失败{}", e.getMessage());
        }
    }

    @Override
    public void updateLikeCount() {
        /*
        * Redis 抖动会静默丢脏标记
        * 增量任务遍历 `members()` 期间的**并发 SADD 会被随后的 SREM 吃掉**
        * 两条都靠整点对账兜底
        * */
        String keys = RedisKeys.videoLikeUpdateFlag();
        try {
            Set<String> dirtyIds = redis.opsForSet().members(keys);
            for(String idStr : dirtyIds){
                try {
                    Long videoId = Long.parseLong(idStr);
                    Integer likeCount = videoLikeMapper.getLikeCount(videoId);
                    videoMapper.updateLikeCount(videoId, likeCount);
                    cacheHelper.delete(RedisKeys.videoDetail(videoId));
                    cacheHelper.rem(keys, idStr);
                }catch (NumberFormatException e){
                    log.warn("非法标记，idStr={}", idStr);
                    cacheHelper.rem(keys, idStr);
                }catch (Exception e) {
                    log.error("更新点赞数失败", e);
                }
            }
        } catch (Exception e) {
            log.warn("更新点赞数失败{}", e.getMessage());
        }
    }

    @Override
    public void updateLikeCountFullAmount() {
        try {
            videoMapper.updateLikeCountFullAmount();
        } catch (Exception e) {
            log.error("全量更新点赞数失败", e);
        }
    }

    @Override
    public void updateFavoriteCount() {
        /*
         * Redis 抖动会静默丢脏标记
         * 增量任务遍历 `members()` 期间的**并发 SADD 会被随后的 SREM 吃掉**
         * 两条都靠整点对账兜底
         * */
        String keys = RedisKeys.videoFavoriteUpdateFlag();
        try {
            Set<String> dirtyIds = redis.opsForSet().members(keys);
            for(String idStr : dirtyIds){
                try {
                    Long videoId = Long.parseLong(idStr);
                    Integer favoriteCount = videoFavoriteMapper.getFavoriteCount(videoId);
                    videoMapper.updateFavoriteCount(videoId, favoriteCount);
                    cacheHelper.delete(RedisKeys.videoDetail(videoId));
                    cacheHelper.rem(keys, idStr);
                }catch (NumberFormatException e){
                    log.warn("非法标记，idStr={}", idStr);
                    cacheHelper.rem(keys, idStr);
                }catch (Exception e) {
                    log.error("更新收藏数失败", e);
                }
            }
        } catch (Exception e) {
            log.warn("更新收藏数失败{}", e.getMessage());
        }
    }

    @Override
    public void updateFavoriteCountFullAmount() {
        try {
            videoMapper.updateFavoriteCountFullAmount();
        } catch (Exception e) {
            log.error("全量更新点赞数失败", e);
        }
    }

    @Override
    public void recomputeHotScore() {
        try {
            updateLikeCount();
            updateViewCount();
            videoMapper.recomputeHotScore();
        } catch (Exception e) {
            log.error("重新计算热门视频失败", e);
        }
    }

    /**
     * 更新单个视频播放量
     * @param key redis key
     */
    private void flushOne(String key){
        Long count = cacheHelper.get(key, Long.class);
        if (count == null || count < 0) return;
        Long videoId = parseVideoId(key);
        if (videoId == null) return;
        try {
            videoMapper.updateViewCount(videoId, count);
        } catch (Exception e) {
            log.error("更新播放量失败，videoId={}, count={}", videoId, count, e);
        }
    }

    /**
     * 解析视频id
     * @param key redis key
     * @return 视频id
     */
    private Long parseVideoId(String key) {
        try {
            int i = key.lastIndexOf(":");
            return Long.parseLong(key.substring(i + 1));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
