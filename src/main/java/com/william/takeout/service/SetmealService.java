package com.william.takeout.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.william.takeout.dto.SetmealDto;
import com.william.takeout.entity.Setmeal;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {
    /**
     * add setmeal，同时保存setmeal和dish的关联关系
     * @param setmealDto
     */
    public void saveWithDish(SetmealDto setmealDto);

    /**
     * delete setmeal，同时删除setmeal和dish的关联关系；操作两张表
     * @param setmealDto
     */
    public void removeWithDish(List<Long> ids);
}
