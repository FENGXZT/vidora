package com.fxzt.vidora.service.impl;

import com.fxzt.vidora.exception.BusinessException;
import com.fxzt.vidora.mapper.CommentMapper;
import com.fxzt.vidora.mapper.VideoMapper;
import com.fxzt.vidora.mapper.VideoLikeMapper;
import com.fxzt.vidora.pojo.entity.Video;
import com.fxzt.vidora.pojo.query.*;
import com.fxzt.vidora.pojo.vo.*;
import com.fxzt.vidora.service.FileService;
import com.fxzt.vidora.service.VideoService;
import com.fxzt.vidora.util.CacheHelper;
import com.fxzt.vidora.util.RedisKeys;
import com.fxzt.vidora.util.UserContextUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class VideoServiceImpl implements VideoService {

    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private VideoLikeMapper videoLikeMapper;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private CacheHelper cacheHelper;
    @Autowired
    private FileService fileService;
    @Autowired
    private StringRedisTemplate redis;

    @Value("${minio.VideoBucketName}")
    private String videoBucket;
    @Value("${minio.ImageBucketName}")
    private String imageBucket;

    @Override
    public VideoVo getVideoById(Long id) {
        /*加"记录浏览历史"时必须判 `userId == null` 就跳过**（匿名没有浏览记录）*/
        // 1. 读缓存
        String key = RedisKeys.videoDetail(id);
        String playKey = RedisKeys.videoPlayCount(id);

        Long playCount = null;
        if (isValidInteger(redis.opsForValue().get(playKey))) {
            playCount = cacheHelper.incr(playKey);
        }
        if (playCount == null){
            Long base = videoMapper.findViewCountById(id);
            if (base == null) throw new BusinessException("视频不存在");
            redis.opsForValue().setIfAbsent(playKey, String.valueOf(base+1));
            playCount = base+1;
        }

        Video video = cacheHelper.get(key, Video.class);
        if (video != null) {
            video.setViewCount(playCount.intValue());
            return toVo(video);
        }

        // 2. 查库
        video = videoMapper.getVideoById(id);
        if (video == null) {
            throw new BusinessException("视频不存在");
        }
        video.setLikeCount(videoLikeMapper.getLikeCount(id));

        // 3. 写缓存
        video.setViewCount(playCount.intValue());
        cacheHelper.set(key, video, Duration.ofHours(2));

        return toVo(video);
    }

    @Override
    public PageResult<VideoListItem> pageVideo(VideoPageDTo videoPageDTo) {
        //参数校验
        videoPageDTo.normalize();

        //查缓存
        String version = cacheHelper.get(RedisKeys.videoSearchVersion(), String.class);
        String key = RedisKeys.videoSearch(videoPageDTo, version);
        PageResult<VideoCacheDto> cached = cacheHelper.get(key, new TypeReference<PageResult<VideoCacheDto>>() {
        });
        if (cached != null) return toPageResult(cached);

        // 2. 查库
        PageHelper.startPage(videoPageDTo.getPage(), videoPageDTo.getPageSize());
        List<VideoCacheDto> videoList = videoMapper.findList(videoPageDTo);
        Page<VideoCacheDto> page = (Page<VideoCacheDto>) videoList;
        cached = new PageResult<>(page.getTotal(), page.getResult());

        // 3. 写缓存
        cacheHelper.set(key, cached, Duration.ofMinutes(30));

        return toPageResult(cached);
    }

    @Override
    public PageResult<VideoListItem> pageMyVideo(VideoMyPageDto videoPageDTo) {
        //参数校验
        videoPageDTo.normalize();

        //查缓存
        String key = RedisKeys.videoSearchMyVideo(videoPageDTo, (Long) UserContextUtil.get().get("id"));
        PageResult<VideoCacheDto> cached = cacheHelper.get(key, new TypeReference<PageResult<VideoCacheDto>>() {
        });
        if (cached != null) return toPageResult(cached);

        // 2. 查库
        Long userId = (Long) UserContextUtil.get().get("id");
        PageHelper.startPage(videoPageDTo.getPage(), videoPageDTo.getPageSize());
        List<VideoCacheDto> videoList = videoMapper.findMyList(videoPageDTo, userId);
        Page<VideoCacheDto> page = (Page<VideoCacheDto>) videoList;
        cached = new PageResult<>(page.getTotal(), page.getResult());

        // 3. 写缓存
        cacheHelper.set(key, cached, Duration.ofMinutes(30));
        return toPageResult(cached);
    }

    @Override
    public void videoLike(Long id) {
        Long userId = (Long) UserContextUtil.get().get("id");
        try {
            videoLikeMapper.addLike(id,userId);
            cacheHelper.add(RedisKeys.videoLikeUpdateFlag(), String.valueOf(id));
            cacheHelper.add(RedisKeys.videoLikeUsers(id),String.valueOf(userId));
        }catch (Exception e){
            log.warn("视频点赞失败{}", e.getMessage());
            throw new BusinessException("视频点赞失败");
        }
    }

    @Override
    public void unlike(Long id) {
        Long userId = (Long) UserContextUtil.get().get("id");
        try {
            videoLikeMapper.unlike(id, userId);
            cacheHelper.add(RedisKeys.videoLikeUpdateFlag(), String.valueOf(id));
            cacheHelper.rem(RedisKeys.videoLikeUsers(id),String.valueOf(userId));
        } catch (Exception e) {
            log.warn("视频取消点赞失败{}", e.getMessage());
            throw new BusinessException("视频取消点赞失败");
        }
    }

    @Override
    public void addComment(Long id, String comment) {
        Long userId = (Long) UserContextUtil.get().get("id");
        if (videoMapper.findById(id) == null)
            throw new BusinessException("视频不存在");
        try {
            commentMapper.addComment(id, userId, comment);
            cacheHelper.incr(RedisKeys.videoCommentVersion(id));
        }catch (Exception e){
            log.warn("视频评论失败{}", e.getMessage());
            throw new BusinessException("视频评论失败");
        }
    }

    @Override
    public PageCommentResult<CommentVo> commentList(Long id, CommentPageDto commentPageDto) {
        String vs = cacheHelper.get(RedisKeys.videoCommentVersion(id), String.class);
        String key = RedisKeys.videoComments(id,commentPageDto.getCursorId(),vs);
        PageCommentResult<CommentCacheDto> cached = cacheHelper.get(key, new TypeReference<PageCommentResult<CommentCacheDto>>() {});
        if (cached != null) return toPageCommentResult(cached);

        int size = commentPageDto.getSize()+1;
        List<CommentCacheDto> commentList = commentMapper.commentList(id, commentPageDto.getCursorTime(), commentPageDto.getCursorId(), size);
        boolean hasMore = commentList.size() > size -1;
        if (hasMore) {
            commentList = commentList.subList(0, size-1);   // 丢掉多查的那条
        }
        LocalDateTime nextCursor = null;
        Long nextCursorId = null;
        if (!commentList.isEmpty()) {
            nextCursor = commentList.getLast().getCreateTime();
            nextCursorId = commentList.getLast().getId();
        }
        cached = new PageCommentResult<>(commentList, hasMore, nextCursor, nextCursorId);
        cacheHelper.set(key, cached, Duration.ofMinutes(30));

        return toPageCommentResult(cached);
    }

    @Override
    public void deleteComment(Long id, Long commentId) {
        Long userId = (Long) UserContextUtil.get().get("id");
        Long commentUserId = commentMapper.findUserIdById(commentId,id);
        if (commentUserId == null) {
            throw new BusinessException("评论不存在");
        } else if (!userId.equals(commentUserId)){
                throw new BusinessException("无权限删除");
        }
        try {
            commentMapper.deleteComment(commentId);
            cacheHelper.incr(RedisKeys.videoCommentVersion(id));
        }catch (Exception e){
            log.warn("视频删除评论失败{}", e.getMessage());
            throw new BusinessException("视频删除评论失败");
        }
    }

    @Override
    public CursorPage<VideoListItem> feedPage(FeedPageDto dto) {
        dto.normalize();
        boolean hasTime = dto.getCursorTime() != null;
        boolean hasId   = dto.getCursorId() != null;
        if (hasTime != hasId) throw new BusinessException("游标参数不完整：cursorTime 与 cursorId 必须同时提供");
        if ("hot".equals(dto.getSort()))
            throw new BusinessException("推荐流暂未上线");
        String key = RedisKeys.videoFeed(dto);
        CursorPage<VideoCacheDto> cached = cacheHelper.get(key, new TypeReference<CursorPage<VideoCacheDto>>() {});
        if (cached != null) return toCursorPage(cached);

        int size = dto.getSize()+1;
        List<VideoCacheDto> list = videoMapper.findFeed(dto.getCursorTime(),dto.getCursorId(),size);
        boolean hasMore = list.size() > size - 1;
        if (hasMore) list = list.subList(0, size-1);

        LocalDateTime nextCursor = null;
        Long nextCursorId = null;
        if (!list.isEmpty()){
            VideoCacheDto last = list.getLast();
            nextCursor = last.getCreateTime();
            nextCursorId = last.getId();
        }
        CursorPage<VideoCacheDto> page = new CursorPage<>(list, hasMore, nextCursor, nextCursorId);
        cacheHelper.set(key, page, Duration.ofMinutes(5));
        return toCursorPage(page);
    }

    private CursorPage<VideoListItem> toCursorPage(CursorPage<VideoCacheDto> cached) {
        List<VideoListItem> list = cached.getItems().stream().
                map(item->{
                    VideoListItem vo = new VideoListItem();
                    BeanUtils.copyProperties(item, vo);
                    vo.setCoverUrl(fileService.presign(imageBucket, item.getCoverKey(), 10));
                    return vo;
                }).toList();
        return new CursorPage<>(list,cached.getHasMore(),cached.getNextCursor(),cached.getNextCursorId());
    }

    //转换为评论详细实体
    private PageCommentResult<CommentVo> toPageCommentResult(PageCommentResult<CommentCacheDto> cached) {
        List<CommentVo> list = cached.getItems().stream().
                map(item -> {
                    CommentVo vo = new CommentVo();
                    BeanUtils.copyProperties(item, vo);
                    vo.setAvatarUrl(item.getAvatarKey() != null ? fileService.presign(imageBucket, item.getAvatarKey(), 30) : null);
                    return vo;
                })
                .toList();
        return new PageCommentResult<>(list,cached.getHasMore(),cached.getNextCursor(),cached.getNextCursorId());
    }

    //转换为视频详细详细实体
    private VideoVo toVo(Video video) {
        VideoVo vo = new VideoVo();
        BeanUtils.copyProperties(video, vo);
        String coverUrl = video.getCoverKey() != null ? fileService.presign(imageBucket, video.getCoverKey(), 30) : null;
        String videoUrl = video.getVideoKey() != null ? fileService.presign(videoBucket, video.getVideoKey(), 100) : null;
        String creatorCoverUrl = video.getAvatarKey() != null ? fileService.presign(imageBucket, video.getAvatarKey(), 30) : null;
        vo.setCoverUrl(coverUrl);
        vo.setVideoUrl(videoUrl);
        vo.setCreatorCoverUrl(creatorCoverUrl);
        Claims claims = UserContextUtil.get();
        Long userId = claims==null ? null : (Long) claims.get("id");
        if (userId == null){
            vo.setLiked(0);
        } else{
            String key = RedisKeys.videoLikeUsers(video.getId());
            Integer liked = cacheHelper.isMember(key, String.valueOf(userId));
            if(liked == null){
                if (video.getLikeCount() != null && video.getLikeCount() == 0)
                    liked = 0;
                else {
                    liked = videoLikeMapper.isLiked(video.getId(), userId);
                    if (liked != null && liked == 1)
                        cacheHelper.add(key,String.valueOf(userId));
                }
            }
            vo.setLiked(liked == null ? 0 : liked );
        }
        vo.setLikeCount(video.getLikeCount());
        return vo;
    }

    //缓存转换为分页结果
    private PageResult<VideoListItem> toPageResult(PageResult<VideoCacheDto> cached) {
        List<VideoListItem> list = cached.getItems().stream().
                map(item -> {
                    VideoListItem vo = new VideoListItem();
                    BeanUtils.copyProperties(item, vo);
                    vo.setCoverUrl(fileService.presign(imageBucket, item.getCoverKey(), 10));
                    return vo;
                })
                .toList();
        return new PageResult<>(cached.getTotal(), list);
    }

    // 判断字符串是否为有效整数
    private boolean isValidInteger(String s) {
        if (s == null || s.isEmpty()) return false;
        // 只接受纯数字（不含负号，播放量不该是负数）
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return false;
        }
        return true;
    }
}
