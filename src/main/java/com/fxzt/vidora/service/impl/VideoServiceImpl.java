package com.fxzt.vidora.service.impl;

import com.fxzt.vidora.mapper.VideoMapper;
import com.fxzt.vidora.pojo.entity.Video;
import com.fxzt.vidora.pojo.query.VideoPageDTo;
import com.fxzt.vidora.pojo.vo.PageResult;
import com.fxzt.vidora.service.VideoService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisConnectionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
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


    @Override
    public void addVideo(Video video) {
        videoMapper.addVideo(video);
    }

    @Override
    public Video getVideoById(Long id) {
        String key = "vidora:video:detail:" + id;
        // 1. 读缓存：任何异常都当作"未命中"
        String json = null;
        try {
            json = redis.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json, Video.class);
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

        // 3. 写缓存：失败只记日志，不影响返回
        if (video != null) {
            try {
                redis.opsForValue().set(key, objectMapper.writeValueAsString(video), Duration.ofMinutes(30));
            }catch (RedisConnectionFailureException | RedisConnectionException | RedisCommandTimeoutException e){
                log.debug("写缓存失败，key={}", key, e);
            } catch (Exception e) {
                log.error("写缓存失败(未知原因)，key={}", key, e);
            }
        }
        return video;
    }

    @Override
    public PageResult<Video> pageVideo(VideoPageDTo videoPageDTo) {
        // 去除空值，设置默认页码
        videoPageDTo.setPage(videoPageDTo.getPage()!=null?videoPageDTo.getPage():1);
        // 设置每页最大数量
        videoPageDTo.setPageSize(videoPageDTo.getPageSize()!=null?Math.min(videoPageDTo.getPageSize(), 100):10);
        //防止非法排序字段，防止非法排序方式
        List<String> orderbylist = List.of("create_time", "like_count", "view_count", "favorite_count");
        if(!orderbylist.contains(videoPageDTo.getOrderBy())) videoPageDTo.setOrderBy("create_time");
        if(!"asc".equals(videoPageDTo.getOrder())&&!"desc".equals(videoPageDTo.getOrder())) videoPageDTo.setOrder("desc");

        PageHelper.startPage(videoPageDTo.getPage(), videoPageDTo.getPageSize());
        List<Video> videoList = videoMapper.findAll(videoPageDTo);
        Page<Video> page = (Page<Video>) videoList;
        return new PageResult<Video>(page.getTotal(), page.getResult());
    }
}
