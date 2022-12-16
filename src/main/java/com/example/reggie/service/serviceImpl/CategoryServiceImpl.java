package com.example.reggie.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.reggie.common.CustomException;
import com.example.reggie.entity.Category;
import com.example.reggie.entity.Dish;
import com.example.reggie.entity.Setmeal;
import com.example.reggie.mapper.CategoryMapper;
import com.example.reggie.service.CategoryService;
import com.example.reggie.service.DishService;
import com.example.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private DishService dishService;

    @Autowired
    private SetmealService setmealService;

    @Override
    public void remove(Long id) {
        // 构造查询条件，根据分类id查询菜品个数
        LambdaQueryWrapper<Dish> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.eq(Dish::getCategoryId, id);
        int count = dishService.count(queryWrapper1);
        // 如果关联了菜品，抛出一个业务异常
        if(count > 0){
            throw new CustomException("当前分类下关联了菜品，不能删除");
        }
        // 构造查询条件，根据分类id查询套餐个数
        LambdaQueryWrapper<Setmeal> queryWrapper2 = new LambdaQueryWrapper<>();
        queryWrapper2.eq(Setmeal::getCategoryId, id);
        int count1 = setmealService.count(queryWrapper2);
        //如果关联了套餐，抛出一个业务异常
        if(count1 > 0){
            throw new CustomException("当前分类下关联了套餐，不能删除");
        }
        // 没有关联菜品和套餐，正常删除
        super.removeById(id);
    }
}
