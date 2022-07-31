package com.nacl.secondkill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nacl.secondkill.entity.SeckillGoods;
import com.nacl.secondkill.entity.SeckillOrder;
import com.nacl.secondkill.mapper.SeckillOrderMapper;
import com.nacl.secondkill.service.ISeckillGoodsService;
import com.nacl.secondkill.service.ISeckillOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.management.Query;
import javax.management.relation.RelationNotFoundException;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author NaCl
 * @since 2022-03-07
 */
@Service
@Slf4j
public class SeckillOrderServiceImpl extends ServiceImpl<SeckillOrderMapper, SeckillOrder> implements ISeckillOrderService {
    @Autowired
    ISeckillOrderService iSeckillOrderService;
    @Autowired
    ISeckillGoodsService iSeckillGoodsService;
    @Override
    public long inSeckillOrder(Long userId, String goodsId) {
        QueryWrapper<SeckillOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",userId);
        wrapper.eq("goods_id",goodsId);
        SeckillOrder seckillOrder = iSeckillOrderService.getOne(wrapper);
        log.info("nacl"+seckillOrder.getGoodsId());
        if(seckillOrder!=null){
            return seckillOrder.getOrderId();
        }else {
            //查库存
            SeckillGoods goods = iSeckillGoodsService.getOne(new QueryWrapper<SeckillGoods>().eq("goods_id", goodsId));
            if(goods.getStockCount()<=0){
                return -1L;
            }else {
                return 0L;
            }
        }
    }
}
