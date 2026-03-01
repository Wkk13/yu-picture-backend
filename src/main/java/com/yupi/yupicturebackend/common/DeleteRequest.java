package com.yupi.yupicturebackend.common;

import lombok.Data;

import java.io.Serializable;
@Data
public class DeleteRequest implements Serializable {
    /**
     * 通用的删除请求
     */
    private Long id;
    private static final long serialVersionUID = 1L;
}
