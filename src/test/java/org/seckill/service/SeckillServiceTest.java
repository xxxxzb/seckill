package org.seckill.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.seckill.dao.SeckillDao;
import org.seckill.dto.Expose;
import org.seckill.dto.SeckillExecution;
import org.seckill.entity.Seckill;
import org.seckill.exception.RepeatKillException;
import org.seckill.exception.SeckillCloseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:spring/spring-dao.xml",
                        "classpath:spring/spring-service.xml"})
public class SeckillServiceTest {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SeckillService seckillService;

    @Test
    public void getSeckillList() {
        List<Seckill> seckills = seckillService.getSeckillList();
        logger.info("list={}", seckills);
    }

    @Test
    public void getSeckillById() {
        long id = 1000;
        Seckill seckill = seckillService.getSeckillById(id);
        logger.info("seckill={}", seckill);
    }

    //集成测试代码完整逻辑，注意可重复执行
    @Test
    public void testSeckillLogic() {
        long id = 1001;
        Expose expose = seckillService.exposeSeckillUrl(id);
        if (expose.isExposed()) {
            logger.info("expose={}", expose);
            long phone = 13700100000L;
            String md5 = expose.getMd5();
            try {
                SeckillExecution seckillExecution = seckillService.executeSeckill(id, phone, md5);
                logger.info("result={}", seckillExecution);
            } catch (RepeatKillException e) {
                logger.error(e.getMessage());
            } catch (SeckillCloseException e) {
                logger.error(e.getMessage());
            }
        } else {
            //秒杀未开启
            logger.warn("expose={}", expose);
        }
    }

    //测试 存储过程 秒杀操作
    @Test
    public void testSeckillExecution(){
        long id = 1003;
        long phone = 13912345678L;
        Expose expose = seckillService.exposeSeckillUrl(id);
        if (expose.isExposed()){
            String md5 = expose.getMd5();
            SeckillExecution seckillExecution = seckillService.
                    executeSeckillProcedure(id, phone, md5);

            System.out.println(seckillExecution.getStateInfo());

        }

    }

}
