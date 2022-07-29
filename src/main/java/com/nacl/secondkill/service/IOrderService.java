package com.nacl.secondkill.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nacl.secondkill.entity.Order;
import com.nacl.secondkill.entity.User;
import com.nacl.secondkill.vo.GoodsVo;
import com.nacl.secondkill.vo.OrderDetailVo;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author NaCl
 * @since 2022-03-07
 */
public interface IOrderService extends IService<Order> {

    Order seckill(User user, GoodsVo goodsVo);

    OrderDetailVo getDetail(Long orderId);

    String createPath(User user, Long goodsId);

    Boolean checkPath(User user, String path, Long goodsId);

    Boolean checkCptcha(User user, Long goodsId, String captcha);
}
