package com.yyh.gulimall.order.controller;

import java.util.Arrays;
import java.util.Map;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.yyh.gulimall.order.entity.OrderEntity;
import com.yyh.gulimall.order.service.OrderService;
import com.yyh.common.utils.PageUtils;
import com.yyh.common.utils.R;



/**
 * 订单
 *
 * @author yyh
 * @email 1697149091@qq.com
 * @date 2022-06-16 16:24:46
 */
@RestController
@RequestMapping("order/order")
public class OrderController {
    @Autowired
    private OrderService orderService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("order:order:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = orderService.queryPage(params);

        return R.ok().put("page", page);
    }
    /**
     * 分页查询当前用户的所有订单信息
     */
    @PostMapping("/listWithItem")
    //@RequiresPermissions("order:order:list")
    public R listWithItem(@RequestParam Map<String, Object> params){
        PageUtils page = orderService.queryPageWithItem(params);

        return R.ok().put("page", page);
    }
    @GetMapping("/status/{orderSn}")
    public R getOrderStatus(@PathVariable("orderSn") String orderSn){
        OrderEntity orderEntity=orderService.getOrderStatusByOrderSn(orderSn);
        return R.ok().setData(orderEntity);
    }
    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("order:order:info")
    public R info(@PathVariable("id") Long id){
		OrderEntity order = orderService.getById(id);

        return R.ok().put("order", order);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("order:order:save")
    public R save(@RequestBody OrderEntity order){
		orderService.save(order);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("order:order:update")
    public R update(@RequestBody OrderEntity order){
		orderService.updateById(order);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("order:order:delete")
    public R delete(@RequestBody Long[] ids){
		orderService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
