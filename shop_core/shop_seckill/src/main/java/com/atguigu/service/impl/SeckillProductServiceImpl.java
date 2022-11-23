package com.atguigu.service.impl;

import com.atguigu.constant.RedisConst;
import com.atguigu.entity.PrepareSeckillOrder;
import com.atguigu.entity.SeckillProduct;
import com.atguigu.entity.UserSeckillSkuInfo;
import com.atguigu.mapper.SeckillProductMapper;
import com.atguigu.result.RetVal;
import com.atguigu.result.RetValCodeEnum;
import com.atguigu.service.SeckillProductService;
import com.atguigu.util.MD5;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author zhengyuhao
 * @since 2022-11-21
 */
@Service
public class SeckillProductServiceImpl extends ServiceImpl<SeckillProductMapper, SeckillProduct> implements SeckillProductService {


    @Autowired
    private RedisTemplate redisTemplate;


    public Map<Long,SeckillProduct> firstLevelCache = new ConcurrentHashMap<>();


    @Override
    public List<SeckillProduct> queryAllSeckillProduct() {

        List<SeckillProduct> seckillProductList = redisTemplate.boundHashOps(RedisConst.SECKILL_PRODUCT).values();
        List<SeckillProduct> sortedSecKillList = seckillProductList.stream().sorted(Comparator.comparing(SeckillProduct::getStartTime)).collect(Collectors.toList());
        return sortedSecKillList;

    }

    @Override
    public SeckillProduct querySecKillBySkuId(Long skuId) {
        SeckillProduct seckillProduct = firstLevelCache.get(skuId);
        if (seckillProduct == null){
            seckillProduct = (SeckillProduct) redisTemplate.boundHashOps(RedisConst.SECKILL_PRODUCT).get(skuId.toString());
        }
        return seckillProduct;

    }


    @Override
    public void prepareSecKill(UserSeckillSkuInfo userSeckillSkuInfo) {
        Long skuId = userSeckillSkuInfo.getSkuId();
        String userId = userSeckillSkuInfo.getUserId();
       String state = (String)redisTemplate.opsForValue().get(RedisConst.SECKILL_STATE_PREFIX+userId);

        //1.秒杀商品已经售罄 再次做判断的原因 消息在发送过程中 其他请求有可能已经把商品买走了
        if (RedisConst.CAN_NOT_SECKILL.equals(state)){
            return;
        }

        //2.是否可以进行预下单
        Boolean flag = redisTemplate.opsForValue().setIfAbsent(RedisConst.PREPARE_SECKILL_USERID_SKUID + ":" + userId + ":" + skuId, skuId.toString(), RedisConst.PREPARE_SECKILL_LOCK_TIME, TimeUnit.SECONDS);
        if (!flag){
            return;
        }

        //3.校验商品是否还有库存同时还去减库存
        String redisStockSkuId = (String) redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).rightPop();
        if (StringUtils.isEmpty(redisStockSkuId)){
            //通知其他节点改变秒杀状态位
            redisTemplate.convertAndSend(RedisConst.PREPARE_PUB_SUB_SECKILL,skuId+":"+RedisConst.CAN_NOT_SECKILL);
        }
        //4.生成临时订单到redis
        PrepareSeckillOrder prepareSeckillOrder = new PrepareSeckillOrder();
        prepareSeckillOrder.setUserId(userId);
        prepareSeckillOrder.setBuyNum(1);
        SeckillProduct seckillProduct = querySecKillBySkuId(skuId);
        prepareSeckillOrder.setSeckillProduct(seckillProduct);
        //生成一个预购订单码
        prepareSeckillOrder.setPrepareOrderCode(MD5.encrypt(userId+skuId));
        redisTemplate.boundHashOps(RedisConst.PREPARE_SECKILL_USERID_ORDER).put(userId,prepareSeckillOrder);

        //更新库存量
        updateSecKillStockCount(skuId);

    }


    @Override
    public RetVal hasQualified(Long skuId, String userId) {
        //如果订单中有
        Boolean isExist = redisTemplate.hasKey(RedisConst.PREPARE_SECKILL_USERID_SKUID + userId);
        if (isExist){
            PrepareSeckillOrder prepareSeckillOrder = (PrepareSeckillOrder) redisTemplate.boundHashOps(RedisConst.PREPARE_SECKILL_USERID_ORDER).get(userId);
            if (prepareSeckillOrder != null){
             return RetVal.build(prepareSeckillOrder, RetValCodeEnum.PREPARE_SECKILL_SUCCESS);
            }
        }

        //2.接着上一步 用户已经下过订单 (此处要演示需要重复购买相同的商品才能走这里的代码)
        String orderId = (String) redisTemplate.boundHashOps(RedisConst.BOUGHT_SECKILL_USER_ORDER).get(userId);
        if (!StringUtils.isEmpty(orderId)){
            return RetVal.build(orderId,RetValCodeEnum.SECKILL_ORDER_SUCCESS);
        }

        //其他情况就是排队
        return RetVal.build(null,RetValCodeEnum.SECKILL_RUN);
    }

    private void updateSecKillStockCount(Long skuId) {
        //剩余库存量
        Long leftStock = redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX).size();
        //锁定库存量=总库存量-剩余库存量
        SeckillProduct redisSeckillProduct = querySecKillBySkuId(skuId);
        Integer totalStock = redisSeckillProduct.getNum();
        Integer lockStock = totalStock - Integer.parseInt(leftStock+"");

        redisSeckillProduct.setStockCount(lockStock);
        //更新到redis里面的商品信息里面 目的是给消费者看 才能知道当前的一个进度
        redisTemplate.boundHashOps(RedisConst.SECKILL_PRODUCT).put(skuId.toString(),redisSeckillProduct);

        //更新到数据库里面 定义一个规则
        if (leftStock % 2 == 0){
            //更新到数据库
            baseMapper.updateById(redisSeckillProduct);
        }
    }
}
