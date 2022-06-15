package com.william.takeout.dto;

import com.william.takeout.entity.Dish;
import com.william.takeout.entity.DishFlavor;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

//用于封装页面提交的数据
// 前端传输到服务端的数据 和实体类中的属性 不是一一对应关系，
// 需要用到DTO(Data Transfer Object)对象，即数据传输对象，一般用于Controller和Service层之间的数据传输
@Data
/**
 * 前端不光传过来dish的属性，还有一个flavors数组，这个dish class里没有
 * 因此要创建DishDto封装起来，而且要继承Dish父类
 */

public class DishDto extends Dish {
    //  Dish 不符合前端传过来的数据,需要将其转化为DishDto
    // flavors: 菜品对应的口味数据，用来接收页面传过来的这个数组[{name:"甜味",value: "["无糖","少糖","多糖","全糖"]", ....."},...]
    private List<DishFlavor> flavors = new ArrayList<>();//[{DishFlavor1},{DishFlavor2},...]

    private String categoryName;

    private Integer copies;
}
