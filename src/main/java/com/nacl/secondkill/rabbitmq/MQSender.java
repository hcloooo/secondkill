package com.nacl.secondkill.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class MQSender {

    @Autowired
    private RabbitTemplate rabbitTemplate;

//    public void send(Object msg) {
//        log.info("发送消息：" + msg);
//        rabbitTemplate.convertAndSend("fanoutExchange", "", msg);
//    }
//
//    public void send01(Object msg) {
//        log.info("发送red消息：" + msg);
//        rabbitTemplate.convertAndSend("directExchange", "queue.red", msg);
//    }
//
//    public void send02(Object msg) {
//        log.info("发送green消息：" + msg);
//        rabbitTemplate.convertAndSend("directExchange", "queue.green", msg);
//    }
//
//    public void send03(Object msg) {
//        log.info("发送(QUEUE01接收):",msg);
//        rabbitTemplate.convertAndSend("topicExchange", "queue.red.message", msg);
//    }
//
//    public void send04(Object msg) {
//        log.info("发送(被两个QUEUE接收):",msg);
//        rabbitTemplate.convertAndSend("topicExchange", "message.queue.green", msg);
//    }
//
//    public void send05(Object msg) {
//        log.info("发送(两个队列接收):", msg);
//        MessageProperties properties = new MessageProperties();
//        properties.setHeader("color", "red");
//        properties.setHeader("speed", "fast");
//        Message message = new Message(((String)msg).getBytes(), properties);
//        rabbitTemplate.convertAndSend("headerExchange", "", message);
//    }
//
//    public void send06(Object msg) {
//        log.info("发送(QUEUE01接收):", msg);
//        MessageProperties properties = new MessageProperties();
//        properties.setHeader("color", "red");
//        properties.setHeader("speend", "normal");
//        Message message = new Message(((String)msg).getBytes(), properties);
//        rabbitTemplate.convertAndSend("headerExchange", "", message);
//    }

    //发送秒杀信息
    public void sendSeckillMessage(String message) {
        log.info("发送消息：" + message);
        rabbitTemplate.convertAndSend("seckillExchange", "seckill.message", message);
    }

}
