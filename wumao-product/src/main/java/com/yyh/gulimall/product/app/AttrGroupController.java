package com.yyh.gulimall.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


import com.yyh.gulimall.product.entity.AttrEntity;
import com.yyh.gulimall.product.service.AttrAttrgroupRelationService;
import com.yyh.gulimall.product.service.AttrService;
import com.yyh.gulimall.product.service.CategoryService;
import com.yyh.gulimall.product.vo.AttrGroupRelationVo;
import com.yyh.gulimall.product.vo.AttrGroupWithAttrsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.yyh.gulimall.product.entity.AttrGroupEntity;
import com.yyh.gulimall.product.service.AttrGroupService;
import com.yyh.common.utils.PageUtils;
import com.yyh.common.utils.R;



/**
 * 属性分组
 *
 * @author yyh
 * @email 1697149091@qq.com
 * @date 2022-06-19 10:13:22
 */
@RestController
@RequestMapping("product/attrgroup")
public class AttrGroupController {
    @Autowired
    private AttrGroupService attrGroupService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private AttrService attrService;
    @Autowired
    private AttrAttrgroupRelationService relationService;
    @PostMapping("/attr/relation")
    public R addRelation(@RequestBody List<AttrGroupRelationVo> vos){
         relationService.saveBatch(vos);
         return R.ok();
    }

    @GetMapping("/{attrgoupId}/attr/relation")
    public R attrRelation(@PathVariable("attrgoupId") Long attrgoupId){
        List<AttrEntity> attrEntities=attrService.getRelationAttr(attrgoupId);
        return R.ok().put("data",attrEntities);
    }

    @GetMapping("/{attrgoupId}/noattr/relation")
    public R attrNoRelation(@RequestParam Map<String, Object> params,@PathVariable("attrgoupId") Long attrgoupId){
        PageUtils attrEntities=attrService.getNoRelationAttr(attrgoupId,params);
        return R.ok().put("page",attrEntities);
    }

    @GetMapping("/{catalogId}/withattr")
    public R getAttrGroupWithAttrs(@PathVariable("catalogId") Long catalogId){
        List<AttrGroupWithAttrsVo> vos=attrGroupService.getAttrGroupWithAttrsBycatalogId(catalogId);
        return R.ok().put("data",vos);
    }

    /**
     * 列表
     */
    @RequestMapping("/list/{catalogId}")
    //@RequiresPermissions("product:attrgroup:list")
    public R list(@RequestParam Map<String, Object> params, @PathVariable("catalogId") Long catalogId){
/*        PageUtils page = attrGroupService.queryPage(params);*/
        PageUtils page = attrGroupService.queryPage(params,catalogId);
        return R.ok().put("page", page);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{attrGroupId}")
    //@RequiresPermissions("product:attrgroup:info")
    public R info(@PathVariable("attrGroupId") Long attrGroupId){
		AttrGroupEntity attrGroup = attrGroupService.getById(attrGroupId);
        Long catalogId = attrGroup.getCatalogId();
        Long [] path=categoryService.findcatalogPath(catalogId);
        attrGroup.setCatalogPath(path);
        return R.ok().put("attrGroup", attrGroup);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:attrgroup:save")
    public R save(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.save(attrGroup);

        return R.ok();
    }
    @PostMapping("/attr/relation/delete")
    public R deleteRelation(@RequestBody AttrGroupRelationVo[] vos){
        attrService.deleteRelation(vos);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:attrgroup:update")
    public R update(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.updateById(attrGroup);
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:attrgroup:delete")
    public R delete(@RequestBody Long[] attrGroupIds){
		attrGroupService.removeByIds(Arrays.asList(attrGroupIds));

        return R.ok();
    }

}
