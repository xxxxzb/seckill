package org.seckill.dao;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.seckill.entity.SuccessKilled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:spring/spring-dao.xml"})
public class SuccessKilledDaoTest {
    @Autowired
    private SuccessKilledDao successKilledDao;

    @Test
    public void insertSuccessKilled() {
        int result = successKilledDao.insertSuccessKilled(1200,13700000000L);
        System.out.println(result);
    }

    @Test
    public void queryByIdWithSeckill() {
        SuccessKilled successKilled =  successKilledDao.queryByIdWithSeckill(1200,13700000000L);
        System.out.println(successKilled);
        System.out.println(successKilled.getSeckill());
    }
}