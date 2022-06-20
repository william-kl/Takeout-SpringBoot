package com.william.takeout.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.william.takeout.common.MyCustomException;
import com.william.takeout.dto.SetmealDto;
import com.william.takeout.entity.Setmeal;
import com.william.takeout.entity.SetmealDish;
import com.william.takeout.mapper.SetmealMapper;
import com.william.takeout.service.SetmealDishService;
import com.william.takeout.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper,Setmeal> implements SetmealService {

    @Autowired
    private SetmealDishService setmealDishService;
    /**
     * add setmeal，同时保存setmeal和dish的关联关系
     * @param setmealDto
     */
    @Override
    @Transactional /*操作两张表，要加事物注解，保持一致性*/
    public void saveWithDish(SetmealDto setmealDto) {
        //保存套餐的基本信息，操作setmeal,执行insert操作
        this.save(setmealDto);

        //保存setmeal和dish的关联关系
        //List<SetmealDish>里setmealDish的setmealId是没有值的
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();

        //this.save之后，setmealDto里id就赋值了
        setmealDishes.stream().map((item) -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());

        setmealDishService.saveBatch(setmealDishes);
    }


    /**
     * delete setmeal，同时删除setmeal和dish的关联关系；操作两张表
     * @param
     */
    @Override
    @Transactional /*操作两张表，要加事物注解，保持一致性*/
    public void removeWithDish(List<Long> ids) {/*ids为setmeal表中的主键id*/
        //查询setmeal status, 确定是否可以删除(在售卖中的setmeal不能删除)
        //select count(*) from setmeal where id in (1,2,3) and status = 1
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Setmeal::getId, ids);
        queryWrapper.eq(Setmeal::getStatus,1);
        int count = this.count(queryWrapper);
        //如果不能删除，抛出一个业务异常
        if(count > 0) {//不能删除
            throw new MyCustomException("套餐正在售卖中，不能删除");
        }
        //如果可以删除，先删除setmeal表中的数据
        this.removeByIds(ids);

        //删除setmealdish中的数据（要拿到setmealdish表中的主键id）
        //delete from setmealdish where setmealid in (1,2,3)
        LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(SetmealDish::getSetmealId,ids);
        setmealDishService.remove(lambdaQueryWrapper);
    }
}
