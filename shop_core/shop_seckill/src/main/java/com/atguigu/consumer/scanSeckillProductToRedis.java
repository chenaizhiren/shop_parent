package com.atguigu.consumer;

import com.atguigu.constant.MqConst;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.SeckillProduct;
import com.atguigu.entity.UserSeckillSkuInfo;
import com.atguigu.service.SeckillProductService;
import com.atguigu.utils.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rabbitmq.client.Channel;
import javafx.scene.chart.ValueAxis;
import jdk.nashorn.internal.ir.CallNode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@Component
public class scanSeckillProductToRedis {

    @Autowired
    private SeckillProductService seckillProductService;
    @Autowired
    private RedisTemplate redisTemplate;




    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.PREPARE_SECKILL_QUEUE,durable = "false"),
                                                      exchange = @Exchange(value = MqConst.PREPARE_SECKILL_EXCHANGE,durable = "false",autoDelete = "true"),
                                                        key = {MqConst.PREPARE_SECKILL_ROUTE_KEY}))
    public void scanSeckillProductToRedis(Message message, Channel channel) throws IOException {
        //扫描数据库中符合秒杀的商品
        QueryWrapper<SeckillProduct> wrapper = new QueryWrapper<>();
        //1为秒杀商品 2为结束 3审核未通过
        wrapper.eq("status",1);
        //剩余库存数大于0
        wrapper.gt("stock_count",0);
        //去取出当前时间,当日商品
        wrapper.eq("DATE_FORMAT(start_time,'%Y-%m-%d')", DateUtil.formatDate(new Date()));

        List<SeckillProduct> seckillProductlist = seckillProductService.list(wrapper);
        if (!CollectionUtils.isEmpty(seckillProductlist)){
            for (SeckillProduct seckillProduct : seckillProductlist) {
                //判断缓存中是否已经有该秒杀商品
                Boolean flag = redisTemplate.boundHashOps(RedisConst.SECKILL_PRODUCT).hasKey(seckillProduct.getSkuId().toString());
                if (flag){
                    //如果有则结束循环
                    continue;
                }
                //没有该秒杀商品就把它放到缓存
                redisTemplate.boundHashOps(RedisConst.SECKILL_PRODUCT).put(seckillProduct.getSkuId().toString(),seckillProduct);
                //利用list的数据结构存储库存数量 减库存的时候吐出一个 防止超卖
                for (Integer i = 0; i < seckillProduct.getNum(); i++) {
                    redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + seckillProduct.getSkuId()).leftPush(seckillProduct.getSkuId().toString());

                }
                //通知redis集群中其他节点该商品可以进行秒杀了 状态位
                redisTemplate.convertAndSend(RedisConst.PREPARE_PUB_SUB_SECKILL,seckillProduct.getSkuId()+":"+RedisConst.CAN_SECKILL);


            }

            //手动确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }
    }





    //2.预下单之后才能判断用户是否有抢购资格
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.PREPARE_SECKILL_QUEUE,durable = "false"),
                                             exchange = @Exchange(value = MqConst.PREPARE_SECKILL_EXCHANGE,durable = "false",autoDelete = "true"),
                                            key = {MqConst.PREPARE_SECKILL_ROUTE_KEY}))
    public void prepareSeckill(UserSeckillSkuInfo userSeckillSkuInfo,Message message,Channel channel) throws Exception{
        if (userSeckillSkuInfo != null){
            //开始处理预下单
            seckillProductService.prepareSecKill(userSeckillSkuInfo);
        }
            //手动ACK
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    //3.清理redis里面的信息
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.CLEAR_REDIS_QUEUE,durable = "false"),
                                            exchange = @Exchange(value = MqConst.CLEAR_REDIS_EXCHANGE,durable = "false",autoDelete = "true"),
                                            key = {MqConst.CLEAR_REDIS_ROUTE_KEY}))
    public void clearRedis(Message message,Channel channel)throws Exception{

        QueryWrapper<SeckillProduct> wrapper = new QueryWrapper<>();
        //1为秒杀商品
        wrapper.eq("status",1);
        wrapper.eq("end_time",new Date());

        //获取到秒杀结束之后的商品数据
        List<SeckillProduct> seckillProductList = seckillProductService.list(wrapper);
        for (SeckillProduct seckillProduct : seckillProductList) {

            //删除库存数
           redisTemplate.delete(RedisConst.SECKILL_STOCK_PREFIX +seckillProduct.getSkuId());
        }


        //删除秒杀商品信息
        redisTemplate.delete(RedisConst.SECKILL_PRODUCT);
        //删除用户抢得预售订单
        redisTemplate.delete(RedisConst.PREPARE_SECKILL_USERID_ORDER);

        // 删除用户秒杀最终抢到的订单
        redisTemplate.delete(RedisConst.BOUGHT_SECKILL_USER_ORDER);

        //更新数据,更新状态  1:秒杀开始 2:秒杀结束
        SeckillProduct seckillProduct = new SeckillProduct();
        seckillProduct.setStatus("2");
        seckillProductService.updateById(seckillProduct);


        //消息确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);

    }


}
