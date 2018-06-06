-- 在 存储过程 中完成秒杀事务

DELIMITER $$  -- 将;结束符号临时改为两个$$

-- 定义 存储过程
-- 参数: in 输入参数 ；out 输出参数
-- 1、定义一个变量insert_count
-- 2、利用row_count()函数，获取 上一条sql 影响的行数
--    row_count: 0:未修改数据; >0:表示修改的行数; <0:sql错误/未执行修改sql
--  r_result: 1:commit成功;0:不能秒杀（未在秒杀时间内/没库存）;-1:重复秒杀;-2:sql错误/未执行修改sql
CREATE PROCEDURE `seckill`.`execute_seckill`
  (IN v_seckill_id BIGINT, IN v_phone BIGINT,
   IN v_kill_time  TIMESTAMP, OUT r_result INT)
  BEGIN
    DECLARE insert_count INT DEFAULT 0;
    START TRANSACTION;

    INSERT IGNORE INTO success_killed
    (seckill_id, user_phone, create_time)
      VALUE (v_seckill_id, v_phone, v_kill_time);

    SELECT row_count()INTO insert_count;
    IF (insert_count = 0)
    THEN
      ROLLBACK;
      SET r_result = -1;
    ELSEIF (insert_count < 0)
      THEN
        ROLLBACK;
        SET r_result = -2;
    ELSE
      UPDATE seckill
      SET number = number - 1
      WHERE seckill_id = v_seckill_id
            AND end_time > v_kill_time
            AND start_time < v_kill_time
            AND number > 0;

      SELECT row_count()INTO insert_count;
      IF (insert_count = 0)
      THEN
        ROLLBACK;
        SET r_result = 0;
      ELSEIF (insert_count < 0)
        THEN
          ROLLBACK;
          SET r_result = -2;
      ELSE
        COMMIT;
        SET r_result = 1;
      END IF;
    END IF;
  END;
$$
-- 存储过程定义结束


DELIMITER ;  -- 将结束符号改为;

-- 定义变量(mysql语法）
SET @r_result = -3;
CALL execute_seckill(1003, 13999999999, now(), @r_result);

-- 获取结果(mysql语法）
SELECT @r_result;


-- 存储过程注意事项：
-- 1、存储过程优化：事务行级锁持有的时间 减少（QPS:一个秒杀单6000/QPS)
-- 2、不要过度依赖存储过程（一般互联网公司很少会用到，简单的逻辑可以应用；
--        银行会有大量使用，他们会买大型数据库来处理很复杂的sql逻辑）