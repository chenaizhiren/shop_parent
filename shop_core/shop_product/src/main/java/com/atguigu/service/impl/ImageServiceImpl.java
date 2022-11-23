package com.atguigu.service.impl;

import com.atguigu.entity.ProductImage;
import com.atguigu.mapper.ImageMapper;
import com.atguigu.service.ImageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 商品图片表 服务实现类
 * </p>
 *
 * @author zhengyuhao
 * @since 2022-11-01
 */
@Service
public class ImageServiceImpl extends ServiceImpl<ImageMapper, ProductImage> implements ImageService {

}
