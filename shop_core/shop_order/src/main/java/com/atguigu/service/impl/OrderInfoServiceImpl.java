package com.atguigu.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.constant.MqConst;
import com.atguigu.entity.OrderDetail;
import com.atguigu.entity.OrderInfo;
import com.atguigu.enums.OrderStatus;
import com.atguigu.enums.ProcessStatus;
import com.atguigu.feign.CartFeignClient;
import com.atguigu.feign.ProductFeignClient;
import com.atguigu.mapper.OrderDetailMapper;
import com.atguigu.mapper.OrderInfoMapper;
import com.atguigu.service.OrderDetailService;
import com.atguigu.service.OrderInfoService;
import com.atguigu.util.HttpClientUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * 订单表 订单表 服务实现类
 * </p>
 *
 * @author zhengyuhao
 * @since 2022-11-15
 */
@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {


    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private OrderDetailService orderDetailService;
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Value("${cancel.order.delay}")
    private Integer    cancelOrderDelay;

    @Transactional
    @Override
    public Long saveOrderAndDetail(OrderInfo orderInfo) {
        //商品对外交易号
        String outTradeNo="atguigu"+System.currentTimeMillis();
        orderInfo.setOutTradeNo(outTradeNo);
        orderInfo.setTradeBody("天气转冷 买个热水宝");
        orderInfo.setCreateTime(new Date());
        //订单过期时间
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.MINUTE,15);
        orderInfo.setExpireTime(instance.getTime());
        //设置订单的进度状态
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        //保存订单基本信息
        baseMapper.insert(orderInfo);
        //保存订单的详情
        Long orderId = orderInfo.getId();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderId);
        }
        orderDetailService.saveBatch(orderDetailList);

        //发送一个延迟消息,取消订单

        rabbitTemplate.convertAndSend(
                MqConst.CANCEL_ORDER_EXCHANGE,
                MqConst.CANCEL_ORDER_ROUTE_KEY,
                orderId,
                correlationData ->{
                        correlationData.getMessageProperties().setDelay(cancelOrderDelay);
                        return correlationData;
                });


        return orderId;
    }

    @Override
    public String generateTradeNo(String userId) {
        String tradeNo = UUID.randomUUID().toString();
        //往redis中放一份
        String tradeNoKey="user:"+userId+":tradeNo";
        redisTemplate.opsForValue().set(tradeNoKey,tradeNo);
        return tradeNo;
    }

    @Override
    public boolean checkTradeNo(String tradeNoUI, String userId) {
        String tradeNoKey="user:"+userId+":tradeNo";
        String redisTradeNo=(String)redisTemplate.opsForValue().get(tradeNoKey);
        return tradeNoUI.equals(redisTradeNo);
    }

    @Override
    public void deleteTradeNo(String userId) {
        String tradeNoKey="user:"+userId+":tradeNo";
        redisTemplate.delete(tradeNoKey);
    }

    @Override
    public String checkPriceAndStock(OrderInfo orderInfo) {
        StringBuilder sb = new StringBuilder();
        //a.拿到所有的商品清单
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if(!CollectionUtils.isEmpty(orderDetailList)){
            for (OrderDetail orderDetail : orderDetailList) {
                //判断价格是否有变化
                BigDecimal orderPrice = orderDetail.getOrderPrice();
                Long skuId = orderDetail.getSkuId();
                String skuNum = orderDetail.getSkuNum();
                BigDecimal realTimePrice = productFeignClient.getSkuPrice(skuId);
                if(realTimePrice.compareTo(orderPrice)!=0){
                    sb.append(orderDetail.getSkuName()+"价格有变化，需要刷新页面");
                }
                //判断库存是否足够 http://localhost:8100/hasStock?skuId=24&num=97
                String url="http://localhost:8100/hasStock?skuId="+skuId+"&num="+skuNum;
                String result = HttpClientUtil.doGet(url);
                //0：无库存  1：有库存
                if("0".equals(result)){
                    sb.append(orderDetail.getSkuName()+"库存不足");
                }
            }
        }
        return sb.toString();
    }

    @Override
    public OrderInfo getOrderInfoAndDetail(Long orderId) {
        //1.订单的基本信息
        OrderInfo orderInfo = baseMapper.selectById(orderId);
        //2.订单的详情信息
        if(orderInfo!=null){
            QueryWrapper<OrderDetail> wrapper = new QueryWrapper<>();
            wrapper.eq("order_id",orderId);
            List<OrderDetail> orderDetailList = orderDetailService.list(wrapper);
            orderInfo.setOrderDetailList(orderDetailList);
        }
        return orderInfo;
    }


    @Override
    public void updateOrderStatus(Long orderId, ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        //修改订单状态
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());
        orderInfo.setProcessStatus(processStatus.name());
        baseMapper.updateById(orderInfo);

    }


    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        OrderInfo orderInfo = baseMapper.selectById(orderId);
        if (orderInfo != null){
            QueryWrapper<OrderDetail> wrapper = new QueryWrapper<>();
            wrapper.eq("order_id",orderId);
            List<OrderDetail> orderDetailList = orderDetailMapper.selectList(wrapper);
            orderInfo.setOrderDetailList(orderDetailList);
        }
        return orderInfo;
    }


    @Override
    public void sendMsgToWarehouse(Long orderId) {
        //将订单的状态改为已通知仓库
        updateOrderStatus(orderId,ProcessStatus.NOTIFIED_WARE);
        //要发送的字符串
        OrderInfo orderInfo = getOrderInfo(orderId);
        Map<String,Object> wareHouseDateMap= assembWareHouseDate(orderInfo);
        String wareHouseDate = JSON.toJSONString(wareHouseDateMap);

        //发送通知给库存
        rabbitTemplate.convertAndSend(MqConst.DECREASE_STOCK_EXCHANGE,MqConst.DECREASE_STOCK_ROUTE_KEY,wareHouseDate);
    }


    @Override
    public List<OrderInfo> splitOrder(Long orderId, String wareHouseIdSkuIdJson) {
        OrderInfo originalOrder = getOrderInfo(orderId);
        List<Map> wareHouseIdSkuIdMapList = JSON.parseArray(wareHouseIdSkuIdJson, Map.class);

        ArrayList<OrderInfo> childOrderList = new ArrayList<>();
        for (Map wareHouseIdSkuIdMap : wareHouseIdSkuIdMapList) {
            String wareHouseId = (String) wareHouseIdSkuIdMap.get("wareHouseId");
            List<String> skuIdList = (List<String>) wareHouseIdSkuIdMap.get("skuIdList");

            //设置子id信息
            OrderInfo childOrderInfo = new OrderInfo();
            BeanUtils.copyProperties(originalOrder,childOrderInfo);

            childOrderInfo.setId(null);
            childOrderInfo.setParentOrderId(orderId);
            childOrderInfo.setWareHouseId(wareHouseId);

            List<OrderDetail> originalOrderDetailList = originalOrder.getOrderDetailList();
            //设置子订单详情信息
            ArrayList<OrderDetail> childOrderDetailList = new ArrayList<>();
            BigDecimal childTotalMoney = new BigDecimal(0);
            for (OrderDetail originalOrderDetail : originalOrderDetailList) {
                for (String skuId : skuIdList) {

                    //如果原始订单信息属于该订单
                    if (Long.parseLong(skuId) == originalOrderDetail.getSkuId()){
                        //把原始订单信息放入子订单中
                        childOrderDetailList.add(originalOrderDetail);
                        //设置总金额
                        String skuNum = originalOrderDetail.getSkuNum();
                        BigDecimal orderPrice = originalOrderDetail.getOrderPrice();
                        childTotalMoney = childTotalMoney.add(orderPrice.multiply(new BigDecimal(skuNum)));
                    }
                }
            }
            childOrderInfo.setOrderDetailList(childOrderDetailList);
            childOrderInfo.setTotalMoney(childTotalMoney);

            //保存子订单信息
            saveOrderAndDetail(childOrderInfo);
            //添加子订单信息到集合中
            childOrderList.add(childOrderInfo);

        }

        //修改订单状态
        updateOrderStatus(orderId,ProcessStatus.SPLIT);
        return childOrderList;

    }

    @Override
    public Map<String, Object> assembWareHouseDate(OrderInfo orderInfo) {
        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put("orderId",orderInfo.getId());
        dataMap.put("consignee",orderInfo.getConsignee());
        dataMap.put("consigneeTel",orderInfo.getConsigneeTel());
        dataMap.put("orderComment",orderInfo.getOrderComment());
        dataMap.put("orderBody",orderInfo.getTradeBody());
        dataMap.put("deliveryAddress",orderInfo.getDeliveryAddress());
        dataMap.put("paymentWay",2);

        //仓库id减库存使用
        dataMap.put("wareid",orderInfo.getWareHouseId());

        List<Map> skuInfoMap = new ArrayList<>();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            HashMap<String, Object> skuInfo = new HashMap<>();
            Map<String, Object> detailMap = new HashMap<>();
            detailMap.put("skuId",orderDetail.getSkuId());
            detailMap.put("skuNum",orderDetail.getSkuNum());
            detailMap.put("skuName",orderDetail.getSkuName());
            skuInfoMap.add(detailMap);
        }
        dataMap.put("details",skuInfoMap);
        return  dataMap;
    }
}
