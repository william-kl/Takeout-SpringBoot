package com.william.takeout.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.william.takeout.entity.OrderDetail;
import com.william.takeout.mapper.OrderDetailMapper;
import com.william.takeout.service.OrderDetailService;
import org.springframework.stereotype.Service;

@Service
public class OrderDetailServiceImpl extends ServiceImpl<OrderDetailMapper, OrderDetail>
     implements OrderDetailService {
}
