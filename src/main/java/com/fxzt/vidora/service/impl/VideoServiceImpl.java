package com.fxzt.vidora.service.impl;

import com.fxzt.vidora.exception.BusinessException;
import com.fxzt.vidora.mapper.VideoMapper;
import com.fxzt.vidora.pojo.entity.Video;
import com.fxzt.vidora.pojo.query.VideoCacheDto;
import com.fxzt.vidora.pojo.query.VideoCreateDTo;
import com.fxzt.vidora.pojo.query.VideoPageDTo;
import com.fxzt.vidora.pojo.vo.PageResult;
import com.fxzt.vidora.pojo.vo.VideoListItem;
import com.fxzt.vidora.pojo.vo.VideoVo;
import com.fxzt.vidora.service.VideoService;
import com.fxzt.vidora.util.UserContextUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisConnectionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class VideoServiceImpl implements VideoService {

    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private StringRedisTemplate redis;
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
    }

    @Override
    public VideoVo getVideoById(Long id) {
        String key = "vidora:video:detail:" + id;
        // 1. 读缓存：任何异常都当作"未命中"
        String json;
        try {
            json = redis.opsForValue().get(key);
            if (json != null) {
                return toVo(objectMapper.readValue(json, Video.class));
            }
        }  catch (RedisConnectionFailureException |RedisCommandTimeoutException e) {
        // Redis 不可用是预期内的，降级即可 —— 不打堆栈，避免刷屏
        log.warn("读缓存失败(Redis不可用)，降级查库。key={}", key);
        } catch (Exception e) {
        // 其他异常说明可能有代码问题 —— 必须打堆栈
        log.error("读缓存失败(未知原因)。key={}", key, e);
        }

        // 2. 查库
        Video video = videoMapper.getVideoById(id);
        if (video == null) {
            throw new BusinessException("视频不存在");
        }

        /* String coverUrl = video.getCoverKey() != null ? fileService.getImageUrl(video.getCoverKey()) : null;
        String videoUrl = video.getVideoKey() != null ? fileService.getVideoUrl(video.getVideoKey()) : null;
        String creatorCoverUrl = video.getAvatarKey() != null ? fileService.getImageUrl(video.getAvatarKey()) : null;

        VideoVo videoVo = VideoVo.builder()
                .id(video.getId())
                .title(video.getTitle())
                .introduction(video.getIntroduction())
                .duration(video.getDuration())
                .creatorName(video.getNickname())
                .creatorCoverUrl(creatorCoverUrl)
                .coverUrl(coverUrl)
                .videoUrl(videoUrl)
                .likeCount(video.getLikeCount())
                .createTime(video.getCreateTime())
                .favoriteCount(video.getFavoriteCount())
                .viewCount(video.getViewCount())
                .build();*/


        // 3. 写缓存：失败只记日志，不影响返回
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(video), Duration.ofHours(2));
        }catch (RedisConnectionFailureException | RedisConnectionException | RedisCommandTimeoutException e){
            log.debug("写缓存失败，key={}", key, e);
        }catch (Exception e) {
            log.error("写缓存失败(未知原因)，key={}", key, e);
        }
        return toVo(video);
    }

    @Override
    public PageResult<VideoListItem> pageVideo(VideoPageDTo videoPageDTo) {
        // 去除空值，设置默认页码
        videoPageDTo.setPage(videoPageDTo.getPage()!=null?videoPageDTo.getPage():1);
        // 设置每页最大数量
        videoPageDTo.setPageSize(videoPageDTo.getPageSize()!=null?Math.min(videoPageDTo.getPageSize(), 100):10);
        //防止非法排序字段，防止非法排序方式
        List<String> orderbylist = List.of("create_time", "like_count", "view_count", "favorite_count");
        if(!orderbylist.contains(videoPageDTo.getOrderBy())) videoPageDTo.setOrderBy("create_time");
        if(!"asc".equals(videoPageDTo.getOrder())&&!"desc".equals(videoPageDTo.getOrder())) videoPageDTo.setOrder("desc");

        String key = String.format("vidora:video:search:%s:%s:%s:%d:%d",
                videoPageDTo.getKeyword(), videoPageDTo.getOrderBy(), videoPageDTo.getOrder(), videoPageDTo.getPage(), videoPageDTo.getPageSize());

        //查缓存
        String json;
        try {
            json = redis.opsForValue().get(key);
            if (json != null) {
                return toPageResult(objectMapper.readValue(json, new TypeReference<PageResult<VideoCacheDto>>() {}));
            }
        }  catch (RedisConnectionFailureException |RedisCommandTimeoutException e) {
            // Redis 不可用是预期内的，降级即可 —— 不打堆栈，避免刷屏
            log.warn("读缓存失败(Redis不可用)，降级查库。key={}", key);
        } catch (Exception e) {
            // 其他异常说明可能有代码问题 —— 必须打堆栈
            log.error("读缓存失败(未知原因)。key={}", key, e);
        }

        // 2. 查库
        PageHelper.startPage(videoPageDTo.getPage(), videoPageDTo.getPageSize());
        List<VideoCacheDto> videoList = videoMapper.findList(videoPageDTo);
        Page<VideoCacheDto> page = (Page<VideoCacheDto>) videoList;
        PageResult<VideoCacheDto> cached = new PageResult<>(page.getTotal(), page.getResult());

        // 3. 写缓存：失败只记日志，不影响返回
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(cached), Duration.ofMinutes(30));
        }catch (RedisConnectionFailureException | RedisConnectionException | RedisCommandTimeoutException e){
            log.debug("写缓存失败，key={}", key, e);
        }catch (Exception e) {
            log.error("写缓存失败(未知原因)，key={}", key, e);
        }

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
