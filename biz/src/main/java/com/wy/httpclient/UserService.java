package com.wy.httpclient;

import com.wy.persistence.entity.User;

/**
 * Created by yang.wang on 12/20/16.
 */
public interface UserService {
    User findFirstUser();

    void transactionalTest() throws Exception;
}
