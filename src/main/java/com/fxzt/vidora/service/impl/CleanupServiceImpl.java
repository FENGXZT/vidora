package com.fxzt.vidora.service.impl;

import com.fxzt.vidora.mapper.VideoMapper;
import com.fxzt.vidora.pojo.entity.Video;
import com.fxzt.vidora.service.CleanupService;
import com.fxzt.vidora.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CleanupServiceImpl implements CleanupService {

    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private FileService fileService;

    @Override
    public int cleanStaleUploads(int hours) {
        LocalDateTime before = LocalDateTime.now().minusHours(hours);
        List<Video> videos = videoMapper.cleanList(0, before);
        if (videos != null && !videos.isEmpty()){
            for (Video video : videos){
                fileService.abort(video.getId());
                videoMapper.deleteById(video.getId());
            }
            return videos.size();
        }
        return 0;
    }
}
