package com.william.takeout.dto;

import com.william.takeout.entity.Setmeal;
import com.william.takeout.entity.SetmealDish;

import lombok.Data;
import java.util.List;

@Data
public class SetmealDto extends Setmeal {

    private List<SetmealDish> setmealDishes;

    private String categoryName;
}
