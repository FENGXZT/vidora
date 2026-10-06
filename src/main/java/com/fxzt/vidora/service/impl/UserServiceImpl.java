package com.fxzt.vidora.service.impl;

import com.fxzt.vidora.exception.BusinessException;
import com.fxzt.vidora.exception.UnauthorizedException;
import com.fxzt.vidora.mapper.UserFollowMapper;
import com.fxzt.vidora.mapper.UserMapper;
import com.fxzt.vidora.mapper.VideoMapper;
import com.fxzt.vidora.pojo.entity.User;
import com.fxzt.vidora.pojo.query.*;
import com.fxzt.vidora.pojo.vo.*;
import com.fxzt.vidora.service.FileService;
import com.fxzt.vidora.service.UserService;
import com.fxzt.vidora.util.*;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private UserFollowMapper userFollowMapper;

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private SearchCacheVersion searchCacheVersion;
    @Autowired
    private CursorPageChange cursorPageChange;
    @Autowired
    private CacheHelper cacheHelper;
    @Autowired
    private FileService fileService;

    @Value("${minio.ImageBucketName}")
    private String imageBucket;

    @Override
    public void register(@Valid UserRegisterDto userRegisterDto) {
        //判断用户名是否已存在
        if (userMapper.findByUsername(userRegisterDto.getUsername()) !=null ) throw new BusinessException("该用户名已存在");

        //密码加密
        userRegisterDto.setPassword(PasswordUtil.encrypt(userRegisterDto.getPassword()));

        //保存用户信息
        userMapper.insert(userRegisterDto);

    }

    @Override
    public UserLoginResult login(UserLoginDTo userLoginDTo) {
        //判断用户名是否存在
        User user = userMapper.findByUsername(userLoginDTo.getUsername());
        if (user == null) throw new BusinessException("用户名或密码错误");
        //判断密码是否正确
        if (!PasswordUtil.match(userLoginDTo.getPassword(), user.getPassword())) throw new BusinessException("用户名或密码错误");
        //生成token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        //封装用户信息
        String avatarUrl = user.getAvatar() != null ? fileService.presign(imageBucket,user.getAvatar(),10): null;
        return UserLoginResult.builder()
                .token(token)
                .nickname(user.getNickname())
                .id(user.getId())
                .avatarUrl(avatarUrl)
                .build();
    }

    @Override
    public CursorPage<VideoListItem> liked(CursorPageDto dto) {
        dto.verifyCursor();
        dto.normalize();
        Long userId = (Long) UserContextUtil.get().get("id");
        int size = dto.getSize()+1;
        List<VideoCacheDto> cached = videoMapper.findLikedByUser(dto.getCursorTime(), dto.getCursorId(), size, userId);
        CursorPage<VideoCacheDto> cursorPage = cursorPageChange.buildCursorPage(cached, size, VideoCacheDto::getCursorTime, VideoCacheDto::getCursorId);
        return cursorPageChange.toCursorPage(cursorPage, VideoListItem::new
                ,VideoCacheDto::getCoverKey,VideoListItem::setCoverUrl
                ,VideoListItem::setLikeTime,VideoCacheDto::getCursorTime);
    }

    @Override
    public CursorPage<VideoListItem> favorite(CursorPageDto dto) {
        dto.verifyCursor();
        dto.normalize();
        Long userId = (Long) UserContextUtil.get().get("id");
        int size = dto.getSize()+1;
        List<VideoCacheDto> cached = videoMapper.findFavoritesByUser(dto.getCursorTime(), dto.getCursorId(), size, userId);
        CursorPage<VideoCacheDto> cursorPage = cursorPageChange.buildCursorPage(cached, size, VideoCacheDto::getCursorTime, VideoCacheDto::getCursorId);
        return cursorPageChange.toCursorPage(cursorPage, VideoListItem::new
                ,VideoCacheDto::getCoverKey,VideoListItem::setCoverUrl
                ,VideoListItem::setFavoriteTime,VideoCacheDto::getCursorTime);
    }

    @Override
    public UserMeVo getCurrentUser() {
        Long userId = (Long) UserContextUtil.get().get("id");
        User user = userMapper.findById(userId);
        if (user == null) throw new UnauthorizedException("登录已过期");
        UserMeVo vo =UserMeVo.builder()
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatar() != null ? fileService.presign(imageBucket,user.getAvatar(),10): null)
                .username(user.getUsername())
                .id(user.getId())
                .build();

        return vo;
    }

    @Override
    public UserProfileVo updateProfile(UpdateProfileDto dto) {
        Long userId = (Long) UserContextUtil.get().get("id");
        User user = userMapper.findById(userId);
        if (user == null) throw new UnauthorizedException("登录已过期");

        boolean nicknameChanged = StringUtils.hasText(dto.getNickname());
        if (nicknameChanged) user.setNickname(dto.getNickname().trim());
        if (StringUtils.hasText(dto.getAvatar())) user.setAvatar(dto.getAvatar().trim());
        userMapper.update(user);
        //如果修改了昵称，则需要更新搜索缓存的版本号,以确保搜索结果的准确性
        if (nicknameChanged) searchCacheVersion.bump();

        return bulidUserProfileVo(user);
    }

    @Override
    public void follow(Long id) {
        Long me = (Long) UserContextUtil.get().get("id");
        if (me.equals(id)) throw new BusinessException("不能关注自己");
        if(userMapper.findById(id)==null) throw new BusinessException("用户不存在");
        try{
            userFollowMapper.addFollow(me, id);
        }catch (Exception e){
            log.warn("关注失败", e);
            throw new BusinessException("关注失败");
        }
    }

    @Override
    public void unFollow(Long id) {
        Long me = (Long) UserContextUtil.get().get("id");
        if (me.equals(id)) throw new BusinessException("不能取消关注自己");
        if(userMapper.findById(id)==null) throw new BusinessException("用户不存在");
        try{
            userFollowMapper.unfollow(me, id);
        }catch (Exception e){
            log.warn("取消关注失败", e);
            throw new BusinessException("取消关注失败");
        }
    }

    @Override
    public CursorPage<UserListItem> following(CursorPageDto dto) {
        dto.verifyCursor();
        dto.normalize();
        Long userId = (Long) UserContextUtil.get().get("id");
        int size = dto.getSize()+1;
        List<UserCacheDto> cached = userMapper.findFollowingByUser(dto.getCursorTime(), dto.getCursorId(), size, userId);
        CursorPage<UserCacheDto> cursorPage = cursorPageChange.buildCursorPage(cached, size, UserCacheDto::getCursorTime, UserCacheDto::getCursorId);
        return cursorPageChange.toCursorPage(cursorPage, UserListItem::new
                ,UserCacheDto::getAvatar,UserListItem::setAvatarUrl
                ,UserListItem::setFollowTime,UserCacheDto::getCursorTime);
    }

    @Override
    public ScoreCursorPage<HistoryItem> history(HistoryPageDto dto) {
        dto.normalize();
        Long userId = (Long) UserContextUtil.get().get("id");
        if (userId == null) throw new UnauthorizedException("登录已过期");
        String key = RedisKeys.userHistory(userId);
        int pageSize = dto.getSize();
        double max = dto.getCursorScore() ==null ? Double.POSITIVE_INFINITY : dto.getCursorScore() - 1;
        List<HistoryItem> items = new ArrayList<>();
        List<Long> scores = new ArrayList<>();
        Long lastScore = null;

        for (int round = 0 ;round < 3 && items.size()<pageSize+1;round++){
            int need = pageSize+1 - items.size();
            List<CacheHelper.ScoredMember> batch = cacheHelper.zReverseRangeByScore(key,0,max,need);
            if (batch==null)
                throw new BusinessException("浏览记录暂时不可用，请稍后重试");
            if(batch.isEmpty()) break;

            List<Long> ids = batch.stream()
                    .map(m->Long.parseLong(m.member()))
                    .toList();
            if (ids.isEmpty()) break;
            Map<Long,VideoCacheDto> videoMap = videoMapper.findByIds(ids)
                    .stream()
                    .collect(Collectors.toMap(VideoCacheDto::getId,v->v,(a,b)->a));

            for (CacheHelper.ScoredMember m:batch){
                Long vid = Long.parseLong(m.member());
                VideoCacheDto video = videoMap.get(vid);
                if (video == null) continue;    //已删除/下架，跳过

                HistoryItem item = HistoryItem.builder()
                        .id(video.getId())
                        .title(video.getTitle())
                        .coverUrl(video.getCoverKey()==null ? null : fileService.presign(imageBucket, video.getCoverKey(), 10))
                        .viewTime(LocalDateTime.ofInstant(
                                Instant.ofEpochMilli((long) m.score()),
                                ZoneId.systemDefault()
                        ).withNano(0))
                        .build();
                items.add(item);
                scores.add((long) m.score());
            }

            double lastBatchScore = batch.getLast().score();
            max = lastBatchScore-1;
        }

        boolean hasMore = items.size() > pageSize;
        if (hasMore) {
            items = items.subList(0, pageSize);
            scores = scores.subList(0, pageSize);
        }
        Long nextCursorScore = hasMore && !scores.isEmpty() ? scores.getLast() : null;

        return ScoreCursorPage.<HistoryItem>builder()
                .items(items)
                .hasMore(hasMore)
                .nextCursorScore(nextCursorScore)
                .build();
    }

    private UserProfileVo bulidUserProfileVo(User user) {
        return  UserProfileVo.builder()
                .id(user.getId())
                .avatarUrl(user.getAvatar() != null ? fileService.presign(imageBucket,user.getAvatar(),10): null)
                .nickname(user.getNickname())
                .build();
    }
}
