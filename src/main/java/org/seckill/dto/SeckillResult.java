package org.seckill.dto;

//所有ajax请求返回类型，封装json结果
public class SeckillResult<T> {
    //是否成功执行
    private boolean success;
    private T data;
    private String error;

    //构造器：执行成功,把对象传出去
    public SeckillResult(boolean success, T data) {
        this.success = success;
        this.data = data;
    }

    //构造器：执行失败，传错误信息
    public SeckillResult(boolean success, String error) {
        this.success = success;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
