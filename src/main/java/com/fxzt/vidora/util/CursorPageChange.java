package com.fxzt.vidora.util;

import com.fxzt.vidora.pojo.vo.CursorPage;
import com.fxzt.vidora.pojo.vo.ScoreCursorPageWithId;
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
        List<R> list = mapItems(cached.getItems(), factory, keyGet, keySet, timeSet, timeGet);
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

    public <T,R> ScoreCursorPageWithId<R> toHotCursorPage(ScoreCursorPageWithId<T> cached, Supplier<R> factory
            , Function<T,String> keyGet, BiConsumer<R,String> keySet
            , BiConsumer<R, LocalDateTime> timeSet, Function<T,LocalDateTime> timeGet){
        List<R> list = mapItems(cached.getItems(), factory, keyGet, keySet, timeSet, timeGet);
        return new ScoreCursorPageWithId<>(list, cached.getHasMore(), cached.getNextCursorScore(), cached.getNextCursorId());
    }

    private <T, R> List<R> mapItems(List<T> src,
                                    Supplier<R> factory,
                                    Function<T, String> keyGet,
                                    BiConsumer<R, String> keySet,
                                    BiConsumer<R, LocalDateTime> timeSet,
                                    Function<T, LocalDateTime> timeGet) {
        return src.stream()
                .map(item -> {
                    R vo = factory.get();
                    BeanUtils.copyProperties(item, vo);

                    String key = keyGet.apply(item);
                    String url = key != null ? fileService.presign(imageBucket, key, 10) : null;
                    keySet.accept(vo, url);

                    timeSet.accept(vo, timeGet.apply(item));
                    return vo;
                })
                .toList();
    }
}
