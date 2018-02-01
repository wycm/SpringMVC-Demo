package com.wy.httpclient.impl;

import com.wy.persistence.dao.UserDao;
import com.wy.persistence.entity.User;
import com.wy.httpclient.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * Created by yang.wang on 12/20/16.
 */
@Service
public class UserServiceImpl implements UserService {
    @Resource
    private UserDao userDao;

    @Override
    public User findFirstUser() {
        return userDao.findFirstUser();
    }

    @Transactional
    @Override
    public void transactionalTest(){
        try {
            userDao.deleteById(981353);
        } catch (IOException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            System.out.println("删除失败");
        }
    }
}
