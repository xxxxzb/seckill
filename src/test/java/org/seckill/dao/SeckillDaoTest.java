package org.seckill.dao;

import org.apache.ibatis.annotations.Param;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.seckill.entity.Seckill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

//配置spring和junit整合，junit启动时加载springIOC容器
@RunWith(SpringJUnit4ClassRunner.class)
//告诉junit spring配置文件
@ContextConfiguration({"classpath:spring/spring-dao.xml"})
public class SeckillDaoTest {

    @Autowired
    private SeckillDao seckillDao;
    @Test
    public void reduceNumber() throws Exception {
        //当前时间前1天
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DAY_OF_MONTH,-1);
        Date seckillTime = calendar.getTime();

        int updateCount = seckillDao.reduceNumber(1000, seckillTime);
        System.out.println(updateCount);
    }

    @Test
    public void queryById() throws Exception {
        long id = 1000;
        Seckill seckill = seckillDao.queryById(id);
        System.err.println(seckill.getName());
        System.err.println(seckill);
    }

    @Test
    public void queryAll() throws Exception {
        List<Seckill> seckills = seckillDao.queryAll( 0, 10);
        for (Seckill s:seckills) {
            System.out.println(s.getName());
        }
    }

}