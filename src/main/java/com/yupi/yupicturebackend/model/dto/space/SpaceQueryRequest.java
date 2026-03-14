package com.yupi.yupicturebackend.model.dto.space;

import com.yupi.yupicturebackend.common.PageRequest;
import lombok.Data;
import org.springframework.scheduling.annotation.Async;

import java.io.Serializable;

/**
 * 创建空间请求
 */
@Data
public class SpaceQueryRequest extends PageRequest implements Serializable {
    /**
     * id
     */
    private Long id;
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 空间名称
     */
    private String spaceName;
    /**
     * 空间等级：0-普通版，1-专业版，2-旗舰版
     */
    private String spaceLevel;
    private static final long serialVersionUID = 1L;
}
