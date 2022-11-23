package com.atguigu.feign;

import com.atguigu.result.RetVal;
import com.atguigu.search.SearchParam;
import com.atguigu.search.SearchResponseVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "shop-search")
public interface SearchFeignClient {



    //2.商品的上架
    @GetMapping("/search/onSale/{skuId}")
    public String onSale(@PathVariable Long skuId);
    //3.商品的上架
    @GetMapping("/search/offSale/{skuId}")
    public String offSale(@PathVariable Long skuId);
    //4.商品的搜索
    @PostMapping("/search/searchProduct")
    public RetVal searchProduct(SearchParam searchParam);
    //5.商品热度加1
    @GetMapping("/search/incrHotScore/{skuId}")
    public String incrHotScore(@PathVariable Long skuId);
}
