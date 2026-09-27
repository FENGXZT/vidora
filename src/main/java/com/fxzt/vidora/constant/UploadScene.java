package com.fxzt.vidora.constant;

import lombok.Getter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public enum UploadScene {
    COVER("cover", 50 * 1024 * 1024L,FileTypeConstants.IMAGE_TYPES),   // 50MB
    AVATAR("avatar", 20 * 1024 * 1024L, FileTypeConstants.IMAGE_TYPES),  // 20MB
    VIDEO("video", 100 * 1024 * 1024L, FileTypeConstants.VIDEO_TYPES);

    private final String dir;
    private final long maxSize;
    private final Map<String, String> allowedTypes;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    UploadScene(String dir, long maxSize, Map<String, String> allowedTypes) {
        this.dir = dir;
        this.maxSize = maxSize;
        this.allowedTypes = allowedTypes;
    }

    private static final Map<String, UploadScene> DIR_MAP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(UploadScene::getDir, s -> s));

    /**
     * 根据 dir 查枚举，找不到返回 null
     */
    public static UploadScene fromDir(String dir) {
        if (dir == null) return null;
        return DIR_MAP.get(dir);
    }

    public String buildObjectKey(String ext) {
        String datePath = LocalDate.now().format(FMT);
        String uuid = UUID.randomUUID().toString();
        if (dir.equals("video")) {
            return datePath + "/" + uuid + "." + ext;
        }
        return dir + "/" + datePath + "/" + uuid + "." + ext;
    }
}