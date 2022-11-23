package com.atguigu.cache;

import com.alibaba.fastjson.JSON;
import com.atguigu.constant.RedisConst;
import com.atguigu.exception.SleepUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Component
@Aspect
public class ShopCacheAspect {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;


    //表示通过key来获取缓存数据
    private Object cacheHit(String key){
        //从缓存中获取查询信息
        Object cache = redisTemplate.opsForValue().get(key);
        //如果缓存不为空,把信息返回
        if (!StringUtils.isEmpty(cache)){
            return cache;
        }
        return null;

    }

    @Around("@annotation(com.atguigu.cache.ShopCache)")
    public Object cacheAroundAdvice(ProceedingJoinPoint target){

        Object retVal = null;

        //获取目标方法
        MethodSignature signature = (MethodSignature) target.getSignature();
        //获取目标方法上的注解
        ShopCache shopCache = signature.getMethod().getAnnotation(ShopCache.class);

        //获取目标方法注解上的前缀sku
        String prefix = shopCache.prefix();
        //目标方法上的参数
        Object[] methodParams = target.getArgs();
        //拼接参数
        String key = prefix + Arrays.asList(methodParams).toString();
        //通过Key来获取缓存中的数据
        retVal  = cacheHit(key);
        if (retVal == null){

            try {
                //使用redisson做分布式锁拿到锁sku[18]:lock
                RLock lock = redissonClient.getLock(key + ":lock");
                //判断是否加锁成功
                boolean acquireLock = lock.tryLock(RedisConst.WAITTIN_GET_LOCK_TIME, RedisConst.LOCK_EXPIRE_TIME, TimeUnit.SECONDS);

                if (acquireLock){
                    try {
                        //如果拿到锁,执行目标方法
                        retVal = target.proceed(target.getArgs());

                        //如果数据库没有数据,为防止缓存穿透,加一个空值
                        if (retVal == null){
                            Object emptyRetVal = new Object();
                            //缓存中设置值,这个值应该给一个短暂的过期时间
                            redisTemplate.opsForValue().set(key, JSON.toJSON(emptyRetVal),RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                            return emptyRetVal;
                        }
                        //如果查询出来的数据不为空
                        redisTemplate.opsForValue().set(key,retVal,RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);
                        return retVal;

                    }catch (Throwable e){
                        e.printStackTrace();
                    }finally {
                        lock.unlock();
                    }
                }else {
                    //睡一会,睡醒之后从缓存中获取数据
                    SleepUtils.sleep(1);
                    return cacheHit(key);
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }else {
            return retVal;
        }

        return retVal;
    }
}
