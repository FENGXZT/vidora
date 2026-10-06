package com.fxzt.vidora.pojo.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoCacheDto {
    private Long id;                    //视频id
    private String title;                    //视频标题
    private String coverKey;                 //视频封面
    private Integer duration;               //视频时长
    private Integer viewCount;              //播放次数
    private LocalDateTime createTime;          //创建时间

    private String nickName;          //创建者名称

    /** ★ 仅供游标使用：= vl.id / vf.id（点赞/收藏【行】的 id），与排序/过滤列同源 */
    private Long cursorId;                  // 游标id
    private LocalDateTime cursorTime;      // 游标时间
}
