package com.wy.persistence.dao;

import com.fasterxml.jackson.databind.annotation.JsonAppend;
import com.wy.persistence.entity.User;

import java.io.IOException;

/**
 * Created by yang.wang on 12/20/16.
 */
public interface UserDao {
    User findFirstUser();

    void insert(User user);

    void deleteById(Integer id) throws IOException;
}
