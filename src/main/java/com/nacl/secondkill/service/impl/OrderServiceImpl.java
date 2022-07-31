package com.nacl.secondkill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nacl.secondkill.entity.*;
import com.nacl.secondkill.mapper.OrderMapper;
import com.nacl.secondkill.service.IGoodsService;
import com.nacl.secondkill.service.IOrderService;
import com.nacl.secondkill.service.ISeckillGoodsService;
import com.nacl.secondkill.service.ISeckillOrderService;
import com.nacl.secondkill.utils.MD5Util;
import com.nacl.secondkill.utils.UUIDUtil;
import com.nacl.secondkill.vo.GoodsVo;
import com.nacl.secondkill.vo.OrderDetailVo;
import com.nacl.secondkill.entity.Order;
import com.nacl.secondkill.entity.SeckillGoods;
import com.nacl.secondkill.entity.SeckillOrder;
import com.nacl.secondkill.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author NaCl
 * @since 2022-03-07
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    @Autowired
    private ISeckillOrderService seckillOrderService;
    @Autowired
    private ISeckillGoodsService seckillGoodsService;
    @Autowired
    private IOrderService orderService;
    @Autowired
    private IGoodsService goodsService;
    @Autowired
    private RedisTemplate redisTemplate;

    @Transactional
    @Override
    public Order seckill(User user, GoodsVo goodsVo) {
        //减秒杀商品的库存
        QueryWrapper<SeckillGoods> wrapper1 = new QueryWrapper<>();
        wrapper1.eq("goods_id", goodsVo.getId());
        SeckillGoods seckillGoods = seckillGoodsService.getOne(wrapper1);

        //判断库存是否大于0
        UpdateWrapper<SeckillGoods> wrapper2 = new UpdateWrapper<>();
        wrapper2.setSql("stock_count="+"stock_count-1");
        wrapper2.eq("id", seckillGoods.getId());
        wrapper2.gt("stock_count", 0);
        boolean secKillres = seckillGoodsService.update(wrapper2);
        if(!secKillres) {
            return null;
        }
        //生成订单
        Order order = new Order();
        order.setCreateDate(new Date());
        order.setGoodsCount(1);
        order.setGoodsName(goodsVo.getGoodsName());
        order.setGoodsId(goodsVo.getId());
        order.setGoodsPrice(goodsVo.getSeckillPrice());
        order.setUserId(user.getId());
        order.setDeliveryAddrId(0l);
        order.setOrderChannel(1);
        order.setStatus(0);
        orderService.save(order);

        //生成秒杀订单
        SeckillOrder seckillOrder = new SeckillOrder();
        seckillOrder.setOrderId(order.getId());
        seckillOrder.setGoodsId(goodsVo.getId());
        seckillOrder.setUserId(user.getId());
        seckillOrderService.save(seckillOrder);
        redisTemplate.opsForValue().set("order:" + user.getId() + ":" + goodsVo.getId(), seckillOrder);
        return order;
    }

    @Override
    public OrderDetailVo getDetail(Long orderId) {
        Order order = baseMapper.selectById(orderId);
        Long goodsId = order.getGoodsId();
        GoodsVo goodsVo = goodsService.getGoodsVoById(goodsId);
        OrderDetailVo res = new OrderDetailVo();
        res.setOrder(order);
        res.setGoodsVo(goodsVo);
        return res;
    }

    //获取秒杀地址
    @Override
    public String createPath(User user, Long goodsId) {
        String str = MD5Util.md5(UUIDUtil.uuid() + "123456");
        redisTemplate.opsForValue().set("seckillPath:" + user.getId() + ":" + goodsId, str, 60, TimeUnit.SECONDS);
        return str;
    }

    //校验秒杀地址
    @Override
    public Boolean checkPath(User user, String path, Long goodsId) {
        String key = "seckillPath:" + user.getId() + ":" + goodsId;
        String rightPath = (String) redisTemplate.opsForValue().get(key);
        if(rightPath == null || !rightPath.equals(path)) {
            return false;
        }
        return true;
    }

    //校验验证码
    @Override
    public Boolean checkCptcha(User user, Long goodsId, String captcha) {
        if(StringUtils.isEmpty(captcha) || user == null || goodsId < 0) {
            return false;
        }
        String rightCaptcha = (String) redisTemplate.opsForValue().get("captcha:" + user.getId() + ":" + goodsId);
        if(!captcha.equals(rightCaptcha)) {
            return false;
        }
        return true;
    }
}
