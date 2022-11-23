package com.atguigu.controller;

import com.atguigu.result.RetVal;
import com.atguigu.search.Product;
import com.atguigu.search.SearchParam;
import com.atguigu.search.SearchResponseVo;
import com.atguigu.service.SearchService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/search")
public class SearchController {

    @Autowired
    private ElasticsearchRestTemplate esRestTemplate;
    @Autowired
    private SearchService searchService;
    //1.创建索引
    @GetMapping("createIndex")
    public String createIndex(){
        esRestTemplate.createIndex(Product.class);
        esRestTemplate.putMapping(Product.class);
        return "success";
    }
    //2.商品的上架
    @GetMapping("onSale/{skuId}")
    public String onSale(@PathVariable Long skuId){
        searchService.onSale(skuId);
        return "success";
    }
    //3.商品的上架
    @GetMapping("offSale/{skuId}")
    public String offSale(@PathVariable Long skuId){
        searchService.offSale(skuId);
        return "success";
    }

    //4.商品的搜索
    @PostMapping("searchProduct")
    public RetVal searchProduct(@RequestBody SearchParam searchParam){
        SearchResponseVo searchResponseVo=searchService.searchProduct(searchParam);
        return RetVal.ok(searchResponseVo);
    }

    //5.商品热度加1
    @GetMapping("incrHotScore/{skuId}")
    public String incrHotScore(@PathVariable Long skuId){
        searchService.incrHotScore(skuId);
        return "success";
    }

}
