package com.yupi.yupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 *
 */
@Data
public class UserAddRequest implements Serializable {
    /**
     * 昵称
     */
    private String userName;
    /**
     * 账号
     */
    private String userAccount;
    /**
     * 头像
     */
    private String userAvatar;
    /**
     * 简介
     */
    private String userProfile;
    /**
     * 角色：user/admin
     */
    private String userRole;

    private static final long serialVersionUID = 1L;
}
