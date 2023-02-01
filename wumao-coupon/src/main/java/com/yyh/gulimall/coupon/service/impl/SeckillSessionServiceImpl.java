package com.yyh.gulimall.coupon.service.impl;

import com.yyh.gulimall.coupon.entity.SeckillSkuRelationEntity;
import com.yyh.gulimall.coupon.service.SeckillSkuRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yyh.common.utils.PageUtils;
import com.yyh.common.utils.Query;

import com.yyh.gulimall.coupon.dao.SeckillSessionDao;
import com.yyh.gulimall.coupon.entity.SeckillSessionEntity;
import com.yyh.gulimall.coupon.service.SeckillSessionService;


@Service("seckillSessionService")
public class SeckillSessionServiceImpl extends ServiceImpl<SeckillSessionDao, SeckillSessionEntity> implements SeckillSessionService {
    @Autowired
    SeckillSkuRelationService seckillSkuRelationService;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SeckillSessionEntity> page = this.page(
                new Query<SeckillSessionEntity>().getPage(params),
                new QueryWrapper<SeckillSessionEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<SeckillSessionEntity> getLates3DaySession() {
        List<SeckillSessionEntity> list = this.list(
                new QueryWrapper<SeckillSessionEntity>().between("start_time", startTime(), endTime()));
        if(list!=null&&list.size()>0){

            List<SeckillSessionEntity> collect = list.stream().map(session -> {
                Long id = session.getId();
                QueryWrapper<SeckillSkuRelationEntity> queryWrapper = new QueryWrapper<SeckillSkuRelationEntity>().eq("promotion_session_id", id);
                List<SeckillSkuRelationEntity> relationEntities = seckillSkuRelationService.list(queryWrapper);
                session.setRelationEntities(relationEntities);
                return session;
            }).collect(Collectors.toList());
            return collect;
        }
        return null;
    }

    private String startTime(){
        LocalDate now = LocalDate.now();
        LocalTime min=LocalTime.MIN;
        LocalDateTime start = LocalDateTime.of(now, min);
        String format = start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return format;
    }
    private String endTime(){
        LocalDate now = LocalDate.now();
        LocalTime max=LocalTime.MAX;
        LocalDateTime end = LocalDateTime.of(now, max);
        String format = end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return format;
    }

}