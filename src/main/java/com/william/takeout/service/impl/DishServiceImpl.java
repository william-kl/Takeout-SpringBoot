package com.william.takeout.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.william.takeout.dto.DishDto;
import com.william.takeout.entity.Dish;
import com.william.takeout.entity.DishFlavor;
import com.william.takeout.mapper.DishMapper;
import com.william.takeout.service.DishFlavorService;
import com.william.takeout.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private DishFlavorService dishFlavorService;
    @Override
    @Transactional //加入了事务管理，保证事务的一致性，因为操作了两张表
    public void saveWithFlavor(DishDto dishDto) {//这里dishDto里只封装了dishFlavor的name和value，没有dishId
        //保存菜品到dish表
        super.save(dishDto);

        Long dishId = dishDto.getId();//保存之后就有ID了

        List<DishFlavor> flavors = dishDto.getFlavors();
        flavors = flavors.stream().map((dishFlavor) -> {//把每一个DishFlavor拿出来并付ID
            dishFlavor.setDishId(dishId);
            return dishFlavor;
        }).collect(Collectors.toList());

        //保存菜品到dishFlavor口味表
        dishFlavorService.saveBatch(flavors);//批量保存集合
    }
}
