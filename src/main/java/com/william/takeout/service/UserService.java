package com.william.takeout.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.william.takeout.entity.User;

public interface UserService extends IService<User> {

    public void sendMsg(String to,String subject,String context);
}
