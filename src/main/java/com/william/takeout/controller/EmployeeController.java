package com.william.takeout.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.william.takeout.common.Result;
import com.william.takeout.entity.Employee;
import com.william.takeout.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    //    @RequestBody接收前端 发送过来的JSON风格的数据，将其转化为相应的对象

    /**  登录功能处理逻辑如下 **/
    @PostMapping("/login")
    public Result<Employee> login(HttpServletRequest request, @RequestBody Employee employee){
        // 1、将页面提交的密码password进行 MD5 加密处理
        String password = employee.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        // 2、根据页面提交的用户名username查询数据库
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Employee::getUsername,employee.getUsername());

        //emp为数据库里查出来的对象；Employee username是唯一的unique,所以用getOne
        Employee emp = employeeService.getOne(queryWrapper);

        // 3、如果没有查询到数据，则返回登录失败的结果
        if (emp == null){
            return Result.error("用户名不存在！");
        }

        // 4、进行密码比对，如果不一致，则返回登录失败的结果
        if (!emp.getPassword().equals(password)){
            return Result.error("用户名或密码错误！");
        }

        // 5、查看员工状态，如果为 已禁用状态，则返回被禁用的结果信息
        if (emp.getStatus() != 1){  // 账号被禁用; status == 1,账号可以正常登录
            return Result.error("账号被禁用，请联系管理员或客服！");
        }

        // 6、登录成功，将员工id 存入Session并返回登录成功的结果
        request.getSession().setAttribute("employee",emp.getId());

        // 将从数据库查出来的emp放进去
        return Result.success(emp);
    }

    //  退出功能实现
    //  1、LocalStorage 清理Session中的用户id
    //  2、返回结果
    @PostMapping("/logout")
    public Result<String> logout(HttpServletRequest request){
        request.getSession().removeAttribute("employee");
        return Result.success("安全退出成功！");
    }

    @PostMapping
    public Result<String> save(HttpServletRequest request,@RequestBody Employee employee){
        log.info("新增员工，员工信息:{}",employee.toString());

        // 在新增员工操作中，设置员工的初始密码，并对员工的密码进行( MD5加密 )
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));

        // 下面设置 公共属性的值(createTime、updateTime、createUser、updateUser)交给 MyMetaObjectHandler类处理
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());

        Long empId = (Long) request.getSession().getAttribute("employee");

        employee.setCreateUser(empId);
        employee.setUpdateUser(empId);

        employeeService.save(employee);

        return Result.success("成功新增员工");
    }

}
