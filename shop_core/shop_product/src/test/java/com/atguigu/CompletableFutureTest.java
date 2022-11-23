package com.atguigu;



import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class CompletableFutureTest {

    public static void main(String[] args) throws Exception {
        //runAsync();

        supplyAsync();

    }

    public static void runAsync(){
        //runAsync没有返回值
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                System.out.println("你好runAsync!");
               int i = 10 / 0;
            }
        }).whenComplete(new BiConsumer<Void, Throwable>() {
            @Override
            public void accept(Void acceptVal, Throwable throwable) {
                System.out.println("接收到的值:" + acceptVal);
                System.out.println("whenComplete接收到发生的异常:" + throwable);
            }
        }).exceptionally(new Function<Throwable, Void>() {
            @Override
            //发生异常的时候执行
            public Void apply(Throwable throwable) {
                System.out.println("exceptionally接收到发生的异常: " + throwable);
                return null;
            }
        });
    }


    //supplyAsync有返回值
    public static void  supplyAsync() throws Exception{
        CompletableFuture<Object> supplyFuture = CompletableFuture.supplyAsync(new Supplier<Object>() {
            @Override
            public Object get() {
                System.out.println("你好supplyAsync!!");
              //  int i = 10 / 0;
                return 1024;
            }
        }).whenComplete(new BiConsumer<Object, Throwable>() {
            @Override
            public void accept(Object o, Throwable throwable) {
                System.out.println("接收到的值:" + o);
                System.out.println("whenComplete接收到发生的异常:" + throwable);
            }
        }).exceptionally(new Function<Throwable, Object>() {
            @Override
            public Object apply(Throwable throwable) {
                System.out.println("exceptionally接收到发生异常的值: " + throwable);
                return "发生异常返回的值888888";
            }
        });


        System.out.println(supplyFuture.get());
    }




}
