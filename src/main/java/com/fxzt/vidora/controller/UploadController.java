package com.fxzt.vidora.controller;

import com.fxzt.vidora.common.Result;
import com.fxzt.vidora.pojo.query.InitUploadDto;
import com.fxzt.vidora.pojo.vo.InitUploadVo;
import com.fxzt.vidora.pojo.vo.PartVo;
import com.fxzt.vidora.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


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

    /**
     * 初始化上传
     * @param initUploadDto 初始化上传参数
     */
    @PostMapping("/video/init")
    public Result<InitUploadVo> init(@RequestBody InitUploadDto initUploadDto){
        log.info("初始化上传{}",initUploadDto.getOriginalFilename());
        return Result.success(fileService.init(initUploadDto));
    }

    /**
     * 上传分片
     * @param file 分片文件
     */
    @PostMapping("/video/part")
    public Result<PartVo> uploadPart(MultipartFile file,Integer partNumber,Long id){
        log.info("上传分片{}",partNumber);
        return Result.success(fileService.uploadPart(file, partNumber, id));
    }

    /**
     * 合并分片
     * @param id 视频id
     * @param parts 分片列表
     */
    @PostMapping("/video/complete")
    public Result<Void> complete(Long id,@RequestBody List<PartVo> parts){
        log.info("分片完成上传，开始合并{}",id);
        fileService.complete(id, parts);
        return Result.success();
    }

    /**
     * 中断上传,清理
     * @param id 视频id
     */
    @PostMapping("/video/abort")
    public Result<Void> abort(Long id){
        log.info("中断上传，开始清理{}",id);
        fileService.abort(id);
        return Result.success();
    }
}
