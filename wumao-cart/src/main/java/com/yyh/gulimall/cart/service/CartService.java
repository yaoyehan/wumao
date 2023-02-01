package com.yyh.gulimall.cart.service;

import com.yyh.gulimall.cart.vo.Cart;
import com.yyh.gulimall.cart.vo.CartItem;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface CartService {
    CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    CartItem getCartItem(Long skuId);

    Cart getCart() throws ExecutionException, InterruptedException;

    void clearCart(String cartKey);

    void checkItem(Long skuId, Integer checked);

    void changeItemCount(Long skuId, Integer num);

    void deleteItem(Long skuId);

    List<CartItem> getUserCartItems();
}
