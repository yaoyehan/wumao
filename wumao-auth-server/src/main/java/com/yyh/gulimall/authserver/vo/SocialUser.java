package com.yyh.gulimall.authserver.vo;

import lombok.Data;

@Data
public class SocialUser {
    private String socialUid;
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private String refreshToken;
    private String scope;
    private long createdAt;


}