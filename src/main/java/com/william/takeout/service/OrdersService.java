package com.william.takeout.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.william.takeout.entity.OrderDetail;
import com.william.takeout.entity.Orders;

import java.util.List;

public interface OrdersService extends IService<Orders> {

    public void submit(Orders orders);

    public List<OrderDetail> getOrderDetailsByOrderId(Long orderId);
}
