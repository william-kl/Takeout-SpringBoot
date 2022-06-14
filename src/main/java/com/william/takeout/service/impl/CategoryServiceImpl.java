package com.william.takeout.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.william.takeout.common.MyCustomException;
import com.william.takeout.entity.Category;
import com.william.takeout.entity.Dish;
import com.william.takeout.entity.Setmeal;
import com.william.takeout.mapper.CategoryMapper;
import com.william.takeout.service.CategoryService;
import com.william.takeout.service.DishService;
import com.william.takeout.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper,Category> implements CategoryService {
    @Autowired
    private DishService dishService;//查菜品表，要用到

    @Autowired
    private SetmealService setmealService;//查套餐表，要用到

    /**
     * 根据id删除分类，删除之前需要进行判断
     * 扩展自己的方法：判断当前的分类是否关联了菜品或者套餐，如果没有关联，才能删除
     * @param id
     */
    @Override
    public void remove(Long id) {
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();

        //添加查询条件，根据分类id进行查询
        dishLambdaQueryWrapper.eq(Dish::getCategoryId,id);
        int count1 = dishService.count(dishLambdaQueryWrapper);
        //查询当前分类是否关联了菜品，如果已经关联，抛出一个业务异常
        if (count1 > 0) {//关联的菜品数 > 0
            throw new MyCustomException("当前分类下关联了菜品，不能删除");//common包下自定义的异常
        }
        //查询当前分类是否关联了套餐，如果已经关联，抛出一个业务异常
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealLambdaQueryWrapper.eq(Setmeal::getCategoryId,id);
        int count2 = setmealService.count(setmealLambdaQueryWrapper);
        if (count2 > 0) {
            throw new MyCustomException("当前分类下关联了套餐，不能删除");
        }

        //正常删除分类
        super.removeById(id);
    }
}
