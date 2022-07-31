package com.nacl.secondkill.controller;


import com.nacl.secondkill.entity.User;
import com.nacl.secondkill.rabbitmq.MQSender;
import com.nacl.secondkill.vo.RespBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author hexiangdong
 * @since 2022-03-07
 */
@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    MQSender mqSender;

    @RequestMapping("/info")
    @ResponseBody
    public RespBean info(User user) {
        return RespBean.success(user);
    }


//    //测试发送消息
//    @RequestMapping("/mq")
//    @ResponseBody
//    public void mq() {
//        mqSender.send("hello");
//    }

//    @RequestMapping("/mq/fanout")
//    @ResponseBody
//    public void mq01() {
//        mqSender.send("hello");
//    }
//
//    @RequestMapping("/mq/direct01")
//    @ResponseBody
//    public void mq02() {
//        mqSender.send01("Hello Red");
//    }
//
//    @RequestMapping("/mq/direct02")
//    @ResponseBody
//    public void mq03() {
//        mqSender.send02("Hello Green");
//    }
//
//    //topic模式
//    @RequestMapping("/mq/topic01")
//    @ResponseBody
//    public void mq04() {
//        mqSender.send03("Hello");
//    }
//
//    @RequestMapping("/mq/topic02")
//    @ResponseBody
//    public void mq05() {
//        mqSender.send04("Hello");
//    }
//
//    @RequestMapping("/mq/header01")
//    @ResponseBody
//    public void mq06() {
//        mqSender.send05("hello");
//    }
//
//    @RequestMapping("/mq/header02")
//    @ResponseBody
//    public void mq07() {
//        mqSender.send06("hello");
//    }
}
