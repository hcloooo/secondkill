# 环境搭建

## 依赖安装

引入thymeleaf组件、web组件、mysql依赖、mybatis-plus依赖、lombok组件、md5组件、自定义注解组件、redis组件。

设置配置文件，设置服务端口、名称、数据库连接、配置xml文件路径、日志级别

## 统一返回结果对象

返回结果类型编码对象

```java
package com.hehehe.secondkill.vo;


public enum RespBeanEnum {
    SUCCESS(200,"SUCCESS"),
    ERROR(500,"服务端异常"),
    LOGIN_ERROR(500210,"帐号或密码错误"),
    MOBILE_ERROR(500310,"手机格式错误"),
    BIND_ERROR(500410,"参数校验异常"),
    EMPTY_STOCK(500510, "库存不足"),
    HAS_SECKILL(500610, "已经秒杀过了"),
    PASSWORD_UPDATE(500710,"更新密码失败"),
    USER_TIME_OUT(500810,"用户登录过期"),
    PATH_ERROR(500910, "路径错误"),
    ERROR_CAPTCHA(500110,"验证码错误"),
    REQUEST_FAST(501000,"请求频繁");

    private final Integer code;
    private final String message;

    RespBeanEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
    @Override
    public String toString() {
        return "RespBeanEnum{" +
                "code=" + code +
                ", message='" + message + '\'' +
                '}';
    }
}
```

返回结果内容对象

```java
package com.hehehe.secondkill.vo;

import com.nacl.secondkill.vo.RespBeanEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RespBean {

    private long code;
    private String message;
    private Object obj;

    //成功返回
    public static RespBean success() {
        return new RespBean(RespBeanEnum.SUCCESS.getCode(), RespBeanEnum.SUCCESS.getMessage(), null);
    }

    public static RespBean success(Object obj) {
        return new RespBean(RespBeanEnum.SUCCESS.getCode(), RespBeanEnum.SUCCESS.getMessage(), obj);
    }

    //因为失败各有不同，所以传入枚举
    public static RespBean error(RespBeanEnum respBeanEnum) {
        return new RespBean(respBeanEnum.getCode(), respBeanEnum.getMessage(), null);
    }

    public static RespBean error(RespBeanEnum respBeanEnum, Object obj) {
        return new RespBean(respBeanEnum.getCode(), respBeanEnum.getMessage(), obj);
    }
}
```

## 逆向工程生成基本结构

# 登录功能

## 安全加密

因为http是明文传输的，也就是中间的路由可以看到我们用户提交的帐号密码信息，因此我们选择对用户的密码进行MD5加密。

使用两次MD5加密：

客户端：PASS=MD5（密码+固定Salt）在前端实现。防止用户密码直接在网络中传输。

服务端：PASS=MD5（用户输入+随机salt）在后端实现。防止数据库泄露用户密码。

MD5Util.java

```java
package com.hehehe.secondkill.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

@Component
public class MD5Util {
    public static String md5(String src) {
        return DigestUtils.md5Hex(src);
    }

    //盐
    private static final String salt="hexiangdong";

    //对原理密码加密一次，后端接收的就是这个密码
    public static String inputPassToFromPass(String inputPass) {
        String str = "" + salt.charAt(0) + salt.charAt(1) + inputPass + salt.charAt(2) + salt.charAt(3);
        return md5(str);
    }

    //对加密过的密码再加密一次，然后可以放到数据库中了
    public static String formPassToDBPass(String formPass, String salt) {
        String str = salt.charAt(0) + salt.charAt(1) + formPass + salt.charAt(2) + salt.charAt(3);
        return md5(str);
    }

    //对原始密码直接加密两次，放入数据库中
    public static String inputPassDBPass(String inputPass, String salt) {
        String fromPass = inputPassToFromPass(inputPass);
        String dbPass = formPassToDBPass(fromPass, salt);
        return dbPass;
    }

    public static void main(String[] args) {
        //91b1e895ad031ed5dfac5f1273e40485
        System.out.println(inputPassToFromPass("123456"));
        System.out.println(formPassToDBPass("cf6473ca0856077cdd4fb2f9c4dfaeae",salt));
        System.out.println(inputPassDBPass("123456", salt));
    }
}
```

## 后端手机号码格式验证

前端代码是可见的，只有前端校验是不够的，用户完全可以修改前端代码跳过校验。因此后端也需要校验。

编写校验注解IsMobile

①新建Annotation文件，IsMobile

```java
package com.hehehe.secondkill.validator;

import IsMobileValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = {IsMobileValidator.class})
public @interface IsMobile {

    boolean require() default true;

    String message() default "手机号码格式错误";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
```

IsMobileValidator就是校验时的判断的逻辑

②编写IsMobileValidator.java

```java
package com.hehehe.secondkill.vo;

import ValidatorUtil;
import IsMobile;
import org.springframework.util.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class IsMobileValidator implements ConstraintValidator<IsMobile, String> {

    private boolean require = false;

    @Override
    public void initialize(IsMobile constraintAnnotation) {
        require = constraintAnnotation.require();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (require) {
            return ValidatorUtil.isMobiile(value);
        } else {
            if (StringUtils.isEmpty(value)) {
                return true;
            } else {
                return ValidatorUtil.isMobiile(value);
            }
        }
    }
}
```

③ValidatorUtil里面使用正则表达式判断

```java
package com.hehehe.secondkill.utils;

import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidatorUtil {
    private static final Pattern mobile_pattern = Pattern.compile("[1]([3-9])[0-9]{9}$");
    public static boolean isMobiile(String mobile) {
        if(StringUtils.isEmpty(mobile)) {
            return false;
        }
        Matcher matcher = mobile_pattern.matcher(mobile);
        return matcher.matches();
    }
}

```

④在controller中加上@Valid注解，表示需要对传入的参数进行校验，当然LoginVo类里面的mobile属性也要加上@IsMobile

```java
@RequestMapping("/doLogin")
    @ResponseBody
    public RespBean doLogin(@Valid LoginVo loginVo, HttpServletRequest request, HttpServletResponse response) {
        RespBean res = userService.doLogin(loginVo,request,response);
        return res;
    }
```

## 分布式session问题

使用cookie+session来解决，将用户信息存入redis

导入CookieUtil.java工具类、UUIDUtil.java工具类

## service层实现

```java
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
        //其实就是找到请求的发起地址，给发请求的地址设置一个cookie，后面的请求都让他带上这个userTicket
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
```



# 统一异常处理

ControllerAdvice 和 @ExceptionHandler 注解。 

使用 ErrorController类 来实现 

区别： 

1. @ControllerAdvice 方式只能处理控制器抛出的异常。此时请求已经进入控制器中。 
2. ErrorController类 方式可以处理所有的异常，包括未进入控制器的错误，比如404,401等错误 
3. 如果应用中两者共同存在，则 @ControllerAdvice 方式处理控制器抛出的异常， ErrorController类 方式处理未进入控制器的异常
4. @ControllerAdvice 方式可以定义多个拦截方法，拦截不同的异常类，并且可以获取抛出的异常 信息，自由度更大。 

创建GlobalException异常类

```java
package com.hehehe.secondkill.exception;

import RespBeanEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlobalException extends RuntimeException {
    private RespBeanEnum respBeanEnum;
}

```

创建GlobalExceptionHandler异常拦截器

```java
package com.hehehe.secondkill.exception;

import com.nacl.secondkill.exception.GlobalException;
import com.nacl.secondkill.vo.RespBean;
import RespBeanEnum;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public RespBean ExceptionHandler(Exception e) {
        if (e instanceof GlobalException) {
            GlobalException ex = (GlobalException) e;
            return RespBean.error(ex.getRespBeanEnum());
        } else if (e instanceof BindException) {
            BindException be = (BindException) e;
            RespBean respBean = RespBean.error(RespBeanEnum.BIND_ERROR);
            respBean.setMessage("参数校验异常:" + be.getAllErrors().get(0).getDefaultMessage());
            return respBean;
        }
        return RespBean.error(RespBeanEnum.ERROR);
    }
}

```

# 商品相关功能

## 商品列表

GoodsController

```java
@Controller
@RequestMapping("/goods")
public class GoodsController {
 /**
 * 跳转登录页
 *
 * @return
 */
    @RequestMapping("/toList")
    public String toLogin(HttpSession session, Model model, 
    	@CookieValue("userTicket") String ticket) {//去请求体里面的cookie值
        if (StringUtils.isEmpty(ticket)) {//没有cookie说明就没有登陆，回去登录去
        	return "login";
        }
        //从服务器的session中获取
        User user = (User) session.getAttribute(ticket);
        if (null == user) {
            return "login";
        }
        model.addAttribute("user", user);
        return "goodsList";
    }

```

后面我们有引入了redis作为分布式session管理，将用户信息从服务器session放到了redis中，因此修改获取用户信息的方法

```java
@RequestMapping("/toList")
public String toLogin(HttpServletRequest request,HttpServletResponse
	response, Model model, @CookieValue("userTicket") String ticket) {
    if (StringUtils.isEmpty(ticket)) {
    	return "login";
    }
    //从redis中获取
    User user = userService.getByUserTicket(ticket,request,response);
    if (null == user) {
    	return "login";
    }
    model.addAttribute("user", user);
    return "goodsList";
}

```

## 优化登录功能

我们每次访问页面，都要先获取cookie中的userTicket，然后使用这个userTicket去redis获取用户，如果每个controller都这样写会非常的臃肿。因此我们可以将这部分内容放到拦截器中实现。

 UserArgumentResolver 

```java
@Component
public class UserArgumentResolver implements HandlerMethodArgumentResolver {
    @Autowired
    private IUserService userService;
    
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        Class<?> clazz = parameter.getParameterType();
        return clazz == User.class;
    }
    
    @Override
    public Object resolveArgument(MethodParameter parameter, 
        ModelAndViewContainer mavContainer, NativeWebRequest webRequest, 
        WebDataBinderFactory binderFactory) throws Exception {
        
        HttpServletRequest request =
        webRequest.getNativeRequest(HttpServletRequest.class);
        
        HttpServletResponse response =
        webRequest.getNativeResponse(HttpServletResponse.class);
        
        String ticket = CookieUtil.getCookieValue(request, "userTicket");
        
        if (StringUtils.isEmpty(ticket)) {
        	return null;
        }
        return userService.getByUserTicket(ticket, request, response);
    }
}

```

然后编写WebConfig文件，将这个拦截器注册到配置中

```java
@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private UserArgumentResolver userArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(userArgumentResolver);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        //防止图片等静态资源被拦截
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
    }

}

```

优化商品列表功能

```java
@RequestMapping("/toList")
public String toLogin(Model model, User user) {
    if (null == user) {
    	return "login";
    }
    model.addAttribute("user", user);
    return "goodsList";
}

```

# 秒杀功能

## 商品详情

```java
@RequestMapping("/toDetail/{goodsId}")
public String toDetail(Model model, User user, @PathVariable Long goodsId) {
	model.addAttribute("user", user);
   	GoodsVo goods = goodsService.findGoodsVoByGoodsId(goodsId);
   	model.addAttribute("goods", goods);
   	Date startDate = goods.getStartDate();
   	Date endDate = goods.getEndDate();
   	Date nowDate = new Date();
   	//秒杀状态
   	int secKillStatus = 0;
   	//剩余开始时间
   	int remainSeconds = 0;
   	//秒杀还未开始
   	if (nowDate.before(startDate)) {
    	remainSeconds = (int) ((startDate.getTime()-nowDate.getTime())/1000);
   	// 秒杀已结束
   	} else if (nowDate.after(endDate)) {
        secKillStatus = 2;
        remainSeconds = -1;
   	// 秒杀中
    } else {
        secKillStatus = 1;
        remainSeconds = 0;
    }
    model.addAttribute("secKillStatus",secKillStatus);
    model.addAttribute("remainSeconds",remainSeconds);
    return "goodsDetail";
}

```

## 秒杀功能实现

controller层doSeckill方法

```java
@RequestMapping("/doSeckill")
public String doSeckill(Model model, User user, Long goodsId) {
    if (user == null) {
    	return "login";
    }
    model.addAttribute("user", user);
    GoodsVo goods = goodsService.findGoodsVoByGoodsId(goodsId);
    //判断库存
    if (goods.getStockCount() < 1) {
        model.addAttribute("errmsg", RespBeanEnum.EMPTY_STOCK.getMessage());
        return "seckillFail";
    }
    
    //判断是否重复抢购
    SeckillOrder seckillOrder = seckillOrderService.getOne(
        new QueryWrapper<SeckillOrder>().eq("user_id",user.getId()).
        eq("goods_id",goodsId));
    
    if (seckillOrder != null) {
        model.addAttribute("errmsg", RespBeanEnum.REPEATE_ERROR.getMessage());
        return "seckillFail";
    }
    Order order = orderService.seckill(user, goods);
    model.addAttribute("order",order);
    model.addAttribute("goods",goods);
    return "orderDetail";
}

```

service层seckill()方法

```java
@Override
@Transactional
public Order seckill(User user, GoodsVo goods) {
    //秒杀商品表减库存
    SeckillGoods seckillGoods = seckillGoodsService.getOne(new
    QueryWrapper<SeckillGoods>().eq("goods_id",goods.getId()));
    seckillGoods.setStockCount(seckillGoods.getStockCount()-1);
    seckillGoodsService.updateById(seckillGoods);
    //生成订单
    Order order = new Order();
    order.setUserId(user.getId());
    order.setGoodsId(goods.getId());
    order.setDeliveryAddrId(0L);
    order.setGoodsName(goods.getGoodsName());
    order.setGoodsCount(1);
    order.setGoodsPrice(seckillGoods.getSeckillPrice());
    order.setOrderChannel(1);
    order.setStatus(0);
    order.setCreateDate(new Date());
    orderMapper.insert(order);
    //生成秒杀订单
    SeckillOrder seckillOrder = new SeckillOrder();
    seckillOrder.setOrderId(order.getId());
    seckillOrder.setUserId(user.getId());
    seckillOrder.setGoodsId(goods.getId());
    seckillOrderService.save(seckillOrder);
    return order;
   }
}

```

大体上看没有问题，但是有没有思考并发的情况呢？一定会出现超卖的，兄弟！！

# 速度优化

## 1.页面优化

### 1.1商品详情页面缓存

因为商品详情页面几乎是静态的，所以可以将整个html的内容放入redis中

```java
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

```

### 1.2商品详情页面静态化

将整个页面放入redis实际上放的是html源代码，如果这个页面过大，他的html源代码也会很长，在网络传输中的速度就会变慢。我们可以将这个页面直接变成静态的页面，具体的数据使用ajax请求来获取，只更新极少量的数据就可以了。

```java
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

```

这个是商品详情页面，秒杀页面也是可以静态化的

```java
 @RequestMapping("/doSeckill2")
    public String doSeckill2(Model model, User user, Long goodsId) {
        if(user == null) {
            return "login";
        }
        model.addAttribute("user", user);
        GoodsVo goodsVo = goodsService.getGoodsVoById(goodsId);
        model.addAttribute("goods", goodsVo);
        //判断库存
        if(goodsVo.getStockCount() <= 0) {
            model.addAttribute("errmsg", RespBeanEnum.EMPTY_STOCK.getMessage());
            return "secKillFail";
        }
        //判断当前用户是否已经秒杀过
        QueryWrapper<SeckillOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", user.getId());
        wrapper.eq("goods_id", goodsId);
        SeckillOrder seckillOrder = seckillOrderService.getOne(wrapper);
        if(seckillOrder != null) {
            model.addAttribute("errmsg", RespBeanEnum.HAS_SECKILL.getMessage());
            return "secKillFail";
        }
        //下订单
        Order order = orderService.seckill(user,goodsVo);
        model.addAttribute("order", order);
        return "orderDetail";
    }

```

订单详情静态化

```java
//订单信息
    @RequestMapping("/detail")
    @ResponseBody
    public RespBean detail(User user, Long orderId) {
        if(user == null) {
            return RespBean.error(RespBeanEnum.USER_TIME_OUT);
        }

        OrderDetailVo detail = orderService.getDetail(orderId);

        return RespBean.success(detail);

    }

```

前端文件就不写了，详情可以看源代码。

**这一点看完建议先看安全优化的1和2**

## 2.秒杀优化

前文我们已经解决了超卖问题和单一用户多次购买问题。我们使用唯一性索引的方式来防止用户多次购买问题。但是如果一个用户发出的请求次数过多，那么后面的请求会直接访问数据库，虽然唯一性索引会保证安全，但是让数据库去处理这些大量的无效请求不太好。

因此当用户成功秒杀到商品的时候，我们将用户id和商品id作为key放到redis中，后续的秒杀请求需要先判断redis是否已经存在这个订单，存在就返回错误即可。

在秒杀service中加入以下代码

```java
//生成秒杀订单
SeckillOrder seckillOrder = new SeckillOrder();
seckillOrder.setOrderId(order.getId());
seckillOrder.setGoodsId(goodsVo.getId());
seckillOrder.setUserId(user.getId());
seckillOrderService.save(seckillOrder);
//放入redis中
redisTemplate.opsForValue().set("order" + user.getId() + ":" + goodsVo.getId(), seckillOrder);

```

在秒杀的controller接口中加入以下代码

```java
ValueOperations valueOperations = redisTemplate.opsForValue();
//判断redis中是存在该用户的订单
SeckillOrder seckillOrder = (SeckillOrder)valueOperations.get("order:"+user.getId()+":"+goodsId);
if(seckillOrder != null) {
    return RespBean.error(RespBeanEnum.HAS_SECKILL);
}

```

## 3.服务优化

### 3.1秒杀商品库存入redis

系统初始化完成后，将所有的秒杀的商品的库存信息放入redis中。

让SeckillController实现InitializingBean接口，重写里面的afterPropertiesSet方法

```java
//加载完毕后执行，将商品库存数量加载到redis
@Override
public void afterPropertiesSet() throws Exception {
    List<GoodsVo> goodsVos = goodsService.getGoodsVo();
    if(goodsVos.size() == 0) {
    	return ;
    }
    ValueOperations valueOperations = redisTemplate.opsForValue();
    for(GoodsVo goodsVo : goodsVos) {
        valueOperations.set("seckillGoods:" + goodsVo.getId(), goodsVo.getStockCount());
        emptyMap.put(goodsVo.getId(), false);
    }
}

```

### 3.2使用redis预减库存

使用redis预减库存，如果库存不足，直接返回错误。

但是这一点需要使用redis+lua脚本实现原子操作。

stock.lua

```lua
if (redis.call("exists",KEYS[1])==1) then
    local stock = tonumber(redis.call("get",KEYS[1]));
    if (stock > 0) then
        redis.call("incrby",KEYS[1],-1);
        return stock;
    end;
        return 0;
end;

```

在RedisConfig.java中添加这个lua脚本

```java
@Bean
public DefaultRedisScript<Long> script() {
    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
    //lock.lua脚本位置和application.properties同级目录
    redisScript.setLocation(new ClassPathResource("stock.lua"));
    redisScript.setResultType(Long.class);
    return redisScript;
}

```

在秒杀接口中判断

```java
//使用lua脚本
Long stock = (Long) redisTemplate.execute(script, Collections.singletonList("seckillGoods:" + goodsId), Collections.EMPTY_LIST);
if(stock == 0) {
    emptyMap.put(goodsId, true);
    //valueOperations.increment("seckillGoods:" + goodsId);
    return RespBean.error(RespBeanEnum.EMPTY_STOCK);
}

```

### 3.3使用本地内存判断库存

如果在redis中预减库存是发现库存已经是0了，后续的请求还是会访问redis，虽然redis确实很快，但是访问redis是需要网络传输的，肯定没有本地内存块。

我们可以将没有库存的商品放入本地HashMap中，如果判断哈希表中存在这个商品，那就是没库存了，不用再看redis了

### 3.4使用rabbitmq处理秒杀请求

创建MQSender

```java
@Service
@Slf4j
public class MQSender {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    
        //发送秒杀信息
    public void sendSeckillMessage(String message) {
        log.info("发送消息：" + message);
        rabbitTemplate.convertAndSend("seckillExchange", "seckill.message", message);
    }

}

```

创建MQReceiver

```java
@Service
@Slf4j
public class MQReceiver {
    @Autowired
    private IGoodsService goodsService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private IOrderService orderService;

    //进行下单
    @RabbitListener(queues = "seckillQueue")
    public void receive(String message) {
        log.info("接收到订单消息:",message);
        SeckillMessage seckillMessage = JSON.parseObject(message, SeckillMessage.class);
        Long goodsId = seckillMessage.getGoodsId();
        User user = seckillMessage.getUser();

        GoodsVo goodsVo = goodsService.getGoodsVoById(goodsId);
        if(goodsVo.getStockCount() < 1) {
            return ;
        }
        //再次判断
        ValueOperations valueOperations = redisTemplate.opsForValue();
        SeckillOrder seckillOrder = (SeckillOrder)valueOperations.get("order:"+user.getId()+":"+goodsId);
        if(seckillOrder != null) {
            return ;
        }

        //下单操作
        try {
            orderService.seckill(user, goodsVo);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return ;
        }

    }
}

```

最后前端轮询查询秒杀结果

```java
//查询用户秒杀的结果
@RequestMapping("/result")
@ResponseBody
public RespBean getResult(User user, String goodsId) {
    if(user == null) {
        return RespBean.error(RespBeanEnum.USER_TIME_OUT);
    }
    String key = "order" + user.getId() + ":" + goodsId;
    ValueOperations valueOperations = redisTemplate.opsForValue();
    Long orderId = -1l;
    if(valueOperations.get(key) != null) {
        SeckillOrder seckillOrder = (SeckillOrder) valueOperations.get(key);
        orderId = seckillOrder.getOrderId();
    } else if((int)valueOperations.get("seckillGoods:" + goodsId) <= 0) {
        orderId = -1l;
    } else {
        orderId = 0l;
    }
    return RespBean.success(orderId);
}

```

**看完看安全优化3**

# 安全优化

## 1.超卖问题

原始的方案是先查询数据库中库存数量是否大于0，大于零的话再执行减库存的操作，这两个操作不是原子的，在高并发情况下一定会出现超卖问题的。

我们改进思路，使用sql语句直接更新数据库，将对应商品的库存减一，如果更新成功的话说明还没有超卖。但是使用sql语句更新会发生并发问题吗？答案是不会的，因为使用update关键字是，在mysql中是当前读，是要给这一行加行锁的。所以没有并发问题。

将service中减库存的操作改为如下内容

```javascript
//判断库存是否大于0
UpdateWrapper<SeckillGoods> wrapper2 = new UpdateWrapper<>();
wrapper2.setSql("stock_count="+"stock_count-1");
wrapper2.eq("id", seckillGoods.getId());
wrapper2.gt("stock_count", 0);
boolean secKillres = seckillGoodsService.update(wrapper2);
if(!secKillres) {
    return null;
}

```

## 2.单用户多次秒杀问题

在之前的代码中，判断用户是不是已经秒杀过我们是这样做的

```java
//判断当前用户是否已经秒杀过
QueryWrapper<SeckillOrder> wrapper = new QueryWrapper<>();
wrapper.eq("user_id", user.getId());
wrapper.eq("goods_id", goodsId);
SeckillOrder seckillOrder = seckillOrderService.getOne(wrapper);
if(seckillOrder != null) {
    model.addAttribute("errmsg", RespBeanEnum.HAS_SECKILL.getMessage());
    return "secKillFail";
}

```

也就是查询数据库，看数据库中存不存在该用户相关的订单，如果存在就返回错误。毋庸置疑，一定是存在并发问题的。

我们可以在秒杀商品订单表中添加唯一性索引，将这个工作交给mysql处理，我们可以在秒杀商品订单表中以用户id列和商品id列组合建立一个唯一性索引就可以解决了。

**看完之后看速度优化的2**

## 3.接口隐藏

我们在前端的一些重要的接口地址手机直接暴露的，如果一些专业人员使用脚本不断地请求我们的接口，会导致刚开始秒杀就将商品秒杀完。

我们可以对接口隐藏，用户想要秒杀商品，首先需要获取这个秒杀的接口，采用UUID随机生成的策略。生成后的接口绑定上用户id和商品id，放入redis。

```java
@RequestMapping(value = "/path", method = RequestMethod.GET)
@ResponseBody
public RespBean getPath(User user, Long goodsId) {
    if (user == null) {
        return RespBean.error(RespBeanEnum.SESSION_ERROR);
    }
    //使用UUID生成随机值并使用MD5加密一下然后放入redis
    String str = orderService.createPath(user,goodsId);
    return RespBean.success(str);
}

```

在秒杀接口加入以下内容，首先是映射地址，加上path，path就是我们上一步说的用户获取的属于他自己的接口地址。然后在redis中取出这个path，并判断是否和用户id和商品id相等

```java
@RequestMapping("/{path}/doSeckill")
@ResponseBody
public RespBean doSeckill(@PathVariable String path, User user, Long goodsId) {
    if(user == null) {
    	return RespBean.error(RespBeanEnum.USER_TIME_OUT);
    }

    ValueOperations valueOperations = redisTemplate.opsForValue();
    //检查path是否正确
    Boolean check = orderService.checkPath(user, path, goodsId);
    if(!check) {
    	return RespBean.error(RespBeanEnum.PATH_ERROR);
    }
    //判断当前用户是否已经秒杀过
    SeckillOrder seckillOrder = (SeckillOrder)valueOperations.get("order:"+user.getId()+":"+goodsId);
    if(seckillOrder != null) {
    	return RespBean.error(RespBeanEnum.HAS_SECKILL);
    }

    if(emptyMap.get(goodsId)) {
    	return RespBean.error(RespBeanEnum.EMPTY_STOCK);
    }
            //redis预减库存
    //        Long stock = valueOperations.decrement("seckillGoods:" + goodsId);
            //使用lua脚本
    Long stock = (Long) redisTemplate.execute(script, Collections.singletonList("seckillGoods:" + goodsId), Collections.EMPTY_LIST);
    if(stock == 0) {
        emptyMap.put(goodsId, true);
        //valueOperations.increment("seckillGoods:" + goodsId);
        return RespBean.error(RespBeanEnum.EMPTY_STOCK);
    }
            //下订单
    SeckillMessage seckillMessage = new SeckillMessage(user, goodsId);
    mqSender.sendSeckillMessage(JSON.toJSONString(seckillMessage));
    return RespBean.success(0);
}

```

## 4.接口限流

基本限流逻辑

```java
ValueOperations valueOperations = redisTemplate.opsForValue();
//限制访问次数，5秒内访问5次
String uri = request.getRequestURI();
//方便测试
captcha = "0";
Integer count = (Integer) valueOperations.get(uri + ":" + user.getId());
if (count==null){
    valueOperations.set(uri + ":" + user.getId(),1,5,TimeUnit.SECONDS);
}else if (count<5){
    valueOperations.increment(uri + ":" + user.getId());
}else {
    return RespBean.error(RespBeanEnum.ACCESS_LIMIT_REACHED);
}

```

但是很多接口都需要用到这一部分逻辑，如果都写一遍，就太麻烦了，我们将他改为注解就很棒！

创建AccessLimit注解

```java
//运行时的注解
@Retention(RetentionPolicy.RUNTIME)
//放在方法上的注解
@Target(ElementType.METHOD)
public @interface AccessLimit {
    int second();
    int maxCount();
    boolean needLogin() default true;
}

```

创建拦截器，捕获AccessLimit注解，并进行相关限流操作

```java
@Component
public class AccessLimitInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private IUserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断他是不是一个被拦截的一个方法
        if(handler instanceof HandlerMethod) {
            User user = getUser(request, response);
            UserContext.setUser(user);
            HandlerMethod hm = (HandlerMethod) handler;
            //获取这个方法上面的注解
            AccessLimit accessLimit = hm.getMethodAnnotation(AccessLimit.class);
            //看有没有这个注解
            if(accessLimit == null) {
                return true;
            }
            //拿到这个注解的相关描述
            int second = accessLimit.second();
            int maxCount = accessLimit.maxCount();
            boolean needLogin = accessLimit.needLogin();
            if(needLogin) {
                if(user == null) {
                    render(response, RespBeanEnum.USER_TIME_OUT);
                    return false;
                }
            }

            //开始限流处理，核心代码
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

```

创建UserContext，里面是一个ThreadLocal变量，用于不同的线程存储自己的User信息

```java
public class UserContext {
    private static ThreadLocal<User> userHolder = new ThreadLocal<User>();

    public static void setUser(User user) {
        userHolder.set(user);
    }
    public static User getUser() {
        return userHolder.get();
    }
}

```

# RabbitMQ使用

## 安装

①下载erlang

 https://www.erlang-solutions.com/resources/download.html 

安装erlang

```shell
yum -y install esl-erlang_23.0.2-1_centos_7_amd64.rpm

```

检测erlang

```shell
erl

```

②安装RabbitMQ

下载地址http://www.rabbitmq.com/download.html

安装rabbitmq

```shell
install rabbitmq-server-3.8.5-1.el7.noarch.rpm

```

安装UI插件

```shell
rabbitmq-plugins enable rabbitmq_management

```

③启动rabbitmq服务

```shell
systemctl start rabbitmq-server.service

```

检测服务

```shell
systemctl status rabbitmq-server.service

```

访问15672端口，帐号guest 密码guest

在/etc/rabbitmq目录下创建一个rabbitmq.config文件

```shell
vim /etc/rabbitmq rabbitmq.config
加入以下内容
[{rabbit, [{loopback_users, []}]}].

```

重启reabbitmq服务

```shell
systemctl restart rabbitmq-server.service

```

## 依赖和配置

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>

```

```properties
#rabbitmq配置
spring.rabbitmq.host=101.132.146.181
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
spring.rabbitmq.virtual-host=/
spring.rabbitmq.port=5672

```

编写RabbitMQ的java配置文件RabbitMQConfig.java

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        //key的序列化
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        //value的序列化
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        //hash类型 key的序列化
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        //hash类型 value序列化
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        //注入连接工厂
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }

    @Bean
    public DefaultRedisScript<Long> script() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        //lock.lua脚本位置和application.properties同级目录
        redisScript.setLocation(new ClassPathResource("stock.lua"));
        redisScript.setResultType(Long.class);
        return redisScript;
    }
}

```

## 简单测试

1.在config包下创建`RabbitMQConfig.java`文件

```java
@Configuration
public class RabbitMQConfig {
    @Bean
    public Queue queue() {
        return new Queue("queue", true);
    }
}

```

2.在rabbitmq包下创建`MQSender.java`文件

```java
@Service
@Slf4j
public class MQSender {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void send(Object msg) {
        log.info("发送消息：" + msg);
        //这个"queue"就是在配置文件中新建的Queue名字
        rabbitTemplate.convertAndSend("queue", msg);
    }
}

```

3.在rabbitmq包下创建`MQReceiver.java`文件

```java
@Service
@Slf4j
public class MQReceiver {

    //接受队列"queue"中的消息
    @RabbitListener(queues="queue")
    public void receive(Object msg) {
        log.info("接收消息：" + msg);
    }
}

```

4.去Controller中测试

```java
//测试发送消息
@RequestMapping("/mq")
@ResponseBody
public void mq() {
    mqSender.send("hello");
}

```

## 交换机测试

### Fanout模式

1.在config包下创建`RabbitMQConfig.java`文件，创建两个队列和一个交换机，并将这两个队列绑定到交换机上，发送消息时会将消息发给所有绑定的队列中。

```java
@Configuration
public class RabbitMQConfig {

    private static final String QUEUE01 = "queue_fanout01";
    private static final String QUEUE02 = "queue_fanout02";
    private static final String EXCHANGE = "fanoutExchange";

    @Bean
    public Queue queue01() {
        return new Queue(QUEUE01);
    }

    @Bean
    public Queue queue02() {
        return new Queue(QUEUE02);
    }

    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(EXCHANGE);
    }

    @Bean
    public Binding binding01() {
        return BindingBuilder.bind(queue01()).to(fanoutExchange());
    }

    @Bean
    public Binding binding02() {
        return BindingBuilder.bind(queue02()).to(fanoutExchange());
    }
}

```

2.在rabbitmq包下创建`MQSender.java`文件

```java
@Service
@Slf4j
public class MQSender {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void send(Object msg) {
        log.info("发送消息：" + msg);
        //发送给交换机
        rabbitTemplate.convertAndSend("fanoutExchange", "", msg);
    }
}

```

3.在rabbitmq包下创建`MQReceiver.java`文件

```java
@Service
@Slf4j
public class MQReceiver {
    
    @RabbitListener(queues = "queue_fanout01")
    public void receive01(Object msg) {
        log.info("QUEUE01接收消息：" + msg);
    }

    @RabbitListener(queues = "queue_fanout02")
    public void receive02(Object msg) {
        log.info("QUEUE02接收消息：" + msg);
    }
    
}

```

4.去Controller中测试

```java
@RequestMapping("/mq/fanout")
@ResponseBody
public void mq01() {
    mqSender.send("hello");
}

```

### topic模式

1.在config包下创建`RabbitMQTopicConfig.java`文件，创建两个队列、两个路由匹配规则和一个交换机，并将这两个队列连同匹配规则绑定到交换机。

```java
@Configuration
public class RabbitMQTopicConfig {

    private static final String QUEUE01 = "queue_topic01";
    private static final String QUEUE02 = "queue_topic02";
    private static final String EXCHANGE = "topicExchange";
    private static final String ROUTINGKEY01 = "#.queue.#";
    private static final String ROUTINGKEY02 = "*.queue.#";

    @Bean
    public Queue queue01() {
        return new Queue(QUEUE01);
    }

    @Bean
    public Queue queue02() {
        return new Queue(QUEUE02);
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Binding binding01() {
        return BindingBuilder.bind(queue01()).to(topicExchange()).with(ROUTINGKEY01);
    }

    @Bean
    public Binding binding02() {
        return BindingBuilder.bind(queue02()).to(topicExchange()).with(ROUTINGKEY02);
    }
}

```

2.在rabbitmq包下创建`MQSender.java`文件

```java
@Service
@Slf4j
public class MQSender {
    public void send03(Object msg) {
        log.info("发送(QUEUE01接收):",msg);
        rabbitTemplate.convertAndSend("topicExchange", "queue.red.message", msg);
    }

    public void send04(Object msg) {
        log.info("发送(被两个QUEUE接收):",msg);
        rabbitTemplate.convertAndSend("topicExchange", "message.queue.green", msg);
    }
}

```

3.在rabbitmq包下创建`MQReceiver.java`文件

```java
@Service
@Slf4j
public class MQReceiver {
    @RabbitListener(queues = "queue_topic01")
    private void receive05(Object msg) {
        log.info("QUEUE01接收消息：" + msg);
    }

    @RabbitListener(queues = "queue_topic02")
    private void receive06(Object msg) {
        log.info("QUEUE02接收消息：" + msg);
    }
}

```

4.去Controller中测试

```java
@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    MQSender mqSender;    
    
	//topic模式
    @RequestMapping("/mq/topic01")
    @ResponseBody
    public void mq04() {
        mqSender.send03("Hello");
    }

    @RequestMapping("/mq/topic02")
    @ResponseBody
    public void mq05() {
        mqSender.send04("Hello");
    }
}

```

### header模式

1.在config包下创建`RabbitMQHeaderConfig.java`文件，创建两个队列和一个交换机，并将这两个队列连同匹配规则绑定到交换机。和topic中的匹配规则不同的是，header中的匹配规则是使用键值对的匹配规则。

```java
@Configuration
public class RabbitMQHeadersConfig {

    private static final String QUEUE01 = "queue_hearder01";
    private static final String QUEUE02 = "queue_hearder02";
    private static final String EXCHANGE = "headerExchange";

    @Bean
    public Queue queue01() {
        return new Queue(QUEUE01);
    }

    @Bean
    public Queue queue02() {
        return new Queue(QUEUE02);
    }

    @Bean
    public HeadersExchange headersExchange() {
        return new HeadersExchange(EXCHANGE);
    }

    @Bean
    public Binding binding01() {
        Map<String, Object> map = new HashMap<String, Object>(){{
            put("color", "red");
            put("speed", "low");
        }};
        return BindingBuilder.bind(queue01()).to(headersExchange()).whereAny(map).match();
    }

    @Bean
    public Binding binding02() {
        Map<String, Object> map = new HashMap<String, Object>(){{
            put("color", "red");
            put("speed", "fast");
        }};
        return BindingBuilder.bind(queue02()).to(headersExchange()).whereAll(map).match();
    }
}

```

2.在rabbitmq包下创建`MQSender.java`文件

```java
@Service
@Slf4j
public class MQSender {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void send(Object msg) {
        log.info("发送消息：" + msg);
        rabbitTemplate.convertAndSend("fanoutExchange", "", msg);
    }

    public void send01(Object msg) {
        log.info("发送red消息：" + msg);
        rabbitTemplate.convertAndSend("directExchange", "queue.red", msg);
    }

    public void send02(Object msg) {
        log.info("发送green消息：" + msg);
        rabbitTemplate.convertAndSend("directExchange", "queue.green", msg);
    }

    public void send03(Object msg) {
        log.info("发送(QUEUE01接收):",msg);
        rabbitTemplate.convertAndSend("topicExchange", "queue.red.message", msg);
    }

    public void send04(Object msg) {
        log.info("发送(被两个QUEUE接收):",msg);
        rabbitTemplate.convertAndSend("topicExchange", "message.queue.green", msg);
    }

    public void send05(Object msg) {
        log.info("发送(两个队列接收):", msg);
        MessageProperties properties = new MessageProperties();
        properties.setHeader("color", "red");
        properties.setHeader("speed", "fast");
        Message message = new Message(((String)msg).getBytes(), properties);
        rabbitTemplate.convertAndSend("headerExchange", "", message);
    }

    public void send06(Object msg) {
        log.info("发送(QUEUE01接收):", msg);
        MessageProperties properties = new MessageProperties();
        properties.setHeader("color", "red");
        properties.setHeader("speend", "normal");
        Message message = new Message(((String)msg).getBytes(), properties);
        rabbitTemplate.convertAndSend("headerExchange", "", message);
    }
}

```

3.在rabbitmq包下创建`MQReceiver.java`文件

```java
@Service
@Slf4j
public class MQReceiver {

    @RabbitListener(queues="queue")
    public void receive(Object msg) {
        log.info("接收消息：" + msg);
    }

    @RabbitListener(queues = "queue_fanout01")
    public void receive01(Object msg) {
        log.info("QUEUE01接收消息：" + msg);
    }

    @RabbitListener(queues = "queue_fanout02")
    public void receive02(Object msg) {
        log.info("QUEUE02接收消息：" + msg);
    }

    @RabbitListener(queues = "queue_direct01")
    public void receive03(Object msg) {
        log.info("QUEUE01接受消息:" + msg);
    }

    @RabbitListener(queues = "queue_direct02")
    public void receive04(Object msg) {
        log.info("QUEUE02接受消息:" + msg);
    }

    @RabbitListener(queues = "queue_topic01")
    private void receive05(Object msg) {
        log.info("QUEUE01接收消息：" + msg);
    }

    @RabbitListener(queues = "queue_topic02")
    private void receive06(Object msg) {
        log.info("QUEUE02接收消息：" + msg);
    }

    @RabbitListener(queues = "queue_hearder01")
    public void receive07(Message message) {
        log.info("QUEUE01接收Message对象：" + message);
        log.info("QUEUE01接收消息：", new String(message.getBody()));
    }

    @RabbitListener(queues = "queue_hearder02")
    public void receive08(Message message) {
        log.info("QUEUE02接收Message对象：" + message);
        log.info("QUEUE02接收消息：", new String(message.getBody()));
    }
}

```

4.去Controller中测试

```java
@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    MQSender mqSender;
	@RequestMapping("/mq/header01")
    @ResponseBody
    public void mq06() {
        mqSender.send05("hello");
    }

    @RequestMapping("/mq/header02")
    @ResponseBody
    public void mq07() {
        mqSender.send06("hello");
    }
}


```

# 项目总结

## 项目环境搭建

1.SpringBoot环境搭建

2.集成Thymeleaf

3.Mybatis

## 分布式会话

1.用户登录。明文密码二次MD5加密

2.参数校验+全局异常处理

3.共享Session使用redis实现

## 功能开发

1.商品列表

2.商品详情

3.秒杀

4.订单详情

## 系统压测

1.JMeter工具

2.自定义变量模拟多用户

3.压测商品列表和秒杀

## 优化思路步骤

①缓存页面，将整个页面内容缓存进入redis，但是这样的话每次传输其实还是传输了整个页面。

②页面静态化，优化商品详情页和订单页，因为这两个页面只有一个商品，所以将页面设置为静态的，使用ajax发送请求，然后用jquery设置页面内容。

③将秒杀的商品以及库存放入redis中，先判断redis中的库存是否大于0，如果是就对redis的库存减1。

④当redis中的库存小于0是，后续用户还会查询redis，我们可以用一个HashMap记录库存，对于哈希表中库存小于等于0的，直接返回错误；

⑤将下订单的请求放入RabbitMQ中，让消费者处理，前端用户轮询，达到削峰效果

⑥将使用redis查库存减库存的操作放入lua脚本中，可以保证原子性。

⑦为了防止黄牛获取接口之后使用脚本调用，在秒杀之前，我们先让用户获取接口，在接口中加入UUID并绑定用户和秒杀的商品id，放入redis中。然后用户使用获取的专属接口去秒杀。

⑧为了防止秒杀刚开始请求过多，我们加入验证码，输入正确的验证码后才可以获取接口然后秒杀

⑨使用redis记录用户访问的url，记录30s访问次数，大于5次就提示请求频繁。然后将限流使用注解实现。

## 解决超卖思路

①首先查询数据库判断商品库存是否小于等于0，小于等于0就返回失败；

②尝试从redis中获取用户相关的订单，key为`order:+用户id+":"+商品id`，如果redis中不存在，就开始秒杀。

③秒杀时的减库存操作实则时高并发操作，使用sql语句进行减库存