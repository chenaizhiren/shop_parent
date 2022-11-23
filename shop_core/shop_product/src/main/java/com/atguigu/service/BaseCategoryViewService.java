package com.atguigu.service;

import com.atguigu.entity.BaseCategoryView;
import com.baomidou.mybatisplus.extension.service.IService;
import com.alibaba.fastjson.JSONObject;

import java.util.List;

/**
 * <p>
 * VIEW 服务类
 * </p>
 *
 * @author zhengyuhao
 * @since 2022-11-02
 */
public interface BaseCategoryViewService extends IService<BaseCategoryView> {

    BaseCategoryView getCategoryViewByCategory3Id(Long category3Id);

    List<JSONObject> getIndexCategoryInfo() ;
}
