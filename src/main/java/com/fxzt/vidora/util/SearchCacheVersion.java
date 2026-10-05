package com.fxzt.vidora.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 搜索/列表缓存版本号：INCR 之后旧缓存自然失效（不是删 key，DEL 不支持通配符）。
 * <p>★ 不变式：<b>任何让视频变成 status=2（可播放）的路径，都必须调 bump()</b>，
 * 否则新视频最长 30 分钟不出现在列表里。
 */
@Component
public class SearchCacheVersion {

    @Autowired
    private CacheHelper cacheHelper;

    public void bump() {
        cacheHelper.incr(RedisKeys.videoSearchVersion());
    }
}