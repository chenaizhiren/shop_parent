package com.atguigu.utils;

import io.minio.MinioClient;
import io.minio.PutObjectOptions;
import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.InputStream;
import java.util.UUID;

//开启配置类属性
@EnableConfigurationProperties(MinioProperties.class)
@Component
public class MinioUploader {

    @Resource
    private MinioProperties minioProperties;

    @Autowired
    private MinioClient minioClient;


    @Bean
    public MinioClient minioClient() throws Exception {
        MinioClient minioClient = new MinioClient(minioProperties.getEndpoint(), minioProperties.getAccessKey(), minioProperties.getSecretKey());

        boolean isExist = minioClient.bucketExists(minioProperties.getBucketName());

        if (isExist){
            System.out.println("Bucket already exists!");
        }else {
            minioClient.makeBucket(minioProperties.getBucketName());
        }

        return minioClient;

    }


    public String uploadFile(MultipartFile file) throws Exception{
        String fileName = UUID.randomUUID().toString() + file.getOriginalFilename();

        InputStream inputStream = file.getInputStream();
        PutObjectOptions options = new PutObjectOptions(inputStream.available(), -1);
        options.setContentType(file.getContentType());
        minioClient.putObject(minioProperties.getBucketName(),fileName,inputStream,options);
        String retUrl = minioProperties.getEndpoint() + "/" + minioProperties.getBucketName() + "/" + fileName;
        return retUrl;
    }



}
