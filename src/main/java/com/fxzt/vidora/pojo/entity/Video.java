package com.fxzt.vidora.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Video {

    private Long id;                  //视频id
    private Long userId;             //发布者ID
    private String title;               //视频标题
    private String introduction;        //视频简介
    private String coverKey;            //封面图片
    private String videoKey;            //视频文件
    private Integer duration;           //视频时长
    private Integer status;            //视频状态 状态:0上传中,1转码中,2可播放,3转码失败,4已下架
    private Integer likeCount;         //点赞数
    private Integer viewCount;         //播放数
    private Integer favoriteCount;     //收藏数
    private LocalDateTime createTime;  //创建时间
    private LocalDateTime updateTime;  //更新时间


    private String originalFilename;    //原始文件名


    private String nickName;          //发布者昵称
    private String avatarKey;         //发布者头像
}
