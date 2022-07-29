package com.nacl.secondkill.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SeckillMessage {

    private User user;
    private Long goodsId;

}
