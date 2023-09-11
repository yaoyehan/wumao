package com.yyh.gulimall.product.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
public class AttrRespVo extends AttrVo{
    @TableId(value = "uid",type =IdType.ID_WORKER)
    private String catalogName;
    private String groupName;
    private Long[] catalogPath;

}
