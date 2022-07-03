package com.william.takeout.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.william.takeout.common.BaseContexts;
import com.william.takeout.common.Result;
import com.william.takeout.entity.ShoppingCart;
import com.william.takeout.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/shoppingCart")
@Slf4j
public class ShoppingCartController {
    @Autowired
    private DataSource dataSource;

    @Autowired
    private ShoppingCartService shoppingCartService;


    /**
     *
     * @param shoppingCart
     * Request URL: http://localhost:8080/shoppingCart/add
     * Request Method: POST
     * 添加setmeal和dish到购物车都是走的这个方法，只是传过来的payload（参数）不同
     * @return
     */
    @PostMapping("/add")
    public Result<ShoppingCart> addToCart(@RequestBody ShoppingCart shoppingCart){
        log.info("购物车中的数据:{}"+shoppingCart.toString());
        
        //1.设置用户id,指定当前是哪个用户的 购物车数据
        Long userId = BaseContexts.getCurrentId();
        shoppingCart.setUserId(userId);
        
        //2.查询当前菜品或套餐是否 在购物车中;看是菜品还是套餐
        Long dishId = shoppingCart.getDishId();

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId,userId);  // 根据登录用户的 userId去ShoppingCart表中查询该用户的购物车数据

        if (dishId != null){ // 添加进购物车的是菜品，且 购物车中已经添加过 该菜品
             queryWrapper.eq(ShoppingCart::getDishId,dishId);
        }else {
            queryWrapper.eq(ShoppingCart::getSetmealId,shoppingCart.getSetmealId());
        }
        //查询当前菜品或套餐是否 在购物车中
        //sql:select * from shopping_cart where user_id = ? and dish_id/setmeal_id = ?
        ShoppingCart oneCart = shoppingCartService.getOne(queryWrapper);

        //3.如果已经存在，就在原来数量基础上加1
        if (oneCart != null){
            Integer number = oneCart.getNumber();
            oneCart.setNumber(number + 1);

            shoppingCartService.updateById(oneCart);
        }else {//4.不存在，则添加到购物车，数量默认就是1
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartService.save(shoppingCart);
            oneCart = shoppingCart;//进的else分支，oneCart初始为空，需要赋值覆盖一下
        }


        return Result.success(oneCart);
    }

    // 在购物车中删减订单
    @PostMapping("/sub")
    public Result<String> subToCart(@RequestBody ShoppingCart shoppingCart){
        log.info("购物车中的数据:{}"+shoppingCart.toString());

        shoppingCart.setUserId(BaseContexts.getCurrentId());

        // 查询当前菜品或套餐是否 在购物车中
        Long dishId = shoppingCart.getDishId();

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId,BaseContexts.getCurrentId());  // 根据登录用户的 userId去ShoppingCart表中查询该用户的购物车数据

        if (dishId != null){ // 添加进购物车的是菜品，且 购物车中已经添加过 该菜品
            queryWrapper.eq(ShoppingCart::getDishId,dishId);
        }else {
            queryWrapper.eq(ShoppingCart::getSetmealId,shoppingCart.getSetmealId());
        }


        ShoppingCart oneCart = shoppingCartService.getOne(queryWrapper);
        //  如果购物车中 已经存在该菜品或套餐，其数量+1，不存在，就将该购物车数据保存到数据库中
        if (oneCart != null){
            Integer number = oneCart.getNumber();
            if (number != 0){
                oneCart.setNumber(number - 1);
                shoppingCartService.updateById(oneCart);
            }else {
                shoppingCartService.remove(queryWrapper);
            }

        }
        return Result.success("成功删减订单!");
    }

    /**
     * 查看购物车；每添加一次菜品，都要重新调用这个list方法去显示购物车
     * @return
     */
    @GetMapping("/list")
    public Result<List<ShoppingCart>> list(){
        log.info("查看购物车");
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        //按用户id来查购物车信息:不同的用户有不同的购物车
        queryWrapper.eq(ShoppingCart::getUserId,BaseContexts.getCurrentId());

        // 最晚下单的 菜品或套餐在购物车中最先展示
        //上面入购物车的时候加一句，要不排序没意义：shoppingCart.setCreateTime(LocalDateTime.now());
        queryWrapper.orderByDesc(ShoppingCart::getCreateTime);
        List<ShoppingCart> list = shoppingCartService.list(queryWrapper);

        return Result.success(list);
    }

    @DeleteMapping("/clean")
    public Result<String> cleanCart(){

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        // DELETE FROM shopping_cart WHERE (user_id = ?)
        queryWrapper.eq(ShoppingCart::getUserId,BaseContexts.getCurrentId());


        shoppingCartService.remove(queryWrapper);

        return Result.success("成功清空购物车！");
    }


}
