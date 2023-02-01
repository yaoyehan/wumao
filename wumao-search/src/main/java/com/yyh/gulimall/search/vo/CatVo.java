package com.yyh.gulimall.search.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class CatVo {
    /**
     * 分类id
     */

    private Long catId;
    /**
     * 分类名称
     */
    private String name;
}
