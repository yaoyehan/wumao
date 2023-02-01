package com.yyh.gulimall.ware.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


import com.yyh.common.exception.BizCodeEnume;
import com.yyh.common.exception.NoStockException;
import com.yyh.gulimall.ware.Vo.SkuHasStockVo;
import com.yyh.gulimall.ware.Vo.WareSkuLockVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.yyh.gulimall.ware.entity.WareSkuEntity;
import com.yyh.gulimall.ware.service.WareSkuService;
import com.yyh.common.utils.PageUtils;
import com.yyh.common.utils.R;



/**
 * 商品库存
 *
 * @author yyh
 * @email 1697149091@qq.com
 * @date 2022-06-16 17:05:54
 */
@RestController
@RequestMapping("ware/waresku")
public class WareSkuController {
    @Autowired
    private WareSkuService wareSkuService;
    @PostMapping("/lock/order")
    public R orderLockStock(@RequestBody WareSkuLockVo wareSkuLockVo){
        try {
            Boolean stockResultVos=wareSkuService.orderLockStock(wareSkuLockVo);
            return R.ok().setData(stockResultVos);
        }catch (NoStockException e){
            return R.error(BizCodeEnume.NO_STOCK_EXCEPTION.getCode(),BizCodeEnume.NO_STOCK_EXCEPTION.getMsg());
        }
    }


    @PostMapping("/hasstock")
    public R getSkuHasStock(@RequestBody List<Long> skuIds){
        List<SkuHasStockVo> vos=wareSkuService.getSkuHasStock(skuIds);

        return R.ok().setData(vos);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("ware:waresku:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = wareSkuService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("ware:waresku:info")
    public R info(@PathVariable("id") Long id){
		WareSkuEntity wareSku = wareSkuService.getById(id);

        return R.ok().put("wareSku", wareSku);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("ware:waresku:save")
    public R save(@RequestBody WareSkuEntity wareSku){
		wareSkuService.save(wareSku);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("ware:waresku:update")
    public R update(@RequestBody WareSkuEntity wareSku){
		wareSkuService.updateById(wareSku);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("ware:waresku:delete")
    public R delete(@RequestBody Long[] ids){
		wareSkuService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
