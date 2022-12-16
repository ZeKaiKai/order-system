package com.example.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.reggie.common.BaseContext;
import com.example.reggie.common.R;
import com.example.reggie.entity.ShoppingCart;
import com.example.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shoppingCart")
@Slf4j
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 添加购物车
     * @param shoppingCart
     * @return
     */
    @PostMapping("/add")
    public R<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart){
        log.info("购物车 {}", shoppingCart);
        // 设置用户id
        shoppingCart.setUserId(BaseContext.getCurrentId());

        // 先从购物车表中查询是否有该菜品/套餐
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getDishId, shoppingCart.getDishId());
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
        ShoppingCart one = shoppingCartService.getOne(queryWrapper);

        // 有则数量+1即可,没有则插入数据
        if (one != null){
            one.setNumber(one.getNumber() + 1);
            shoppingCartService.update(one, queryWrapper);

            return R.success(one);
        }else{
            shoppingCartService.save(shoppingCart);

            return R.success(shoppingCart);
        }
    }

    /**
     * 查看购物车
     * @return
     */
    @GetMapping("/list")
    public R<List<ShoppingCart>> list(){
        Long currentId = BaseContext.getCurrentId();
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, currentId);
        queryWrapper.orderByDesc(ShoppingCart::getCreateTime);
        List<ShoppingCart> list = shoppingCartService.list(queryWrapper);

        return R.success(list);
    }

    /**
     * 清空购物车
     * @return
     */
    @DeleteMapping("/clean")
    public R<String> delete(){
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());

        shoppingCartService.remove(queryWrapper);

        return R.success("清空成功");
    }

    /**
     * 购物车减少菜品或套餐
     * @param shoppingCart
     * @return
     */
    @PostMapping("/sub")
    public R<ShoppingCart> sub(@RequestBody ShoppingCart shoppingCart){

        // 条件构造器，条件为当前用户id和菜品id
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
        // 可能是对菜品操作，也可能是对套餐操作
        if(shoppingCart.getDishId() != null){
            queryWrapper.eq(ShoppingCart::getDishId, shoppingCart.getDishId());
        }else{
            queryWrapper.eq(ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        }

        // 查询到的菜品
        ShoppingCart one = shoppingCartService.getOne(queryWrapper);
        // 数量减一
        int count = one.getNumber();
        if(count > 1){
            one.setNumber(one.getNumber()-1);
            shoppingCartService.update(one, queryWrapper);
        }else{
            shoppingCartService.remove(queryWrapper);
        }

        return R.success(one);
    }
}
