package com.xuecheng.orders.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.utils.IdWorkerUtils;
import com.xuecheng.base.utils.QRCodeUtil;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xuecheng.orders.config.PayNotifyConfig;
import com.xuecheng.orders.mapper.XcOrdersGoodsMapper;
import com.xuecheng.orders.mapper.XcOrdersMapper;
import com.xuecheng.orders.mapper.XcPayRecordMapper;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;
import com.xuecheng.orders.model.po.XcOrders;
import com.xuecheng.orders.model.po.XcOrdersGoods;
import com.xuecheng.orders.model.po.XcPayRecord;
import com.xuecheng.orders.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author mechrev
 * @ClassName createOrderImpl
 * @description: TODO
 * @date 2023年02月12日
 * @version: 1.0
 */
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {


    @Value("${pay.alipay.APP_ID}")
    String APP_ID;
    @Value("${pay.alipay.APP_PRIVATE_KEY}")
    String APP_PRIVATE_KEY;

    @Value("${pay.alipay.ALIPAY_PUBLIC_KEY}")
    String ALIPAY_PUBLIC_KEY;


    @Transactional
    @Override
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto) {
        //创建商品订单
        XcOrders xcOrders = saveXcOrders(userId, addOrderDto);

        //添加支付交易记录
        XcPayRecord payRecord = createPayRecord(xcOrders);


        //生成支付二维码
        String qrCode = null;
        try {
            //url要可以被模拟器访问到，url为下单接口(稍后定义)
            qrCode = new QRCodeUtil().createQRCode("http://192.168.101.1/api/orders/requestpay?payNo=" + payRecord.getPayNo(), 200, 200);
        } catch (IOException e) {
            XueChengPlusException.cast("生成二维码出错");
        }
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord, payRecordDto);
        payRecordDto.setQrcode(qrCode);

        return payRecordDto;
    }

    @Autowired
    XcPayRecordMapper xcPayRecordMapper;

    @Override
    public XcPayRecord getPayRecordByPayno(String payNo) {
        XcPayRecord xcPayRecord = xcPayRecordMapper.selectOne(new LambdaQueryWrapper<XcPayRecord>().eq(XcPayRecord::getPayNo, payNo));
        return xcPayRecord;

    }

    @Autowired
    XcPayRecordMapper payRecordMapper;

    @Autowired
    MqMessageService mqMessageService;

    @Override
    public void saveAliPayStatus(PayStatusDto payStatusDto) {

        //当支付成功才更新订单状态
        //首先判断是否支付成功

        String trade_status = payStatusDto.getTrade_status();


        if (trade_status.equals("TRADE_SUCCESS")) {
            //支付流水号
            String payNo = payStatusDto.getOut_trade_no();
            //查询支付流水
            XcPayRecord payRecord = getPayRecordByPayno(payNo);
            //支付金额变为分
            Float totalPrice = payRecord.getTotalPrice() * 100;
            Float total_amount = Float.parseFloat(payStatusDto.getTotal_amount()) * 100;
            //校验是否一致
            if (payRecord != null
                    && payStatusDto.getApp_id().equals(APP_ID)
                    && totalPrice.intValue() == total_amount.intValue()) {
                String status = payRecord.getStatus();
                if ("601001".equals(status)) {//未支付时进行处理

                    log.debug("更新支付结果,支付交易流水号:{},支付结果:{}", payNo, trade_status);
                    XcPayRecord payRecord_u = new XcPayRecord();
                    payRecord_u.setStatus("601002");//支付成功
                    payRecord_u.setOutPayChannel("Alipay");
                    payRecord_u.setOutPayNo(payStatusDto.getTrade_no());//支付宝交易号
                    payRecord_u.setPaySuccessTime(LocalDateTime.now());//通知时间
                    int update1 = payRecordMapper.update(payRecord_u, new LambdaQueryWrapper<XcPayRecord>().eq(XcPayRecord::getPayNo, payNo));

                    if (update1 > 0) {
                        log.info("收到支付通知，更新支付交易状态成功.付交易流水号:{},支付结果:{}", payNo, trade_status);
                    } else {
                        log.error("收到支付通知，更新支付交易状态失败.支付交易流水号:{},支付结果:{}", payNo, trade_status);
                    }
                    //关联的订单号
                    Long orderId = payRecord.getOrderId();
                    XcOrders orders = ordersMapper.selectById(orderId);
                    if (orders != null) {
                        XcOrders order_u = new XcOrders();
                        order_u.setStatus("600002");
                        int update = ordersMapper.update(order_u, new LambdaQueryWrapper<XcOrders>().eq(XcOrders::getId, orderId));
                        if (update > 0) {
                            log.info("收到支付通知，更新订单状态成功.付交易流水号:{},支付结果:{},订单号:{},状态:{}", payNo, trade_status, orderId, "600002");
                            //订单类型,购买课程、购买学习资料..
                            String orderType = orders.getOrderType();
                            //写消息记录
                            mqMessageService.addMessage(PayNotifyConfig.MESSAGE_TYPE,orders.getOutBusinessId(),orderType,null);

                        } else {
                            log.error("收到支付通知，更新订单状态失败.支付交易流水号:{},支付结果:{},订单号:{},状态:{}", payNo, trade_status, orderId, "600002");
                        }

                    } else {
                        log.error("收到支付通知，根据交易记录找不到订单,交易记录号:{},订单号:{}",  orderId);
                    }


                }
            }
        }

    }


    public XcPayRecord createPayRecord(XcOrders orders) {
        XcPayRecord payRecord = new XcPayRecord();
        long id = IdWorkerUtils.getInstance().nextId();
        payRecord.setPayNo(id);


        payRecord.setOrderId(orders.getId());//商品订单号
        payRecord.setOrderName(orders.getOrderName());
        payRecord.setTotalPrice(orders.getTotalPrice());
        payRecord.setCurrency("CNY");
        payRecord.setCreateDate(LocalDateTime.now());
        payRecord.setStatus("601001");//未支付
        payRecord.setUserId(orders.getUserId());
        xcPayRecordMapper.insert(payRecord);
        return payRecord;
    }


    @Autowired
    XcOrdersGoodsMapper xcOrdersGoodsMapper;

    @Transactional
    public XcOrders saveXcOrders(String userId, AddOrderDto addOrderDto) {
        //幂等性处理
        XcOrders order = getOrderByBusinessId(addOrderDto.getOutBusinessId());

        if (order != null) {
            return order;
        }

        order = new XcOrders();
        //生成订单号
        long orderId = IdWorkerUtils.getInstance().nextId();
        order.setId(orderId);
        order.setTotalPrice(addOrderDto.getTotalPrice());
        order.setCreateDate(LocalDateTime.now());
        order.setStatus("600001");//未支付
        order.setUserId(userId);
        order.setOrderType(addOrderDto.getOrderType());
        order.setOrderName(addOrderDto.getOrderName());
        order.setOrderDetail(addOrderDto.getOrderDetail());
        order.setOrderDescrip(addOrderDto.getOrderDescrip());
        order.setOutBusinessId(addOrderDto.getOutBusinessId());//选课记录id
        ordersMapper.insert(order);
        String orderDetail = addOrderDto.getOrderDetail();
        List<XcOrdersGoods> xcOrdersGoodsList = JSON.parseArray(orderDetail, XcOrdersGoods.class);
        xcOrdersGoodsList.forEach(goods -> {
            XcOrdersGoods xcOrdersGoods = new XcOrdersGoods();
            BeanUtils.copyProperties(goods, xcOrdersGoods);
            xcOrdersGoods.setOrderId(orderId);
            xcOrdersGoodsMapper.insert(xcOrdersGoods);
        });
        return order;
    }


    @Autowired
    XcOrdersMapper ordersMapper;


    //根据业务id查询订单
    public XcOrders getOrderByBusinessId(String businessId) {
        return ordersMapper.selectOne(new LambdaQueryWrapper<XcOrders>().eq(XcOrders::getOutBusinessId, businessId));
    }

}
