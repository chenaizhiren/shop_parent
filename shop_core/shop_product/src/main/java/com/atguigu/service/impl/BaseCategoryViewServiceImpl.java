package com.atguigu.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.entity.BaseCategoryView;
import com.atguigu.mapper.BaseCategoryViewMapper;
import com.atguigu.service.BaseCategoryViewService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;



import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Map;

import java.util.concurrent.atomic.AtomicInteger;

import java.util.stream.Collectors;

/**
 * <p>
 * VIEW 服务实现类
 * </p>
 *
 * @author zhengyuhao
 * @since 2022-11-02
 */
@Service
public class BaseCategoryViewServiceImpl extends ServiceImpl<BaseCategoryViewMapper, BaseCategoryView> implements BaseCategoryViewService {



    @Override
    public BaseCategoryView getCategoryViewByCategory3Id(Long category3Id) {
        return baseMapper.selectById(category3Id);
    }

    @Override
    public List<JSONObject> getIndexCategoryInfo()  {
            //a.查询所有的商品分类信息
            List<BaseCategoryView> allCategoryView = baseMapper.selectList(null);
//        for (Map.Entry<Long, List<BaseCategoryView>> category1Entry : category1Map.entrySet()) {
//        }
            //b.找到所有的一级分类
            AtomicInteger index= new AtomicInteger(1);
            Map<Long, List<BaseCategoryView>> category1Map = allCategoryView.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
            List<JSONObject> categoryList = category1Map.entrySet().stream().map(category1Entry -> {
                Long category1Id = category1Entry.getKey();
                List<BaseCategoryView> category1List = category1Entry.getValue();
                //创建一个Json数据
                JSONObject category1Json = new JSONObject();

                    category1Json.put("index", index.getAndIncrement());
                    category1Json.put("categoryId", category1Id);
                    category1Json.put("categoryName", category1List.get(0).getCategory1Name());


                //c.找到所有的二级分类
                Map<Long, List<BaseCategoryView>> category2Map = category1List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
                List<JSONObject> category1Children = category2Map.entrySet().stream().map(category2Entry -> {
                    Long category2Id = category2Entry.getKey();
                    List<BaseCategoryView> category2List = category2Entry.getValue();
                    //创建一个Json数据
                    JSONObject category2Json = new JSONObject();

                        category2Json.put("categoryId", category2Id);
                        category2Json.put("categoryName", category2List.get(0).getCategory2Name());

                    //d.找到所有的三级分类
                    Map<Long, List<BaseCategoryView>> category3Map = category2List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory3Id));
                    List<JSONObject> category2Children = category3Map.entrySet().stream().map(category3Entry -> {
                        Long category3Id = category3Entry.getKey();
                        List<BaseCategoryView> category3List = category3Entry.getValue();
                        //创建一个Json数据
                        JSONObject category3Json = new JSONObject();

                            category3Json.put("categoryId", category3Id);
                            category3Json.put("categoryName", category3List.get(0).getCategory3Name());

                        return category3Json;
                    }).collect(Collectors.toList());
                        category2Json.put("categoryChild", category2Children);

                    return category2Json;
                }).collect(Collectors.toList());
                    category1Json.put("categoryChild", category1Children);

                return category1Json;
            }).collect(Collectors.toList());
            return categoryList;
        }
}
