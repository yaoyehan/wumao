package com.yyh.gulimall.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


import com.yyh.gulimall.product.entity.ProductAttrValueEntity;
import com.yyh.gulimall.product.service.ProductAttrValueService;
import com.yyh.gulimall.product.vo.AttrRespVo;
import com.yyh.gulimall.product.vo.AttrVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.yyh.gulimall.product.service.AttrService;
import com.yyh.common.utils.PageUtils;
import com.yyh.common.utils.R;



/**
 * 商品属性
 *
 * @author yyh
 * @email 1697149091@qq.com
 * @date 2022-06-19 10:13:21
 */
@RestController
@RequestMapping("product/attr")
public class AttrController {
    @Autowired
    private AttrService attrService;
    @Autowired
    ProductAttrValueService productAttrValueService;
    @GetMapping("/base/listforspu/{spuId}")
    public R baseAttrlistforspu(@PathVariable("spuId") Long spuId){
        List<ProductAttrValueEntity> entities=productAttrValueService.baseAttrlistforspu(spuId);
        return R.ok().put("data",entities);
    }





    @GetMapping("/{attrType}/list/{catalogId}")
    public R baseAttrList(@RequestParam Map<String,Object> params,
                          @PathVariable("catalogId") Long catalogId,
                          @PathVariable("attrType") String type){
        PageUtils page=attrService.queryBaseAttrPage(params,catalogId,type);
        return R.ok().put("page",page);
    }


    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("product:attr:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = attrService.queryPage(params);
        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{attrId}")
    //@RequiresPermissions("product:attr:info")
    public R info(@PathVariable("attrId") Long attrId){
		AttrRespVo attrRespVo = attrService.getAttrInfo(attrId);
        return R.ok().put("attr", attrRespVo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:attr:save")
    public R save(@RequestBody AttrVo attr){
		attrService.saveAttr(attr);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:attr:update")
    public R update(@RequestBody AttrVo attr){
		attrService.updateAttr(attr);
        return R.ok();
    }
    /**
     * 修改
     */
    @PostMapping("/update/{spuId}")
    //@RequiresPermissions("product:attr:update")
    public R update(@PathVariable("spuId") Long spuId,
                    @RequestBody List<ProductAttrValueEntity> entities){
        productAttrValueService.updateSpuAttr(spuId,entities);
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:attr:delete")
    public R delete(@RequestBody Long[] attrIds){
		attrService.removeByIds(Arrays.asList(attrIds));
        return R.ok();
    }

}
