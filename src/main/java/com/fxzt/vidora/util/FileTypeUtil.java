package com.fxzt.vidora.util;

import java.util.Map;

public class FileTypeUtil {

    /**
     * 根据原始文件名提取后缀名
     * @param fileName 文件名
     */
    public static String getFileExtension(String fileName){
        if (fileName == null || fileName.isEmpty()){
            return null;
        }

        String name = fileName.replace("\\","/");
        int lastIndexOf = name.lastIndexOf("/");
        if (lastIndexOf >= 0) name=name.substring(lastIndexOf+1);
        lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf < 0 || lastIndexOf == name.length() - 1) return null;

        return name.substring(lastIndexOf + 1).toLowerCase();
    }

    public static String getFileMime(String ext, Map<String, String> mimeMap){
        return mimeMap.get(ext);
    }
}
