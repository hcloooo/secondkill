package com.nacl.secondkill.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nacl.secondkill.entity.SeckillOrder;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author NaCl
 * @since 2022-03-07
 */
public interface ISeckillOrderService extends IService<SeckillOrder> {
    //判断是否在秒杀订单表中，即添加是否成功
    long inSeckillOrder(Long userId,String goodsId);
}
