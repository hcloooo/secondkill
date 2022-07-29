package com.nacl.secondkill.controller;

import com.nacl.secondkill.entity.User;
import com.nacl.secondkill.service.IGoodsService;
import com.nacl.secondkill.service.IUserService;
import com.nacl.secondkill.vo.DetailVo;
import com.nacl.secondkill.vo.GoodsVo;
import com.nacl.secondkill.vo.RespBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/goods")
public class GoodsController {

    @Autowired
    private IUserService userService;

    @Autowired
    private IGoodsService goodsService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ThymeleafViewResolver viewResolver;

    /*
    windows优化前QPS：1332
            页面缓存后： 2342
    linux优化前QPS：207
     */
    @RequestMapping(value = "/toList", produces = "text/html;charset=utf-8")
    @ResponseBody
    public String toList(Model model, User user, HttpServletRequest request, HttpServletResponse response) {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String html = (String) valueOperations.get("goodsList");
        //如果不为空直接返回
        if(!StringUtils.isEmpty(html)) {
            valueOperations.set("goodsList", html, 60, TimeUnit.SECONDS);
            return html;
        }
        //否则手动渲染
        model.addAttribute("user", user);
        model.addAttribute("goodsList", goodsService.getGoodsVo());
        WebContext webContext = new WebContext(request, response, request.getServletContext(), request.getLocale(),model.asMap());
        String newHtml = viewResolver.getTemplateEngine().process("goodsList", webContext);
        if(!StringUtils.isEmpty(newHtml)) {
            valueOperations.set("goodsList", newHtml, 60, TimeUnit.SECONDS);
        }
        return newHtml;
    }

    //商品详情
    @RequestMapping(value = "/toDetail/{goodsId}", produces = "text/html;charset=utf-8")
    @ResponseBody
    public String toDetail(Model model, User user, @PathVariable Long goodsId, HttpServletRequest request, HttpServletResponse response) {
        if(user == null) {
            return "login";
        }
        //redis中获取页面
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String html = (String) valueOperations.get("goodsDetail:" + goodsId);
        if(!StringUtils.isEmpty(html)) {
            return html;
        }
        model.addAttribute("user", user);
        GoodsVo goodsVo = goodsService.getGoodsVoById(goodsId);
        model.addAttribute("goods", goodsVo);
        Date startDate = goodsVo.getStartDate();
        Date endDate = goodsVo.getEndDate();
        Date nowDate = new Date();

        int secKillStatus = 0;
        //秒杀倒计时
        int remainSeconds = 0;
        if(nowDate.before(startDate)) {
            secKillStatus = 0;
            remainSeconds = (int)((startDate.getTime() - nowDate.getTime()) / 1000);
        } else if(nowDate.after(endDate)) {
            secKillStatus = 2;
            remainSeconds = -1;
        } else {
            secKillStatus = 1;
            remainSeconds = 0;
        }
        model.addAttribute("secKillStatus", secKillStatus);
        model.addAttribute("remainSeconds", remainSeconds);
        WebContext webContext = new WebContext(request, response, request.getServletContext(), request.getLocale(),model.asMap());
        String newHtml = viewResolver.getTemplateEngine().process("goodsDetail", webContext);
        if(!StringUtils.isEmpty(newHtml)) {
            valueOperations.set("goodsDetail:"+goodsId, newHtml, 60, TimeUnit.SECONDS);
        }
        return newHtml;
    }

    @RequestMapping("/detail/{goodsId}")
    @ResponseBody
    public RespBean toDetail(Model model, User user, @PathVariable Long goodsId) {
        GoodsVo goodsVo = goodsService.getGoodsVoById(goodsId);
        Date startDate = goodsVo.getStartDate();
        Date endDate = goodsVo.getEndDate();
        Date nowDate = new Date();
        int secKillStatus = 0;
        //秒杀倒计时
        int remainSeconds = 0;
        if(nowDate.before(startDate)) {
            secKillStatus = 0;
            remainSeconds = (int)((startDate.getTime() - nowDate.getTime()) / 1000);
        } else if(nowDate.after(endDate)) {
            secKillStatus = 2;
            remainSeconds = -1;
        } else {
            secKillStatus = 1;
            remainSeconds = 0;
        }
        DetailVo detailVo = new DetailVo();
        detailVo.setUser(user);
        detailVo.setGoodsVo(goodsVo);
        detailVo.setRemainSeconds(remainSeconds);
        detailVo.setSecKillStatus(secKillStatus);
        return RespBean.success(detailVo);
    }
}
