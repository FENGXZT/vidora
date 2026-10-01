package com.fxzt.vidora.service.impl;

import com.fxzt.vidora.exception.BusinessException;
import com.fxzt.vidora.mapper.VideoMapper;
import com.fxzt.vidora.pojo.entity.Video;
import com.fxzt.vidora.pojo.query.VideoCacheDto;
import com.fxzt.vidora.pojo.query.VideoCreateDTo;
import com.fxzt.vidora.pojo.query.VideoMyPageDto;
import com.fxzt.vidora.pojo.query.VideoPageDTo;
import com.fxzt.vidora.pojo.vo.PageResult;
import com.fxzt.vidora.pojo.vo.VideoListItem;
import com.fxzt.vidora.pojo.vo.VideoVo;
import com.fxzt.vidora.service.VideoService;
import com.fxzt.vidora.util.CacheHelper;
import com.fxzt.vidora.util.RedisKeys;
import com.fxzt.vidora.util.UserContextUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class VideoServiceImpl implements VideoService {

    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private CacheHelper cacheHelper;
    @Autowired
    private FileServiceImpl fileService;

    @Value("${minio.VideoBucketName}")
    private String videoBucket;
    @Value("${minio.ImageBucketName}")
    private String imageBucket;

    @Override
    public void addVideo(VideoCreateDTo videoCreateDTo ) {
        Video video = Video.builder()
                        .videoKey(videoCreateDTo.getVideoKey())
                        .coverKey(videoCreateDTo.getCoverKey())
                        .title(videoCreateDTo.getTitle())
                        .introduction(videoCreateDTo.getIntroduction())
                        .duration(videoCreateDTo.getDuration())
                        .status(2)
                        .userId((Long) UserContextUtil.get().get("id"))
                        .build();
        videoMapper.addVideo(video);
        // 删除缓存
        cacheHelper.delete(RedisKeys.videoSearchAll());
    }

    @Override
    public VideoVo getVideoById(Long id) {
        // 1. 读缓存
        String key = RedisKeys.videoDetail(id);
        Video video = cacheHelper.get(key,Video.class);
        if (video != null) return toVo(video);

        // 2. 查库
        video = videoMapper.getVideoById(id);
        if (video == null) {
            throw new BusinessException("视频不存在");
        }

        // 3. 写缓存
        cacheHelper.set(key,video,Duration.ofHours(2));

        return toVo(video);
    }

    @Override
    public PageResult<VideoListItem> pageVideo(VideoPageDTo videoPageDTo) {
        //参数校验
        videoPageDTo.normalize();

        //查缓存
        String key = RedisKeys.videoSearch(videoPageDTo);
        PageResult<VideoCacheDto> cached = cacheHelper.get(key, new TypeReference<PageResult<VideoCacheDto>>() {
        });
        if (cached != null) return toPageResult(cached);

        // 2. 查库
        PageHelper.startPage(videoPageDTo.getPage(), videoPageDTo.getPageSize());
        List<VideoCacheDto> videoList = videoMapper.findList(videoPageDTo);
        Page<VideoCacheDto> page = (Page<VideoCacheDto>) videoList;
        cached = new PageResult<>(page.getTotal(), page.getResult());

        // 3. 写缓存
        cacheHelper.set(key,cached,Duration.ofMinutes(30));

        return toPageResult(cached);
    }

    @Override
    public PageResult<VideoListItem> pageMyVideo(VideoMyPageDto videoPageDTo) {
        //参数校验
        videoPageDTo.normalize();

        //查缓存
        String key = RedisKeys.videoSearchMyVideo(videoPageDTo,(Long) UserContextUtil.get().get("id"));
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
        cacheHelper.set(key,cached,Duration.ofMinutes(30));
        return toPageResult(cached);
    }

    //转换为视频详细详细实体
    private VideoVo toVo(Video video){
        VideoVo vo =new VideoVo();
        BeanUtils.copyProperties(video, vo);
        String coverUrl = video.getCoverKey() != null ? fileService.presign(imageBucket,video.getCoverKey(),30) : null;
        String videoUrl = video.getVideoKey() != null ? fileService.presign(videoBucket,video.getVideoKey(),100) : null;
        String creatorCoverUrl = video.getAvatarKey() != null ? fileService.presign(imageBucket,video.getAvatarKey(),30) : null;
        vo.setCoverUrl(coverUrl);
        vo.setVideoUrl(videoUrl);
        vo.setCreatorCoverUrl(creatorCoverUrl);
        return vo;
    }

    //缓存转换为分页结果
    private PageResult<VideoListItem> toPageResult(PageResult<VideoCacheDto> cached){
        List<VideoListItem> list = cached.getItems().stream().
                map(item ->{
                    VideoListItem vo = new VideoListItem();
                    BeanUtils.copyProperties(item, vo);
                    vo.setCoverUrl(fileService.presign(imageBucket,item.getCoverKey(),10));
                    return vo;
                })
                .toList();
        return new PageResult<>(cached.getTotal(), list);
    }
}
