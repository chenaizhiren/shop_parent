package com.atguigu.service.impl;

import com.atguigu.constant.RedisConst;
import com.atguigu.entity.CartInfo;
import com.atguigu.entity.SkuImage;
import com.atguigu.entity.SkuInfo;
import com.atguigu.feign.ProductFeignClient;
import com.atguigu.mapper.CartInfoMapper;
import com.atguigu.result.RetVal;
import com.atguigu.service.AsyncCartInfoService;
import com.atguigu.service.CartInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 购物车表 用户登录系统时更新冗余 服务实现类
 * </p>
 *
 * @author zhengyuhao
 * @since 2022-11-14
 */
@Service
public class CartInfoServiceImpl extends ServiceImpl<CartInfoMapper, CartInfo> implements CartInfoService {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ProductFeignClient productFeignClient;




    @Override
    public RetVal addCart(String oneOfUserId, Long skuId, Integer skuNum) {
        //a.查询缓存中是否有该购物车信息
        BoundHashOperations operations = getRedisOptionByUserId(oneOfUserId);
        boolean isExist = operations.hasKey(skuId.toString());
        //加入购物车之前对所有商品数量进行判断
        if (operations.size() > RedisConst.CART_TOTAL_SIZE) {
            return RetVal.fail().message("购物车数量超过总额限制");
        }
        if (skuNum > RedisConst.CART_SINGLE_SIZE) {
            return RetVal.fail().message("单个商品数量超过限制");
        }
        if (isExist) {
            //b.如果购物车中已经有该购物车 直接对数量进行相加
            CartInfo redisCartInfo = (CartInfo) operations.get(skuId.toString());
            redisCartInfo.setSkuNum(redisCartInfo.getSkuNum() + skuNum);
            //如果后台系统修改了商品的价格 redis里面的价格也需要修改
            redisCartInfo.setRealTimePrice(productFeignClient.getSkuPrice(skuId));
            operations.put(skuId.toString(), redisCartInfo);
        } else {
            //c.如果之前没有添加过该商品 把该商品信息存储在redis里面
            CartInfo cartInfo = new CartInfo();
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            cartInfo.setUserId(oneOfUserId);
            cartInfo.setSkuId(skuId);
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setRealTimePrice(skuInfo.getPrice());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setSkuNum(skuNum);
            cartInfo.setCreateTime(new Date());
            cartInfo.setUpdateTime(new Date());
            //设置默认商品是勾选的
            cartInfo.setIsChecked(1);
            operations.put(skuId.toString(), cartInfo);
            setExpireTime(oneOfUserId);
        }
        return RetVal.ok();
    }

    //设置购物车的过期时间
    private void setExpireTime(String oneOfUserId) {
        String userCartKey = getUserCartKey(oneOfUserId);
        redisTemplate.expire(userCartKey, RedisConst.USER_CART_EXPIRE, TimeUnit.SECONDS);
    }

    //根据用户id得到redis的操作对象
    private BoundHashOperations getRedisOptionByUserId(String oneOfUserId) {
        String userCartKey = getUserCartKey(oneOfUserId);
        return redisTemplate.boundHashOps(userCartKey);
    }

    //得到用户购物车keyId
    private static String getUserCartKey(String oneOfUserId) {
        //user:3:cart
        String userCartKey = RedisConst.USER_KEY_PREFIX + oneOfUserId + RedisConst.USER_CART_KEY_SUFFIX;
        return userCartKey;
    }

    @SneakyThrows
    @Override
    public RetVal getCartList(String userId, String userTempId) {
        String oneOfUserId = "";
        //1.未登录
        if (StringUtils.isEmpty(userId) && !StringUtils.isEmpty(userTempId)) {

            oneOfUserId = userTempId;
        }
        //2.已登录
        if (!StringUtils.isEmpty(userId)) {
            oneOfUserId = userId;
            //查询未登录的时候购物车是否有信息
            BoundHashOperations hashOperations = getRedisOptionByUserId(userTempId);
            Set keys = hashOperations.keys();
            if (!CollectionUtils.isEmpty(keys)) {
                //合并未登录与登录之后的购物项
                mergeCartInfoList(userId, userTempId);
            }
        }
        List<CartInfo> retCartInfoList = queryCartInfoFromRedis(oneOfUserId);
        return RetVal.ok(retCartInfoList);
    }

    @Override
    public void checkCart(String oneOfUserId, Long skuId, Integer isChecked) {
        //a.从redis里面去拿到数据进行修改
        BoundHashOperations operations = getRedisOptionByUserId(oneOfUserId);
        if (operations.hasKey(skuId.toString())) {
            CartInfo redisCartInfo = (CartInfo) operations.get(skuId.toString());
            redisCartInfo.setIsChecked(isChecked);
            //b.更新redis数据并保存
            operations.put(skuId.toString(), redisCartInfo);
            //c.更新一下它的过期时间
            setExpireTime(oneOfUserId);
        }
    }

    @Override
    public void deleteCart(String oneOfUserId, Long skuId) {
        BoundHashOperations operations = getRedisOptionByUserId(oneOfUserId);
        if (operations.hasKey(skuId.toString())) {
            operations.delete(skuId.toString());
        }
    }

    @Override
    public List<CartInfo> getSelectedCartInfo(String userId) {
        BoundHashOperations operations = getRedisOptionByUserId(userId);
        List<CartInfo> redisCartInfoList = operations.values();
        if (!CollectionUtils.isEmpty(redisCartInfoList)) {
            List<CartInfo> selectedCartInfoList =new ArrayList<>();
            for (CartInfo redisCartInfo : redisCartInfoList) {
                if (redisCartInfo.getIsChecked() == 1) {
                    selectedCartInfoList.add(redisCartInfo);
                }
            }
            return selectedCartInfoList;
        }
        return null;
    }

    private void mergeCartInfoList(String userId, String userTempId) {
        //拿到未登录的购物项
        BoundHashOperations tempOperation = getRedisOptionByUserId(userTempId);
        List<CartInfo> noLoginCartInfoList = tempOperation.values();
        //拿到已登录的购物项
        BoundHashOperations userIdOperation = getRedisOptionByUserId(userId);
        List<CartInfo> loginCartInfoList = userIdOperation.values();

        Map<String, CartInfo> loginMap = new HashMap<>();
        for (CartInfo loginCartInfo : loginCartInfoList) {
            loginMap.put(loginCartInfo.getSkuId().toString(), loginCartInfo);
        }
        for (CartInfo noLoginCartInfo : noLoginCartInfoList) {
            String noLoginSkuId = noLoginCartInfo.getSkuId().toString();
            //看已登录购物项里面是否有
            if (loginMap.containsKey(noLoginSkuId)) {
                CartInfo loginCartInfo = loginMap.get(noLoginSkuId);
                loginCartInfo.setSkuNum(loginCartInfo.getSkuNum() + noLoginCartInfo.getSkuNum());
                if (noLoginCartInfo.getIsChecked() == 0) {
                    loginCartInfo.setIsChecked(1);
                }
            } else {
                noLoginCartInfo.setUserId(userId);
                //把这个未登录的购物项添加到已登录的购物项里面
                loginMap.put(noLoginSkuId, noLoginCartInfo);
            }
        }
        userIdOperation.putAll(loginMap);
        //把临时购物车的信息删除
        String userCartKey = getUserCartKey(userTempId);
        redisTemplate.delete(userCartKey);
    }

    private List<CartInfo> queryCartInfoFromRedis(String oneOfUserId) throws InterruptedException, ExecutionException {
        BoundHashOperations hashOption = getRedisOptionByUserId(oneOfUserId);
        List<CartInfo> cartInfoList = hashOption.values();
        //异步修改购物车里面的价格信息
        CompletableFuture<List<CartInfo>> realTimePriceFuture = CompletableFuture.supplyAsync(() -> {
            updateCartRealTimePrice(oneOfUserId, cartInfoList);
            return cartInfoList;
        });
        //对购物车列表时间进行排序
        CompletableFuture<List<CartInfo>> compareFuture = realTimePriceFuture.thenApplyAsync(acceptVal -> {
            return acceptVal.stream().sorted(Comparator.comparing(CartInfo::getCreateTime).reversed()).collect(Collectors.toList());
        });
        List<CartInfo> retCartInfoList = compareFuture.get();
        return retCartInfoList;
    }

    private void updateCartRealTimePrice(String userTempId, List<CartInfo> cartInfoList) {
        BoundHashOperations hashOption = getRedisOptionByUserId(userTempId);
        for (CartInfo cartInfo : cartInfoList) {
            //拿到skuId
            Long skuId = cartInfo.getSkuId();
            //通过远程拿到实时价格
            BigDecimal realTimePrice = productFeignClient.getSkuPrice(skuId);
            if (!cartInfo.getRealTimePrice().equals(realTimePrice)) {
                //更新价格 然后同步到redis里面
                cartInfo.setRealTimePrice(realTimePrice);
                hashOption.put(skuId.toString(), cartInfo);
            }
        }
    }


}
