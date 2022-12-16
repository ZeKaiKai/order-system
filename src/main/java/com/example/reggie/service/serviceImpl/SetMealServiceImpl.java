package com.example.reggie.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.reggie.common.CustomException;
import com.example.reggie.dto.SetmealDto;
import com.example.reggie.entity.Setmeal;
import com.example.reggie.entity.SetmealDish;
import com.example.reggie.mapper.SetmealMapper;
import com.example.reggie.service.SetMealDishService;
import com.example.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SetMealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    @Autowired
    private SetMealDishService setMealDishService;

    @Autowired
    private SetmealService setmealService;

    /**
     * 保存套餐，需要同时保存setMeal和setMeal_dish两张表
     * @param setmealDto
     */
    @Override
    @Transactional
    public void saveWithDish(SetmealDto setmealDto){
        // 保存套餐的基本信息到setmeal
        this.save(setmealDto);

        // 处理空缺的值
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        setmealDishes.stream().map((item) -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());

        // 保存套餐和菜品的关联信息到setmeal_dish
        setMealDishService.saveBatch(setmealDishes);
    }

    /**
     * 根据ids删除套餐，需要同时删除setMeal和setMeal_dish两张表
     * @param ids
     */
    @Override
    @Transactional
    public void deleteByIds(List<Long> ids) {
        // 通过id查询该菜品是否在售
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Setmeal::getId, ids);
        queryWrapper.eq(Setmeal::getStatus, 1);
        int count = setmealService.count(queryWrapper);
        // 如果存在售卖中的商品，则抛出自定义异常
        if (count > 0){
            throw new CustomException("商品正在售卖，无法删除");
        }

        // 可以删除，则先删除套餐表中的内容
        LambdaQueryWrapper<Setmeal> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.in(Setmeal::getId, ids);
        setmealService.remove(queryWrapper1);

        // 然后删除套餐和菜品联系表中的内容
        LambdaQueryWrapper<SetmealDish> queryWrapper2 = new LambdaQueryWrapper<>();
        queryWrapper2.in(SetmealDish::getSetmealId, ids);
        setMealDishService.remove(queryWrapper2);
    }
}
