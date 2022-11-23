package com.atguigu.collection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Product {
    public int id;
    public int num;
    public int price;
    public String name;
    public String category;


}
