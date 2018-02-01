package com.wy.persistence.dao;

import java.util.List;

/**
 * Created by wy on 12/25/2016.
 */
public interface BaseDao<T> {
    T findById(Integer id);

    T deleteById(Integer id);

    List<T> findAll();
}
