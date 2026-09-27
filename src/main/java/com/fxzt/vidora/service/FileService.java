package com.fxzt.vidora.service;

import com.fxzt.vidora.common.Result;
import com.fxzt.vidora.pojo.query.InitUploadDto;
import com.fxzt.vidora.pojo.vo.InitUploadVo;
import com.fxzt.vidora.pojo.vo.PartVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
     * 获取预签名URL
     * @param bucket 桶名
     * @param key 对象名
     * @param duration 有效期
     * @return 预签名URL
     */
    String presign(String bucket,String key,Integer duration);

    /**
     * 初始化上传
     * @param initUploadDto 初始化上传参数
     * @return 初始化上传结果
     */
    InitUploadVo init(InitUploadDto initUploadDto);

    /**
     * 上传分片
     * @param file 分片文件
     * @param partNumber 分片编号
     * @param uploadId 上传ID
     * @param id 视频ID
     * @return 上传分片结果
     */
    PartVo uploadPart(MultipartFile file, Integer partNumber, String uploadId, Long id);

    /**
     * 合并分片
     * @param id 视频ID
     * @param parts 分片数据
     */
    void complete(Long id, List<PartVo> parts, String uploadId);

    /**
     * 中断上传,清理
     * @param id 视频ID
     * @param uploadId 上传ID
     */
    void abort(Long id, String uploadId);
}
