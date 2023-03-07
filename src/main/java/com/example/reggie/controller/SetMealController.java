package com.example.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.reggie.common.R;
import com.example.reggie.dto.SetmealDto;
import com.example.reggie.entity.Category;
import com.example.reggie.entity.Setmeal;
import com.example.reggie.entity.SetmealDish;
import com.example.reggie.service.CategoryService;
import com.example.reggie.service.SetMealDishService;
import com.example.reggie.service.SetmealService;
import com.example.reggie.service.serviceImpl.SetMealDishServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/setmeal")
@Slf4j
public class SetMealController {

    @Autowired
    private SetMealDishService setMealDishService;

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private CategoryService categoryService;

    /**
     * 通过分类id查询对应菜品
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<SetmealDto> getSetmealById(@PathVariable String id){
        SetmealDto setmealDto = new SetmealDto();

        // 查询套餐基本数据（单表）
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Setmeal::getCategoryId, id);
        Setmeal setmeal = setmealService.getById(id);

        // 获取套餐的分类名称
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getId, setmeal.getCategoryId());
        Category category = categoryService.getOne(wrapper);
        String categoryName = category.getName();

        // 获取套餐的对应菜品数据
        LambdaQueryWrapper<SetmealDish> dishesWrapper = new LambdaQueryWrapper<>();
        dishesWrapper.eq(SetmealDish::getSetmealId, setmeal.getId());
        List<SetmealDish> dishes = setMealDishService.list(dishesWrapper);

        // 合并成DishDto
        BeanUtils.copyProperties(setmeal, setmealDto);
        setmealDto.setCategoryName(categoryName);
        setmealDto.setSetmealDishes(dishes);

        return R.success(setmealDto);
    }

    /**
     * 分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name){
        // 分页构造器
        Page<Setmeal> pageInfo = new Page<>(page, pageSize);
        Page<SetmealDto> setmealDtoPage = new Page();

        // 条件构造器
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(name!=null, Setmeal::getName, name);
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        // 调用服务
        setmealService.page(pageInfo, queryWrapper);

        // 复制结果到结果对象中， records另外处理
        BeanUtils.copyProperties(pageInfo, setmealDtoPage, "records");
        // 单独处理
        List<Setmeal> records = pageInfo.getRecords();
        // 遍历处理，获取每条records的categoryId，调用服务查询categoryName，并创建和赋值新的SetmealDto对象替换原有的Setmeal(records)
        List<SetmealDto> list = records.stream().map((item) -> {
            SetmealDto setmealDto = new SetmealDto();
            BeanUtils.copyProperties(item, setmealDto);
            Long categoryId = item.getCategoryId();
            Category category = categoryService.getById(categoryId);
            if(category!=null){
                String categoryName = category.getName();
                setmealDto.setCategoryName(categoryName);
            }
            return setmealDto;
        }).collect(Collectors.toList());

        setmealDtoPage.setRecords(list);

        return R.success(setmealDtoPage);
    }

    /**
     * 保存套餐
     * @param setmealDto
     * @return
     */
    @PostMapping
    @CacheEvict(value = "setMealCache", allEntries = true)
    public R<String> add(@RequestBody SetmealDto setmealDto){
        setmealService.saveWithDish(setmealDto);

        return R.success("套餐保存成功");
    }

    /**
     * 根据id批量删除套餐
     * @param ids
     * @return
     */
    @DeleteMapping
    @CacheEvict(value = "setMealCache", allEntries = true)  //一但进行删除操作，清除所有缓存
    public R<String> delete(@RequestParam List<Long> ids){
        setmealService.deleteByIds(ids);

        return R.success("批量删除成功");
    }

    /**
     * 根据id修改套餐状态
     * @param status
     * @param ids
     * @return
     */
    @PostMapping("/status/{status}")
    public R<String> changeStatus(@PathVariable String status, @RequestParam List<Long> ids){
        // 条件构造器
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Setmeal::getId, ids);

        // 查询套餐
        List<Setmeal> list = setmealService.list(queryWrapper);

        // 修改状态
        list.stream().map((item) -> {
            item.setStatus(Integer.parseInt(status));
            return item;
        }).collect(Collectors.toList());

        setmealService.updateBatchById(list);

        return R.success("状态修改成功");
    }

    /**
     * 根据条件查询套餐数据
     * @param setmeal
     * @return
     */

    @GetMapping("/list")
    @Cacheable(value = "setMealCache", key = "#setmeal.categoryId + ' ' + #setmeal.status")
    public R<List<Setmeal>> list(Setmeal setmeal){

        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Setmeal::getCategoryId, setmeal.getCategoryId());
        queryWrapper.eq(Setmeal::getStatus, setmeal.getStatus());
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        List<Setmeal> list = setmealService.list(queryWrapper);

        return R.success(list);
    }

    /**
     * 更新套餐信息
     * （更新了套餐表和套餐菜品表）
     * 有多个更新操作，开启事务管理
     * @param setmealDto
     * @return
     */
    @PutMapping
    @Transactional
    @CacheEvict(value = "setMealCache", allEntries = true)
    public R<String> update(@RequestBody SetmealDto setmealDto) {
        // 更新未完成
        // 更新setMeal表
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDto, setmeal);
        setmealService.updateById(setmeal);
        // 更新setMealDish表
        Long setmealId = setmeal.getId();
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setSetmealId(setmealId);
            Long dishId = setmealDish.getDishId();
            // 根据菜品id和套餐id查找
            LambdaQueryWrapper<SetmealDish> setmealDishQueryWrapper = new LambdaQueryWrapper<>();
            setmealDishQueryWrapper.eq(SetmealDish::getDishId, dishId);
            setmealDishQueryWrapper.eq(SetmealDish::getSetmealId, setmealId);
            SetmealDish dish = setMealDishService.getOne(setmealDishQueryWrapper);

            if (dish != null) {
                // 在setMealDish中可以查询到对应套餐有该菜品的话，更新即可
                setmealDish.setId(dish.getId());
                setMealDishService.updateById(setmealDish);
            } else {
                // 否则进行添加操作
                setMealDishService.save(setmealDish);
            }
        }

        setMealDishService.updateBatchById(setmealDishes);

        return R.success("更新成功");
    }
}
