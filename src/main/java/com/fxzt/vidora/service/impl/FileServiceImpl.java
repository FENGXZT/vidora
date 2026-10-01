package com.fxzt.vidora.service.impl;

import com.fxzt.vidora.constant.UploadScene;
import com.fxzt.vidora.exception.BusinessException;
import com.fxzt.vidora.exception.UploadException;
import com.fxzt.vidora.mapper.VideoMapper;
import com.fxzt.vidora.pojo.entity.Video;
import com.fxzt.vidora.pojo.query.InitUploadDto;
import com.fxzt.vidora.pojo.vo.InitUploadVo;
import com.fxzt.vidora.pojo.vo.PartVo;
import com.fxzt.vidora.service.FileService;
import com.fxzt.vidora.util.FileTypeUtil;
import com.fxzt.vidora.util.UserContextUtil;
import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private S3Client s3Client;
    @Autowired
    private S3Presigner s3Presigner;

    @Autowired
    private VideoMapper videoMapper;

    @Value("${minio.VideoBucketName}")
    private String videoBucketName;

    @Value("${minio.ImageBucketName}")
    private String imageBucketName;

    @Override
    public String uploadVideo(MultipartFile file) {
        UploadScene uploadScene = UploadScene.VIDEO;
        //获取文件类型
        String ext = FileTypeUtil.getFileExtension(file.getOriginalFilename());
        String mime = resolveMime(file.getOriginalFilename(), file.getContentType(), uploadScene);

        long size = file.getSize();
        if (size > uploadScene.getMaxSize()){
            throw new UploadException(413,"视频太大");
        }
        // 上传文件
        String objectName = uploadScene.buildObjectKey(ext);
        try {
            InputStream inputStream = file.getInputStream();
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(videoBucketName)
                    .key(objectName)
                    .contentType(mime)
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, size));
            return objectName;
        } catch (Exception e) {
            throw new UploadException(500, "上传失败");
        }
    }

    @Override
    public String uploadImage(MultipartFile file, String scene) {
        //获取上传场景
        if (UploadScene.fromDir(scene) == null) {
            throw new BusinessException("上传场景错误");
        }
        UploadScene uploadScene = UploadScene.valueOf(scene.toUpperCase());
        String ext = FileTypeUtil.getFileExtension(file.getOriginalFilename());
        String mime = resolveMime(file.getOriginalFilename(), file.getContentType(), uploadScene);

        // 上传文件
        String objectName = uploadScene.buildObjectKey(ext);
        long size = file.getSize();
        if (size > uploadScene.getMaxSize())
            throw new UploadException(413,"图片太大");
        try {
            InputStream inputStream = file.getInputStream();
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(imageBucketName)
                    .key(objectName)
                    .contentType(mime)
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, size));
            return objectName;
        } catch (Exception e) {
            throw new UploadException(500, "上传失败");
        }
    }

    @Override
    public String presign(String bucket,String key,Integer duration){
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(duration))
                    .getObjectRequest(
                            GetObjectRequest.builder().bucket(bucket).key(key).build()
                    )
                    .build();
            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            log.error("生成预签名 URL 失败", e);
            throw new UploadException(500, "生成预签名 URL 失败");
        }
    }

    @Override
    public InitUploadVo init(InitUploadDto initUploadDto) {
        Boolean isReuse = false;
        //分析文件hash，决定分支
        if (StringUtils.hasText(initUploadDto.getFileHash())) {
            //1.秒传，复用视频key创建新视频记录
            Video exist = videoMapper.findReusableByHash(initUploadDto.getFileHash());
            if (exist != null) {
                    Video newVideo = Video.builder()
                            .title(initUploadDto.getTitle())
                            .videoKey(exist.getVideoKey())
                            .originalFilename(initUploadDto.getOriginalFilename())
                            .status(2)
                            .userId((Long) UserContextUtil.get().get("id"))
                            .coverKey(initUploadDto.getCoverKey())
                            .duration(initUploadDto.getDuration())
                            .introduction(initUploadDto.getIntroduction())
                            .fileHash(initUploadDto.getFileHash())
                            .build();
                    videoMapper.addVideo(newVideo);
                    return InitUploadVo.builder()
                            .mode("instant")
                            .id(newVideo.getId())
                            .build();
            }
            //2.断点续传
            exist = videoMapper.findResumableByHash(initUploadDto.getFileHash(), (Long) UserContextUtil.get().get("id"));
            if (exist != null){
                try {
                    List<Integer> uploadedParts = listUploadedParts(exist.getUploadId(), exist.getVideoKey());
                    return InitUploadVo.builder()
                            .mode("resume")
                            .id(exist.getId())
                            .uploadId(exist.getUploadId())
                            .partSize(5 * 1024 * 1024)
                            .videoKey(exist.getVideoKey())
                            .uploadedParts(uploadedParts)
                            .build();
                }catch (NoSuchUploadException e){
                    log.warn("上传会话已失效，降级处理");
                    isReuse = true;
                }catch (Exception e) {
                    throw e;
                }
            }
        }

        //全新上传
        //生成key
        UploadScene uploadScene = UploadScene.VIDEO;
        String mime = resolveMime(initUploadDto.getOriginalFilename(),initUploadDto.getContentType() ,uploadScene);
        String objectName = uploadScene.buildObjectKey(FileTypeUtil.getFileExtension(initUploadDto.getOriginalFilename()));

        //创建分片上传任务
        CreateMultipartUploadResponse resp;
        try {
             resp = s3Client.createMultipartUpload(
                    CreateMultipartUploadRequest.builder()
                            .bucket(videoBucketName)
                            .key(objectName)
                            .contentType(mime)
                            .build()
            );
        } catch (Exception e) {
            throw new BusinessException("创建上传任务失败");
        }
        String uploadId = resp.uploadId();
        Video video ;
        if (!isReuse) {
            video = Video.builder()
                    .title(initUploadDto.getTitle())
                    .videoKey(objectName)
                    .originalFilename(initUploadDto.getOriginalFilename())
                    .status(0)
                    .userId((Long) UserContextUtil.get().get("id"))
                    .coverKey(initUploadDto.getCoverKey())
                    .duration(initUploadDto.getDuration())
                    .introduction(initUploadDto.getIntroduction())
                    .uploadId(uploadId)
                    .fileHash(initUploadDto.getFileHash())
                    .build();
            videoMapper.addVideo(video);
        } else {
            video = videoMapper.findResumableByHash(initUploadDto.getFileHash(), (Long) UserContextUtil.get().get("id"));
            video.setUploadId(uploadId);
            video.setVideoKey(objectName);
            videoMapper.updateVideo(video);
        }

        return InitUploadVo.builder()
                .id(video.getId())
                .uploadId(uploadId)
                .videoKey(objectName)
                .partSize(5 * 1024 * 1024)
                .uploadedParts(Collections.emptyList())
                .mode("new")
                .build();
    }

    @Override
    public PartVo uploadPart(MultipartFile file, Integer partNumber, Long id) {
        Video video = videoMapper.findById(id);
        if (video == null || video.getStatus() !=0){
            throw new BusinessException("视频不存在或已处理完成");
        }
        checkOwner(video.getUserId());
        try{
            InputStream in = file.getInputStream();
            UploadPartResponse resp = s3Client.uploadPart(b -> b
                            .bucket(videoBucketName)
                    .key(video.getVideoKey())
                    .uploadId(video.getUploadId())
                    .partNumber(partNumber)
                    .contentLength(file.getSize()), RequestBody.fromInputStream(in, file.getSize())
            );

            return new PartVo(partNumber, resp.eTag());
        } catch (IOException e) {
            // 网络抖动 → 可重试
            throw new BusinessException("分片上传失败，请重试");
        } catch (S3Exception e) {
            if (e.awsErrorDetails().errorCode().equals("NoSuchUpload")) {
                // uploadId 失效 → 致命，需要重新 init
                throw new BusinessException("上传会话已失效");
            }
            throw new BusinessException("分片上传失败");
        }
    }

    @Transactional
    @Override
    public void complete(Long id, List<PartVo> parts) {
        Video video = videoMapper.findById(id);
        if (video == null || video.getStatus() !=0) throw new BusinessException("视频不存在或已处理完成");
        checkOwner(video.getUserId());
        List<Integer> uploadedParts = null;
        try {
            uploadedParts = listUploadedParts(video.getUploadId(), video.getVideoKey());
        } catch (Exception e) {
            throw new BusinessException("获取已上传分片失败");
        }
        if (parts.size() != uploadedParts.size()) throw new BusinessException("分片数量不一致");
        List<CompletedPart> completedParts = parts.stream()
                .sorted(Comparator.comparingInt(PartVo::getPartNumber))
                .map(p -> CompletedPart.builder()
                        .partNumber(p.getPartNumber())
                        .eTag(p.getEtag())
                        .build())
                .collect(Collectors.toList());

        try {
            s3Client.completeMultipartUpload(b -> b
                    .bucket(videoBucketName)
                    .key(video.getVideoKey())
                    .uploadId(video.getUploadId())
                    .multipartUpload(m -> m.parts(completedParts))
            );
        } catch (S3Exception e) {
            throw new BusinessException("分片信息不一致，请重新上传");
        }
        video.setStatus(2);
        video.setUploadId(null);
        videoMapper.updateVideo(video);
    }

    @Override
    public void abort(Long id) {
        Video video = videoMapper.findById(id);
        if (video == null || video.getStatus() !=0) return;
        checkOwner(video.getUserId());
        try {
            s3Client.abortMultipartUpload(b -> b
                    .bucket(videoBucketName)
                    .key(video.getVideoKey())
                    .uploadId(video.getUploadId())
            );
        } catch (Exception e) { log.error("清除上传会话失败", e); }
        videoMapper.deleteById(id);
    }


    /**
     * 列出已上传的分片
     * @param uploadId 上传会话 ID
     * @param key 对象键
     * @return 已上传的分片列表
     */
    private List<Integer> listUploadedParts(String uploadId, String key) {
        if (uploadId == null) return Collections.emptyList();
        try{
            ListPartsResponse resp = s3Client.listParts(b -> b
                    .bucket(videoBucketName)
                    .key(key)
                    .uploadId(uploadId)
            );
            return resp.parts().stream()
                    .map(Part::partNumber)
                    .sorted()
                    .collect(Collectors.toList());
        }catch (NoSuchUploadException e){
            throw e;
        }catch (Exception e){
            throw new BusinessException("获取已上传分片失败");
        }
    }

    /**
     * 根据文件名和内容类型，确定文件的 MIME 类型
     * @param fileName 文件名
     * @param contentType 内容类型
     * @param scene 上传场景
     * @return MIME 类型
     */
    private String resolveMime(String fileName, String contentType, UploadScene scene) {
        String ext = FileTypeUtil.getFileExtension(fileName);
        if (ext==null || !scene.getAllowedTypes().containsKey(ext)){
            throw new UploadException(415,"不支持的文件类型");
        }
        String mime = FileTypeUtil.getFileMime(ext, scene.getAllowedTypes());
        if (!mime.equals(contentType)){
            log.warn("客户端 Content-Type={} 与扩展名推断={} 不一致，以扩展名为准", contentType, mime);
        }
        return mime;
    }

    private void checkOwner(Long id){
        Claims claims = UserContextUtil.get();
        if (claims != null && !id.equals(claims.get("id"))) throw new BusinessException("无权限上传");
    }
}
