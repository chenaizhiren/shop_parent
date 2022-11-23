package com.atguigu.controller;


import com.atguigu.feign.ProductFeignClient;
import com.atguigu.feign.SearchFeignClient;
import com.atguigu.result.RetVal;
import com.atguigu.search.SearchParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class WebIndexController {
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private SearchFeignClient searchFeignClient;

    @RequestMapping({"/", "index.html"})
    public String index(Model model) {
        //通过远程RPC调用shop-product服务 拿到商品的分类信息
        RetVal retVal = productFeignClient.getIndexCategoryInfo();
        model.addAttribute("list", retVal.getData());
        return "index/index";
    }

    @RequestMapping("search.html")
    public String searchIndex(SearchParam searchParam, Model model) {
        RetVal<Map> retVal = searchFeignClient.searchProduct(searchParam);
        model.addAllAttributes(retVal.getData());
        //提前预支代码---还没有写
        //1.浏览器路径上面的参数进行拼接
        String brandName = searchParam.getBrandName();
        String urlParam = browserPageUrlParam(searchParam);
        model.addAttribute("urlParam", urlParam);
        //2.页面品牌信息进行回显
        String brandNameParam=pageBrandParam(brandName);
        model.addAttribute("brandNameParam", brandNameParam);
        //3.平台属性信息进行回显
        List<Map<String,String>> propMapList=pagePlatformParam(searchParam.getProps());
        model.addAttribute("propsParamList", propMapList);
        //4.浏览器排序参数的拼接
        Map<String,Object> orderMap=pageSortParam(searchParam.getOrder());
        model.addAttribute("orderMap", orderMap);
        return "search/index";
    }

    //&order=2:asc
    private Map<String, Object> pageSortParam(String order) {
        Map<String, Object> orderMap=new HashMap<>();
        if(!StringUtils.isEmpty(order)){
            String[] orderSplit = order.split(":");
            if(orderSplit.length==2){
                orderMap.put("type",orderSplit[0]);
                orderMap.put("sort",orderSplit[1]);
            }
        }else{
            //默认给一个综合排序
            orderMap.put("type",1);
            orderMap.put("sort","desc");
        }
        return orderMap;
    }

    //&props=4:苹果A14:CPU型号&props=5:6.0～6.24英寸:屏幕尺寸
    private List<Map<String, String>> pagePlatformParam(String[] props) {
        List<Map<String, String>> propMapList=new ArrayList<>();
        if(props!=null&&props.length>0){
            for (String prop : props) {
                //props=4:苹果A14:CPU型号
                String[] propSplit = prop.split(":");
                if(propSplit.length==3){
                    Map<String, String> propMap = new HashMap<>();
                    propMap.put("propertyKeyId",propSplit[0]);
                    propMap.put("propertyKey",propSplit[2]);
                    propMap.put("propertyValue",propSplit[1]);
                    propMapList.add(propMap);
                }
            }
        }
        return propMapList;
    }

    //brandName=1:苹果
    private String pageBrandParam(String brandName) {
        //判断是否有关键字
        if(!StringUtils.isEmpty(brandName)){
            String[] brandSplit = brandName.split(":");
            if(brandSplit.length==2){
                return "品牌:"+brandSplit[1];
            }
        }
        return null;
    }

    //search.html?keyword=苹果三星&brandName=1:苹果&props=4:苹果A14:CPU型号&props=5:6.0～6.24英寸:屏幕尺寸
    private String browserPageUrlParam(SearchParam searchParam) {
        StringBuilder urlParam = new StringBuilder();
        //判断是否有关键字
        if(!StringUtils.isEmpty(searchParam.getKeyword())){
            urlParam.append("keyword=").append(searchParam.getKeyword());
        }
        //判断是否有品牌
        if(!StringUtils.isEmpty(searchParam.getBrandName())){
            if(urlParam.length()>0){
                urlParam.append("&brandName=").append(searchParam.getBrandName());
            }
        }
        //判断是否有平台属性参数
        if(!StringUtils.isEmpty(searchParam.getProps())){
            if(urlParam.length()>0){
                for (String prop : searchParam.getProps()) {
                    urlParam.append("&props="+prop);
                }

            }
        }
        return "search.html?" + urlParam.toString();
    }
}

