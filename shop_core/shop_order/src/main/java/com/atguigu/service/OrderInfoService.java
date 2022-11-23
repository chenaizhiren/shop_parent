package com.atguigu.service;

import com.atguigu.entity.OrderInfo;
import com.atguigu.enums.ProcessStatus;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 订单表 订单表 服务类
 * </p>
 *
 * @author zhengyuhao
 * @since 2022-11-15
 */
public interface OrderInfoService extends IService<OrderInfo> {

    Long saveOrderAndDetail(OrderInfo orderInfo);

    String generateTradeNo(String userId);

    boolean checkTradeNo(String tradeNoUI, String userId);

    void deleteTradeNo(String userId);

    String checkPriceAndStock(OrderInfo orderInfo);

    OrderInfo getOrderInfoAndDetail(Long orderId);

    void updateOrderStatus(Long orderId, ProcessStatus processStatus);

    OrderInfo getOrderInfo(Long orderId);
    void sendMsgToWarehouse(Long orderId);

    Map<String, Object> assembWareHouseDate(OrderInfo orderInfo);

    List<OrderInfo> splitOrder(Long orderId,String wareHouseIdSkuIdJson);
}
