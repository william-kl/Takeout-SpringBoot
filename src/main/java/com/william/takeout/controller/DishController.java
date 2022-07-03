package com.william.takeout.controller;
/**
 *  新增菜品:
 *  1、页面发送Ajax请求(点击新建菜品)，请求服务端获取菜品分类列表 并展示到下拉框中（在CategoryController里取编写）
 *  2、页面发送请求进行图片上传，请求服务端将图片 保存到服务器（CommonController）
 *  3、页面发送请求 进行图片下载，将上传的图片进行回显（CommonController）
 *  4、点击保存按钮，发送Ajax请求，将菜品相关数据 以JSON形式提交到服务器（DishController）
 *
 *  开发新增菜品功能，其实就是在服务端编写代码去处理前端页面发来的这4次请求即可
 */


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.william.takeout.common.Result;
import com.william.takeout.dto.DishDto;
import com.william.takeout.entity.Category;
import com.william.takeout.entity.Dish;
import com.william.takeout.entity.DishFlavor;
import com.william.takeout.service.CategoryService;
import com.william.takeout.service.DishFlavorService;
import com.william.takeout.service.DishService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dish")
@Slf4j
@Api(tags = "dish management")
public class DishController {
    @Autowired
    private DataSource dataSource;

    @Autowired
    private DishService dishService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private CategoryService categoryService;


    /**
     * 在高并发的情况下，频繁查询数据库会导致系统性能下降，服务端响应时间增长。需要对方法进行缓存优化，提高系统的性能
     * 具体的实现思路如下：
     * 1.改造DishController的list方法，先从Redis中获取菜品数据，如果有则直接返回，无需查询数据库；
     * 如果没有则查询数据库，并将查询到的菜品数据放入Redis
     * 2.改造DishController的save和update方法，加入清理缓存的逻辑；否则造成缓存和数据库的不一致（脏数据）
     * 注意！在使用缓存过程中，要注意保证数据库中的数据和缓存中的数据一致，如果数据库中的数据发生变化，需要及时清理缓存数据
     */
    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping
    @ApiOperation(httpMethod = "POST", value = "add dish: insert into table dish and table dish_flavor")
    public Result<String> save(@RequestBody DishDto dishDto){
        log.info(dishDto.toString());
        /**
         * 要同时向Dish和DishFlavor两张表中插入数据，我们需要自己扩展方法
         */
        dishService.saveWithFlavor(dishDto);

        //清理所有菜品的缓存数据
        //Set keys = redisTemplate.keys("dish_*");
        //redisTemplate.delete(keys);

        //只清理具体菜品（湘菜）缓存数据
        String key = "dish_" + dishDto.getCategoryId() + "_" + dishDto.getStatus();
        redisTemplate.delete(key);

        return Result.success("新增菜品操作成功！");
    }

    //  分页展示菜品信息
    // dish/page?page=1&pageSize=10&name=122334,name 是搜索框中的输入值
    @GetMapping("/page")
    @ApiOperation(httpMethod = "GET", value = "pagination for dish")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page", value = "current page", required = true),
            @ApiImplicitParam(name = "pageSize", value = "total numbers of pages", required = true),
            @ApiImplicitParam(name = "name", value = "dish name", required = false)
    })
    public Result<Page> pageShow(int page,int pageSize,String name){

        Page<Dish> dishPage = new Page<>(page,pageSize);
        Page<DishDto> dtoPage = new Page<>();

        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        // 添加过滤条件(根据name来做模糊查询)
        queryWrapper.like(name != null,Dish::getName,name);

        // 添加排序条件(按更新时间降序排列)
        queryWrapper.orderByDesc(Dish::getUpdateTime);

        //  执行分页查询
        dishService.page(dishPage,queryWrapper);
        /** 用dto的原因
         * 如果只return一个dishPage给前端的话，只会给前端一个categoryId,
         * 因为dish class里只有菜品分类的categoryId
         * 而在页面要展示具体的菜品分类名称(川菜，湘菜...)，因此要多返回一个字段
         * 因此要在DishDto里加一个categoryName属性
         */
        // return Result.success(dishPage);

        // 对象拷贝
        //  将 dishPage 中的属性值复制到 dtoPage，但是忽略records，因为要给records里每个dish换成dishDto
        //  records需要另外去设置(都是Page<T>里的属性,records这里就是页面上展示的列表数据的一个List<T>)
        BeanUtils.copyProperties(dishPage,dtoPage,"records");

        List<Dish> records = dishPage.getRecords();//dishPage的records里是一个一个的Dish对象；给他拿出来
        List<DishDto> dtoList = records.stream().map((dish) -> {  // dish 为每个菜品对象
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(dish,dishDto);// 把dish中普通属性拷贝过来；dishDto中还要放一个categoryName

            Long categoryId = dish.getCategoryId();  // 菜品的分类id
            Category category = categoryService.getById(categoryId);//根据id查到分类对象
            if (category != null){
                String categoryName = category.getName();//根据分类对象，拿到分类名称
                dishDto.setCategoryName(categoryName);//付给dishDto
            }

            return dishDto;
        }).collect(Collectors.toList());//把这些dishDto对象收集起来

        dtoPage.setRecords(dtoList);
        return Result.success(dtoPage);
    }






    @GetMapping("/{id}")
    /**
     * 根据id查询菜品（Dish）信息和对应的口味（flavors）信息
     * 要返回一个DishDto，因为页面需要展示flavors添加口味; Dish没有flavor
     * 要查询Dish和Flavor, 需要查询两张表，因此要自定义查询方法
     */
    @ApiOperation(httpMethod = "GET", value = "get dish info with flavor")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "current dish id", required = true),
    })
    public Result<DishDto> get(@PathVariable Long id){

        DishDto dishDto = dishService.getByIdWithFlavor(id);

        return Result.success(dishDto);
    }



    /**
     * 修改菜品（点击修改按钮）发送4个请求：（处理2，4就好了）
     * 1.发送请求填充菜品分类下拉框(我们在add里已经做过了，在CategoryController的CategoryList方法)
     * 2.根据dish ID查询dish信息和dishFlavor信息回显填充
     * 3.下载图片，用于图片回显（CommonController里已经写过）
     * 4.点击保存（save按钮），发送ajax请求，数据以json提交到胡无端
     * @param
     * @return
     */
//    修改菜品，点击save提交
    @PutMapping
    public Result<String> update(@RequestBody DishDto dishDto){
        log.info(dishDto.toString());

        dishService.updateWithFlavor(dishDto);

        //清理所有菜品的缓存数据
        //Set keys = redisTemplate.keys("dish_*");
        //redisTemplate.delete(keys);

        //只清理具体菜品（湘菜）缓存数据
        String key = "dish_" + dishDto.getCategoryId() + "_" + dishDto.getStatus();
        redisTemplate.delete(key);

        return Result.success("修改菜品操作成功！");
    }


    @GetMapping("/list")
    public Result<List<DishDto>> list(Dish dish){
        List<DishDto> dishDtoList = null;
        //1.先从Redis中获取缓存数据
        //  根据菜品的category(湘菜、川菜) 去缓存菜品数据(点击湘菜，出来湘菜的list),所以这里动态构建一个key,包含CategoryId
        String key = "dish_" + dish.getCategoryId() + "_" + dish.getStatus();//dish_1393888482348234923_1

        dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);

        //2.如果存在，直接返回，无需查询数据库
        if (dishDtoList != null){
          return Result.success(dishDtoList);
        }

        //3.如果不存在，需要查询数据库，将查询到的数据缓存到Redis
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getCategoryId()!= null,Dish::getCategoryId,dish.getCategoryId());

        // 添加条件，status 为 1: 还在售卖的菜品
        queryWrapper.eq(Dish::getStatus,1);
        // 添加排序条件，根据sort 属性升序片排列
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        List<Dish> list = dishService.list(queryWrapper);

        dishDtoList = list.stream().map((item) -> {
            DishDto dishDto = new DishDto();

            BeanUtils.copyProperties(item, dishDto);

            // 每个item表示 一个菜品 dish，根据菜品的分类id 给菜品设置 菜品的分类名
            Long itemCategoryId = item.getCategoryId();
            Category category = categoryService.getById(itemCategoryId);

            if (category != null) {
                dishDto.setCategoryName(category.getName());
            }

            // 当前菜品的id,根据dishId去查询当前菜品对应的口味
            Long dishId = item.getId();
            LambdaQueryWrapper<DishFlavor> flavorQueryWrapper = new LambdaQueryWrapper<>();
            flavorQueryWrapper.eq(DishFlavor::getDishId, dishId);

            //sql:select * from dish_flavor where dish_id = ?
            List<DishFlavor> dishFlavorList = dishFlavorService.list(flavorQueryWrapper);
            dishDto.setFlavors(dishFlavorList);
            return dishDto;

        }).collect(Collectors.toList());

        // 用户每次查看菜品，都要从数据库查询，如果查询的人很多，数据库压力非常大
        //  将查询到的菜品数据缓存到Redis,并且设置其 查询到的菜品数据有效时间为1小时，其后会清除菜品该菜品数据
        redisTemplate.opsForValue().set(key,dishDtoList,60L,TimeUnit.MINUTES);
        // 注意: 如果RedisConfig中配置了value的 序列化方式，则存储key-value时，value应该是String类型，而非List类型

        return Result.success(dishDtoList);
    }
    // 改变菜品的销售状态
    @PostMapping("/status/{status}")
    public Result<String> updateSaleStatus(@PathVariable("status") Integer status,@RequestParam List<Long> ids){
        //  菜品具体的售卖状态 由前端修改并返回，该方法传入的status是 修改之后的售卖状态，可以直接根据一个或多个菜品id进行查询并修改售卖即可
        log.info("ids :"+ids);
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(ids != null,Dish::getId,ids);


        List<Dish> list = dishService.list(queryWrapper);
        if (list != null){
            for (Dish dish : list) {
                dish.setStatus(status);
                dishService.updateById(dish);
            }
            return Result.success("菜品的售卖状态已更改！");
        }
        return Result.error("售卖状态不可更改,请联系管理员或客服！");

    }

    @DeleteMapping
    public Result<String> batchDelete(@RequestParam("ids") List<Long> ids){
        //dishService.batchDeleteByIds(ids);

        return Result.success("成功删除菜品！");
    }

}
