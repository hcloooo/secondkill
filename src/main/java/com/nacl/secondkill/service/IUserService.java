package com.nacl.secondkill.service;

import com.nacl.secondkill.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nacl.secondkill.vo.LoginVo;
import com.nacl.secondkill.vo.RespBean;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author NaCl
 * @since 2022-03-07
 */
public interface IUserService extends IService<User> {

    RespBean doLogin(LoginVo loginVo, HttpServletRequest request, HttpServletResponse response);

    //根据cookie获取用户
    User getUserByCookie(String userTicket, HttpServletRequest request, HttpServletResponse response);

    //更新密码
    RespBean updatePassword(String userTicket, String password, HttpServletRequest request, HttpServletResponse response);
}
