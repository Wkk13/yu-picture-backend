package com.yupi.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupicturebackend.model.dto.picture.*;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.vo.PictureVO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface PictureService extends IService<Picture> {
    /**
     * 校验图片
     *
     * @param picture
     */

    void validatePicture(Picture picture);

    /**
     * 上传图片
     *
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);
    /**
     * 获取图片封装类(单条)
     *
     * @param picture
     * @param request
     * @return
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);
    /**
     * 获取图片封装类(列表)
     *
     * @param picturePage
     * @param request
     * @return
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 获取查询包装类
     *
     * @param pictureUploadRequest
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureUploadRequest);
    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充审核参数
     *
     * @param picture
     * @param loginUser
     */
    void fillPictureParams(Picture picture, User loginUser);

    /**
     * 批量上传图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return
     */
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest,
                                        User loginUser);

    /**
     * 删除对象存储图片文件
     *
     * @param oldPicture
     */
    void clearPictureFile(Picture oldPicture);
    /**
     * 编辑图片
     *
     * @param pictureEditRequest
     * @param loginUser
     */

    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);
    /**
     * 删除图片
     *
     * @param pictureId
     * @param loginUser
     */

    void deletePicture(Long pictureId, User loginUser);

    /**
     * 校验空间图片权限
     *
     * @param loginUser
     * @param picture
     */
    void checkPictureAuth(User loginUser, Picture picture);
   /**
     * 根据颜色搜索图片
     *
     * @param spaceId
     * @param picColor
     * @param loginUser
     * @return
     */

    List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);
    /**
     * 批量编辑图片
     *
     * @param pictureEditByBatchRequest
     * @param loginUser
     */

    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);
}
