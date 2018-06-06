package org.seckill.service.impl;

import org.apache.commons.collections.MapUtils;
import org.seckill.dao.cache.RedisDao;
import org.seckill.dto.Expose;
import org.seckill.dto.SeckillExecution;
import org.seckill.enums.SeckillStateEnum;
import org.seckill.exception.RepeatKillException;
import org.seckill.exception.SeckillCloseException;
import org.seckill.exception.SeckillException;
import org.seckill.dao.SeckillDao;
import org.seckill.dao.SuccessKilledDao;
import org.seckill.entity.Seckill;
import org.seckill.entity.SuccessKilled;
import org.seckill.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SeckillServiceImpl implements SeckillService {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    //md5盐值字符串，用于混淆md5
    private final String salt = "slfgihawlghalkghniao@#$ta$%*(&^+";

    @Autowired
    private SeckillDao seckillDao;
    @Autowired
    private SuccessKilledDao successKilledDao;

    @Autowired
    private RedisDao redisDao;

    /**
     * 查询全部的秒杀记录
     *
     * @return
     */
    @Override
    public List<Seckill> getSeckillList() {
        return seckillDao.queryAll(0, 10);
    }

    /**
     * 根据商品种类的ID查询商品
     *
     * @param seckillId
     * @return
     */
    @Override
    public Seckill getSeckillById(long seckillId) {
        return seckillDao.queryById(seckillId);
    }


    /**
     * 在秒杀开启时输出秒杀接口的地址 秒杀未开启则输出系统时间和秒杀时间
     *
     * @param seckillId
     */
    @Override
    public Expose exposeSeckillUrl(long seckillId) {
        //优化点：缓存优化-->超时的基础上维护 数据一致性
        //1、访问redis
        Seckill seckill = redisDao.getSeckill(seckillId);

        if (seckill == null) {
            //2、从数据库取数据
            seckill = seckillDao.queryById(seckillId);

            if (seckill == null) {
                return new Expose(false, seckillId);
            }
            //3、放入redis
            redisDao.getSeckill(seckillId);
        }

        //2、判断秒杀时间是否开启
        Date startTime = seckill.getStartTime();
        Date endTime = seckill.getEndTime();
        Date nowTime = new Date();
        //Date().getTime 毫秒时间
        if (nowTime.getTime() < startTime.getTime()
                || nowTime.getTime() > endTime.getTime()) {
            return new Expose(false, seckillId, nowTime.getTime(), startTime.getTime(), endTime.getTime());
        }
        //3、开启秒杀，暴露接口
        //转化特定字符串的过程，不可逆
        String md5 = getMd5(seckillId);
        return new Expose(true, md5, seckillId);
    }

    /**
     * seckillId进行MD5加密
     */
    private String getMd5(long seckillId) {
        String base = seckillId + "/" + salt;
        String md5 = DigestUtils.md5DigestAsHex(base.getBytes());
        return md5;
    }

    @Override
    /**
     * 执行秒杀操作，有可能失败，有可能成功，所以要抛出我们允许的异常
     *
     *
     * 使用注解控制事务方法的注意点：
     * 1、开发团队达成一致约定，明确标注事务方法的编程风格
     * 2、保证事务方法的执行时间尽可能短，不要穿插其他网络操作（RPC/HTTP请求），或者剥离到事务方法外
     * 3、不是所有的方法都需要事务，如：只有一条修改操作、只读操作
     */
    @Transactional
    public SeckillExecution executeSeckill(long seckillId, long userPhone, String md5)
            throws SeckillException, RepeatKillException, SeckillCloseException {

        // 这段数据库操作的代码可能在任何地方发生未知的异常，所以要整体用try/catch括起来，把所有编译期异常转化为运行期异常
        // 运行期异常一旦有错，因为开启了@Transactional，Spring会帮我们回滚
        // 之所以要把抛出的RepeatKillException等异常再catch住，是把这些具体的异常类型抛出来，
        // 否则省略这段代码的话，RepeatKillException抛出来时就显示为SeckillException，就很笼统了
        try {
            //1、判断md5是否一样
            if (md5 == null || !md5.equals(getMd5(seckillId))) {
                throw new SeckillException("秒杀数据被篡改了，拒绝执行秒杀操作");
            }

            //2、记录购买行为
            int insertCount = successKilledDao.insertSuccessKilled(seckillId, userPhone);
            if (insertCount == 0) {
                //已经有记录了 不能重复秒杀
                throw new RepeatKillException("重复秒杀了");
            } else {
                //减库存（热点商品竞争激烈）
                Date nowTime = new Date();
                int updateCount = seckillDao.reduceNumber(seckillId, nowTime);
                if (updateCount == 0) {
                    throw new SeckillCloseException("秒杀结束了");
                } else {
                    //秒杀成功
                    SuccessKilled successKilled = successKilledDao.queryByIdWithSeckill(seckillId, userPhone);
                    return new SeckillExecution(seckillId, SeckillStateEnum.SUCCESS, successKilled);
                }
            }
        } catch (SeckillCloseException e1) {
            throw e1;
        } catch (RepeatKillException e2) {
            throw e2;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            //所有编译器异常 转化为运行期异常
            throw new SeckillException("seckill inner error:" + e.getMessage());
        }
    }

    /**
     * 通过 存储过程 优化秒杀操作
     * @param seckillId
     * @param userPhone
     * @param md5
     * @return
     */
    @Override
    public SeckillExecution executeSeckillProcedure(long seckillId, long userPhone, String md5) {
        if (md5 == null && !md5.equals(getMd5(seckillId))) {
            return new SeckillExecution(seckillId, SeckillStateEnum.DATA_REWRITE);
        }

        Date killTime = new Date();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("seckillId", seckillId);
        map.put("phone", userPhone);
        map.put("killTime", killTime);
        map.put("result", null);
        //执行 存储过程,result被赋值
        try {
            seckillDao.killByProcedure(map);
            //利用commons-collections工具
            Integer result = MapUtils.getInteger(map, "result", -2);

            if (result == 1) {
                //秒杀成功
                SuccessKilled successKilled = successKilledDao.
                        queryByIdWithSeckill(seckillId, userPhone);
                return new SeckillExecution(seckillId, SeckillStateEnum.SUCCESS, successKilled);
            } else {
                //根据返回的result 判断对应的错误
                return new SeckillExecution(seckillId, SeckillStateEnum.stateOf(result));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new SeckillExecution(seckillId, SeckillStateEnum.INNER_ERROR);
        }
    }
}
