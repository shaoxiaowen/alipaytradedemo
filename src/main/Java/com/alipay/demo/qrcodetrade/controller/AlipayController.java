package com.alipay.demo.qrcodetrade.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.demo.qrcodetrade.common.Const;
import com.alipay.demo.qrcodetrade.common.ServerResponse;
import com.alipay.demo.qrcodetrade.service.IQRCodePayService;
import com.alipay.demo.trade.config.Configs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Iterator;
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
        Map<String,String> resultMap=new HashMap<>();

        Map<String, String[]> requestParams = request.getParameterMap();
        for(Iterator iter = requestParams.keySet().iterator(); iter.hasNext();){
            String name= (String) iter.next();
            String[] values=requestParams.get(name);
            String valueStr="";

            //将key对应的数组中的内容拼接一下，存入map中
            for(int i=0;i<values.length;i++){
                valueStr=(i==values.length-1)?valueStr+values[i]:valueStr+values[i]+",";
            }
            resultMap.put(name,valueStr);
        }
        //打印回调参数
        logger.info("支付宝回调，sign:{},trade_status:{},参数:{}",resultMap.get("sign"),resultMap.get("trade_status"),resultMap.toString());

        //需要移除sign_type结点
        resultMap.remove("sign_type");
        //非常重要，验证回调的正确性，是不是支付宝发的，还要避免重复通知
        try {
            boolean alipayRSACheckedV2=AlipaySignature.rsaCheckV2(resultMap,Configs.getAlipayPublicKey(),"utf-8",Configs.getSignType());
            if(!alipayRSACheckedV2){
                return ServerResponse.createByErrorMessage("非法请求，验证不通过，再恶意请求就报警了");
            }
        } catch (AlipayApiException e) {
            logger.error("支付宝验证回调异常",e);
        }
        String tradeStatus = resultMap.get("trade_status");
        if (Const.AlipayCallBack.TRADE_STATUS_TRADE_SUCCESS.equals(tradeStatus)) {
            return Const.AlipayCallBack.RESPONSE_SUCCESS;
        }
        return Const.AlipayCallBack.RESPONSE_FAILED;
    }
}
