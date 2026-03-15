package com.yupi.yupicturebackend.model.dto.picture;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;

/**
 * 以图搜图请求
 */
@Data
public class SearchPictureByPictureRequest implements Serializable {

    /**
     * 图片 id
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long pictureId;

    private static final long serialVersionUID = 1L;
}