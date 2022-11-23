package com.atguigu.service.impl;

import com.atguigu.constant.RedisConst;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuImage;
import com.atguigu.entity.SkuInfo;
import com.atguigu.exception.SleepUtils;
import com.atguigu.mapper.SalePropertyKeyMapper;
import com.atguigu.mapper.SkuSalePropertyValueMapper;
import com.atguigu.service.SkuDetailService;
import com.atguigu.service.SkuImageService;
import com.atguigu.service.SkuInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class SkuDetailServiceImpl implements SkuDetailService {



    @Autowired
    private SkuInfoService skuInfoService;
    @Autowired
    private SkuImageService skuImageService;
    @Autowired
    private SalePropertyKeyMapper salePropertyKeyMapper;
    @Autowired
    private SkuSalePropertyValueMapper skuSalePropertyValueMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RBloomFilter skuBloomFilter;





    private SkuInfo getSkuInfoFromDB(Long skuId){
        SkuInfo skuInfo = skuInfoService.getById(skuId);
        if (skuInfo != null){
            QueryWrapper<SkuImage> wrapper = new QueryWrapper<>();

            wrapper.eq("sku_id",skuId);
            List<SkuImage> skuImageList = skuImageService.list(wrapper);
            skuInfo.setSkuImageList(skuImageList);
        }
            return skuInfo;
    }




    private SkuInfo getInfoFromRedis(Long skuId){
        String cacheKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
        SkuInfo skuInfoRedis = (SkuInfo) redisTemplate.opsForValue().get(cacheKey);

        if (skuInfoRedis == null){
            SkuInfo skuInfoDb = getSkuInfoFromDB(skuId);
            redisTemplate.opsForValue().set(cacheKey,skuInfoDb,RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
            return skuInfoDb;
        }
        return skuInfoRedis;
    }



    private SkuInfo getSkuInfoFromRedisAul(Long skuId){
        ThreadLocal<String> threadLocal = new ThreadLocal<>();
        String localKey = "local-" + skuId;
        String cacheKey = RedisConst.SKUKEY_PREFIX + RedisConst.SKULOCK_SUFFIX;
        SkuInfo skuRedisInfo = (SkuInfo) redisTemplate.opsForValue().get(cacheKey);
        if (skuRedisInfo == null){
            String token = threadLocal.get();
            boolean acquireLocal = false;
            if (token != null){
                acquireLocal = true;
            }else {
                token = UUID.randomUUID().toString();
                acquireLocal = redisTemplate.opsForValue().setIfAbsent(localKey,token,3,TimeUnit.SECONDS);

            }

            if (acquireLocal){
                SkuInfo skuInfoFromDB = getSkuInfoFromDB(skuId);
                redisTemplate.opsForValue().set(cacheKey,skuInfoFromDB,RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);

                String luaScript="if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();

                redisScript.setScriptText(luaScript);

                redisScript.setResultType(Long.class);
                redisTemplate.execute(redisScript, Arrays.asList(localKey),token);


                threadLocal.remove();
                return skuInfoFromDB;
            }else {

                while (true){
                    SleepUtils.sleep(3);
                    Boolean retryAcquireLock = redisTemplate.opsForValue().setIfAbsent(localKey, token, 3, TimeUnit.SECONDS);
                    if (retryAcquireLock){
                        threadLocal.set(token);
                        break;
                    }
                }

                return getSkuInfoFromRedisAul(skuId);
            }

        }
        return skuRedisInfo;

    }



    private SkuInfo getSkuInfoFromRedisson(Long skuId){

        //定义一个skuKey
        String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
        //从redis缓存中获取数据
        SkuInfo skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);

        //如果缓存中没有数据
        if (skuInfo == null){
            //如果缓存中没有数据先获取锁,然后把数据放入缓存
            String lockKey = RedisConst.SKUKEY_SUFFIX + skuId + RedisConst.SKULOCK_SUFFIX;
            RLock lock = redissonClient.getLock(lockKey);
            try {
                boolean acquireLock = lock.tryLock(RedisConst.WAITTIN_GET_LOCK_TIME, RedisConst.LOCK_EXPIRE_TIME, TimeUnit.SECONDS);

                if (acquireLock){
                    skuInfo = getSkuInfoFromDB(skuId);


                    //防止缓存穿透
                    if (skuInfo == null){
                        //返回一个空对象,这个对象应该有一个短暂的过期时间
                        SkuInfo emptySkuInfo = new SkuInfo();
                        redisTemplate.opsForValue().set(skuId,emptySkuInfo,RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                        //返回不存在的对象
                        return emptySkuInfo;
                    }
                    //将从数据库中查到的数据放入缓存  商品详情页面数据 同时应该有一个过期时间
                    redisTemplate.opsForValue().set(skuKey,skuInfo,RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                    return skuInfo;
                }else {
                    //自旋等待
                    SleepUtils.sleep(1);
                    return getSkuInfo(skuId);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }finally {
                //不管有没有return都会执行
                lock.unlock();
            }

        }else {
            //直接返回缓存中的数据
            return skuInfo;
        }

        //数据库兜底
        return getSkuInfoFromDB(skuId);
    }

    private SkuInfo getSkuInfoFromRedissonBloom(Long skuId){

        String localKey = "local-" +skuId;
        String cacheKey = RedisConst.SKUKEY_PREFIX + RedisConst.SKULOCK_SUFFIX;
        SkuInfo skuInfoRedis = (SkuInfo) redisTemplate.opsForValue().get(cacheKey);
        RLock lock = redissonClient.getLock(localKey);
        if (skuInfoRedis == null){
            try{
                lock.lock();
                boolean flag = skuBloomFilter.contains(skuId);
                if (flag){
                    SkuInfo skuInfoFromDB = getSkuInfoFromDB(skuId);

                    redisTemplate.opsForValue().set(cacheKey,skuInfoFromDB,RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);
                    return skuInfoFromDB;
            }

            }finally {
                    lock.unlock();
            }
        }
        return skuInfoRedis;
    }







    @Override
    public SkuInfo getSkuInfo(Long skuId) {
        SkuInfo skuInfo = getInfoFromRedis(skuId);

        return skuInfo;
    }




    @Override
    public List<ProductSalePropertyKey> getSkuSalePropertyKeyAndValue(Long productId, Long skuId) {
        return salePropertyKeyMapper.getSkuSalePropertyKeyAndValue(productId, skuId);
    }

    @Override
    public Map<Object,Object> getSkuSalePropertyValueId(Long productId) {

        HashMap<Object, Object> retMap = new HashMap<>();
        List<Map> valueIdMap = skuSalePropertyValueMapper.getSkuSalePropertyValueId(productId);
        if (!CollectionUtils.isEmpty(valueIdMap)){
            for (Map skuMap : valueIdMap) {
                retMap.put(skuMap.get("sale_property_value_id"),skuMap.get("sku_id"));
            }
        }
        return retMap;
    }
}
