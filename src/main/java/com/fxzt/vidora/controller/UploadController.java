package com.fxzt.vidora.controller;

import com.fxzt.vidora.common.Result;
import com.fxzt.vidora.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@Slf4j
@RestController
@RequestMapping("/upload")
public class UploadController {

    @Autowired
    private FileService fileService;

    /**
     * 上传视频
     * @param file 视频文件
     */

    @PostMapping("/video")
    public Result<String> uploadVideo(MultipartFile file) {
        log.info("上传视频{}",file.getName());
        return Result.success(fileService.uploadVideo(file));
    }

    @PostMapping("/image")
    public Result<String> uploadImage(MultipartFile file,String scene){
        log.info("上传图片{}",file.getName());
        return Result.success(fileService.uploadImage(file, scene));
    }

}
