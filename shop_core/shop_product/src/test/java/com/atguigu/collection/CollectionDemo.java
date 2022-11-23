package com.atguigu.collection;

import org.assertj.core.util.Lists;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CollectionDemo {
    public static void main(String[] args) {


        Product prod1 = new Product(1, 1, 15, "面包", "零食");
        Product prod2 = new Product(2, 2, 20, "饼干", "零食");
        Product prod3 = new Product(3, 3, 30, "月饼", "零食");
        Product prod4 = new Product(4, 3, 10, "青岛啤酒", "啤酒");
        Product prod5 = new Product(5, 10, 15, "百威啤酒", "啤酒");

        //把上面的对象转换成集合
        ArrayList<Product> productsList = Lists.newArrayList(prod1, prod2, prod3, prod4, prod5);
        //对集合进行迭代,并对商品进行分类
        Map<String, List<Product>> productMap = productsList.stream().collect(Collectors.groupingBy(Product::getCategory));

        Set<Map.Entry<String, List<Product>>> productSet = productMap.entrySet();
        for (Map.Entry<String, List<Product>> productEntry : productSet) {
            System.out.println(productEntry.getKey());
            System.out.println(productEntry.getValue());
        }



//        productSet.forEach(new Consumer<Map.Entry<String, List<Product>>>() {
//            @Override
//            public void accept(Map.Entry<String, List<Product>> productEntry) {
//                System.out.println(productEntry.getKey());
//                System.out.println(productEntry.getValue());
//            }
//        });


    }
}
