package com.yupi.yupicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
/*
 * 根据颜色搜索图片请求
 */
@Data
public class SearchPictureByColorRequest implements Serializable {
    /**
     * 图片颜色
     */
    private String pictureColor;
    /**
     * 空间id
     */
    private Long spaceId;
    private static final long serialVersionUID = 1L;
}
