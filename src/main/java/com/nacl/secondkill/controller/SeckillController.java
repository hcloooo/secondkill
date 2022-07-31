package com.nacl.secondkill.controller;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nacl.secondkill.config.AccessLimit;
import com.nacl.secondkill.entity.Order;
import com.nacl.secondkill.entity.SeckillMessage;
import com.nacl.secondkill.entity.SeckillOrder;
import com.nacl.secondkill.entity.User;
import com.nacl.secondkill.exception.GlobalException;
import com.nacl.secondkill.rabbitmq.MQSender;
import com.nacl.secondkill.service.IGoodsService;
import com.nacl.secondkill.service.IOrderService;
import com.nacl.secondkill.service.ISeckillOrderService;
import com.nacl.secondkill.service.impl.SeckillOrderServiceImpl;
import com.nacl.secondkill.vo.GoodsVo;
import com.nacl.secondkill.vo.RespBean;
import com.nacl.secondkill.vo.RespBeanEnum;
import com.wf.captcha.ArithmeticCaptcha;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/seckill")
public class SeckillController implements InitializingBean {

    @Autowired
    private IGoodsService goodsService;
    @Autowired
    private ISeckillOrderService seckillOrderService;
    @Autowired
    private IOrderService orderService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private MQSender mqSender;
    @Autowired
    private RedisScript<Long> script;

    private Map<Long, Boolean> emptyMap = new HashMap<>();

    /*
    windows优化前QPS 785.9
            缓存后QPS 1350
            优化后QPS 2450
    linux又花前QPS 170
     */
    @RequestMapping("/doSeckill2")
    public String doSeckill2(Model model, User user, Long goodsId) {
        System.out.println("doSeckill2");
        if(user == null) {
            return "login";
        }
        model.addAttribute("user", user);
        GoodsVo goodsVo = goodsService.getGoodsVoById(goodsId);
        model.addAttribute("goods", goodsVo);
        //判断库存
        if(goodsVo.getStockCount() <= 0) {
            model.addAttribute("errmsg", RespBeanEnum.EMPTY_STOCK.getMessage());
            return "secKillFail";
        }
        //判断当前用户是否已经秒杀过
        QueryWrapper<SeckillOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", user.getId());
        wrapper.eq("goods_id", goodsId);
        SeckillOrder seckillOrder = seckillOrderService.getOne(wrapper);
        if(seckillOrder != null) {
            model.addAttribute("errmsg", RespBeanEnum.HAS_SECKILL.getMessage());
            return "secKillFail";
        }
        //下订单
        Order order = orderService.seckill(user,goodsVo);
        model.addAttribute("order", order);
        return "orderDetail";
    }

    @RequestMapping("/{path}/doSeckill")
    @ResponseBody
    public RespBean doSeckill(@PathVariable String path, User user, Long goodsId) {
        if(user == null) {
            return RespBean.error(RespBeanEnum.USER_TIME_OUT);
        }

        ValueOperations valueOperations = redisTemplate.opsForValue();
        Boolean check = orderService.checkPath(user, path, goodsId);
        if(!check) {
            return RespBean.error(RespBeanEnum.PATH_ERROR);
        }
        //判断当前用户是否已经秒杀过
        SeckillOrder seckillOrder = (SeckillOrder)valueOperations.get("order:"+user.getId()+":"+goodsId);
        if(seckillOrder != null) {
            return RespBean.error(RespBeanEnum.HAS_SECKILL);
        }

        if(emptyMap.get(goodsId)) {
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }
        //redis预减库存
        //Long stock = valueOperations.decrement("seckillGoods:" + goodsId);
        //使用lua脚本
        Long stock = (Long) redisTemplate.execute(script, Collections.singletonList("seckillGoods:" + goodsId), Collections.EMPTY_LIST);
        if(stock == 0) {
            emptyMap.put(goodsId, true);
            //valueOperations.increment("seckillGoods:" + goodsId);
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }
        //下订单
        SeckillMessage seckillMessage = new SeckillMessage(user, goodsId);
        mqSender.sendSeckillMessage(JSON.toJSONString(seckillMessage));
        return RespBean.success(0);
    }

    //加载完毕后执行，将商品库存数量加载到redis
    @Override
    public void afterPropertiesSet() throws Exception {
        List<GoodsVo> goodsVos = goodsService.getGoodsVo();
        if(goodsVos.size() == 0) {
            return ;
        }
        ValueOperations valueOperations = redisTemplate.opsForValue();
        for(GoodsVo goodsVo : goodsVos) {
            valueOperations.set("seckillGoods:" + goodsVo.getId(), goodsVo.getStockCount());
            emptyMap.put(goodsVo.getId(), false);
        }
    }

    //前端异步轮询查询后端结果
    //查询用户秒杀的结果
    @RequestMapping("/result")
    @ResponseBody
    public RespBean getResult(User user, String goodsId) {
        if(user == null) {
            return RespBean.error(RespBeanEnum.USER_TIME_OUT);
        }
        String key = "order:" + user.getId() + ":" + goodsId;
        ValueOperations valueOperations = redisTemplate.opsForValue();
        //有可能还没下单就开始查询了
        Long orderId = 0l;
       /* if(valueOperations.get(key) != null) {
            SeckillOrder seckillOrder = (SeckillOrder) valueOperations.get(key);
            orderId = seckillOrder.getOrderId();
        } else {
             orderId = seckillOrderService.inSeckillOrder(user.getId(), goodsId);
        }*/

        if(valueOperations.get(key) != null) {
            orderId = seckillOrderService.inSeckillOrder(user.getId(), goodsId);
        }
        return RespBean.success(orderId);
    }

    //获取秒杀的接口
    @RequestMapping("/path")
    @ResponseBody
    @AccessLimit(second=5,maxCount=5,needLogin=true)
    public RespBean getPath(User user, Long goodsId, String captcha, HttpServletRequest request) {
        if(user == null) {
            return RespBean.error(RespBeanEnum.USER_TIME_OUT);
        }

        //判断验证码是否正确
        Boolean check = orderService.checkCptcha(user, goodsId, captcha);
        if(!check) {
            return RespBean.error(RespBeanEnum.ERROR_CAPTCHA);
        }

        //生成秒杀的接口
        String str = orderService.createPath(user, goodsId);
        return RespBean.success(str);
    }

    //生成验证码
    @RequestMapping("/captcha")
    public void vertifyCode(User user, Long goodsId, HttpServletResponse response) {
        if(user == null || goodsId < 0) {
            throw new GlobalException(RespBeanEnum.ERROR);
        }

        //设置请求头为输出图片数据
        response.setContentType("image/jpg");
        response.setHeader("Pargam","No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        //生成验证码，将结果放入redis中
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(130, 32, 3);
        redisTemplate.opsForValue().set("captcha:" + user.getId() + ":" + goodsId, captcha.text(), 300, TimeUnit.SECONDS);
        try {
            captcha.out(response.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
