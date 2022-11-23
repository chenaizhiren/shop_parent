package com.atguigu.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//用在什么地方
@Target(ElementType.METHOD)
//定义方法的声明周期
@Retention(RetentionPolicy.RUNTIME)
public @interface ShopCache {

    ////定义一个前缀,用来区分哪个方法的缓存
    String prefix() default "cache";
}
