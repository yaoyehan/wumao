package com.yyh.gulimall.cart.controller;

import com.yyh.common.constant.AuthServerConstant;
import com.yyh.gulimall.cart.interceptor.CartInterceptor;
import com.yyh.gulimall.cart.service.CartService;
import com.yyh.gulimall.cart.vo.Cart;
import com.yyh.gulimall.cart.vo.CartItem;
import com.yyh.gulimall.cart.vo.UserInfoTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Controller
public class CartController {
    @Autowired
    CartService cartService;

    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId){
        cartService.deleteItem(skuId);
        return "redirect:http://cart.gulimall.com/cart.html";
    }
    @ResponseBody
    @GetMapping("/`deleteSession`")
    public String deleteSession(HttpSession session){
        session.invalidate();
        return "ok";
    }
    /**
     * 获取当前用户的购物车商品项
     * @return
     */
    @GetMapping(value = "/currentUserCartItems")
    @ResponseBody
    public List<CartItem> getCurrentCartItems() {

        List<CartItem> cartItemVoList = cartService.getUserCartItems();

        return cartItemVoList;
    }


    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId") Long skuId,@RequestParam("num") Integer num){
        cartService.changeItemCount(skuId,num);
        return "redirect:http://cart.gulimall.com/cart.html";
    }


    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId,@RequestParam("checked") Integer checked){
        cartService.checkItem(skuId,checked);
        return "redirect:http://cart.gulimall.com/cart.html";
    }

    @GetMapping("cart.html")
    public String cart(Model model) throws ExecutionException, InterruptedException {
        Cart cart=cartService.getCart();
        model.addAttribute("cart",cart);
        return "cartList";
    }
    @GetMapping("addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num, RedirectAttributes redirectAttributes) throws ExecutionException, InterruptedException {
        CartItem cartItem=cartService.addToCart(skuId,num);

        redirectAttributes.addAttribute("skuId",skuId);
        //添加商品到购物车
        return "redirect:http://cart.gulimall.com/addToCartSuccess.html";
    }
    @GetMapping("addToCartSuccess.html")
    public String addToCartSuccessPage(@RequestParam("skuId") Long skuId,Model model) {
        CartItem cartItem=cartService.getCartItem(skuId);
        model.addAttribute("item",cartItem);
        return "success";
    }
}
