package com.nacl.secondkill.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nacl.secondkill.entity.User;
import com.nacl.secondkill.service.IUserService;
import com.nacl.secondkill.utils.CookieUtil;
import com.nacl.secondkill.vo.RespBean;
import com.nacl.secondkill.vo.RespBeanEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

@Component
public class AccessLimitInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private IUserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(handler instanceof HandlerMethod) {
            User user = getUser(request, response);
            UserContext.setUser(user);
            HandlerMethod hm = (HandlerMethod) handler;
            AccessLimit accessLimit = hm.getMethodAnnotation(AccessLimit.class);
            if(accessLimit == null) {
                return true;
            }
            int second = accessLimit.second();
            int maxCount = accessLimit.maxCount();
            boolean needLogin = accessLimit.needLogin();
            if(needLogin) {
                if(user == null) {
                    render(response, RespBeanEnum.USER_TIME_OUT);
                    return false;
                }
            }

            ValueOperations valueOperations = redisTemplate.opsForValue();
            //发起请求的地址，限制访问次数，
            String uri = request.getRequestURI();
            Integer count = (Integer) valueOperations.get(uri + ":" + user.getId());
            String key = uri + ":" + user.getId();
            if(count == null) {
                valueOperations.set(key, 1, second, TimeUnit.SECONDS);
            } else if(count < maxCount) {
                valueOperations.increment(key);
            } else {
                render(response, RespBeanEnum.REQUEST_FAST);
                return false;
            }
        }

        return true;
    }

    //构建返回对象
    private void render(HttpServletResponse response, RespBeanEnum error) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        RespBean respBean = RespBean.error(error);
        PrintWriter writer = response.getWriter();
        writer.write(new ObjectMapper().writeValueAsString(respBean));
        writer.flush();
        writer.close();
    }

    private User getUser(HttpServletRequest request, HttpServletResponse response) {
        String ticket = CookieUtil.getCookieValue(request, "userTicket");
        if(StringUtils.isEmpty(ticket)) {
            return null;
        }
        return userService.getUserByCookie(ticket,request,response);
    }
}
