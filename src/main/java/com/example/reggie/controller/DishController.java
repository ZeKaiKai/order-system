package com.example.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.reggie.common.R;
import com.example.reggie.dto.DishDto;
import com.example.reggie.entity.Category;
import com.example.reggie.entity.Dish;
import com.example.reggie.entity.DishFlavor;
import com.example.reggie.service.CategoryService;
import com.example.reggie.service.DishFlavorService;
import com.example.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {
    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private DishService dishService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     *
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto) {
        log.info(dishDto.toString());

        dishService.saveWithFlavor(dishDto);

        // 数据库发生改变时，清除对应分类的缓存
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);

        return R.success("新增菜品成功");
    }

    /**
     * 菜品信息分页查询
     *
     * @param page
     * @param pageSize
     *
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name) {
        // 分页构造器
        Page<Dish> pageInfo = new Page<>(page, pageSize);
        Page<DishDto> dishDtoPage = new Page<>();

        // 条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Dish::getUpdateTime);

        // 执行分页查询
        dishService.page(pageInfo, queryWrapper);

        // Page<Dish>拷贝到Page<DishDto>，records另外处理
        BeanUtils.copyProperties(pageInfo, dishDtoPage, "records");
        // 提取出records
        List<Dish> records = pageInfo.getRecords();
        // 遍历，先records中的属性都拷贝到DishDto中，再根据categoryId查询出CategoryName，设置进dishDto中
        List<DishDto> list = records.stream().map((item) -> {

            DishDto dishDto = new DishDto();

            BeanUtils.copyProperties(item, dishDto);

            String categoryName = categoryService.getById(item.getCategoryId()).getName();

            dishDto.setCategoryName(categoryName);

            return dishDto;
        }).collect(Collectors.toList());

        // records也设置进Page<dishDto>
        dishDtoPage.setRecords(list);

        return R.success(dishDtoPage);
    }

    /**
     * 根据id查询菜品信息和对应口味
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable Long id) {
        DishDto dishDto = dishService.getByIdWithFlavor(id);

        return R.success(dishDto);
    }

    /**
     * 更新菜品
     *
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto) {
        dishService.updateWithFlavor(dishDto);

        // 更新数据库时删除redis中的对应缓存
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);

        return R.success("更新菜品成功");
    }

    /**
     * 根据条件查询对应菜品数据
     *
     * @param dish
     * @return
     */
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish) {
        // 创建返回前端的Dto对象
        List<DishDto> dishDtoList = null;

        // 构造该分类在redis中缓存的key
        String key = "dish_" + dish.getCategoryId() + "_" + dish.getStatus();
        // 尝试从redis中获取缓存数据
        dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);

        if (dishDtoList != null) {
            // 如果redis中存在数据，则无需查询数据库，直接返回
        } else {
            // 如果redis中没有缓存数据，从数据库中查询
            // 构造查询条件
            LambdaQueryWrapper<Dish> dishWrapper = new LambdaQueryWrapper<>();
            dishWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
            dishWrapper.eq(Dish::getStatus, 1); // 确定菜品是否起售
            dishWrapper.orderByAsc(Dish::getSort);

            List<Dish> list = dishService.list(dishWrapper);

            // 处理口味信息
            dishDtoList = list.stream().map((item) -> {
                DishDto dishDto = new DishDto();
                BeanUtils.copyProperties(item, dishDto);
                // 查询口味信息
                LambdaQueryWrapper<DishFlavor> flavorWrapper = new LambdaQueryWrapper<>();
                flavorWrapper.eq(DishFlavor::getDishId, item.getId());
                List<DishFlavor> flavors = dishFlavorService.list(flavorWrapper);
                // 设置口味信息
                dishDto.setFlavors(flavors);

                return dishDto;
            }).collect(Collectors.toList());

            // 将数据缓存到redis中
            redisTemplate.opsForValue().set(key, dishDtoList);
            // 返回结果
        }
        return R.success(dishDtoList);

    }
}
