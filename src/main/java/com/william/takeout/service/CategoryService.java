package com.william.takeout.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.william.takeout.entity.Category;

public interface CategoryService extends IService<Category> {
    //扩展自己的方法：判断当前的分类是否关联了菜品或者套餐，如果没有关联，才能删除
    public void remove(Long id);
}
