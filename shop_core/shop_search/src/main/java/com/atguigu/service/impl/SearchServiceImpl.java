package com.atguigu.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.dao.ProductMapper;
import com.atguigu.entity.*;
import com.atguigu.feign.ProductFeignClient;
import com.atguigu.search.*;
import com.atguigu.service.SearchService;
import lombok.SneakyThrows;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;

import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private RestHighLevelClient restHighLevelClient;
    @Autowired
    private RedisTemplate redisTemplate;
    @Override
    public void onSale(Long skuId) {
        Product product = new Product();
        //a.商品的基本信息
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        if (skuInfo != null) {
            product.setId(skuInfo.getId());
            product.setProductName(skuInfo.getSkuName());
            product.setCreateTime(new Date());
            product.setPrice(skuInfo.getPrice().doubleValue());
            product.setDefaultImage(skuInfo.getSkuDefaultImg());
            //b.品牌信息
            Long brandId = skuInfo.getBrandId();
            BaseBrand brand = productFeignClient.getBrandById(brandId);
            if (brand != null) {
                product.setBrandId(brandId);
                product.setBrandName(brand.getBrandName());
                product.setBrandLogoUrl(brand.getBrandLogoUrl());
            }
            //c.商品的分类信息
            Long category3Id = skuInfo.getCategory3Id();
            BaseCategoryView categoryView = productFeignClient.getCategoryView(category3Id);
            if (categoryView != null) {
                product.setCategory1Id(categoryView.getCategory1Id());
                product.setCategory1Name(categoryView.getCategory1Name());
                product.setCategory2Id(categoryView.getCategory2Id());
                product.setCategory2Name(categoryView.getCategory2Name());
                product.setCategory3Id(categoryView.getCategory3Id());
                product.setCategory3Name(categoryView.getCategory3Name());
            }
            //d.商品的平台属性信息
            List<PlatformPropertyKey> platformPropertyList = productFeignClient.getPlatformPropertyBySkuId(skuId);
            if (!CollectionUtils.isEmpty(platformPropertyList)) {
                List<SearchPlatformProperty> searchPropertyList = platformPropertyList.stream().map(platformPropertyKey -> {
                    SearchPlatformProperty searchPlatformProperty = new SearchPlatformProperty();
                    //平台属性Id
                    searchPlatformProperty.setPropertyKeyId(platformPropertyKey.getId());
                    //平台属性名称
                    String propertyKey = platformPropertyKey.getPropertyKey();
                    searchPlatformProperty.setPropertyKey(propertyKey);
                    //平台属性值
                    String propertyValue = platformPropertyKey.getPropertyValueList().get(0).getPropertyValue();
                    searchPlatformProperty.setPropertyValue(propertyValue);
                    return searchPlatformProperty;
                }).collect(Collectors.toList());
                product.setPlatformProperty(searchPropertyList);
            }
        }
        //e.存储数据到es中
        productMapper.save(product);
    }

    @Override
    public void offSale(Long skuId) {
        //从es中删除记录
        productMapper.deleteById(skuId);
    }


    @SneakyThrows
    @Override
    public SearchResponseVo searchProduct(SearchParam searchParam) {
        //1.生成DSL语句
        SearchRequest searchRequest = buildQueryDsl(searchParam);
        //2.实现对DSL语句的调用
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        //3.对结果进行分析
        SearchResponseVo searchResponseVo = parseSearchResult(searchResponse);
        //4.设置其他参数信息
        searchResponseVo.setPageNo(searchParam.getPageNo());
        searchResponseVo.setPageSize(searchParam.getPageSize());
        //设置总的页数
        boolean flag=searchResponseVo.getTotal()%searchParam.getPageSize()==0;
        long totalPages=0;
        if(flag){
            totalPages=searchResponseVo.getTotal()/searchParam.getPageSize();
        }else{
            totalPages=searchResponseVo.getTotal()/searchParam.getPageSize()+1;
        }
        return searchResponseVo;
    }

    @Override
    public void incrHotScore(Long skuId) {
        //1.在redis中对hotScore进行操作
        String hotScoreKey="sku:hotscore";
        double count = redisTemplate.opsForZSet().incrementScore(hotScoreKey, skuId, 1);
        //2.达到一定次数之后 同步到es当中
        if(count%2==0){
            Optional<Product> optional = productMapper.findById(skuId);
            Product esProduct = optional.get();
            esProduct.setHotScore(Math.round(count));
            productMapper.save(esProduct);
        }
    }

    //对结果进行解析
    private SearchResponseVo parseSearchResult(SearchResponse searchResponse) {
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        //1.商品的基本信息集合
        SearchHits firstHits = searchResponse.getHits();
        //拿到总的记录数
        long totalHits = firstHits.totalHits;
        searchResponseVo.setTotal(totalHits);
        SearchHit[] secondHits = firstHits.getHits();
        //这个地方如果不判断 可能要报空指针异常
        if (secondHits != null && secondHits.length > 0) {
            for (SearchHit secondHit : secondHits) {
                Product product = JSONObject.parseObject(secondHit.getSourceAsString(), Product.class);
                HighlightField highlightField = secondHit.getHighlightFields().get("productName");
                if (highlightField != null) {
                    Text fragment = highlightField.getFragments()[0];
                    product.setProductName(fragment.toString());
                }
                //把单个的product信息添加到返回vo对象的list里面
                searchResponseVo.getProductList().add(product);
            }
        }
        //2.品牌聚合信息
        ParsedLongTerms brandIdAgg = searchResponse.getAggregations().get("brandIdAgg");
        List<SearchBrandVo> brandVoList = brandIdAgg.getBuckets().stream().map(bucket -> {
            SearchBrandVo searchBrandVo = new SearchBrandVo();
            //品牌的id
            Number brandId = bucket.getKeyAsNumber();
            searchBrandVo.setBrandId(brandId.longValue());
            //品牌的名称
            ParsedStringTerms brandNameAgg = bucket.getAggregations().get("brandNameAgg");
            String brandName = brandNameAgg.getBuckets().get(0).getKeyAsString();
            searchBrandVo.setBrandName(brandName);
            //品牌的图片地址
            ParsedStringTerms brandLogoUrlAgg = bucket.getAggregations().get("brandLogoUrlAgg");
            String brandLogoUrl = brandLogoUrlAgg.getBuckets().get(0).getKeyAsString();
            searchBrandVo.setBrandLogoUrl(brandLogoUrl);
            return searchBrandVo;
        }).collect(Collectors.toList());
        searchResponseVo.setBrandVoList(brandVoList);
        //3.平台属性聚合信息
        ParsedNested platformPropertyAgg = searchResponse.getAggregations().get("platformPropertyAgg");
        ParsedLongTerms propertyKeyIdAgg = platformPropertyAgg.getAggregations().get("propertyKeyIdAgg");
        List<SearchPlatformPropertyVo> platformPropertyList = propertyKeyIdAgg.getBuckets().stream().map(bucket -> {
            SearchPlatformPropertyVo searchPlatformPropertyVo = new SearchPlatformPropertyVo();
            //平台属性Id
            Number propertyKeyId = bucket.getKeyAsNumber();
            searchPlatformPropertyVo.setPropertyKeyId(propertyKeyId.longValue());
            //属性名称
            ParsedStringTerms propertyKeyAgg = bucket.getAggregations().get("propertyKeyAgg");
            String propertyKey = propertyKeyAgg.getBuckets().get(0).getKeyAsString();
            searchPlatformPropertyVo.setPropertyKey(propertyKey);
            //当前属性值的集合
            ParsedStringTerms propertyValueAgg = bucket.getAggregations().get("propertyValueAgg");
            List<String> propertyValueList =propertyValueAgg.getBuckets().stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
            searchPlatformPropertyVo.setPropertyValueList(propertyValueList);
            return searchPlatformPropertyVo;
        }).collect(Collectors.toList());
        searchResponseVo.setPlatformPropertyList(platformPropertyList);
        return searchResponseVo;
    }

    private SearchRequest buildQueryDsl(SearchParam searchParam) {
        //1.构造一个大括号
        SearchSourceBuilder esSqlBuilder = new SearchSourceBuilder();
        //2.构造第一个bool
        BoolQueryBuilder firstBool = QueryBuilders.boolQuery();
        //3.构造一级分类过滤器
        if (!StringUtils.isEmpty(searchParam.getCategory1Id())) {
            TermQueryBuilder category1Id = QueryBuilders.termQuery("category1Id", searchParam.getCategory1Id());
            firstBool.filter(category1Id);
        }
        //3.构造二级分类过滤器
        if (!StringUtils.isEmpty(searchParam.getCategory2Id())) {
            TermQueryBuilder category2Id = QueryBuilders.termQuery("category2Id", searchParam.getCategory2Id());
            firstBool.filter(category2Id);
        }
        //3.构造一级分类过滤器
        if (!StringUtils.isEmpty(searchParam.getCategory3Id())) {
            TermQueryBuilder category3Id = QueryBuilders.termQuery("category3Id", searchParam.getCategory3Id());
            firstBool.filter(category3Id);
        }
        //4.构造一个品牌过滤器  brandName=1:苹果
        String brandName = searchParam.getBrandName();
        if (!StringUtils.isEmpty(brandName)) {
            String[] brandParam = brandName.split(":");
            if (brandParam.length == 2) {
                firstBool.filter(QueryBuilders.termQuery("brandId", brandParam[0]));
            }
        }
        //5.构造关键字查询 keyword=手机
        String keyword = searchParam.getKeyword();
        if (!StringUtils.isEmpty(keyword)) {
            MatchQueryBuilder matchQuery = QueryBuilders.matchQuery("productName", keyword).operator(Operator.OR);
            firstBool.must(matchQuery);
        }
        //6.构造平台属性过滤器 平台属性keyId:平台属性值:平台属性名称 props=4:苹果A14:CPU型号&props=5:5.0～5.49英寸:屏幕尺寸
        String[] props = searchParam.getProps();
        if (props != null && props.length > 0) {
            for (String prop : props) {
                //prop-----> props=4:苹果A14:CPU型号
                String[] platformParams = prop.split(":");
                if (platformParams.length == 3) {
                    //构造第二个bool
                    BoolQueryBuilder secondBool = QueryBuilders.boolQuery();
                    secondBool.must(QueryBuilders.termQuery("platformProperty.propertyKeyId", platformParams[0]));
                    secondBool.must(QueryBuilders.termQuery("platformProperty.propertyValue", platformParams[1]));
                    //ScoreMode.None 就是不做评分机制
                    NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("platformProperty", secondBool, ScoreMode.None);
                    firstBool.filter(nestedQuery);
                }
            }
        }
        //把firstBool放到我们的query里面去
        esSqlBuilder.query(firstBool);
        //7.构造分页信息
        //分页的第几条记录怎么计算
        int from = (searchParam.getPageNo() - 1) * searchParam.getPageSize();
        esSqlBuilder.from(from);
        esSqlBuilder.size(searchParam.getPageSize());
        /**
         * 8.商品搜索排序
         *  综合排序 order=1:desc 点击商品的次数 hotScore
         *  价格排序 order=2:desc price
         */
        String uiOrder = searchParam.getOrder();
        if (!StringUtils.isEmpty(uiOrder)) {
            String[] orderParam = uiOrder.split(":");
            if (orderParam.length == 2) {
                String fileName = "";
                String param = orderParam[0];
                switch (param) {
                    case "1":
                        fileName = "hotScore";
                        break;
                    case "2":
                        fileName = "price";
                        break;
                }
                //排序字段的名称,排序的方式
                esSqlBuilder.sort(fileName, "asc".equals(orderParam[1]) ? SortOrder.ASC : SortOrder.DESC);
            }
        }else{
            //如果前端没有给一个排序规则
            esSqlBuilder.sort("hotScore", SortOrder.DESC);
        }
        //9.构造高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("productName");
        highlightBuilder.preTags("<span style=color:red>");
        highlightBuilder.postTags("</span>");
        esSqlBuilder.highlighter(highlightBuilder);
        //10.构造品牌聚合
        TermsAggregationBuilder brandIdAggBuilder = AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("brandLogoUrlAgg").field("brandLogoUrl"));
        esSqlBuilder.aggregation(brandIdAggBuilder);

        //11.构造平台属性聚合(1+2+4构造法)
        esSqlBuilder.aggregation(AggregationBuilders.nested("platformPropertyAgg", "platformProperty")
                .subAggregation(AggregationBuilders.terms("propertyKeyIdAgg").field("platformProperty.propertyKeyId")
                        .subAggregation(AggregationBuilders.terms("propertyKeyAgg").field("platformProperty.propertyKey"))
                        .subAggregation(AggregationBuilders.terms("propertyValueAgg").field("platformProperty.propertyValue"))));
        //12.需要一个发起搜索请求(request) 查询哪个index和type
        SearchRequest searchRequest = new SearchRequest("product2");
        searchRequest.types("info");
        searchRequest.source(esSqlBuilder);
        System.out.println("拼接好的DSL语句:" + esSqlBuilder.toString());
        return searchRequest;
    }




}

