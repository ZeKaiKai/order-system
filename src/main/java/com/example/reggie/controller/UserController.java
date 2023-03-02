package com.example.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.reggie.common.CustomException;
import com.example.reggie.common.R;
import com.example.reggie.entity.User;
import com.example.reggie.service.UserService;
import com.example.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 发送验证码
     * @param map
     * @param session
     * @return
     */
    @PostMapping("/checkCode")
    public R<String> sendMsg(@RequestBody Map map, HttpSession session) {
        // 获取手机号
        String phone = map.get("phone").toString();

        if (StringUtils.isNotEmpty(phone)) {
            //生成随机的四位验证码
            String code = ValidateCodeUtils.generateValidateCode(4).toString();
            log.info("Check code={}", code);

            // 处于成本考虑，这里没有实现
            // 调用短信服务API完成发送短信
            //Utils.sendMessage("番茄外卖",  "", phone, code);

            // 改为存入redis
            // 将代码存入session方便校验
            //session.setAttribute(phone, code);

            //将验证码缓存入redis中，设置有效期为5分钟
            redisTemplate.opsForValue().set(phone, code, 5, TimeUnit.MINUTES);

            return R.success("手机验证码发送成功");
        }
        return R.success("验证码短信发送失败");
    }

    /**
     * 登录
     * 旧用户登录，新用户自动注册
     * @param map
     * @return
     */
    @PostMapping("/login")
    public R<User> login(@RequestBody Map map, HttpServletRequest request){
        log.info("登录 {}", map);

        // 获取手机号
        String phone = map.get("phone").toString();

        // 获取验证码
        String code = map.get("code").toString();

        // 从redis中获取缓存的验证码
        Object codeInRedis = redisTemplate.opsForValue().get(phone);

        // 进行页面提交验证码和预先缓存验证码的比对
        if (codeInRedis != null && codeInRedis.equals(code)) {
            // 验证码比对成功，用电话号码从数据库中查询用户
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone, phone);
            User user = userService.getOne(queryWrapper);
            // 非空，登录成功
            if(user != null){
                // 更新用户状态为1，并存入数据库
                user.setStatus(1);
                userService.updateById(user);
                // 用户信息存入session，保持登录状态
                request.getSession().setAttribute("user", user);
                // 删除redis中缓存的验证码
                redisTemplate.delete(phone);
                return R.success(user);
            }
        }
        // 否则登录失败
        return R.error("登录失败");
    }

//    @PostMapping("/loginout")
//    public R<String> logout(){
//
//    }

}
