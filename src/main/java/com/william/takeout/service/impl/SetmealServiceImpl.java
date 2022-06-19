package com.william.takeout.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.william.takeout.dto.SetmealDto;
import com.william.takeout.entity.Setmeal;
import com.william.takeout.entity.SetmealDish;
import com.william.takeout.mapper.SetmealMapper;
import com.william.takeout.service.SetmealDishService;
import com.william.takeout.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper,Setmeal> implements SetmealService {

    @Autowired
    private SetmealDishService setmealDishService;
    /**
     * add setmeal，同时保存setmeal和dish的关联关系
     * @param setmealDto
     */
    @Override
    @Transactional
    public void saveWithDish(SetmealDto setmealDto) {
        //保存套餐的基本信息，操作setmeal,执行insert操作
        this.save(setmealDto);

        //保存setmeal和dish的关联关系
        //List<SetmealDish>里setmealDish的setmealId是没有值的
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();

        //this.save之后，setmealDto里id就赋值了
        setmealDishes.stream().map((item) -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());

        setmealDishService.saveBatch(setmealDishes);
    }
}
