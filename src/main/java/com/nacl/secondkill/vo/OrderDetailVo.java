package com.nacl.secondkill.vo;

import com.nacl.secondkill.entity.Order;
import lombok.Data;

@Data
public class OrderDetailVo {

    private Order order;
    private GoodsVo goodsVo;
}
