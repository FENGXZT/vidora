package com.fxzt.vidora.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoListItem {

    private Long id;                    //视频id
    private String title;               //视频标题
    private String coverUrl;            //封面图片
    private Integer duration;           //视频时长
    private Integer viewCount;          //播放数
    private LocalDateTime createTime;   //创建时间

    private String nickName;            //发布者昵称

    private LocalDateTime likeTime;      //点赞时间
    private LocalDateTime favoriteTime;  //收藏时间
}
