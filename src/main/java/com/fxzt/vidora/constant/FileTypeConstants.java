package com.fxzt.vidora.constant;

import java.util.Map;
import java.util.Set;

public class FileTypeConstants {

    /** 视频：扩展名 -> MIME */
    public static final Map<String, String> VIDEO_TYPES = Map.of(
            "mp4",  "video/mp4",
            "webm", "video/webm",
            "mov",  "video/quicktime",
            "avi",  "video/x-msvideo",
            "mkv",  "video/x-matroska"
    );

    /** 图片：扩展名 -> MIME */
    public static final Map<String, String> IMAGE_TYPES = Map.of(
            "jpg",  "image/jpeg",
            "jpeg", "image/jpeg",
            "png",  "image/png",
            "gif",  "image/gif",
            "webp", "image/webp"
    );

    public static final Set<String> VIDEO_EXT = VIDEO_TYPES.keySet();
    public static final Set<String> IMAGE_EXT = IMAGE_TYPES.keySet();
}
