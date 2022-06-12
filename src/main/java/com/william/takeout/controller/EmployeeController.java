package com.william.takeout.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.william.takeout.common.Result;
import com.william.takeout.entity.Employee;
import com.william.takeout.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

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
    //返回给页面code，没有data，所以只用Result<String>就可以
    public Result<String> save(HttpServletRequest request,@RequestBody Employee employee){
        log.info("新增员工，员工信息:{}",employee.toString());

        // 在新增员工操作中，设置员工的初始密码，并对员工的密码进行( MD5加密 )
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));

        // 下面设置 公共属性的值(createTime、updateTime、createUser、updateUser)交给 MyMetaObjectHandler类处理
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());

        //获得当前登录用户的id(当前登录用户创建了这个employee)
        //返回的是object类型，需要强转
        Long empId = (Long) request.getSession().getAttribute("employee");

        employee.setCreateUser(empId);
        employee.setUpdateUser(empId);

        //employee username唯一；如果第二次添加相同的username，数据库会抛出异常
        //SQLIntegrityConstraintViolationException: Duplicate entry...for key username..
        //这里可以写try, catch; 但是不好，因为要写很多。使用异常处理器进行全局异常捕获
        //在common下创建一个GlobalExceptionHandler
        employeeService.save(employee);

        return Result.success("成功新增员工");
    }

        /*  pageShow方法的返回对象 应该是MP 中的
       Page对象(包含分页数据集合records、数据总数、每页的大小)
         protected List<T> records;
         protected long total;
         protected long size;
     */

    // 分页展示功能的流程分析:
//     1、页面发送Ajax请求，将分页查询参数(page、pageSize、name)提交到服务端
//     2、服务端Controller接收页面提交的数据 并调用Service查询数据
//     3、Service调用Mapper操作数据库，查询分页的数据
//     4、Controller将查询的分页数据 响应给页面
//     5、页面接收到分页数据并通过前端(ElementUI)的table组件展示到页面上
    @GetMapping("/page")
    public Result<Page> pageShow(int page, int pageSize, String name){//这个name是页面放大镜搜索输入的
        log.info("page = {},pageSize = {},name = {}",page,pageSize,name);

        // 创建分页构造器对象
        //page和pageSize是前端提供的默认值
        Page pageInfo = new Page(page,pageSize);

        //  构造条件构造器(如果name传进来的话)
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper();
        //   name不为null，才会 比较 getUsername方法和前端传入的name是否匹配 的过滤条件
        queryWrapper.like(StringUtils.isNotEmpty(name),Employee::getUsername,name);
        //  根据 更新用户的时间升序 分页展示
        queryWrapper.orderByAsc(Employee::getUpdateTime);

        // 去数据库查询
        employeeService.page(pageInfo,queryWrapper);
        return Result.success(pageInfo);//把查询封装的员工对象返回给前端
    }
}
