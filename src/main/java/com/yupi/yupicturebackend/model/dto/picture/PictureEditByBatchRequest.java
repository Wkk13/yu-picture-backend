package com.yupi.yupicturebackend.model.dto.picture;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 图片批量编辑请求
 */
@Data
public class PictureEditByBatchRequest implements Serializable {

    /**
     * 图片 id 列表
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private List<Long> pictureIdList;

    /**
     * 空间 id
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long spaceId;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签
     */
    private List<String> tags;

    /**
     * 命名规则
     */
    private String nameRule;

    private static final long serialVersionUID = 1L;
}