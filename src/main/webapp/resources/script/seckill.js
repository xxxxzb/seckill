//存放主要交互逻辑的js代码
//javascript 很容易写乱，要模块化

//创建seckill的json对象
var seckill= {
    //封装秒杀相关ajax的url
    URL: {
        now: function () {
            return '/seckill/time/now';
        },
        expose: function (seckillId) {
            return '/seckill/' + seckillId + '/expose';
        },
        execution: function (seckillId, md5) {
            return '/seckill/' + seckillId + '/' + md5 + '/execution';
        }
    },

    //秒杀逻辑
    handleSeckillkill : function (seckillId,node) {
        //获取秒杀地址，控制显示逻辑，执行秒杀
        node.hide()
            .html('<button class="btn btn-primary btn-lg" id="killBtn">开始秒杀</button>');//按钮
        $.post(seckill.URL.expose(seckillId),{},function (result) {
            // 在回调函数中执行交互流程
            if(result && result['success']){
                var expose = result['data'];
                if(expose['exposed']){
                    //开启秒杀
                    // 获取秒杀地址
                    var md5 = expose['md5'];
                    var killUrl = seckill.URL.execution(seckillId, md5);
                    //用‘one’绑定一次点击事件
                    $('#killBtn').one('click', function () {
                        // 执行秒杀请求
                        // 1、先禁用按钮
                        $(this).addClass('disabled');
                        // 2、发送秒杀请求 执行秒杀
                        $.post(killUrl, {}, function (result) {
                            if (result && result['success']) {
                                var result = result['data'];
                                var state = result['state'];
                                var stateInfo = result['stateInfo'];
                                // 3、显示秒杀结果
                                node.html('<span class="label label-success">' + stateInfo + '</span>');
                            }
                        });
                    });
                    node.show();
                }else {
                    //未开启秒杀（可能是因为客户端和服务端计时偏差）
                    var now = expose['now'];
                    var start = expose['start'];
                    var end = expose['end'];
                    // 重新计算计时逻辑
                    seckill.countdown(seckillId,now,start,end);
                }
            }else{
                console.log('result' + result);
            }
        });
    },

    //验证手机号
    validatePhone: function (phone) {
        if (phone && phone.length == 11 && !isNaN(phone)) {
            return true;
        } else {
            return false;
        }
    },

    //计时逻辑
    countdown: function (seckillId, nowTime, startTime, endTime) {
        var seckillBox = $('#seckill-box');
        //时间判断
        if (nowTime > endTime) {
            //秒杀结束
            seckillBox.html('秒杀结束！');
        }else if(nowTime < startTime){
            //秒杀未开始,计时事件绑定
            var killTime = new Date(startTime);
            //调用jQuery countdown
            seckillBox.countdown(killTime,function (event) {
                // 时间格式
                var format = event.strftime('秒杀倒计时：%D天 %H时 %H分 %S秒');
                seckillBox.html(format);
                //时间完成后回调事件
            }).on('finish.countdown',function () {
                //获取秒杀地址，控制显示逻辑，执行秒杀
                seckill.handleSeckillkill(seckillId,seckillBox);
            })
        }else {
            //秒杀开始
            seckill.handleSeckillkill(seckillId,seckillBox);
        }

    },

    //详情页秒杀逻辑
    detail: {
        //详情页初始化
        init: function (params) {
            //手机验证和登陆，计时交互
            //规划交互流程

            //在cookie中查找手机号
            var killPhone = $.cookie('killPhone');

            //验证手机号
            if (!seckill.validatePhone(killPhone)) {
                //绑定窗口，必须phone
                //控制输出
                var killPhoneModal = $('#killPhoneModal');
                //显示弹出层
                killPhoneModal.modal({
                    show: true, //显示弹出层
                    backdrop: 'static', // 禁止位置关闭
                    keyboard: false, //关闭键盘事件
                });
                $('#killPhoneBtn').click(function () {
                    var inputPhone = $('#killPhoneKey').val();
                    if (seckill.validatePhone(inputPhone)) {
                        //将电话写入cookie
                        $.cookie('killPhone', inputPhone, {expires:1, path: '/'});
                        //刷新页面
                        window.location.reload();
                    } else {
                        $('#killPhoneMessage').hide().html('<label class="label label-danger">手机号错误！</label>').show(300);
                    }
                });
            }
            //已经登陆
            //计时交互
            //获取传过来的json
            var startTime = params['startTime'];
            var endTime = params['endTime'];
            var seckillId = params['seckillId'];
            $.get(seckill.URL.now(),{}, function (result) {
                if(result && result['success']){
                    var nowTime = result['data'];
                    //时间判断,计时交互
                    seckill.countdown(seckillId,nowTime,startTime,endTime);
                }else {
                    console.log('result:'+result);
                }
            })
        }
    }
}