package com.fxzt.vidora.constant;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public enum UploadScene {
    COVER("cover", 50 * 1024 * 1024L),   // 50MB
    AVATAR("avatar", 20 * 1024 * 1024L);  // 20MB

    private final String dir;
    private final long maxSize;

    UploadScene(String dir, long maxSize) {
        this.dir = dir;
        this.maxSize = maxSize;
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
}