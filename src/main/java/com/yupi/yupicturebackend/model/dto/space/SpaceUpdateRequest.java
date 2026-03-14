package com.yupi.yupicturebackend.model.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间更新请求
 */
@Data

public class SpaceUpdateRequest implements Serializable {
    /**
     * id
     */
    private Long id;
    /**
     * 空间等级：0-普通版，1-专业版，2-旗舰版
     */
    private Integer spaceLevel;
    /**
     * 最大容量
     */
    private Long maxSize;
    /**
     * 最大数量
     */
    private Long maxCount;
    private static final long serialVersionUID = 1L;
}
