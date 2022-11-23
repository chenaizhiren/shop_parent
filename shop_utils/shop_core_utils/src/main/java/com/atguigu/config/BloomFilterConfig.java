package com.atguigu.config;

import com.atguigu.constant.RedisConst;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BloomFilterConfig {

    @Autowired
    private RedissonClient redissonClient;


    @Bean
    public RBloomFilter SkuBloomFilter(){
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(RedisConst.BLOOM_SKU_ID);


        bloomFilter.tryInit(10000,0.001);
        return bloomFilter;
    }

}
