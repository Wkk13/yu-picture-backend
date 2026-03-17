package com.yupi.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicturebackend.model.dto.space.SpaceAddRequest;
import com.yupi.yupicturebackend.model.dto.space.SpaceQueryRequest;
import com.yupi.yupicturebackend.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

public interface SpaceService extends IService<Space> {
    /**
     * 创建空间
     *
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);
    /**
     * 校验
     *
     * @param space
     * @param add   是否为创建时校验
     */
    void validateSpace(Space space, boolean add);

    /**
     * 获取空间封装类(单条)
     *
     * @param space
     * @param request
     * @return
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 获取空间封装类(列表)
     *
     * @param SpacePage
     * @param request
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> SpacePage, HttpServletRequest request);

    /**
     * 获取查询包装类
     *
     * @param SpaceUploadRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest SpaceUploadRequest);
    /**
     * 根据空间级别填充空间对象
     * @param space
     */
    void fillSpaceSpaceLevel(Space space);
    /**
     * 校验某个用户是否有该空间的权限
     */

    void checkSpaceAuth(User loginUser, Space  space);
}
