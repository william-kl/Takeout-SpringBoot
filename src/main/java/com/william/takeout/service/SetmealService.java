package com.william.takeout.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.william.takeout.dto.SetmealDto;
import com.william.takeout.entity.Setmeal;

public interface SetmealService extends IService<Setmeal> {
    /**
     * add setmeal，同时保存setmeal和dish的关联关系
     * @param setmealDto
     */
    public void saveWithDish(SetmealDto setmealDto);
}
