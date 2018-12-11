package com.alipay.demo.qrcodetrade.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayResponse;
import com.alipay.api.domain.TradeFundBill;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.demo.qrcodetrade.Util.PropertiesUtil;
import com.alipay.demo.qrcodetrade.common.ServerResponse;
import com.alipay.demo.qrcodetrade.service.IQRCodePayService;
import com.alipay.demo.trade.config.Configs;
import com.alipay.demo.trade.model.ExtendParams;
import com.alipay.demo.trade.model.GoodsDetail;
import com.alipay.demo.trade.model.builder.AlipayTradePrecreateRequestBuilder;
import com.alipay.demo.trade.model.builder.AlipayTradeQueryRequestBuilder;
import com.alipay.demo.trade.model.result.AlipayF2FPrecreateResult;
import com.alipay.demo.trade.model.result.AlipayF2FQueryResult;
import com.alipay.demo.trade.service.AlipayTradeService;
import com.alipay.demo.trade.service.impl.AlipayTradeServiceImpl;
import com.alipay.demo.trade.utils.Utils;
import com.alipay.demo.trade.utils.ZxingUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

/**
 * @description: 扫码支付
 * @author: xiaowen
 * @create: 2018-12-11 08:57
 **/
@Service("iQRCodePayService")
public class qRCodePayServiceImpl implements IQRCodePayService {

    private static final Logger logger = LoggerFactory.getLogger(qRCodePayServiceImpl.class);

    // 支付宝当面付2.0服务
    private static AlipayTradeService   tradeService;
    static {
        /** 一定要在创建AlipayTradeService之前调用Configs.init()设置默认参数
         *  Configs会读取classpath下的zfbinfo.properties文件配置信息，如果找不到该文件则确认该文件是否在classpath目录
         */
        Configs.init("zfbinfo.properties");

        /** 使用Configs提供的默认参数
         *  AlipayTradeService可以使用单例或者为静态成员对象，不需要反复new
         */
        tradeService = new AlipayTradeServiceImpl.ClientBuilder().build();
    }
    /**
     * 付款逻辑
     *
     * @param orderNo 订单号
     * @param path    支付二维码保存的路径
     * @return
     */
    public ServerResponse qrCodePay(Long orderNo, String path) {
        //todo 可以根据userId检测是否为当前用户的订单

        // (必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
        // 需保证商户系统端不能重复，建议通过数据库sequence生成。这里随机生成一个订单号，根据具体需求修改
//        String outTradeNo=String.valueOf(orderNo);
        String outTradeNo = "tradeprecreate" + System.currentTimeMillis()
                + (long) (Math.random() * 10000000L);

        Map<String, String> resultMap = new HashMap();
        resultMap.put("orderNo", outTradeNo);
        // (必填) 订单标题，粗略描述用户的支付目的。如“xxx品牌xxx门店当面付扫码消费”
        String subject = "xxx品牌xxx门店当面付扫码消费";

        // (必填) 订单总金额，单位为元，不能超过1亿元
        // 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
        String totalAmount = "0.01";

        // (可选) 订单不可打折金额，可以配合商家平台配置折扣活动，如果酒水不参与打折，则将对应金额填写至此字段
        // 如果该值未传入,但传入了【订单总金额】,【打折金额】,则该值默认为【订单总金额】-【打折金额】
        String undiscountableAmount = "0";

        // 卖家支付宝账号ID，用于支持一个签约账号下支持打款到不同的收款账号，(打款到sellerId对应的支付宝账号)
        // 如果该字段为空，则默认为与支付宝签约的商户的PID，也就是appid对应的PID
        String sellerId = "";

        // 订单描述，可以对交易或商品进行一个详细地描述，比如填写"购买商品2件共15.00元"
        String body = "购买商品3件共20.00元";

        // 商户操作员编号，添加此参数可以为商户操作员做销售统计
        String operatorId = "test_operator_id";

        // (必填) 商户门店编号，通过门店号和商家后台可以配置精准到门店的折扣信息，详询支付宝技术支持
        String storeId = "test_store_id";

        // 业务扩展参数，目前可添加由支付宝分配的系统商编号(通过setSysServiceProviderId方法)，详情请咨询支付宝技术支持
        ExtendParams extendParams = new ExtendParams();
        extendParams.setSysServiceProviderId("2088100200300400500");

        // 支付超时，定义为120分钟
        String timeoutExpress = "120m";

        // 商品明细列表，需填写购买商品详细信息，
        List<GoodsDetail> goodsDetailList = new ArrayList<GoodsDetail>();
        // 创建一个商品信息，参数含义分别为商品id（使用国标）、名称、单价（单位为分）、数量，如果需要添加商品类别，详见GoodsDetail
        GoodsDetail goods1 = GoodsDetail.newInstance("goods_id001", "xxx小面包", 1000, 1);
        // 创建好一个商品后添加至商品明细列表
        goodsDetailList.add(goods1);

        // 继续创建并添加第一条商品信息，用户购买的产品为“黑人牙刷”，单价为5.00元，购买了两件
        GoodsDetail goods2 = GoodsDetail.newInstance("goods_id002", "xxx牙刷", 500, 2);
        goodsDetailList.add(goods2);

        // 创建扫码支付请求builder，设置请求参数
        AlipayTradePrecreateRequestBuilder builder = new AlipayTradePrecreateRequestBuilder()
                .setSubject(subject).setTotalAmount(totalAmount).setOutTradeNo(outTradeNo)
                .setUndiscountableAmount(undiscountableAmount).setSellerId(sellerId).setBody(body)
                .setOperatorId(operatorId).setStoreId(storeId).setExtendParams(extendParams)
                .setTimeoutExpress(timeoutExpress)
                .setNotifyUrl(PropertiesUtil.getProperty("alipay.callback.url"))//支付宝服务器主动通知商户服务器里指定的页面http路径,和沙箱配置中的授权回调路径一致
                .setGoodsDetailList(goodsDetailList);
        AlipayF2FPrecreateResult result = tradeService.tradePrecreate(builder);
        switch (result.getTradeStatus()) {
            case SUCCESS:
                logger.info("支付宝预下单成功: )");

                AlipayTradePrecreateResponse response = result.getResponse();
                dumpResponse(response);

                /**
                 * 生成二维码的逻辑
                 */

                File folder = new File(path);
                if (!folder.exists()) {
                    folder.setWritable(true);
                    folder.mkdirs();
                }
                // 根据订单号在指定路径下生成二维码
                String qrPath = String.format(path + "/qr-%s.png",
                        response.getOutTradeNo());
                //在指定路径生成二维码
                ZxingUtils.getQRCodeImge(response.getQrCode(), 256, qrPath);
                //todo 可以将二维码上传到ftp服务器上 以供访问
                resultMap.put("qrPath", qrPath);
                return ServerResponse.createBySuccess(resultMap);

            case FAILED:
                logger.error("支付宝预下单失败!!!");
                return ServerResponse.createByErrorMessage("支付宝预下单失败!!!");

            case UNKNOWN:
                logger.error("系统异常，预下单状态未知!!!");
                return ServerResponse.createByErrorMessage("系统异常，预下单状态未知!!!");

            default:
                logger.error("不支持的交易状态，交易返回异常!!!");
                return ServerResponse.createByErrorMessage("不支持的交易状态，交易返回异常!!!");
        }
    }

    public Map<String,String> alipayCallback(Map<String, String[]> requestParams){
        Map<String,String> resultMap=new HashMap<>();
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
                logger.error("非法请求，验证不通过");
            }
        } catch (AlipayApiException e) {
            logger.error("支付宝验证回调异常",e);
        }
        return resultMap;
    }

    public ServerResponse trade_query(String tradeNo) {
        // (必填) 商户订单号，通过此商户订单号查询当面付的交易状态
        String outTradeNo = tradeNo;

        // 创建查询请求builder，设置请求参数
        AlipayTradeQueryRequestBuilder builder = new AlipayTradeQueryRequestBuilder()
                .setOutTradeNo(outTradeNo);

        AlipayF2FQueryResult result = tradeService.queryTradeResult(builder);
        switch (result.getTradeStatus()) {
            case SUCCESS:
                logger.info("查询返回该订单支付成功: )");

                AlipayTradeQueryResponse response = result.getResponse();
                dumpResponse(response);

                logger.info(response.getTradeStatus());
                if (Utils.isListNotEmpty(response.getFundBillList())) {
                    for (TradeFundBill bill : response.getFundBillList()) {
                        logger.info(bill.getFundChannel() + ":" + bill.getAmount());
                    }
                }
                return ServerResponse.createBySuccessMessage("查询返回该订单支付成功");

            case FAILED:
                logger.error("查询返回该订单支付失败或被关闭!!!");
                return ServerResponse.createBySuccessMessage("查询返回该订单支付失败或被关闭!!!");

            case UNKNOWN:
                logger.error("系统异常，订单支付状态未知!!!");
                return ServerResponse.createBySuccessMessage("系统异常，订单支付状态未知!!!");

            default:
                logger.error("不支持的交易状态，交易返回异常!!!");
                return ServerResponse.createBySuccessMessage("不支持的交易状态，交易返回异常!!!");
        }
    }

    // 简单打印应答
    private void dumpResponse(AlipayResponse response) {
        if (response != null) {
            logger.info(String.format("code:%s, msg:%s", response.getCode(), response.getMsg()));
            if (StringUtils.isNotEmpty(response.getSubCode())) {
                logger.info(String.format("subCode:%s, subMsg:%s", response.getSubCode(),
                        response.getSubMsg()));
            }
            logger.info("body:" + response.getBody());
        }
    }
}
