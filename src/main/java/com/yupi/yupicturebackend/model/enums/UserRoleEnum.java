package com.yupi.yupicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
public enum UserRoleEnum {

    USER("user", "用户"),
    VIP("vip", "会员"),
    ADMIN("admin", "管理员");

    private final String text;   // 角色标识（英文）
    private final String value;  // 角色描述（中文）

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据角色标识（text字段）获取枚举
     *
     * @param text 角色标识（如"user", "admin"）
     * @return 枚举值
     */
    public static UserRoleEnum getEnumByText(String text) {
        if (ObjUtil.isEmpty(text)) {
            return null;
        }
        for (UserRoleEnum userRoleEnum : UserRoleEnum.values()) {
            if (userRoleEnum.text.equals(text)) {
                return userRoleEnum;
            }
        }
        return null;
    }

    /**
     * 根据角色描述（value字段）获取枚举
     *
     * @param value 角色描述（如"用户", "管理员"）
     * @return 枚举值
     */
    public static UserRoleEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (UserRoleEnum userRoleEnum : UserRoleEnum.values()) {
            if (userRoleEnum.value.equals(value)) {
                return userRoleEnum;
            }
        }
        return null;
    }
    
    /**
     * 智能查找方法，支持按角色标识或描述查找
     *
     * @param role 角色标识或描述
     * @return 枚举值
     */
    public static UserRoleEnum getEnumSmart(String role) {
        if (ObjUtil.isEmpty(role)) {
            return null;
        }
        
        // 先尝试按标识查找
        UserRoleEnum byText = getEnumByText(role);
        if (byText != null) {
            return byText;
        }
        
        // 再尝试按描述查找
        return getEnumByValue(role);
    }
}