package org.seckill.dao.cache;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.runtime.RuntimeSchema;
import org.seckill.entity.Seckill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisDao {
    private static final Logger logger = LoggerFactory.getLogger(RedisDao.class);

    //创建JedisPool（类似于数据连接池）
    private final JedisPool jedisPool;

    //初始化 JedisPool
    public RedisDao(String ip, int port) {
        jedisPool = new JedisPool(ip, port);
    }

    /**
     * 因为redis自身并不会序列化操作
     * get->byte[] -->反序列化 --> object(Seckill)
     * protostuff序列化性能非常好，所以采用其进行序列化操作
     * protostuff要求class必须是POJO.(里面有setter/getter方法等等)
     * ，且需要创建全局的RuntimeSchema（在上面）
     */
    //创建全局的RuntimeSchema，protostuff帮我序列化对象
    private RuntimeSchema<Seckill> schema = RuntimeSchema.createFrom(Seckill.class);

    /**
     * 在缓存中拿Seckill对象
     *
     * @param seckillId
     * @return
     */
    public Seckill getSeckill(long seckillId) {

        //拿到一个jedis对象
        Jedis jedis = jedisPool.getResource();
        //redis操作逻辑
        try {
            String key = "seckillId:" + seckillId;
            byte[] bytes = jedis.get(key.getBytes());
            // 判断key是否存在缓存中
            if (bytes != null) {
                //通过schema 创建空对象
                Seckill seckill = schema.newMessage();
                //Seckill 被反序列化
                ProtostuffIOUtil.mergeFrom(bytes, seckill, schema);
                return seckill;
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            jedis.close();
        }
        return null;
    }

    /**
     * 将Seckill对象放入缓存中
     *
     * @param seckill
     * @return OK:表示成功
     */
    public String putSeckill(Seckill seckill) {
        //set Object(Seckill) --> 序列化 --> byte[]

        Jedis jedis = jedisPool.getResource();
        try {
            String key = "seckillId:" + seckill.getSeckillId();
            //将对象序列化
            byte[] bytes = ProtostuffIOUtil.toByteArray(seckill, schema,
                    LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));
            //set 超时缓存
            int timeout = 60 * 60;//1小时
            String result = jedis.setex(key.getBytes(), timeout, bytes);
            return result;

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            jedis.close();
        }
        return null;
    }
}
