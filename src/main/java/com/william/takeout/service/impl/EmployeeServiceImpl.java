package com.william.takeout.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.william.takeout.entity.Employee;
import com.william.takeout.mapper.EmployeeMapper;
import com.william.takeout.service.EmployeeService;
import org.springframework.stereotype.Service;

@Service
/*extends father impl, implements father interface*/
public class EmployeeServiceImpl extends ServiceImpl <EmployeeMapper, Employee> implements EmployeeService {
}
