package com.fxzt.vidora.util;

import com.fxzt.vidora.pojo.query.FeedPageDto;
import com.fxzt.vidora.pojo.query.VideoMyPageDto;
import com.fxzt.vidora.pojo.query.VideoPageDTo;

import java.time.ZoneId;

public final class RedisKeys {
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
    public static String videoSearch(VideoPageDTo vo,String version){
        String keyword = vo.getKeyword() !=null?vo.getKeyword():"all";
        return PREFIX + "video:search:" + version + ":" + keyword + ":" + vo.getOrderBy()
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
            return PREFIX + "video:myvideo:search:" +id + ":all:" + keyword + ":" + vo.getOrderBy()
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

    /**
     * 播放量计数缓存key
     * @param videoId 视频id
     * @return 缓存key
     */
    public static String videoPlayCount(Long videoId){
        return PREFIX + "video:play:" + videoId;
    }

    /**
     * 播放量计数缓存key
     * @return 缓存key
     */
    public static String videoPlayCount(){
        return PREFIX + "video:play:*";
    }

    /**
     * 视频搜索版本号缓存key
     * @return 缓存key
     */
    public static String  videoSearchVersion(){
        return PREFIX+"video:search:version";
    }

    /**
     * 视频点赞用户集合缓存key
     * @param videoId 视频id
     * @return 缓存key
     */
    public static String videoLikeUsers(Long videoId){
        return PREFIX + "video:like_users:" + videoId;
    }

    /**
     * 视频点赞量计数缓存key
     * @param videoId 视频id
     * @return 缓存key
     */
    public static String videoLikeCount(Long videoId){
        return PREFIX + "video:like_count:" + videoId;
    }

    /**
     * 点赞更新标记缓存key
     * @return 缓存key
     */
    public static String videoLikeUpdateFlag(){
        return PREFIX + "video:like_dirty" ;
    }

    /**
     * 视频评论缓存key
     * @param videoId 视频id
     * @return 缓存key
     */
    public static String videoComments(Long videoId,Long id,String version){
        return PREFIX+"video:comments:" +version + ":" +videoId + ":" + id;
    }

    /**
     * 视频评论版本号缓存key
     * @param id 视频id
     * @return 缓存key
     */
    public static String videoCommentVersion(Long id) {
        return PREFIX+"video:comment:version:"+id;
    }

    public static String uploadContext(String uploadId) {
        return PREFIX+"upload:context:"+uploadId;
    }

    /**
     * 推荐视频 feed 缓存 key
     * @param dto feed 参数
     * @return 缓存 key
     */
    public static String videoFeed(FeedPageDto dto) {
       /* feed 对所有人一样（无个性化）才能共享缓存；一旦按用户个性化，这个 key 必须加 userId 或不缓存*/
        String cursor = dto.getCursorTime()==null
                ? "first"
                : dto.getCursorTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                +"_"+dto.getCursorId();
        return PREFIX+"video:feed:"+dto.getSort()+":"+cursor+":"+dto.getSize();
    }
}
