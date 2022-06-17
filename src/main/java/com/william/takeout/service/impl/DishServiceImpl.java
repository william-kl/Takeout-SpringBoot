package com.william.takeout.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.william.takeout.dto.DishDto;
import com.william.takeout.entity.Dish;
import com.william.takeout.entity.DishFlavor;
import com.william.takeout.mapper.DishMapper;
import com.william.takeout.service.DishFlavorService;
import com.william.takeout.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
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


    //根据id查询菜品信息和对应的口味信息
    @Override
    public DishDto getByIdWithFlavor(Long id) {
        //查询dish基本信息
        Dish dish = this.getById(id);
        //基本dish信息复制过去
        DishDto dishDto = new DishDto();
        BeanUtils.copyProperties(dish,dishDto);

        //查询口味信息，从dish_flavor查询
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId,dish.getId());
        List<DishFlavor> flavors = dishFlavorService.list(queryWrapper);
        //基本口味flavor信息复制过去
        dishDto.setFlavors(flavors);
        return dishDto;
    }

    @Override
    /**
     * 要更新两个表：dish and dish_flavor
     */
    @Transactional
    public void updateWithFlavor(DishDto dishDto) {
       //更新dish表
        this.updateById(dishDto);//本来应为dish, 但是dishDto是dish的子类，也可以；
                                 //这样的话就会更新dishDto里应有的字段

       //更新dish_flavor表：1.清理当前口味数据---dish_flavor表的delete操作
        //delete from dish_flavor where dish_id = ???
        LambdaQueryWrapper <DishFlavor> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.eq(DishFlavor::getDishId,dishDto.getId());
        dishFlavorService.remove(queryWrapper);

        // 2. dish_flavor表的insert操作:添加当前提交过来的口味数据
        List<DishFlavor> flavors = dishDto.getFlavors();

        flavors = flavors.stream().map((item) -> {//跟add一样的操作，每个DishFlavor都给一个dishId
            item.setDishId(dishDto.getId());
            return item;
        }).collect(Collectors.toList());
        dishFlavorService.saveBatch(flavors);
    }
}
