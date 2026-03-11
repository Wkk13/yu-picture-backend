package com.yupi.yupicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.manager.FileManager;
import com.yupi.yupicturebackend.manager.upload.FilePictureUpload;
import com.yupi.yupicturebackend.manager.upload.PictureUploadTemplate;
import com.yupi.yupicturebackend.manager.upload.UrlPictureUpload;
import com.yupi.yupicturebackend.mapper.PictureMapper;
import com.yupi.yupicturebackend.model.dto.file.UploadPictureResult;
import com.yupi.yupicturebackend.model.dto.picture.PictureQueryRequest;
import com.yupi.yupicturebackend.model.dto.picture.PictureReviewRequest;
import com.yupi.yupicturebackend.model.dto.picture.PictureUploadRequest;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.enums.PictureReviewStatusEnum;
import com.yupi.yupicturebackend.model.vo.PictureVO;
import com.yupi.yupicturebackend.model.vo.UserVO;
import com.yupi.yupicturebackend.service.PictureService;
import com.yupi.yupicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
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
    private FilePictureUpload filePictureUpload;
    @Resource
    private UrlPictureUpload urlPictureUpload;
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
    public PictureVO uploadPicture(Object inputSource,
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
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(ObjUtil.isNull(oldPicture), ErrorCode.NOT_FOUND_ERROR);
            //仅本人或者管理员可以更新
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)){
                throw new BusinessException(ErrorCode.NOT_AUTHORIZED_ERROR);
            }
        }
        //上传图片,得到图片信息
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        //根据inputSource类型区分上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String){
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        //构造picture
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        //补充审核参数
        this.fillPictureParams( picture, loginUser);
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
            Date reviewTime = pictureQueryRequest.getReviewTime();
            Long reviewerId = pictureQueryRequest.getReviewerId();
            String reviewMessage = pictureQueryRequest.getReviewMessage();
            Integer reviewStatus = pictureQueryRequest.getReviewStatus();

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
            queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
            queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
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

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        //1.检验参数
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum))
             throw new BusinessException(ErrorCode.PARAMS_ERROR);
        //2.判断图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //3.校验审核状态是否重复
        if (oldPicture.getReviewStatus().equals(reviewStatus)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        //4.数据库操作
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewTime(new Date());
        updatePicture.setReviewerId(loginUser.getId());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 填充审核参数
     * @param picture
     * @param loginUser
     */
    @Override
    public void fillPictureParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)){
            //管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            //普通用户,无论是编辑还是上传,都需要审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }





}
