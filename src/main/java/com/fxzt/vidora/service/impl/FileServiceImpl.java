package com.fxzt.vidora.service.impl;

import com.fxzt.vidora.constant.UploadScene;
import com.fxzt.vidora.exception.BusinessException;
import com.fxzt.vidora.exception.UnauthorizedException;
import com.fxzt.vidora.exception.UploadException;
import com.fxzt.vidora.mapper.VideoFavoriteMapper;
import com.fxzt.vidora.mapper.VideoLikeMapper;
import com.fxzt.vidora.mapper.VideoMapper;
import com.fxzt.vidora.pojo.entity.Video;
import com.fxzt.vidora.pojo.query.InitUploadDto;
import com.fxzt.vidora.pojo.query.UploadContext;
import com.fxzt.vidora.pojo.vo.InitUploadVo;
import com.fxzt.vidora.pojo.vo.PartVo;
import com.fxzt.vidora.service.FileService;
import com.fxzt.vidora.util.*;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
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
import java.time.LocalDateTime;
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
    @Autowired
    private VideoFavoriteMapper videoFavoriteMapper;
    @Autowired
    private VideoLikeMapper videoLikeMapper;

    @Autowired
    private CacheHelper cacheHelper;
    @Autowired
    private SearchCacheVersion searchCacheVersion;

    @Value("${minio.VideoBucketName}")
    private String videoBucketName;

    @Value("${minio.ImageBucketName}")
    private String imageBucketName;

    private static final int PART_SIZE = 5 * 1024 * 1024;

    @Override
    public String uploadVideo(MultipartFile file) {
        UploadScene uploadScene = UploadScene.VIDEO;
        //获取文件类型
        String ext = FileTypeUtil.getFileExtension(file.getOriginalFilename());
        String mime = resolveMime(file.getOriginalFilename(), file.getContentType(), uploadScene);

        long size = file.getSize();
        if (size > uploadScene.getMaxSize()) {
            throw new UploadException(413, "视频太大");
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
            throw new UploadException(413, "图片太大");
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
    public String presign(String bucket, String key, Integer duration) {
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
        if (initUploadDto.getFileSize() <= 0) throw new UploadException(400, "文件大小非法");
        if (initUploadDto.getFileSize() > UploadScene.VIDEO.getMaxSize()) throw new UploadException(413, "文件太大");
        boolean isReuse = false;
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
                        .declaredSize(initUploadDto.getFileSize())
                        .build();
                videoMapper.addVideo(newVideo);
                searchCacheVersion.bump();
                return InitUploadVo.builder()
                        .mode("instant")
                        .id(newVideo.getId())
                        .build();
            }
            //2.断点续传
            exist = videoMapper.findResumableByHash(initUploadDto.getFileHash(), (Long) UserContextUtil.get().get("id"));
            if (exist != null) {
                Long declared = exist.getDeclaredSize();
                if (declared ==null || declared ==0){
                    log.warn("视频 {} 缺少 declared_size（加列前的旧数据），降级为全新上传", exist.getId());
                    isReuse = true;
                } else if (!declared.equals(initUploadDto.getFileSize())) {
                    throw new BusinessException("文件大小与首次声明不一致，请重新上传");
                } else {
                    try {
                        List<PartVo> parts = listUploadedParts(exist.getUploadId(), exist.getVideoKey());
                        List<Integer> uploadedParts = parts
                                .stream()
                                .map(PartVo::getPartNumber)
                                .toList();
                        String key = RedisKeys.uploadContext(exist.getUploadId());
                        if (cacheHelper.get(key,UploadContext.class) == null){
                            Long uploadedBytes = parts.stream().mapToLong(PartVo::getSize).sum();
                            if (uploadedBytes>initUploadDto.getFileSize())
                                throw new BusinessException("文件与上次不一致，请重新上传");
                            cacheHelper.set(key,UploadContext.builder()
                                    .userId(exist.getUserId())
                                    .totalSize(declared)
                                    .uploadId(exist.getUploadId())
                                    .videoKey(exist.getVideoKey())
                                    .build(),Duration.ofHours(24)
                            );
                            log.warn("上传 context 已丢失，已按 ListParts 重建：uploadId={}，已上传 {} 字节"
                                    , exist.getUploadId(), uploadedBytes);
                        }
                        return InitUploadVo.builder()
                                .mode("resume")
                                .id(exist.getId())
                                .uploadId(exist.getUploadId())
                                .partSize(PART_SIZE)
                                .videoKey(exist.getVideoKey())
                                .uploadedParts(uploadedParts)
                                .build();
                    } catch (NoSuchUploadException e) {
                        log.warn("上传会话已失效，降级处理");
                        isReuse = true;
                    } catch (Exception e) {
                        throw e;
                    }
                }
            }
        }

        //全新上传
        //生成key
        UploadScene uploadScene = UploadScene.VIDEO;
        String mime = resolveMime(initUploadDto.getOriginalFilename(), initUploadDto.getContentType(), uploadScene);
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
        Video video;
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
                    .declaredSize(initUploadDto.getFileSize())
                    .build();
            videoMapper.addVideo(video);
        } else {
            video = videoMapper.findResumableByHash(initUploadDto.getFileHash(), (Long) UserContextUtil.get().get("id"));
            video.setUploadId(uploadId);
            video.setVideoKey(objectName);
            video.setDeclaredSize(initUploadDto.getFileSize());
            videoMapper.updateVideo(video);
        }
        // 缓存上传会话
        String key = RedisKeys.uploadContext(uploadId);
        UploadContext text = UploadContext.builder()
                .userId(video.getUserId())
                .totalSize(initUploadDto.getFileSize())
                .uploadId(uploadId)
                .videoKey(objectName)
                .build();
        cacheHelper.set(key, text, Duration.ofHours(24));

        return InitUploadVo.builder()
                .id(video.getId())
                .uploadId(uploadId)
                .videoKey(objectName)
                .partSize(PART_SIZE)
                .uploadedParts(Collections.emptyList())
                .mode("new")
                .build();
    }

    @Override
    public PartVo uploadPart(MultipartFile file, Integer partNumber, Long id) {
        Video video = videoMapper.findById(id);
        if (video == null || video.getStatus() != 0) {
            throw new BusinessException("视频不存在或已处理完成");
        }
        checkOwner(video.getUserId());
        // 分片校验
        if (file.getSize() > PART_SIZE) throw new UploadException(413, "分片大小非法");
        String key = RedisKeys.uploadContext(video.getUploadId());
        UploadContext uploadContext = cacheHelper.get(key, UploadContext.class);
        if (uploadContext == null) throw new BusinessException("上传会话已失效");
        int expectedParts = (int) Math.ceil((double) uploadContext.getTotalSize() / PART_SIZE);
        if (partNumber == null || partNumber < 1 || partNumber > expectedParts) {
            throw new UploadException(400, "分片编号非法");
        }
        try {
            InputStream in = file.getInputStream();
            UploadPartResponse resp = s3Client.uploadPart(b -> b
                    .bucket(videoBucketName)
                    .key(video.getVideoKey())
                    .uploadId(video.getUploadId())
                    .partNumber(partNumber)
                    .contentLength(file.getSize()), RequestBody.fromInputStream(in, file.getSize())
            );
            return new PartVo(partNumber, resp.eTag(),file.getSize());
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
    public void complete(Long id) {
        Video video = videoMapper.findById(id);
        if (video == null || video.getStatus() != 0) throw new BusinessException("视频不存在或已处理完成");
        checkOwner(video.getUserId());
        if (video.getUploadId() == null) throw new BusinessException("上传会话不存在，请重新上传");
        //服务端权威：etag 一律来自 ListParts，
        List<PartVo> serverParts;
        try {
            serverParts = listUploadedParts(video.getUploadId(), video.getVideoKey());
        } catch (NoSuchUploadException e) {
            throw new BusinessException("上传会话已失效，请重新上传");
        } catch (Exception e) {
            throw new BusinessException("获取已上传分片失败");
        }
        if (serverParts.isEmpty()) throw new BusinessException("没有已上传的分片");
        // 完整性校验：期望片数由【服务端已知的总大小】算出，与客户端声明无关
        UploadContext text = cacheHelper.get(RedisKeys.uploadContext(video.getUploadId()), UploadContext.class);
        if (text == null) throw new BusinessException("上传会话已失效，请重新上传");
        if (text.getTotalSize() == null || text.getTotalSize() <= 0)
            throw new BusinessException("上传会话数据异常，请重新上传");
        int expectedParts = (int) Math.ceil((double) text.getTotalSize() / PART_SIZE);
        if (serverParts.size() != expectedParts)
            throw new BusinessException("分片数量不一致，请重新上传");
        Long actualBytes = serverParts.stream().mapToLong(PartVo::getSize).sum();
        if (!actualBytes.equals(text.getTotalSize()))
            throw new BusinessException("分片大小与声明不符：实际 " + actualBytes + " 字节，声明 " + text.getTotalSize() + " 字节");
        for (int i=0 ; i < serverParts.size()-1;i++){
            if (serverParts.get(i).getSize()==null || serverParts.get(i).getSize()!=PART_SIZE){
                throw new BusinessException("分片大小非法：第 " + serverParts.get(i).getPartNumber()
                        + " 片为 " + serverParts.get(i).getSize() + " 字节，非最后一片必须正好 " + PART_SIZE);
            }
        }
        PartVo last = serverParts.getLast();
        if (last.getSize()==null||last.getSize()<=0||last.getSize()>PART_SIZE)
            throw new BusinessException("分片大小非法：最后一片 " + last.getSize() + " 字节");
        // 构建完成上传请求
        List<CompletedPart> completedParts = serverParts.stream()
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
            log.error("合并分片失败 errorCode={}", e.awsErrorDetails() == null ? "?" : e.awsErrorDetails().errorCode(), e);
            throw new BusinessException("分片信息不一致，请重新上传");
        }
        video.setStatus(2);
        video.setUploadId(null);
        video.setCreateTime(LocalDateTime.now());
        videoMapper.updateVideo(video);
        searchCacheVersion.bump();
    }

    @Transactional
    @Override
    public void abort(Long id) {
        Video video = videoMapper.findById(id);
        if (video == null || video.getStatus() != 0) return;
        checkOwner(video.getUserId());
        try {
            s3Client.abortMultipartUpload(b -> b
                    .bucket(videoBucketName)
                    .key(video.getVideoKey())
                    .uploadId(video.getUploadId())
            );
        } catch (Exception e) {
            log.error("清除上传会话失败", e);
        }
        videoMapper.deleteById(id);
        videoFavoriteMapper.deleteByVideoId(id);
        videoLikeMapper.deleteByVideoId(id);
        cacheHelper.delete(RedisKeys.uploadContext(video.getUploadId()));
    }

    /**
     * 列出已上传的分片（含 etag，服务端权威）
     *
     * @param uploadId 上传会话 ID
     * @param key      对象键
     * @return 已上传的分片列表，按 partNumber 升序
     */
    private List<PartVo> listUploadedParts(String uploadId, String key) {
        if (uploadId == null) return Collections.emptyList();
        try {
            ListPartsResponse resp = s3Client.listParts(b -> b
                    .bucket(videoBucketName)
                    .key(key)
                    .uploadId(uploadId)
            );
            return resp.parts().stream()
                    .map(p -> new PartVo(p.partNumber(), p.eTag(),p.size()))
                    .sorted(Comparator.comparingInt(PartVo::getPartNumber))
                    .collect(Collectors.toList());
        } catch (NoSuchUploadException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("获取已上传分片失败");
        }
    }

    /**
     * 根据文件名和内容类型，确定文件的 MIME 类型
     *
     * @param fileName    文件名
     * @param contentType 内容类型
     * @param scene       上传场景
     * @return MIME 类型
     */
    private String resolveMime(String fileName, String contentType, UploadScene scene) {
        String ext = FileTypeUtil.getFileExtension(fileName);
        if (ext == null || !scene.getAllowedTypes().containsKey(ext)) {
            throw new UploadException(415, "不支持的文件类型");
        }
        String mime = FileTypeUtil.getFileMime(ext, scene.getAllowedTypes());
        if (!mime.equals(contentType)) {
            log.warn("客户端 Content-Type={} 与扩展名推断={} 不一致，以扩展名为准", contentType, mime);
        }
        return mime;
    }

    /**
     * 检查当前用户是否是视频的拥有者
     * @param id 视频 ID
     */
    private void checkOwner(Long id) {
        Claims claims = UserContextUtil.get();
        if (claims == null) throw new UnauthorizedException("未登录");
        if (!id.equals(claims.get("id"))) throw new BusinessException("无权限上传");
    }
}
