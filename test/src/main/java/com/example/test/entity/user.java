package com.example.test.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;



@Data
@TableName("test1")
public class user {
    private int id;
    private String name;

}
