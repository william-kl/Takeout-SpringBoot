package com.william.takeout.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.william.takeout.common.Result;
import com.william.takeout.dto.SetmealDto;
import com.william.takeout.entity.Category;
import com.william.takeout.entity.Setmeal;
import com.william.takeout.service.CategoryService;
import com.william.takeout.service.SetmealDishService;
import com.william.takeout.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;



@RestController
@RequestMapping("/setmeal")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private CategoryService categoryService;

    /**
     * 新增套餐（setmeal），其实就是将新增页面录入的套餐信息插入到setmeal表，
     * 还需要向setmeal_dish表插入套餐和菜品关联数据(一个套餐包含几个产品)
     * 例如：湘菜套餐：一个辣子鸡（dish里），一个米饭（dish里），两个王老吉（dish里）
     * setmeal 套餐表
     * setmeal_dish 套餐菜品关系表
     *
     * 新增套餐时前端页面和客户端的交互过程：
     * 1.页面(backend/page/combo/add.html)发送ajax请求，请求服务端获取套餐分类数据并展示到下拉框中（CategoryControler list方法，参数为2获取套餐）
     * 2.页面发送ajax请求，请求服务端获取菜品分类数据并展示到添加菜品窗口的左侧（CategoryControler list方法，参数为1获取菜品）
     * 3.页面发送ajax请求，请求服务端，根据菜品分类查询对应的菜品数据并展示到添加菜品窗口中（点添加菜品，先展示菜品，DishController中完成）
     * 4.页面发送请求进行图片上传，请求服务端将图片保存到服务器
     * 5.页面发送请求进行图片下载，将上传的图片进行回显
     * 6.点击保存按钮，发送ajax请求，将套餐相关数据以json形式提交到服务端
     *
     * 开发新增套餐功能，其实就是在服务端编写代码去处理前端页面发送的这6次请求即可
     */

    // 当前端传输过来的 JSON数据 与 对应实体类 Setmeal中属性有所不同时，可以使用SetmealDto，
    //   SetmealDto 继承Setmeal，并添加 Setmeal中没有的JSON数据
    @PostMapping
    @CacheEvict(value = "setmealCache",allEntries = true)
    public Result<String> save(@RequestBody SetmealDto setmealDto){

        log.info("套餐信息:{}",setmealDto);
        setmealService.saveWithDish(setmealDto);
        return Result.success("套餐添加 成功！");
    }


    /**
     * 套餐分页查询
     * 1.页面发送ajax请求，将分页查询参数(page,pagesize.name)提交到服务端，获取分页数据
     * 2.页面发送请求，请求服务端进行图片下载，用于页面图片展示
     *
     * 开发套餐信息分页查询功能，其实就是在服务端编写代码去处理前端页面发送的两次请求即可
     */
    @GetMapping("/page")
    public Result<Page> showPage(int page,int pageSize,String name){

        Page<Setmeal> setmealPage = new Page<>(page,pageSize);

        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        // 根据name 进行 like模糊查询
        queryWrapper.like(name != null,Setmeal::getName,name);
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        setmealService.page(setmealPage,queryWrapper);
        //return result.success(setmealPage)如果直接这么写，页面“套餐分类”不会显示
        //因为setmealPage里存的泛型是Setmeal
        //要传一个SetmealDto,里面新建一个categoryName，页面就需要这个categoryName
        //然后这个name通过categoryId查出来，再赋值给SetmealDto


        Page<SetmealDto> dtoPage = new Page<>(page,pageSize);
        //分页查询的结果拷贝到dtoPage
        BeanUtils.copyProperties(setmealPage,dtoPage,"records");

        List<Setmeal> records = setmealPage.getRecords();

        List<SetmealDto> newRecords = records.stream().map((record) -> {
            SetmealDto setmealDto = new SetmealDto();
            //对象拷贝
            BeanUtils.copyProperties(record,setmealDto);
            //分类id
            Long categoryId = record.getCategoryId();
            // 根据分类id查询 分类对象
            Category category = categoryService.getById(categoryId);
            if (category != null) {
                String categoryName = category.getName();
                setmealDto.setCategoryName(categoryName);
            }
            return setmealDto;
        }).collect(Collectors.toList());

        dtoPage.setRecords(newRecords);

        return Result.success(dtoPage);
    }

    @DeleteMapping
    @CacheEvict(value = "setmealCache",allEntries = true)   //  删除套餐，就要删除套餐相关的所有缓存数据
    public Result<String> delete(@RequestParam List<Long> ids){

        log.info("ids = " + ids);

        //setmealService.removeWithDish(ids);

        return Result.success("成功删除套餐！");
    }
    // 前端发送的请求：http://localhost:8181/setmeal/list?categoryId=1516353794261180417&status=1
    // 注意: 请求后的参数 是以key-value键值对的方式 传入，而非JSON格式，不需要使用@RequestBody 来标注，
    //   只需要用包含 参数(key)的实体对象接收即可
    @GetMapping("/list")  // 在消费者端 展示套餐信息
    @Cacheable(value = "setmealCache",key = "#setmeal.categoryId+'_' +#setmeal.status")
    public Result<List<Setmeal>> list(Setmeal setmeal){
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        Long categoryId = setmeal.getCategoryId();
        Integer status = setmeal.getStatus();
        queryWrapper.eq(categoryId != null,Setmeal::getCategoryId,categoryId);
        queryWrapper.eq(status != null,Setmeal::getStatus,status);

        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        List<Setmeal> setmeals = setmealService.list(queryWrapper);

        return Result.success(setmeals);
    }

    // http://localhost:8181/setmeal/status/0?ids=1415580119015145474
    @PostMapping("/status/{status}")
    public Result<String> updateStatus(@PathVariable("status") Integer status,@RequestParam("ids") List<Long> ids){

        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(ids != null,Setmeal::getId,ids);

        List<Setmeal> list = setmealService.list(queryWrapper);
        if (list != null){
            for (Setmeal setmeal : list) {
                setmeal.setStatus(status);
                setmealService.updateById(setmeal);
            }
            return Result.success("套餐状态修改成功！");
        }

        return Result.error("套餐状态不能修改,请联系管理或客服！");
    }

    // http://localhost:8181/setmeal/1516369910723248130
    @GetMapping("/{id}")
    public Result<SetmealDto> getSetmel(@PathVariable("id") Long id){
        //SetmealDto setmealDto = setmealService.getSetmealData(id);
        //return Result.success(setmealDto);
        return null;
    }

    @PutMapping
    public Result<String> updateMeal(@RequestBody SetmealDto setmealDto){
        setmealService.updateById(setmealDto);
        return Result.success("套餐修改成功！");
    }


    //套餐的批量删除
//    @DeleteMapping
//    public Result<String> batchDelete(@RequestParam List<Long> ids){
//        setmealService.batchDeleteByIds(ids);
//        return Result.success("套餐删除成功!");
//    }



}
