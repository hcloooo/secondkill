package com.nacl.secondkill.service.impl;

import com.nacl.secondkill.entity.User;
import com.nacl.secondkill.exception.GlobalException;
import com.nacl.secondkill.mapper.UserMapper;
import com.nacl.secondkill.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nacl.secondkill.utils.CookieUtil;
import com.nacl.secondkill.utils.MD5Util;
import com.nacl.secondkill.utils.UUIDUtil;
import com.nacl.secondkill.vo.LoginVo;
import com.nacl.secondkill.vo.RespBean;
import com.nacl.secondkill.vo.RespBeanEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author NaCl
 * @since 2022-03-07
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public RespBean doLogin(LoginVo loginVo, HttpServletRequest request, HttpServletResponse response) {
        String mobile = loginVo.getMobile();
        String formPass = loginVo.getPassword();

        User user = baseMapper.selectById(mobile);
        if(user == null) {
            throw new GlobalException(RespBeanEnum.LOGIN_ERROR);
        }
        if(!MD5Util.formPassToDBPass(formPass, user.getSalt()).equals(user.getPassword())) {
            throw new GlobalException(RespBeanEnum.LOGIN_ERROR);
        }
        //生成Cookie
        String ticket = UUIDUtil.uuid();
        //将用户信息放入redis
        redisTemplate.opsForValue().set("user:"+ticket,user);
        //request.getSession().setAttribute(ticket, user);
        CookieUtil.setCookie(request, response, "userTicket", ticket);
        return RespBean.success(ticket);
    }

    @Override
    public User getUserByCookie(String userTicket, HttpServletRequest request, HttpServletResponse response) {
        if(StringUtils.isEmpty(userTicket)) {
            return null;
        }
        User user = (User)redisTemplate.opsForValue().get("user:" + userTicket);
        if(user != null) {
            CookieUtil.setCookie(request, response,"userTicket",userTicket);
        }
        return user;
    }

    @Override
    public RespBean updatePassword(String userTicket, String password, HttpServletRequest request, HttpServletResponse response) {
        User user = getUserByCookie(userTicket,request,response);
        if(user == null) {
            throw new GlobalException(RespBeanEnum.MOBILE_ERROR);
        }
        user.setPassword(MD5Util.inputPassDBPass(password, user.getSalt()));
        int res = baseMapper.updateById(user);
        if(res == 1) {
            //更新之后删除redis
            redisTemplate.delete("user:" + userTicket);
            return RespBean.success();
        }
        return RespBean.error(RespBeanEnum.PASSWORD_UPDATE);
    }
}
