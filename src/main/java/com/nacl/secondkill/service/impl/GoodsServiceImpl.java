package com.nacl.secondkill.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nacl.secondkill.entity.Goods;
import com.nacl.secondkill.mapper.GoodsMapper;
import com.nacl.secondkill.service.IGoodsService;
import com.nacl.secondkill.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author NaCl
 * @since 2022-03-07
 */
@Service
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements IGoodsService {

    @Autowired
    private GoodsMapper goodsMapper;

    @Override
    public List<GoodsVo> getGoodsVo() {
        List<GoodsVo> res = goodsMapper.getGoodsVo();

        return res;
    }

    @Override
    public GoodsVo getGoodsVoById(Long goodsId) {
        GoodsVo res = goodsMapper.getGoodsVoById(goodsId);
        return res;
    }
}
