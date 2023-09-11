package com.yyh.gulimall.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


import com.yyh.common.valid.AddGroup;
import com.yyh.common.valid.UpdateGroup;
import com.yyh.common.valid.UpdateStatusGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.yyh.gulimall.product.entity.BrandEntity;
import com.yyh.gulimall.product.service.BrandService;
import com.yyh.common.utils.PageUtils;
import com.yyh.common.utils.R;


/**
 * 品牌
 *
 * @author yyh
 * @email 1697149091@qq.com
 * @date 2022-06-19 10:13:22
 */
@RestController
@RequestMapping("product/brand")
public class BrandController {
    @Autowired
    private BrandService brandService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("product:brand:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = brandService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{brandId}")
    //@RequiresPermissions("product:brand:info")
    public R info(@PathVariable("brandId") Long brandId){
		BrandEntity brand = brandService.getById(brandId);
        return R.ok().put("brand", brand);
    }

    @GetMapping("/infos")
    public R BrandsInfo(@RequestParam("brandIds")List<Long> brandIds){
        List<BrandEntity> brand = brandService.getBrandsByIds(brandIds);
        return R.ok().put("brand", brand);
    }


    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:brand:save")
    public R save(@Validated({AddGroup.class}) @RequestBody BrandEntity brand/*, BindingResult bindingResult*/){
/*        if(bindingResult.hasErrors()){
            HashMap<Object, Object> map = new HashMap<>();
            bindingResult.getFieldErrors().forEach(item->{
                //获取错误的提示信息
                String message=item.getDefaultMessage();
                //获取错误属性的名字
                String field = item.getField();
                map.put(field,message);
            });
            return R.error(400,"提交数据不合法").put("data",map);
        }else {
            brandService.save(brand);
        }*/
        brandService.save(brand);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:brand:update")
    public R update(@Validated({UpdateGroup.class}) @RequestBody BrandEntity brand){
		brandService.updateDetail(brand);
        return R.ok();
    }
    /**
     * 修改状态
     */
    @RequestMapping("/update/status")
    //@RequiresPermissions("product:brand:update")
    public R updateStatus(@Validated({UpdateStatusGroup.class}) @RequestBody BrandEntity brand){
        brandService.updateById(brand);
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:brand:delete")
    public R delete(@RequestBody Long[] brandIds){
		brandService.removeByIds(Arrays.asList(brandIds));
        return R.ok();
    }

}
