package com.fxzt.vidora.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VideoVo {
    private Long id;                    //视频id
    private String title;                    //视频标题
    private String introduction;             //视频简介
    private String coverUrl;                 //视频封面
    private String videoUrl;                 //视频地址
    private Integer duration;               //视频时长
    private Long viewCount;              //播放次数
    private Integer likeCount;              //点赞次数
    private Integer favoriteCount;          //收藏次数
    private LocalDateTime createTime;          //创建时间

    private String nickName;          //创建者名称
    private String creatorCoverUrl;     //创建者头像地址

    private Integer liked;              //是否点赞，1为点赞，0为未点赞
    private Integer favorited;          //是否收藏，1为收藏，0为未收藏
}
