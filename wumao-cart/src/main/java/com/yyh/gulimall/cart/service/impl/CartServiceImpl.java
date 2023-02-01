package com.yyh.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.yyh.common.utils.R;
import com.yyh.gulimall.cart.feign.ProductFeignService;
import com.yyh.gulimall.cart.interceptor.CartInterceptor;
import com.yyh.gulimall.cart.service.CartService;
import com.yyh.gulimall.cart.vo.Cart;
import com.yyh.gulimall.cart.vo.CartItem;
import com.yyh.gulimall.cart.vo.SkuInfoVo;
import com.yyh.gulimall.cart.vo.UserInfoTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CartServiceImpl implements CartService {
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    ThreadPoolExecutor executor;



    public final String CART_PREFIX="gulimall:cart:";


    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String result = (String)cartOps.get(skuId.toString());
        if(StringUtils.isEmpty(result)){
            CartItem cartItem = new CartItem();
            //购物车无此商品
            //2.添加新商品到购物车
            //1.远程查询当前要添加的商品的详细信息
            CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
                R skuInfo = productFeignService.getSkuInfo(skuId);
                SkuInfoVo data = skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                });
                cartItem.setCheck(true);
                cartItem.setCount(1);
                cartItem.setCount(num);
                cartItem.setImage(data.getSkuDefaultImg());
                cartItem.setTitle(data.getSkuTitle());
                cartItem.setSkuId(data.getSkuId());
                cartItem.setPrice(data.getPrice());
            }, executor);

            //远程查出sku的组合信息
            CompletableFuture<Void> getSkuSaleAttrValues = CompletableFuture.runAsync(() -> {
                List<String> skuSaleAttrValues = productFeignService.getSkuSaleAttrValues(skuId);
                cartItem.setSkuAttr(skuSaleAttrValues);
            }, executor);
            CompletableFuture.allOf(getSkuInfoTask,getSkuSaleAttrValues).get();
            String jsonString = JSON.toJSONString(cartItem);
            cartOps.put(skuId.toString(),jsonString);
            return cartItem;
        }
        else {
            CartItem cartItem = JSON.parseObject(result, CartItem.class);
            cartItem.setCount(cartItem.getCount()+num);
            String jsonString = JSON.toJSONString(cartItem);
            cartOps.put(skuId.toString(),jsonString);
            return cartItem;
        }

    }

    @Override
    public CartItem getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String str = (String) cartOps.get(skuId.toString());
        CartItem cartItem = JSON.parseObject(str, CartItem.class);
        return cartItem;
    }

    @Override
    public Cart getCart() throws ExecutionException, InterruptedException {
        Cart cart = new Cart();
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if(userInfoTo.getUserId()!=null){
            String cartKey=CART_PREFIX+userInfoTo.getUserId();
            String TempCartKey=CART_PREFIX+userInfoTo.getUserkey();
            //如果临时购物车内有数据，则将数据合并
            List<CartItem> TempCartItems=getCartItems(CART_PREFIX+userInfoTo.getUserkey());
            if(TempCartItems!=null){
                for (CartItem tempCartItem : TempCartItems) {
                    addToCart(tempCartItem.getSkuId(),tempCartItem.getCount());
                }
                //清空临时购物车
                clearCart(TempCartKey);
            }
            //获取合并后的购物车的数据
            List<CartItem> cartItems = getCartItems(cartKey);
            if(cartItems!=null){
                cart.setItems(cartItems);
            }
        }else {
            String cartKey=CART_PREFIX+userInfoTo.getUserkey();
            List<CartItem> cartItems = getCartItems(cartKey);
            if(cartItems!=null){
                cart.setItems(cartItems);
            }

        }
        return cart;
    }

    private List<CartItem> getCartItems(String userIdOrKey) {
        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(userIdOrKey);
        List<Object> values = operations.values();
        if(values!=null&&values.size()>0) {
            List<CartItem> collect = values.stream().map((Object) -> {
                String s =(String) Object;
                CartItem cartItem = JSON.parseObject(s, CartItem.class);
                return cartItem;
            }).collect(Collectors.toList());
            return collect;
        }else {
            return null;
        }
    }

    /**
     * 获取到我们要保存的商品
     * @return
     */
    private BoundHashOperations<String, Object, Object> getCartOps() {
        Cart cart = new Cart();
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        String cartKey="";
        if(userInfoTo.getUserId()!=null){
            cartKey=CART_PREFIX+userInfoTo.getUserId();
        }else {
            cartKey=CART_PREFIX+userInfoTo.getUserkey();
        }
        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(cartKey);
        return operations;
    }

    @Override
    public void clearCart(String cartKey){

        redisTemplate.delete(cartKey);
    }

    @Override
    public void checkItem(Long skuId, Integer checked) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCheck(checked==1?true:false);
        String jsonString = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(),jsonString);
    }

    @Override
    public void changeItemCount(Long skuId, Integer num) {
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCount(num);
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
    }

    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }

    @Override
    public List<CartItem> getUserCartItems() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        //如果用户未登录直接返回null
        if (userInfoTo.getUserId() == null) {
            return null;
        } else {
            //获取购物车项
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            //获取所有的
            List<CartItem> cartItems = getCartItems(cartKey);
            List<CartItem> collect = cartItems.stream()
                    .filter(item -> item.getCheck())
                    .map(item -> {
                        R price = productFeignService.getPrice(item.getSkuId());
                        //更新为最新价格
                        String data=(String)price.get("data");
                        item.setPrice(new BigDecimal(data));
                        return item;
                    }).collect(Collectors.toList());
            return collect;
        }
    }
}
