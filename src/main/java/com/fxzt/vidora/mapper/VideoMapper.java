package com.fxzt.vidora.mapper;

import com.fxzt.vidora.pojo.entity.Video;
import com.fxzt.vidora.pojo.query.VideoPageDTo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface VideoMapper {
    /**
     * 添加视频
     */
    void addVideo(Video video);

    /**
     * 根据id获取视频
     */
    @Select("select * from video where id = #{id}")
    Video getVideoById(Long id);

    /**
     * 分页查询视频
     */
    List<Video> findAll(VideoPageDTo videoPageDTo);
}
