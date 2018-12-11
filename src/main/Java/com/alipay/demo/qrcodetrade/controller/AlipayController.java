package com.alipay.demo.qrcodetrade.controller;

import com.alipay.demo.qrcodetrade.common.Const;
import com.alipay.demo.qrcodetrade.common.ServerResponse;
import com.alipay.demo.qrcodetrade.service.IQRCodePayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @description: TODO
 * @author: xiaowen
 * @create: 2018-12-11 09:12
 **/
@Controller
@RequestMapping("/alipay/")
public class AlipayController {

    private static final Logger logger=LoggerFactory.getLogger(AlipayController.class);
    @Autowired
    private IQRCodePayService iqrCodePayService;

    /**
     *
     * @param orderNo 订单号
     * @param request
     * @return
     */
    @RequestMapping("qrCodePay.do")
    @ResponseBody
    public ServerResponse qrCodePay(Long orderNo,HttpServletRequest request){
        //这里可以根据session做用户校验

        /**
         * 存放二维码的文件路径
         */
        String path=request.getSession().getServletContext().getRealPath("upload");
        return iqrCodePayService.qrCodePay(orderNo,path);
    }

    @RequestMapping("alipay_callback.do")
    @ResponseBody
    public Object alipayCallback(HttpServletRequest request){
        //接受支付宝的请求，并获取参数
        Map<String, String[]> requestParams = request.getParameterMap();
        Map<String,String> resultMap=iqrCodePayService.alipayCallback(requestParams);

        String tradeStatus = resultMap.get("trade_status");
        if (Const.AlipayCallBack.TRADE_STATUS_TRADE_SUCCESS.equals(tradeStatus)) {
            return Const.AlipayCallBack.RESPONSE_SUCCESS;
        }
        return Const.AlipayCallBack.RESPONSE_FAILED;
    }

    @RequestMapping("alipay_trade_query.do")
    @ResponseBody
    public ServerResponse trade_query(String tradeNo){
        return iqrCodePayService.trade_query(tradeNo);
    }
}
