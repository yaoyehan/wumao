package com.yyh.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.yyh.gulimall.product.vo.Catalog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yyh.common.utils.PageUtils;
import com.yyh.common.utils.Query;

import com.yyh.gulimall.product.dao.CategoryDao;
import com.yyh.gulimall.product.entity.CategoryEntity;
import com.yyh.gulimall.product.service.CategoryService;
import org.springframework.util.StringUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {
//    @Autowired
//    private CategoryDao categoryDao;
    @Autowired
    CategoryBrandRelationServiceImpl categoryBrandRelationService;
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    RedissonClient redissonClient;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );
        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);
        List<CategoryEntity> level1Menus=categoryEntities.stream().filter((categoryEntity) ->
                categoryEntity.getParentCid() == 0
        ).map((menu)->{
            menu.setChildren(getChildrens(menu,categoryEntities));
            return menu;
        }).sorted((menu1,menu2)->{
            return (menu1.getSort()==null?0:menu1.getSort())-(menu2.getSort()==null?0: menu2.getSort());
        }).collect(Collectors.toList());
        return level1Menus;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO 1.检查当前的id是否被别的地方调用
        baseMapper.deleteBatchIds(asList);
    }

    @Override
    public Long[] findcatalogPath(Long catalogId) {
        ArrayList<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(paths, catalogId);
        Collections.reverse(parentPath);
        return parentPath.toArray(new Long[parentPath.size()]);
    }
    //更新所有级联的数据
    //删除多个缓存的时候用Caching
/*    @Caching(evict = {
            @CacheEvict(value = "category",key = "'getLevel1Categorys'"),
            @CacheEvict(value = "category",key = "'getCatalogJson'")
    })*/
    @CacheEvict(value = "category",allEntries = true)
    //@CachePut双写模式，修改后再在缓存中保存一份自己修改的最新版
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());
    }
    @Cacheable(value = {"category"},key = "#root.method.name")
    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        List<CategoryEntity> entities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        return entities;
    }
    public Map<String, List<Catalog2Vo> > getCatalogJsonFromDbWithRedissonLock()  {

    //占分布式锁
        RLock lock = redissonClient.getLock("CatalogJson-Lock");
        lock.lock();
        //加锁成功
        Map<String, List<Catalog2Vo>> dataFromDb;
        try {
            dataFromDb= getDataFromDb();
        }finally {
            lock.unlock();
        }
        //删除锁时要保证原子性，故此得借助lua脚本解锁
    /*
    String cataloglock = redisTemplate.opsForValue().get("cataloglock");
            if(uuid.equals(cataloglock)){
                redisTemplate.delete("cataloglock");
            }*/
        return dataFromDb;

    }

    public Map<String, List<Catalog2Vo> > getCatalogJsonFromDbWithRedisLock()  {
        String uuid=UUID.randomUUID().toString();
        //占分布式锁
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("CatalogLock", uuid,300,TimeUnit.SECONDS);
        if(lock){
            //加锁成功
            System.out.println("获取分布式锁成功");
            Map<String, List<Catalog2Vo>> dataFromDb;
            try {
                 dataFromDb= getDataFromDb();
            }finally {
                String script="if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
                Long cataloglock = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.TYPE), Arrays.asList("CatalogLock"), uuid);
            }
        //删除锁时要保证原子性，故此得借助lua脚本解锁
        /*
        String cataloglock = redisTemplate.opsForValue().get("cataloglock");
                if(uuid.equals(cataloglock)){
                    redisTemplate.delete("cataloglock");
                }*/
            return dataFromDb;
        }else {
            //加锁失败
            System.out.println("获取分布式锁失败");
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {

            }
            return getCatalogJsonFromDbWithRedisLock();//自旋
        }
    }

    private Map<String, List<Catalog2Vo>> getDataFromDb() {
        //得再次确认是否有缓存，如果没有才需要到数据库中查询
        String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
        if(!StringUtils.isEmpty(catalogJSON)){
            Map<String, List<Catalog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catalog2Vo>>>() {
            });
            return result;
        }
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        //查出所有1级分类
        List<CategoryEntity> level1Categorys = getParent_cid(selectList,0L);
        //封装数据
        System.out.println("查询了数据库=======================》》》》》》》");
        Map<String, List<Catalog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //查到一级分类内的所有二级分类
            List<CategoryEntity> entities = getParent_cid(selectList,v.getCatId());
            List<Catalog2Vo> catalog2Vos = null;
            if (entities != null) {
                catalog2Vos = entities.stream().map(l2 -> {
                    Catalog2Vo catalog2Vo = new Catalog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    //当前二级分类的三级分类，并对其进行封装
                    List<CategoryEntity> level3Catalog = getParent_cid(selectList,l2.getCatId());
                    if(level3Catalog!=null){
                        //封装为指定格式
                        List<Catalog2Vo.Catalog3Vo> collect = level3Catalog.stream().map(l3 -> {
                            Catalog2Vo.Catalog3Vo catalog3Vo = new Catalog2Vo.Catalog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return catalog3Vo;
                        }).collect(Collectors.toList());
                        catalog2Vo.setCatalog3List(collect);
                    }
                    return catalog2Vo;
                }).collect(Collectors.toList());
            }
            return catalog2Vos;
        }));
        return parent_cid;
    }
    @Cacheable(value = {"category"},key = "#root.methodName")
    @Override
    public Map<String, List<Catalog2Vo>> getCatalogJson() {
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        //查出所有1级分类
        List<CategoryEntity> level1Categorys = getParent_cid(selectList,0L);
        //封装数据
        System.out.println("查询了数据库=======================》》》》》》》");
        Map<String, List<Catalog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //查到一级分类内的所有二级分类
            List<CategoryEntity> entities = getParent_cid(selectList,v.getCatId());
            List<Catalog2Vo> catalog2Vos = null;
            if (entities != null) {
                catalog2Vos = entities.stream().map(l2 -> {
                    Catalog2Vo catalog2Vo = new Catalog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    //当前二级分类的三级分类，并对其进行封装
                    List<CategoryEntity> level3Catalog = getParent_cid(selectList,l2.getCatId());
                    if(level3Catalog!=null){
                        //封装为指定格式
                        List<Catalog2Vo.Catalog3Vo> collect = level3Catalog.stream().map(l3 -> {
                            Catalog2Vo.Catalog3Vo catalog3Vo = new Catalog2Vo.Catalog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return catalog3Vo;
                        }).collect(Collectors.toList());
                        catalog2Vo.setCatalog3List(collect);
                    }
                    return catalog2Vo;
                }).collect(Collectors.toList());
            }

            return catalog2Vos;
        }));
        return parent_cid;
    }

    //@Override
    public Map<String, List<Catalog2Vo>> getCatalogJson2() {
        /**
         * 1.空值结果缓存：解决缓存击穿问题
         * 2.设置过期时间：解决缓存雪崩
         * 3.加锁：解决缓存击穿
         */
        //1加入缓存逻辑
        String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
        if(StringUtils.isEmpty(catalogJSON)){
            System.out.println("缓存不命中，查询数据库");
            Map<String, List<Catalog2Vo>> catalogJsonFromDb = getCatalogJsonFromDbWithRedisLock();
            String jsonString = JSON.toJSONString(catalogJsonFromDb);
            redisTemplate.opsForValue().set("catalogJSON",jsonString,1, TimeUnit.DAYS);
            return catalogJsonFromDb;
        }
        System.out.println("缓存命中，不去查询数据库");
        Map<String, List<Catalog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catalog2Vo>>>() {
        });
        return result;
    }
    //从数据库查询封装分类数据

    public Map<String, List<Catalog2Vo>> getCatalogJsonFromDbWithLocalLock() {
        synchronized (this){
            //得再次确认是否有缓存，如果没有才需要到数据库中查询
            String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
            if(!StringUtils.isEmpty(catalogJSON)){
                Map<String, List<Catalog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catalog2Vo>>>() {
                });
                return result;
            }
            List<CategoryEntity> selectList = baseMapper.selectList(null);
            //查出所有1级分类
            List<CategoryEntity> level1Categorys = getParent_cid(selectList,0L);
            //封装数据
            System.out.println("查询了数据库=======================》》》》》》》");
            Map<String, List<Catalog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
                //查到一级分类内的所有二级分类
                List<CategoryEntity> entities = getParent_cid(selectList,v.getCatId());
                List<Catalog2Vo> catalog2Vos = null;
                if (entities != null) {
                    catalog2Vos = entities.stream().map(l2 -> {
                        Catalog2Vo catalog2Vo = new Catalog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                        //当前二级分类的三级分类，并对其进行封装
                        List<CategoryEntity> level3Catalog = getParent_cid(selectList,l2.getCatId());
                        if(level3Catalog!=null){
                            //封装为指定格式
                            List<Catalog2Vo.Catalog3Vo> collect = level3Catalog.stream().map(l3 -> {
                                Catalog2Vo.Catalog3Vo catalog3Vo = new Catalog2Vo.Catalog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                                return catalog3Vo;
                            }).collect(Collectors.toList());
                            catalog2Vo.setCatalog3List(collect);
                        }
                        return catalog2Vo;
                    }).collect(Collectors.toList());
                }

                return catalog2Vos;
            }));
            String jsonString = JSON.toJSONString(parent_cid);
            redisTemplate.opsForValue().set("catalogJSON",jsonString,1, TimeUnit.DAYS);
            return parent_cid;
        }
    }

    private List<CategoryEntity> getParent_cid( List<CategoryEntity> selectList,Long parent_cid) {
        List<CategoryEntity> collect = selectList.stream().filter(item -> item.getParentCid().equals(parent_cid)).collect(Collectors.toList());

        return collect;
    }

    public List<Long> findParentPath(List<Long> paths,Long categoryId){
        paths.add(categoryId);
        CategoryEntity byId = this.getById(categoryId);
        if(byId.getParentCid()!=0){
            findParentPath(paths,byId.getParentCid());
        }
        return paths;
    }
    //递归
    private List<CategoryEntity> getChildrens(CategoryEntity root,List<CategoryEntity> all){
        List<CategoryEntity> chlidren=all.stream().filter(categoryEntity ->{
            return categoryEntity.getParentCid().equals(root.getCatId());
        }).map(categoryEntity -> {
            categoryEntity.setChildren(getChildrens(categoryEntity,all));
            return categoryEntity;
        }).sorted((menu1,menu2)->{
            return (menu1.getSort()==null?0:menu1.getSort())-(menu2.getSort()==null?0: menu2.getSort());
        }).collect(Collectors.toList());
        return chlidren;
    }

}