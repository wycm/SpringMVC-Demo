package com.wy.persistence.dao;

import com.wy.persistence.entity.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.io.IOException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:applicationContext-persistence-test.xml")
public class UserDaoTest {
    @Resource
    private UserDao userDao;

    @Test
    public void testFindById(){
        User user = userDao.findFirstUser();
        System.out.println(user);
    }
    @Test
    public void testInsert(){
        User user = new User();
        user.setUsername("temp1");
        userDao.insert(user);
        System.out.println("插入成功");
    }
    @Test
    public void testDeleteById() throws IOException {
        userDao.deleteById(981360);
        System.out.println("删除成功");
    }
}
