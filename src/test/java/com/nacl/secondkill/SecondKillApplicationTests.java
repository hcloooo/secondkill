package com.nacl.secondkill;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class SecondKillApplicationTests {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedisScript<Boolean> script;

    @Test
    public void contextLoads() {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        //不存在才可以设置成功
        Boolean isLock = valueOperations.setIfAbsent("k1", "y1");
        //如果占位成功，进行正常操作
        if(isLock) {
            valueOperations.set("name", "xxxx");
            String name = (String) valueOperations.get("name");
            System.out.println("name = " + name);
            //操作结束，删除锁
            redisTemplate.delete("k1");
        } else {
            System.out.println("有线程使用，请稍后再试");
        }
    }

    @Test
    public void testLock02() {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        Boolean lock = valueOperations.setIfAbsent("k1", "y1", 10, TimeUnit.SECONDS);
        if(lock) {
            valueOperations.set("name", "xxxx");
            String name = (String) valueOperations.get("name");
            System.out.println("name = " + name);
            Integer.parseInt("XXXX");
            redisTemplate.delete("name");
        } else {
            System.out.println("有线程使用，请稍后再试");
        }
    }

    @Test
    public void testLock03() {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String value = UUID.randomUUID().toString();
        Boolean isLock = valueOperations.setIfAbsent("k1", value, 120, TimeUnit.SECONDS);
        if (isLock) {
            valueOperations.set("name", "xxxx");
            String name = (String) valueOperations.get("name");
            System.out.println("name = " + name);
            System.out.println("k1 = " + valueOperations.get("k1"));

            Boolean res = (Boolean) redisTemplate.execute(script, Collections.singletonList("k1"), value);
            System.out.println(res);
        } else {
            System.out.println("有线程在使用，请稍后");
        }

    }



}
