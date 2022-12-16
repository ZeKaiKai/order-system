package com.example.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.reggie.dto.SetmealDto;
import com.example.reggie.entity.Setmeal;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {

    /**
     * 保存套餐，需要同时保存setMeal和setMeal_dish两张表
     * @param setmealDto
     */
    public void saveWithDish(SetmealDto setmealDto);

    /**
     * 根据ids删除套餐，需要同时删除setMeal和setMeal_dish两张表
     * @param ids
     */
    public void deleteByIds(List<Long> ids);
}
