package com.yyh.gulimall.order.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceProperties {
    @Autowired
    DataSourceProperties dataSourceProperties;
    @Bean
    public DataSource dataSource(org.springframework.boot.autoconfigure.jdbc.DataSourceProperties dataSourceProperties){

    }
}
