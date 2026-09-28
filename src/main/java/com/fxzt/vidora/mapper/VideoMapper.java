package com.fxzt.vidora.mapper;

import com.fxzt.vidora.pojo.entity.Video;
import com.fxzt.vidora.pojo.query.VideoCacheDto;
import com.fxzt.vidora.pojo.query.VideoPageDTo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
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
    Video getVideoById(Long id);

    /**
     * 分页查询视频
     */
    List<VideoCacheDto> findList(VideoPageDTo videoPageDTo);

    /**
     * 根据id获取视频详细
     */
    @Select("select * from video where id = #{id}")
    Video findById(Long id);

    /**
     * 更新视频
     */
    void updateVideo(Video video);

    /**
     * 根据id删除视频
     */
    @Delete("delete from video where id = #{id}")
    void deleteById(Long id);

    /**
     * 获取清理列表
     */
    @Select("select * from video where status = #{status} and create_time < #{time} ")
    List<Video> cleanList(int status, LocalDateTime time);

    /**
     * 根据文件哈希值获取已存在的视频
     */
    @Select("select * from video where file_hash = #{fileHash} and status = 2 limit 1")
    Video findReusableByHash(String fileHash);

    /**
     * 根据文件哈希值和id,用户上传中断的视频
     */
    @Select("select * from video where file_hash = #{fileHash} and status = 0 and user_id = #{userId} limit 1")
    Video findResumableByHash(String fileHash, Long userId);
}
