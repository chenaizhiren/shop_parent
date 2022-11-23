package com.atguigu.controller;

import com.atguigu.result.RetVal;
import com.atguigu.utils.MinioUploader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/product")
public class MinioController {

    @PostMapping("minioUpload")
    public RetVal minioUpload(@RequestPart("avatar")MultipartFile avatar,
                              @RequestPart("life")MultipartFile life,
                              @RequestPart("secret")MultipartFile secret){
    return RetVal.ok();

    }
}
