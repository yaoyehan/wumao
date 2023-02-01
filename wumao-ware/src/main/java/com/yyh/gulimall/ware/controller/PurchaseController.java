package com.yyh.gulimall.ware.controller;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;


import com.yyh.gulimall.ware.Vo.MergeVo;
import com.yyh.gulimall.ware.Vo.PurchaseDoneVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.yyh.gulimall.ware.entity.PurchaseEntity;
import com.yyh.gulimall.ware.service.PurchaseService;
import com.yyh.common.utils.PageUtils;
import com.yyh.common.utils.R;



/**
 * 采购信息
 *
 * @author yyh
 * @email 1697149091@qq.com
 * @date 2022-06-16 17:05:54
 */
@RestController
@RequestMapping("ware/purchase")
public class PurchaseController {
    @Autowired
    private PurchaseService purchaseService;
    @PostMapping("/done")
    public R finish(@RequestBody PurchaseDoneVo purchaseDoneVo){
        purchaseService.done(purchaseDoneVo);
        return R.ok();
    }


    @PostMapping("merge")
    public R merge(@RequestBody MergeVo mergeVo){
        purchaseService.mergePurchase(mergeVo);
        return R.ok();
    }

    /**
     * 领取采购单
     * @param
     * @return
     */
    @PostMapping("/received")
    public R received(@RequestBody List<Long> ids){
        purchaseService.received(ids);
        return R.ok();

    }


    @RequestMapping("/unreceive/list")
    //@RequiresPermissions("ware:purchase:list")
    public R unreceivelist(@RequestParam Map<String, Object> params){
        PageUtils page = purchaseService.queryPageUnreceivePurchase(params);

        return R.ok().put("page", page);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("ware:purchase:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = purchaseService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("ware:purchase:info")
    public R info(@PathVariable("id") Long id){
		PurchaseEntity purchase = purchaseService.getById(id);

        return R.ok().put("purchase", purchase);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("ware:purchase:save")
    public R save(@RequestBody PurchaseEntity purchase){
        purchase.setUpdateTime(new Date());
        purchase.setCreateTime(new Date());
		purchaseService.save(purchase);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("ware:purchase:update")
    public R update(@RequestBody PurchaseEntity purchase){
		purchaseService.updateById(purchase);
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("ware:purchase:delete")
    public R delete(@RequestBody Long[] ids){
		purchaseService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
