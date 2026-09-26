package com.fxzt.vidora.service.impl;

import com.fxzt.vidora.constant.FileTypeConstants;
import com.fxzt.vidora.constant.UploadScene;
import com.fxzt.vidora.exception.BusinessException;
import com.fxzt.vidora.exception.UploadException;
import com.fxzt.vidora.service.FileService;
import com.fxzt.vidora.util.FileTypeUtil;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.VideoBucketName}")
    private String videoBucketName;

    @Value("${minio.ImageBucketName}")
    private String imageBucketName;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Override
    public String uploadVideo(MultipartFile file) {
        long size = file.getSize();
        Long maxSize = 2 * 1024 * 1024 * 1024L;
        if (size > maxSize){
            throw new UploadException(413,"视频太大");
        }

        //获取文件类型
        String name = file.getOriginalFilename();

        String ext = FileTypeUtil.getFileExtension(name);
        if (ext==null || !FileTypeConstants.VIDEO_EXT.contains(ext)){
            throw new UploadException(415,"不支持的视频文件类型");
        }

        String mime = FileTypeUtil.getFileMime(ext, FileTypeConstants.VIDEO_TYPES);
        String contentType = file.getContentType();
        if (!mime.equals(contentType)){
            log.warn("客户端 Content-Type={} 与扩展名推断={} 不一致，以扩展名为准", contentType, mime);
        }

        // 上传文件
        String objectName = LocalDate.now().format(FMT)+"/"+ UUID.randomUUID()+"."+ext ;
        try {
            InputStream inputStream = file.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(videoBucketName)
                            .stream(inputStream, size , -1L)
                            .object(objectName)
                            .contentType(mime)
                            .build()
            );
            return objectName;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String uploadImage(MultipartFile file, String scene) {
        //获取上传场景
        if (UploadScene.fromDir(scene) == null) {
            throw new BusinessException("上传场景错误");
        }
        UploadScene uploadScene = UploadScene.valueOf(scene.toUpperCase());
        //获取文件类型
        String name = file.getOriginalFilename();

        String ext = FileTypeUtil.getFileExtension(name);
        if (ext==null || !FileTypeConstants.IMAGE_EXT.contains(ext)){
            throw new UploadException(415,"不支持的图片文件类型");
        }

        String mime = FileTypeUtil.getFileMime(ext, FileTypeConstants.IMAGE_TYPES);
        String contentType = file.getContentType();
        if (!mime.equals(contentType)){
            log.warn("客户端 Content-Type={} 与扩展名推断={} 不一致，以扩展名为准", contentType, mime);
        }

        // 上传文件
        String objectName = uploadScene.getDir() + "/"  + LocalDate.now().format(FMT) + "/" + UUID.randomUUID() + "." + ext;
        long size = file.getSize();
        if (size > uploadScene.getMaxSize())
            throw new UploadException(413,"图片太大");
        try {
            InputStream inputStream = file.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(imageBucketName)
                            .stream(inputStream, size , -1L)
                            .object(objectName)
                            .contentType(mime)
                            .build()
            );
            return objectName;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getImageUrl(String imageKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Http.Method.GET)
                            .bucket(imageBucketName)
                            .object(imageKey)
                            .expiry(20, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getVideoUrl(String videoKey) {
        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Http.Method.GET)
                            .bucket(videoBucketName)
                            .object(videoKey)
                            .expiry(2, TimeUnit.HOURS)
                            .build()
            );
            return url;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
