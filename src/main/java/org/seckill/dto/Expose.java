package org.seckill.dto;

/**
 * 暴露秒杀地址DTO
 */
public class Expose {
    //是否开启秒杀
    private boolean exposed;
    //一种加密措施
    private String md5;
    //id
    private long seckillId;
    //系统当前时间（毫秒）
    private long now;
    //开启时间
    private long start;
    //结束时间
    private long end;

    //构造方法：秒杀开启，暴露接口，MD5是商品ID经过加密后的值
    public Expose(boolean exposed, String md5, long seckillId) {
        this.exposed = exposed;
        this.md5 = md5;
        this.seckillId = seckillId;
    }

    //构造方法：秒杀商品不存在，不暴露接口
    public Expose(boolean exposed, long seckillId) {
        this.exposed = exposed;
        this.seckillId = seckillId;
    }

    //构造方法：秒杀未开启，不暴露接口
    public Expose(boolean exposed, long seckillId, long now, long start, long end) {
        this.exposed = exposed;
        this.seckillId = seckillId;
        this.now = now;
        this.start = start;
        this.end = end;
    }

    public boolean isExposed() {
        return exposed;
    }

    public void setExposed(boolean exposed) {
        this.exposed = exposed;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public long getSeckillId() {
        return seckillId;
    }

    public void setSeckillId(long seckillId) {
        this.seckillId = seckillId;
    }

    public long getNow() {
        return now;
    }

    public void setNow(long now) {
        this.now = now;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }
}
