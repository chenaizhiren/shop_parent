package com.atguigu.service.impl;

import com.atguigu.entity.UserInfo;
import com.atguigu.mapper.UserInfoMapper;
import com.atguigu.service.UserInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author zhengyuhao
 * @since 2022-11-13
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Override
    public UserInfo queryUserFromDb(UserInfo userInfo) {

        //根据用户账号密码查询用户信息
        QueryWrapper<UserInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("login_name",userInfo.getLoginName());

        //此时密码由页面传过来,是明文
        String passwd = userInfo.getPasswd();

        //将明文用MD5加密
        String encodePasswd = DigestUtils.md5DigestAsHex(passwd.getBytes());
        wrapper.eq("passwd",encodePasswd);
        return baseMapper.selectOne(wrapper);
    }
}
