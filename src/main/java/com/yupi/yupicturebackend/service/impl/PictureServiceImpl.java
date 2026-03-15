package com.yupi.yupicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.manager.CosManager;
import com.yupi.yupicturebackend.manager.upload.FilePictureUpload;
import com.yupi.yupicturebackend.manager.upload.PictureUploadTemplate;
import com.yupi.yupicturebackend.manager.upload.UrlPictureUpload;
import com.yupi.yupicturebackend.mapper.PictureMapper;
import com.yupi.yupicturebackend.model.dto.file.UploadPictureResult;
import com.yupi.yupicturebackend.model.dto.picture.*;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.yupi.yupicturebackend.model.entity.Space;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.enums.PictureReviewStatusEnum;
import com.yupi.yupicturebackend.model.vo.PictureVO;
import com.yupi.yupicturebackend.model.vo.UserVO;
import com.yupi.yupicturebackend.service.PictureService;
import com.yupi.yupicturebackend.service.SpaceService;
import com.yupi.yupicturebackend.service.UserService;
import com.yupi.yupicturebackend.utils.ColorSimilarUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {
    @Resource
    private FilePictureUpload filePictureUpload;
    @Resource
    private UrlPictureUpload urlPictureUpload;
    @Resource
    private UserService userService;
    @Resource
    private CosManager cosManager;
    @Resource
    private SpaceService spaceService;
    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public void validatePicture(Picture picture) {
        ThrowUtils.throwIf(ObjUtil.isNull(picture), ErrorCode.PARAMS_ERROR);
        //从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();

        // 修复点1：区分新增/更新场景，仅更新时校验id
        // 修改数据时（id非空）才校验id，新增场景id为空无需校验
        if (ObjUtil.isNotNull(id)) {
            ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id不能为空");
        }

        //传了 url 才校验
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    @Override
    public PictureVO uploadPicture(Object inputSource,
                                   PictureUploadRequest pictureUploadRequest,
                                   User loginUser) {
        //校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_AUTHORIZED_ERROR);
        Long spaceId = pictureUploadRequest.getSpaceId();
        Long pictureId = pictureUploadRequest.getId(); // 修复点2：简化pictureId获取逻辑
        Picture oldPicture = null;

        // 修复点3：先判断是否为更新操作，获取oldPicture后再校验空间
        // 如果是更新，判断图片是否存在
        if (pictureId != null) {
            oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(ObjUtil.isNull(oldPicture), ErrorCode.NOT_FOUND_ERROR, "图片不存在，无法更新");
            //仅本人或者管理员可以更新
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)){
                throw new BusinessException(ErrorCode.NOT_AUTHORIZED_ERROR);
            }

            // 修复点4：修正空间ID校验逻辑，移动到oldPicture定义之后
            // 校验空间是否一致
            if (spaceId != null) {
                // 传了spaceId，必须和原图片的空间id一致
                if (ObjUtil.notEqual(spaceId, oldPicture.getSpaceId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间id不一致");
                }
            } else {
                // 没传spaceId，复用原有图片的spaceId
                spaceId = oldPicture.getSpaceId();
            }
        }

        // 校验空间是否存在（新增场景）
        if (spaceId != null && oldPicture == null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(ObjUtil.isNull(space), ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 校验是否有空间的权限，仅管理员才能上传
            if (!loginUser.getId().equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NOT_AUTHORIZED_ERROR, "无空间权限");
            }
            //校验额度
            if(space.getTotalCount() >= space.getMaxCount()){
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数不足");
            }if (space.getTotalSize() >= space.getMaxSize()){
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小不足");
            }
        }
                
        // 校验空间是否存在（更新场景）
        if (spaceId != null && oldPicture != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(ObjUtil.isNull(space), ErrorCode.NOT_FOUND_ERROR, "空间不存在，无法更新图片");
            // 校验是否有空间的权限
            if (!loginUser.getId().equals(space.getUserId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NOT_AUTHORIZED_ERROR, "无空间权限");
            }
        }


        //上传图片,得到图片信息
        // 按照用户id划分目录=>按照空间划分目录
        String uploadPathPrefix;
        if (spaceId == null){
            //公共图库
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        }else {
            //空间
            uploadPathPrefix = String.format("space/%s", spaceId);
        }

        //根据inputSource类型区分上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String){
            pictureUploadTemplate = urlPictureUpload;
        }

        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        //构造picture
        Picture picture = new Picture();
        picture.setSpaceId(spaceId);//指定空间id
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());

        //支持外层上传文件名称
        String pictName = uploadPictureResult.getPicName();
        if (StrUtil.isNotBlank(pictureUploadRequest.getPicName())){
            pictName = pictureUploadRequest.getPicName();
        }
        picture.setName(pictName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setPictureColor(uploadPictureResult.getPicColor());
        picture.setUserId(loginUser.getId());
        //补充审核参数
        this.fillPictureParams(picture, loginUser);


        //操作数据库
        //如果pictureId不为空，则更新，否则新增
        if (pictureId != null) {
            //如果是更新，则设置id和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }

        //开启事务
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status ->  {
            //插入数据
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "上传图片失败");
            
            //更新空间的使用额度（仅当 spaceId 不为 null 时）
            if (finalSpaceId != null) {
                boolean updateResult = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize + " + picture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, 
                        String.format("额度更新失败，spaceId=%d, picSize=%d", finalSpaceId, picture.getPicSize()));
            }
            return picture;
        });

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
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)){
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
        Long spaceId = pictureQueryRequest.getSpaceId();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();

        // 修复点6：修正搜索文本的and/or分组，避免查询逻辑错误
        //从多字段中搜索
        if (StrUtil.isNotBlank(searchText)){
            // 使用and()包裹or条件，保证分组正确
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText));
        }

        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        //> = startEditTime
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        //< endEditTime
        queryWrapper.le(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);

        //JSON数组查询
        if (CollUtil.isNotEmpty(tags)){
            for (String tag : tags){
                queryWrapper.like("tags", "\""+tag+"\"");
            }
        }

        //排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), "ascend".equals(sortOrder), sortField);
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

        // 修复点7：补充if大括号，增加reviewMessage长度校验
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 校验审核备注长度
        if (StrUtil.isNotBlank(reviewMessage)) {
            ThrowUtils.throwIf(reviewMessage.length() > 500, ErrorCode.PARAMS_ERROR, "审核备注过长，最多500字符");
        }

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

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        // 校验参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(StrUtil.isBlank(searchText), ErrorCode.PARAMS_ERROR, "搜索关键词不能为空");
        ThrowUtils.throwIf(count == null || count <= 0 || count > 30, ErrorCode.PARAMS_ERROR, "数量必须为1-30条");

        // 名称前缀默认等于搜索关键词
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }

        // 抓取内容
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取Bing图片页面失败，搜索关键词：{}", searchText, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取图片列表失败");
        }

        // 修复点8：优化DOM选择器，增加空元素容错
        Elements imgElementList = document.select("img[mimg]"); // 替换原dgControl选择器，适配Bing图片结构
        if (CollUtil.isEmpty(imgElementList)) {
            imgElementList = document.select("img.mimg"); // 兼容旧结构
        }
        ThrowUtils.throwIf(CollUtil.isEmpty(imgElementList), ErrorCode.OPERATION_ERROR, "未获取到图片列表");

        // 遍历元素，依次处理上传图片
        int uploadCount = 0;
        // 修复点9：限制循环次数，避免无限循环
        int maxLoop = Math.min(imgElementList.size(), count);
        for (int i = 0; i < maxLoop; i++) {
            Element imgElement = imgElementList.get(i);
            String fileUrl = imgElement.attr("src");
            // 补充data-src属性解析（Bing图片懒加载）
            if (StrUtil.isBlank(fileUrl)) {
                fileUrl = imgElement.attr("data-src");
            }

            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前图片链接为空，已跳过，索引：{}", i);
                continue;
            }

            // 处理图片的地址，防止转义或者和对象存储冲突的问题
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }

            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);
            pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));

            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("批量上传图片成功，id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("批量上传图片失败，URL：{}", fileUrl, e);
                continue;
            }

            if (uploadCount >= count) {
                break;
            }
        }

        log.info("批量上传图片完成，共上传{}张，目标{}张", uploadCount, count);
        return uploadCount;
    }

    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 修复点10：修正删除逻辑，仅当存在多条引用时不删除
        // 判断图片是否被多条记录使用，如果被多条记录使用，则不删除图片
        String pictureUrl = oldPicture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();

        // 只有当引用数<=1时才删除（当前记录已删除，剩余0条）
        if (count > 1) {
            log.info("图片仍被其他记录引用，不删除文件，URL：{}，引用数：{}", pictureUrl, count);
            return;
        }

        //删除图片
        try {
            cosManager.deleteObject(pictureUrl);
            log.info("删除图片文件成功，URL：{}", pictureUrl);
        } catch (Exception e) {
            log.error("删除图片文件失败，URL：{}", pictureUrl, e);
        }

        //删除缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            try {
                cosManager.deleteObject(thumbnailUrl);
                log.info("删除缩略图文件成功，URL：{}", thumbnailUrl);
            } catch (Exception e) {
                log.error("删除缩略图文件失败，URL：{}", thumbnailUrl, e);
            }
        }
    }

    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        //实体类和DTO类进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);

        //将list转化为string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));

        //设置编辑时间
        picture.setEditTime(new Date());

        //数据校验
        this.validatePicture(picture);

        long id = pictureEditRequest.getId();
        //判断是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        //校验权限
        this.checkPictureAuth(loginUser, oldPicture);

        //补充审核参数
        this.fillPictureParams(picture, loginUser);

        //操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void deletePicture(Long pictureId, User loginUser) {
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR, "图片ID非法");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_AUTHORIZED_ERROR);

        //判断是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        //校验权限
        checkPictureAuth(loginUser, oldPicture);
        //开启事物
        transactionTemplate.execute(status -> {
            //操作数据库
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除图片失败");
            //更新空间额度
            boolean updateResult = spaceService.lambdaUpdate()
                    .eq(Space::getId, oldPicture.getSpaceId())
                    .setSql("totalSize = totalSize -" + oldPicture.getPicSize())
                    .setSql("totalCount = totalCount - 1")
                    .update();
            ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新空间额度失败");
           return true;
        });


        this.clearPictureFile(oldPicture);
        log.info("删除图片成功，ID：{}", pictureId);
    }

    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        Long loginUserId = loginUser.getId();

        // 修复点11：简化权限校验逻辑，增加日志
        if (spaceId == null) {
            //公共图库仅本人或管理员可操作
            if (!loginUserId.equals(picture.getUserId()) && !userService.isAdmin(loginUser)) {
                log.warn("公共图库权限校验失败，登录用户ID：{}，图片所属用户ID：{}", loginUserId, picture.getUserId());
                throw new BusinessException(ErrorCode.NOT_AUTHORIZED_ERROR);
            }
        } else {
            //私有空间，仅空间管理员可操作
            if (!loginUserId.equals(picture.getUserId())) {
                log.warn("私有空间权限校验失败，登录用户ID：{}，空间所属用户ID：{}", loginUserId, picture.getUserId());
                throw new BusinessException(ErrorCode.NOT_AUTHORIZED_ERROR);
            }
        }
    }

    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser) {
        //1.校验参数
        ThrowUtils.throwIf(spaceId == null || StrUtil.isBlank(picColor), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_AUTHORIZED_ERROR);
        //2.校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!space.getUserId().equals(loginUser.getId())){
            throw new BusinessException(ErrorCode.NOT_AUTHORIZED_ERROR, "无权限操作该空间");
        }
        //3.查询该空间下的所有图片，图片必须有主色调
        List<Picture> pictureList = this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPictureColor)
                .list();
        //如果没有图片返回空列表
        if (CollUtil.isEmpty(pictureList)){
            return new ArrayList<>();
        }
        //将颜色字符串转化为主色调
        Color targetColor = Color.decode(picColor);
        //4.计算相似度并排序
        List<Picture> sortedPictureList = pictureList.stream()
                .sorted(Comparator.comparingDouble(picture ->{
                    String hexColor = picture.getPictureColor();
                    //没有主色调的图片默认排序到最后
                    if (StrUtil.isBlank(hexColor)){
                        return Double.MAX_VALUE;
                    }
                    Color color = Color.decode(hexColor);
                    //计算相似度
                    return -ColorSimilarUtils.calculateSimilarity(color, targetColor);

                } ))
                .limit(12)
                .collect(Collectors.toList());

        //5.返回结果
        return sortedPictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
    }
}