package com.atguigu.controller;

import com.atguigu.feign.OrderFeignClient;
import com.atguigu.result.RetVal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
public class WebOrderController {

    @Autowired
    private OrderFeignClient orderFeignClient;

    @RequestMapping("confirm.html")
    public String confirm(Model model){
        RetVal<Map> retVal = orderFeignClient.confirm();
        model.addAllAttributes(retVal.getData());
        return "order/confirm";
    }


}
