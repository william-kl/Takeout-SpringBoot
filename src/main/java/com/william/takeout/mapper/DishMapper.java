package com.william.takeout.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.william.takeout.entity.Dish;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DishMapper extends BaseMapper<Dish> {
}
