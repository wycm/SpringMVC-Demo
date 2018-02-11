package com.wy.controller;

import org.springframework.beans.BeanUtils;

/**
 * Created by wangyang on 2018/2/8.
 */
public class BeanUtilsTest {
    public static void main(String[] args){
        System.out.println(BeanUtils.isSimpleProperty(int.class));
        System.out.println(BeanUtils.isSimpleProperty(double.class));
        System.out.println(int.class.equals(Class.class));
    }
}
