package com.william.takeout.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.william.takeout.common.Result;
import com.william.takeout.entity.Category;
import com.william.takeout.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
@Slf4j
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    // @RequestBody: 将前端回传的JSON数据需要使用@RequestBody 转化为 实体对象
    @PostMapping
    //前端只用到code，所以这里返回一个String
    //新增分类
    public Result<String> save(@RequestBody Category category){
        log.info("category:{}",category);

        categoryService.save(category);
        return Result.success("成功新增分类！");

    }

    @GetMapping("/page")
    public Result<Page> showPage(int page,int pageSize){
        //分页构造器
        Page<Category> pageInfo = new Page<>(page,pageSize);

        //条件构造器
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();

        //添加排序条件：根据 Category对象的sort字段来排序
        queryWrapper.orderByAsc(Category::getSort);

        //进行分页查询
        categoryService.page(pageInfo,queryWrapper);

        return Result.success(pageInfo);
    }

    //  删除分类:在分类管理列表页面中，
    //  需要注意: 当分类关联了菜品或者套餐时(需要引入Dish、Setmeal对应的实体类、Mapper、Service)，此分类不许删除
    // 根据分类id 来删除分类
    @DeleteMapping
    public Result<String> delete(Long ids){//category?ids=...的格式传过来，因此不用加@PathVariable
        log.info("删除分类，分类id为: {}",ids);

        //categoryService.removeById(ids);
        //调用我们自己写的remove方法，实现删除逻辑
        categoryService.remove(ids);

        return Result.success("成功删除分类信息！");
    }

    @PutMapping
    public Result<String> update(@RequestBody Category category){
        log.info("修改分类信息:{}",category);
        //更新，自动填充了updateTime, updateUser by MyMetaObjectHandler
        categoryService.updateById(category);
        return Result.success("分类信息 修改成功！");
    }

    // 根据条件查询分类数据
    @GetMapping("/list")
    public Result<List<Category>> categoryList(Category category){
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        //  只有当 category.getType()不为空，才会比较 前端传入的category的type和 实体类中 type属性是否相等
        queryWrapper.eq(category.getType() != null, Category::getType,category.getType());

        queryWrapper.orderByAsc(Category::getSort).orderByDesc(Category::getUpdateTime);

        List<Category> list = categoryService.list(queryWrapper);

        return Result.success(list);
    }
    // 前端传输到服务端的数据 和实体类中的属性 不是一一对应关系，
    // 需要用到DTO(Data Transfer Object)对象，即数据传输对象，一般用于Controller和Service层之间的数据传输



}
