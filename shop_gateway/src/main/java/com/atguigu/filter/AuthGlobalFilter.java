package com.atguigu.filter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.result.RetVal;
import com.atguigu.result.RetValCodeEnum;
import com.atguigu.util.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class AuthGlobalFilter implements GlobalFilter {


    @Autowired
    private RedisTemplate redisTemplate;



    @Value("${filter.whiteList}")
    private  String filterWhiteList;

    //创建匹配对象
    private AntPathMatcher antPathMatcher = new AntPathMatcher();



    //拦截过滤方法
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        //获取请求request
        ServerHttpRequest request = exchange.getRequest();

        String path = request.getURI().getPath();


        if (antPathMatcher.match("/sku/**",path)){
            //这个path中不能访问/sku/getSkuInfo/{skuId}中的数据信息
            ServerHttpResponse response = exchange.getResponse();
            return  writeDataToBrowser(response,RetValCodeEnum.NO_PERMISSION);
        }

        //获取用户id信息
        String userId = getLoginUserId(request);

        //cookie被盗,表示没有权限
        if ("-1".equals(userId)){
            ServerHttpResponse response = exchange.getResponse();
            return writeDataToBrowser(response,RetValCodeEnum.NO_PERMISSION);
        }


        //如果要看订单相关信息是需要进行登录的
        if (antPathMatcher.match("/order/**",path)){
            //如果是未登录状态
            if (StringUtils.isEmpty(userId)){
                ServerHttpResponse response = exchange.getResponse();
                return writeDataToBrowser(response,RetValCodeEnum.NO_LOGIN);
            }
        }

        //访问拦截路径名单 对于这些请求路径需要进行拦截
        for (String filterWhite : filterWhiteList.split("")) {

            //用户访问的url中包含上述数据，并且用户未登录
            if (path.indexOf(filterWhite) != -1 && StringUtils.isEmpty(userId)){

                //设置完成之后重定向
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.SEE_OTHER);
                response.getHeaders().set(HttpHeaders.LOCATION,"http://www.gmall.com/login.html?originalUrl="+request.getURI());
                return response.setComplete();
            }
        }

        //如果说用户登录了呢，那么我们就需要将userId放到request中
        if (!StringUtils.isEmpty(userId)){
            if (!StringUtils.isEmpty(userId)){
                request.mutate().header("userId",userId).build();
            }
            return chain.filter(exchange.mutate().request(request).build());
        }

        return chain.filter(exchange);
    }



    //获取用户的id
    private String getLoginUserId(ServerHttpRequest request){

        String token = "";
        //header一般存储在移动端
        List<String> result = request.getHeaders().get("token");
        if (!StringUtils.isEmpty(request)){
            token = result.get(0);
        }else {
            //如果header中没有获取到token,那么就从cookie中获取
            HttpCookie cookie = request.getCookies().getFirst("token");
            if (cookie != null){
                token = cookie.getValue();
            }
        }

        if (!StringUtils.isEmpty(token)){
            //根据token获取用户登录信息
            String userKey = "user:login:" + token;
            String loginInfoJSON = (String) redisTemplate.opsForValue().get(userKey);

            JSONObject loginInfo = JSONObject.parseObject(loginInfoJSON);
            String loginIp = loginInfo.getString("loginIp");

            //获取到登录时的ip
            String currentIpAddress  = IpUtil.getGatwayIpAddress(request);
            //判断当前电脑登录ip是否与存储ip一致 如果一致取得userId,如果不一致返回"-1"
            if (currentIpAddress.equals(loginIp)){

                return loginInfo.getString("userId");
            }else {
                return "-1";
            }

        }
        return null;

    }




    //把数据写进浏览器中
    private Mono<Void>  writeDataToBrowser(ServerHttpResponse response, RetValCodeEnum retValCodeEnum){
        RetVal<Object> retVal = RetVal.build(null, retValCodeEnum);


        //把对象转换为字节
        byte[] bytes = JSONObject.toJSONString(retVal).getBytes(StandardCharsets.UTF_8);

        //将字节数组转换为数据buffer
        DataBuffer dateBuffer = response.bufferFactory().wrap(bytes);

        //设置页面的头部信息为返回JSON数据,编码为utf-8
        response.getHeaders().add("Content-Type","application/json;charset=UTF-8");

        return response.writeWith(Mono.just(dateBuffer));


    }
}
