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
        //a.?????????????????????
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        if (skuInfo != null) {
            product.setId(skuInfo.getId());
            product.setProductName(skuInfo.getSkuName());
            product.setCreateTime(new Date());
            product.setPrice(skuInfo.getPrice().doubleValue());
            product.setDefaultImage(skuInfo.getSkuDefaultImg());
            //b.????????????
            Long brandId = skuInfo.getBrandId();
            BaseBrand brand = productFeignClient.getBrandById(brandId);
            if (brand != null) {
                product.setBrandId(brandId);
                product.setBrandName(brand.getBrandName());
                product.setBrandLogoUrl(brand.getBrandLogoUrl());
            }
            //c.?????????????????????
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
            //d.???????????????????????????
            List<PlatformPropertyKey> platformPropertyList = productFeignClient.getPlatformPropertyBySkuId(skuId);
            if (!CollectionUtils.isEmpty(platformPropertyList)) {
                List<SearchPlatformProperty> searchPropertyList = platformPropertyList.stream().map(platformPropertyKey -> {
                    SearchPlatformProperty searchPlatformProperty = new SearchPlatformProperty();
                    //????????????Id
                    searchPlatformProperty.setPropertyKeyId(platformPropertyKey.getId());
                    //??????????????????
                    String propertyKey = platformPropertyKey.getPropertyKey();
                    searchPlatformProperty.setPropertyKey(propertyKey);
                    //???????????????
                    String propertyValue = platformPropertyKey.getPropertyValueList().get(0).getPropertyValue();
                    searchPlatformProperty.setPropertyValue(propertyValue);
                    return searchPlatformProperty;
                }).collect(Collectors.toList());
                product.setPlatformProperty(searchPropertyList);
            }
        }
        //e.???????????????es???
        productMapper.save(product);
    }

    @Override
    public void offSale(Long skuId) {
        //???es???????????????
        productMapper.deleteById(skuId);
    }


    @SneakyThrows
    @Override
    public SearchResponseVo searchProduct(SearchParam searchParam) {
        //1.??????DSL??????
        SearchRequest searchRequest = buildQueryDsl(searchParam);
        //2.?????????DSL???????????????
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        //3.?????????????????????
        SearchResponseVo searchResponseVo = parseSearchResult(searchResponse);
        //4.????????????????????????
        searchResponseVo.setPageNo(searchParam.getPageNo());
        searchResponseVo.setPageSize(searchParam.getPageSize());
        //??????????????????
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
        //1.???redis??????hotScore????????????
        String hotScoreKey="sku:hotscore";
        double count = redisTemplate.opsForZSet().incrementScore(hotScoreKey, skuId, 1);
        //2.???????????????????????? ?????????es??????
        if(count%2==0){
            Optional<Product> optional = productMapper.findById(skuId);
            Product esProduct = optional.get();
            esProduct.setHotScore(Math.round(count));
            productMapper.save(esProduct);
        }
    }

    //?????????????????????
    private SearchResponseVo parseSearchResult(SearchResponse searchResponse) {
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        //1.???????????????????????????
        SearchHits firstHits = searchResponse.getHits();
        //?????????????????????
        long totalHits = firstHits.totalHits;
        searchResponseVo.setTotal(totalHits);
        SearchHit[] secondHits = firstHits.getHits();
        //??????????????????????????? ???????????????????????????
        if (secondHits != null && secondHits.length > 0) {
            for (SearchHit secondHit : secondHits) {
                Product product = JSONObject.parseObject(secondHit.getSourceAsString(), Product.class);
                HighlightField highlightField = secondHit.getHighlightFields().get("productName");
                if (highlightField != null) {
                    Text fragment = highlightField.getFragments()[0];
                    product.setProductName(fragment.toString());
                }
                //????????????product?????????????????????vo?????????list??????
                searchResponseVo.getProductList().add(product);
            }
        }
        //2.??????????????????
        ParsedLongTerms brandIdAgg = searchResponse.getAggregations().get("brandIdAgg");
        List<SearchBrandVo> brandVoList = brandIdAgg.getBuckets().stream().map(bucket -> {
            SearchBrandVo searchBrandVo = new SearchBrandVo();
            //?????????id
            Number brandId = bucket.getKeyAsNumber();
            searchBrandVo.setBrandId(brandId.longValue());
            //???????????????
            ParsedStringTerms brandNameAgg = bucket.getAggregations().get("brandNameAgg");
            String brandName = brandNameAgg.getBuckets().get(0).getKeyAsString();
            searchBrandVo.setBrandName(brandName);
            //?????????????????????
            ParsedStringTerms brandLogoUrlAgg = bucket.getAggregations().get("brandLogoUrlAgg");
            String brandLogoUrl = brandLogoUrlAgg.getBuckets().get(0).getKeyAsString();
            searchBrandVo.setBrandLogoUrl(brandLogoUrl);
            return searchBrandVo;
        }).collect(Collectors.toList());
        searchResponseVo.setBrandVoList(brandVoList);
        //3.????????????????????????
        ParsedNested platformPropertyAgg = searchResponse.getAggregations().get("platformPropertyAgg");
        ParsedLongTerms propertyKeyIdAgg = platformPropertyAgg.getAggregations().get("propertyKeyIdAgg");
        List<SearchPlatformPropertyVo> platformPropertyList = propertyKeyIdAgg.getBuckets().stream().map(bucket -> {
            SearchPlatformPropertyVo searchPlatformPropertyVo = new SearchPlatformPropertyVo();
            //????????????Id
            Number propertyKeyId = bucket.getKeyAsNumber();
            searchPlatformPropertyVo.setPropertyKeyId(propertyKeyId.longValue());
            //????????????
            ParsedStringTerms propertyKeyAgg = bucket.getAggregations().get("propertyKeyAgg");
            String propertyKey = propertyKeyAgg.getBuckets().get(0).getKeyAsString();
            searchPlatformPropertyVo.setPropertyKey(propertyKey);
            //????????????????????????
            ParsedStringTerms propertyValueAgg = bucket.getAggregations().get("propertyValueAgg");
            List<String> propertyValueList =propertyValueAgg.getBuckets().stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
            searchPlatformPropertyVo.setPropertyValueList(propertyValueList);
            return searchPlatformPropertyVo;
        }).collect(Collectors.toList());
        searchResponseVo.setPlatformPropertyList(platformPropertyList);
        return searchResponseVo;
    }

    private SearchRequest buildQueryDsl(SearchParam searchParam) {
        //1.?????????????????????
        SearchSourceBuilder esSqlBuilder = new SearchSourceBuilder();
        //2.???????????????bool
        BoolQueryBuilder firstBool = QueryBuilders.boolQuery();
        //3.???????????????????????????
        if (!StringUtils.isEmpty(searchParam.getCategory1Id())) {
            TermQueryBuilder category1Id = QueryBuilders.termQuery("category1Id", searchParam.getCategory1Id());
            firstBool.filter(category1Id);
        }
        //3.???????????????????????????
        if (!StringUtils.isEmpty(searchParam.getCategory2Id())) {
            TermQueryBuilder category2Id = QueryBuilders.termQuery("category2Id", searchParam.getCategory2Id());
            firstBool.filter(category2Id);
        }
        //3.???????????????????????????
        if (!StringUtils.isEmpty(searchParam.getCategory3Id())) {
            TermQueryBuilder category3Id = QueryBuilders.termQuery("category3Id", searchParam.getCategory3Id());
            firstBool.filter(category3Id);
        }
        //4.???????????????????????????  brandName=1:??????
        String brandName = searchParam.getBrandName();
        if (!StringUtils.isEmpty(brandName)) {
            String[] brandParam = brandName.split(":");
            if (brandParam.length == 2) {
                firstBool.filter(QueryBuilders.termQuery("brandId", brandParam[0]));
            }
        }
        //5.????????????????????? keyword=??????
        String keyword = searchParam.getKeyword();
        if (!StringUtils.isEmpty(keyword)) {
            MatchQueryBuilder matchQuery = QueryBuilders.matchQuery("productName", keyword).operator(Operator.OR);
            firstBool.must(matchQuery);
        }
        //6.??????????????????????????? ????????????keyId:???????????????:?????????????????? props=4:??????A14:CPU??????&props=5:5.0???5.49??????:????????????
        String[] props = searchParam.getProps();
        if (props != null && props.length > 0) {
            for (String prop : props) {
                //prop-----> props=4:??????A14:CPU??????
                String[] platformParams = prop.split(":");
                if (platformParams.length == 3) {
                    //???????????????bool
                    BoolQueryBuilder secondBool = QueryBuilders.boolQuery();
                    secondBool.must(QueryBuilders.termQuery("platformProperty.propertyKeyId", platformParams[0]));
                    secondBool.must(QueryBuilders.termQuery("platformProperty.propertyValue", platformParams[1]));
                    //ScoreMode.None ????????????????????????
                    NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("platformProperty", secondBool, ScoreMode.None);
                    firstBool.filter(nestedQuery);
                }
            }
        }
        //???firstBool???????????????query?????????
        esSqlBuilder.query(firstBool);
        //7.??????????????????
        //????????????????????????????????????
        int from = (searchParam.getPageNo() - 1) * searchParam.getPageSize();
        esSqlBuilder.from(from);
        esSqlBuilder.size(searchParam.getPageSize());
        /**
         * 8.??????????????????
         *  ???????????? order=1:desc ????????????????????? hotScore
         *  ???????????? order=2:desc price
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
                //?????????????????????,???????????????
                esSqlBuilder.sort(fileName, "asc".equals(orderParam[1]) ? SortOrder.ASC : SortOrder.DESC);
            }
        }else{
            //???????????????????????????????????????
            esSqlBuilder.sort("hotScore", SortOrder.DESC);
        }
        //9.????????????
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("productName");
        highlightBuilder.preTags("<span style=color:red>");
        highlightBuilder.postTags("</span>");
        esSqlBuilder.highlighter(highlightBuilder);
        //10.??????????????????
        TermsAggregationBuilder brandIdAggBuilder = AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("brandLogoUrlAgg").field("brandLogoUrl"));
        esSqlBuilder.aggregation(brandIdAggBuilder);

        //11.????????????????????????(1+2+4?????????)
        esSqlBuilder.aggregation(AggregationBuilders.nested("platformPropertyAgg", "platformProperty")
                .subAggregation(AggregationBuilders.terms("propertyKeyIdAgg").field("platformProperty.propertyKeyId")
                        .subAggregation(AggregationBuilders.terms("propertyKeyAgg").field("platformProperty.propertyKey"))
                        .subAggregation(AggregationBuilders.terms("propertyValueAgg").field("platformProperty.propertyValue"))));
        //12.??????????????????????????????(request) ????????????index???type
        SearchRequest searchRequest = new SearchRequest("product2");
        searchRequest.types("info");
        searchRequest.source(esSqlBuilder);
        System.out.println("????????????DSL??????:" + esSqlBuilder.toString());
        return searchRequest;
    }




}

