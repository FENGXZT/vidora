package com.fxzt.vidora.util;

import com.fxzt.vidora.pojo.vo.CursorPage;
import com.fxzt.vidora.service.FileService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
public class CursorPageChange {
    @Autowired
    private FileService fileService;

    @Value("${minio.ImageBucketName}")
    private String imageBucket;

    public  <T,R> CursorPage<R> toCursorPage(CursorPage<T> cached, Supplier<R> factory
            , Function<T,String> keyGet, BiConsumer<R,String> keySet
            , BiConsumer<R, LocalDateTime> timeSet, Function<T,LocalDateTime> timeGet){
        List<R> list = cached.getItems().stream().
                map(item->{
                    R vo = factory.get();
                    BeanUtils.copyProperties(item, vo);
                    String Url = keyGet.apply(item) != null ? fileService.presign(imageBucket, keyGet.apply(item), 10) : null;
                    keySet.accept(vo, Url);
                    timeSet.accept(vo, timeGet.apply(item));
                    return vo;
                }).toList();
        return new CursorPage<>(list,cached.getHasMore(),cached.getNextCursor(),cached.getNextCursorId());
    }

    public  <T> CursorPage<T> buildCursorPage(List<T> cached, int size ,
                                              Function<T,LocalDateTime> createTimeGetter, Function<T,Long> idGetter){
        boolean hasMore = cached.size() > size-1;
        if (hasMore) cached = cached.subList(0, size-1);
        LocalDateTime nextCursor = null;
        Long nextCursorId = null;
        if (!cached.isEmpty()){
            T last = cached.getLast();
            nextCursor = createTimeGetter.apply(last);
            nextCursorId = idGetter.apply(last);
        }
        return new CursorPage<T>(cached,hasMore,nextCursor,nextCursorId);
    }
}
