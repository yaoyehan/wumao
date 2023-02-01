package com.yyh.gulimall.cart.vo;

import lombok.Data;
import lombok.ToString;

@ToString
@Data
public class UserInfoTo {
    private Long userId;
    private String userkey;
    private boolean tempUser = false;

}
