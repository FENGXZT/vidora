package com.fxzt.vidora.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    /**
     * 上传视频
     * @param file 视频文件
     */
    String uploadVideo(MultipartFile file);

    /**
     * 上传图片
     * @param file 图片文件
     * @param uploadScene 上传场景
     */
    String uploadImage(MultipartFile file, String uploadScene);

    /**
     * 获取图片的URL
     * @param imageKey 图片的key
     * @return 图片的URL
     */
    String getImageUrl(String imageKey);

    /**
     * 获取视频的URL
     * @param videoKey 视频的key
     * @return 视频的URL
     */
    String getVideoUrl(String videoKey);

}
