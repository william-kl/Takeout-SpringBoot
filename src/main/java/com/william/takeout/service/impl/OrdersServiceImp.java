package com.william.takeout.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.william.takeout.common.BaseContexts;
import com.william.takeout.common.MyCustomException;
import com.william.takeout.entity.*;
import com.william.takeout.mapper.OrdersMapper;
import com.william.takeout.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@Service
@Slf4j
public class OrdersServiceImp extends ServiceImpl<OrdersMapper, Orders>
     implements OrdersService {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private UserService userService;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private OrderDetailService orderDetailService;

    public List<OrderDetail> getOrderDetailsByOrderId(Long orderId){
        LambdaQueryWrapper<OrderDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderDetail::getOrderId, orderId);
        List<OrderDetail> orderDetailList = orderDetailService.list(queryWrapper);
        return orderDetailList;
    }


    // 用户下单
    @Transactional  // 需要操作多张表，涉及到事务
    //  向订单表中 插入数据 并 完善订单明细表，清空当前用户在 购物车 表 中的数据
    public void submit(Orders orders){

        //1. 获取当前用户的id
        Long userId = BaseContexts.getCurrentId();

        //2. 查询当前用户的 购物车数据
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId,userId);
        // 一个用户可能下单了 多个菜品或套餐，故应该查询到的是一个 购物车类型的list
        List<ShoppingCart> shoppingCarts = shoppingCartService.list(queryWrapper);

        if (shoppingCarts == null || shoppingCarts.size() == 0){
            throw new MyCustomException("购物车为空，不能下单！");
        }

        // 查询用户数据: 因为后面的订单表里需要用户的手机号.地址等信息
        User user = userService.getById(userId);
        // 查询用户的派送地址信息
        AddressBook addressBook = addressBookService.getById(orders.getAddressBookId());
        if (addressBook == null){
            throw new MyCustomException("您的地址信息有误，暂不能下单!");
        }

        //3. 向订单表Orders插入数据，一条数据；根据前端传过来的payload，把order里其余的属性补上

        long orderId = IdWorker.getId();  // 订单号

        // 计算购物车中商品的总金额 需要保证在多线程的情况下 也是能计算正确的，故需要使用原子类
        // 并且封装OrderDetail订单明细list
        AtomicInteger amount = new AtomicInteger(0);

        List<OrderDetail> orderDetails = shoppingCarts.stream().map((item) -> {
            OrderDetail orderDetail = new OrderDetail();

            orderDetail.setOrderId(orderId);
            orderDetail.setName(item.getName());
            orderDetail.setImage(item.getImage());

            orderDetail.setDishId(item.getDishId());
            orderDetail.setSetmealId(item.getSetmealId());
            orderDetail.setDishFlavor(item.getDishFlavor());
            orderDetail.setNumber(item.getNumber());
            orderDetail.setAmount(item.getAmount());

            amount.addAndGet(item.getAmount().multiply(new BigDecimal(item.getNumber())).intValue());

            return orderDetail;

        }).collect(Collectors.toList());

        orders.setId(orderId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setCheckoutTime(LocalDateTime.now());
        orders.setStatus(2);
        orders.setAmount(new BigDecimal(amount.get()));//总金额，需要 遍历购物车，计算相关金额来得到
        orders.setUserId(userId);
        orders.setNumber(String.valueOf(orderId));
        orders.setUserName(user.getName());
        orders.setConsignee(addressBook.getConsignee());
        orders.setPhone(addressBook.getPhone());
        orders.setAddress((addressBook.getProvinceName() == null ? "" : addressBook.getProvinceName())
                + (addressBook.getCityName() == null ? "" : addressBook.getCityName())
                + (addressBook.getDistrictName() == null ? "" : addressBook.getDistrictName())
                + (addressBook.getDetail() == null ? "" : addressBook.getDetail()));


        // 向订单表插入数据,一条数据，插入数据之前，需要填充如上属性
        this.save(orders);    //  --> ordersService.save(orders);

        //4. 向订单明细表OrderDetail插入数据，多条数据
        orderDetailService.saveBatch(orderDetails);

        //5. 清空购物车数据
        shoppingCartService.remove(queryWrapper);

    }
}
