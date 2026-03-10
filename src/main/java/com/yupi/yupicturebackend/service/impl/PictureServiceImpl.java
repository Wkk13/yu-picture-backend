package com.yupi.yupicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.manager.FileManager;
import com.yupi.yupicturebackend.mapper.PictureMapper;
import com.yupi.yupicturebackend.model.dto.file.UploadPictureResult;
import com.yupi.yupicturebackend.model.dto.picture.PictureQueryRequest;
import com.yupi.yupicturebackend.model.dto.picture.PictureUploadRequest;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.vo.PictureVO;
import com.yupi.yupicturebackend.model.vo.UserVO;
import com.yupi.yupicturebackend.service.PictureService;
import com.yupi.yupicturebackend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {
    @Resource
    private FileManager fileManager;
    @Resource
    private UserService userService;
    @Override
    public void validatePicture(Picture picture) {
        ThrowUtils.throwIf(ObjUtil.isNull(picture), ErrorCode.PARAMS_ERROR);
        //从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        //修改数据时，id不能为空，有参数则检验
        ThrowUtils.throwIf(ObjUtil.isNull( id), ErrorCode.PARAMS_ERROR,"id不能为空");
        //传了url才校验
        if (StrUtil.isBlank(url)){
            ThrowUtils.throwIf(url.length()> 1024, ErrorCode.PARAMS_ERROR, "url过长");
        }
        if (StrUtil.isBlank(introduction)){
            ThrowUtils.throwIf(introduction.length()> 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    @Override
    public PictureVO uploadPicture(MultipartFile multipartFile,
                                   PictureUploadRequest pictureUploadRequest,
                                   User loginUser) {
        //校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_AUTHORIZED_ERROR);
        //判断是新增还是删除
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        //如果是更新，判断图片是否存在
        if (pictureId != null) {
            boolean exists = this.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .exists();
            ThrowUtils.throwIf(!exists, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        //上传图片,得到图片信息
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);
        //构造picture
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        //操作数据库
        //如果pictureId不为空，则更新，否则新增
        if (pictureId != null) {
            //如果是更新，则设置id和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "上传图片失败");
        return PictureVO.objToVo(picture);
    }

      @Override
     public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        //对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        //关联查询用户信息
        Long userId= picture.getUserId();
        if (userId != null && userId > 0){
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }
    @Override
    public Page<PictureVO> getPictureVOPage(Page< Picture> picturePage, HttpServletRequest  request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isNotEmpty(pictureList)){
            return pictureVOPage;
        }
        //列表转vo
        List<PictureVO> pictureVOList = pictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
        //1.关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        //2.填充用户信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)){
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;


    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        //从对象中取值
            Long id = pictureQueryRequest.getId();
            String name = pictureQueryRequest.getName();
            String introduction = pictureQueryRequest.getIntroduction();
            String category = pictureQueryRequest.getCategory();
            List<String> tags = pictureQueryRequest.getTags();
            Long picSize = pictureQueryRequest.getPicSize();
            Integer picWidth = pictureQueryRequest.getPicWidth();
            Integer picHeight = pictureQueryRequest.getPicHeight();
            Double picScale = pictureQueryRequest.getPicScale();
            String picFormat = pictureQueryRequest.getPicFormat();
            String searchText = pictureQueryRequest.getSearchText();
            Long userId = pictureQueryRequest.getUserId();
            String sortField = pictureQueryRequest.getSortField();
            String sortOrder = pictureQueryRequest.getSortOrder();
            //从多字段中搜索
            if (StrUtil.isNotBlank(searchText)){
                //拼接查询条件
                queryWrapper.and(qw->qw .like("name", searchText))
                                        .or()
                                        .like("introduction", searchText);
                    }

            queryWrapper.eq(ObjUtil.isNotEmpty( id), "id", id);
            queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
            queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
            queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
            queryWrapper.like(StrUtil.isNotBlank(category), "category", category);
            queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
            queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
            queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
            queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
            //JSON数组查询
            if (CollUtil.isNotEmpty( tags)){
                for (String tag : tags){
                    queryWrapper.like("tags", "\""+tag+"\"");
                }
            }
            //排序
            queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
            return queryWrapper;
    }
}
