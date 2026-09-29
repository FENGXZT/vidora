package com.fxzt.vidora.util;

import com.fxzt.vidora.pojo.query.VideoMyPageDto;
import com.fxzt.vidora.pojo.query.VideoPageDTo;

public final class CacheKeys {
    private static final String PREFIX = "vidora:";

    /**
     * 视频详情缓存key
     * @param id 视频id
     * @return 缓存key
     */
    public static String videoDetail(Long id){
        return PREFIX + "video:detail:" + id;
    }

    /**
     * 视频搜索缓存key
     * @param vo 搜索参数
     * @return 缓存key
     */
    public static String videoSearch(VideoPageDTo vo){
        String keyword = vo.getKeyword() !=null?vo.getKeyword():"all";
        return PREFIX + "video:search:" + keyword + ":" + vo.getOrderBy()
                + ":" + vo.getOrder() + ":" + vo.getPage() + ":" + vo.getPageSize();
    }
    
    /**
     * 视频搜索缓存key
     * @param vo 搜索参数
     * @return 缓存key
     */
    public static String videoSearchMyVideo(VideoMyPageDto vo,Long id) {
        Integer status = vo.getStatus();
        String keyword = vo.getKeyword() !=null?vo.getKeyword():"all";
        if (status == null) {
            return PREFIX + "video:search:" +id + ":all:" + keyword + ":" + vo.getOrderBy()
                    + ":" + vo.getOrder() + ":" + vo.getPage() + ":" + vo.getPageSize();
        }
        return PREFIX + "video:search:" +id + ":" + status + ":" + keyword + ":" + vo.getOrderBy()
                + ":" + vo.getOrder() + ":" + vo.getPage() + ":" + vo.getPageSize();
    }

    /**
     * 视频搜索缓存key
     * @return 缓存key
     */
    public static String videoSearchAll() {
        return PREFIX + "video:search:*";
    }
}
