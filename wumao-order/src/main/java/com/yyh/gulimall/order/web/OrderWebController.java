package com.yyh.gulimall.order.web;


import com.yyh.common.exception.NoStockException;
import com.yyh.gulimall.order.service.OrderService;
import com.yyh.gulimall.order.vo.OrderConfirmVo;
import com.yyh.gulimall.order.vo.OrderSubmitVo;
import com.yyh.gulimall.order.vo.SubmitOrderResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.jws.WebParam;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ExecutionException;

@Controller
public class OrderWebController {
    @Autowired
    OrderService orderService;


    /**
     * 去结算确认页
     * @param model
     * @param request
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @GetMapping(value = "/toTrade")
    public String toTrade(Model model, HttpServletRequest request) throws ExecutionException, InterruptedException {

        OrderConfirmVo confirmVo = orderService.confirmOrder();

        model.addAttribute("confirmOrderData",confirmVo);
        //展示订单确认的数据
        return "confirm";
    }
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo orderSubmitVo, Model model, RedirectAttributes redirectAttributes){

        try {
            SubmitOrderResponseVo responseVo=orderService.submitOrder(orderSubmitVo);
            if(responseVo.getCode()==0){
                //成功
                model.addAttribute("submitOrderResp",responseVo);
                return "pay";
            }else {
                String msg="下单失败";
                switch (responseVo.getCode()){
                    case 1: msg += "令牌订单信息过期，请刷新再次提交"; break;
                    case 2: msg += "订单商品价格发生变化，请确认后再次提交"; break;
                    case 3: msg += "库存锁定失败，商品库存不足"; break;
                }
                redirectAttributes.addFlashAttribute("msg",msg);
                return "redirect:http://order.gulimall.com/toTrade";
            }
        }catch (Exception e){
            if (e instanceof NoStockException) {
                String message = ((NoStockException)e).getMessage();
                redirectAttributes.addFlashAttribute("msg",message);
            }
            return "redirect:http://order.gulimall.com/toTrade";
        }

    }

}
