package com.alipay.demo.qrcodetrade.service;

import com.alipay.demo.qrcodetrade.common.ServerResponse;

import java.util.Map;

/**
 * @description: TODO
 * @author: xiaowen
 * @create: 2018-12-11 08:55
 **/
public interface IQRCodePayService {
    ServerResponse qrCodePay(Long orderNo, String path);

    Map<String,String> alipayCallback(Map<String, String[]> requestParams);

    ServerResponse trade_query(String tradeNo);
}
