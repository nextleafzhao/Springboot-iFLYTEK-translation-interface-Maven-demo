package com.nl.example.mydemo.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.nl.example.mydemo.pojo.TransRequests;
import com.nl.example.mydemo.util.HttpUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/springboot")
public class TranslateAct {
    // OTS webapi 接口地址
    private static final String WebOTS_URL = "https://ntrans.xfyun.cn/v2/ots";
    // 应用ID（到控制台获取）
    private static final String APPID = "****";
    // 接口APIKey（到控制台机器翻译服务页面获取）
    private static final String API_KEY = "*********";
    // 接口APISercet（到控制台机器翻译服务页面获取）
    private static final String API_SECRET = "**********";


    @PostMapping("/translate")
    public String startSpringBoot(TransRequests requests) throws Exception {
        // 语种列表参数值请参照接口文档：https://doc.xfyun.cn/rest_api/机器翻译.html
        // 源语种
        String FROM = requests.getFrom();
        // 目标语种
        String TO = requests.getTo();
        // 翻译文本
        String TEXT = requests.getSource().trim();
        if (TEXT == null || "".equals(TEXT)) {
            System.out.println("翻译文本为空");
            return "翻译文本为空";
        }

        if (APPID.equals("") || API_KEY.equals("") || API_SECRET.equals("")) {
            System.out.println("Appid 或APIKey 或APISecret 为空！请打开demo代码，填写相关信息。");
            return "Appid 或APIKey 或APISecret 为空！请打开demo代码，填写相关信息。";
        }
        String body = buildHttpBody(FROM, TO, TEXT);
        System.out.println("【ITR WebAPI body】\n" + body);
        Map<String, String> header = buildHttpHeader(body);
        //返回的数据
        Map<String, Object> resultMap = HttpUtil.doPost2(WebOTS_URL, header, body);
        if (resultMap != null) {
            String resultStr = resultMap.get("body").toString();
            //System.out.println("【OTS WebAPI 接口调用结果】\n" + resultStr);

            //以下仅用于调试
            Gson json = new Gson();
            ResponseData resultData = json.fromJson(resultStr, ResponseData.class);
            String mod = "模式：" + resultData.getData().getResult().getFrom() + "→" + resultData.getData().getResult().getTo() + "<br>";
            String src = "原文：" + resultData.getData().getResult().getTransresult().getSrc() + "<br>";
            String tra = "译文：" + resultData.getData().getResult().getTransresult().getDst();
            int code = resultData.getCode();
            if (resultData.getCode() != 0) {
                System.out.println("请前往https://www.xfyun.cn/document/error-code?code=" + code + "查询解决办法");
            }
            return mod + src + tra;
        } else {
            return "调用失败！请根据错误信息检查代码，接口文档：https://www.xfyun.cn/doc/nlp/niutrans/API.html";
        }
    }

    /**
     * 组装http请求头
     */
    public static Map<String, String> buildHttpHeader(String body) throws Exception {
        Map<String, String> header = new HashMap<String, String>();
        URL url = new URL(WebOTS_URL);

        //时间戳
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date dateD = new Date();
        String date = format.format(dateD);
        //System.out.println("【OTS WebAPI date】\n" + date);

        //对body进行sha256签名,生成digest头部，POST请求必须对body验证
        String digestBase64 = "SHA-256=" + signBody(body);
        //System.out.println("【OTS WebAPI digestBase64】\n" + digestBase64);

        //hmacsha256加密原始字符串
        StringBuilder builder = new StringBuilder("host: ").append(url.getHost()).append("\n").//
                append("date: ").append(date).append("\n").//
                append("POST ").append(url.getPath()).append(" HTTP/1.1").append("\n").//
                append("digest: ").append(digestBase64);
        //System.out.println("【OTS WebAPI builder】\n" + builder);
        String sha = hmacsign(builder.toString(), API_SECRET);
        //System.out.println("【OTS WebAPI sha】\n" + sha);

        //组装authorization
        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", API_KEY, "hmac-sha256", "host date request-line digest", sha);
        //System.out.println("【OTS WebAPI authorization】\n" + authorization);

        header.put("Authorization", authorization);
        header.put("Content-Type", "application/json");
        header.put("Accept", "application/json,version=1.0");
        header.put("Host", url.getHost());
        header.put("Date", date);
        header.put("Digest", digestBase64);
        //System.out.println("【OTS WebAPI header】\n" + header);
        return header;
    }


    /**
     * 组装http请求体
     */
    public static String buildHttpBody(String FROM, String TO, String TEXT) throws Exception {
        JsonObject body = new JsonObject();
        JsonObject business = new JsonObject();
        JsonObject common = new JsonObject();
        JsonObject data = new JsonObject();
        //填充common
        common.addProperty("app_id", APPID);
        //填充business
        business.addProperty("from", FROM);
        business.addProperty("to", TO);
        //填充data
        //System.out.println("【OTS WebAPI TEXT字个数：】\n" + TEXT.length());
        byte[] textByte = TEXT.getBytes(StandardCharsets.UTF_8);
        String textBase64 = new String(Base64.getEncoder().encodeToString(textByte));
        //System.out.println("【OTS WebAPI textBase64编码后长度：】\n" + textBase64.length());
        data.addProperty("text", textBase64);
        //填充body
        body.add("common", common);
        body.add("business", business);
        body.add("data", data);
        return body.toString();
    }


    /**
     * 对body进行SHA-256加密
     */
    private static String signBody(String body) throws Exception {
        MessageDigest messageDigest;
        String encodestr = "";
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(body.getBytes(StandardCharsets.UTF_8));
            encodestr = Base64.getEncoder().encodeToString(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return encodestr;
    }

    /**
     * hmacsha256加密
     */
    private static String hmacsign(String signature, String apiSecret) throws Exception {
        Charset charset = StandardCharsets.UTF_8;
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(charset), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(signature.getBytes(charset));
        return Base64.getEncoder().encodeToString(hexDigits);
    }

    public static class ResponseData {
        private int code;
        private String message;
        private String sid;
        private ResponseDataResult data;

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return this.message;
        }

        public String getSid() {
            return sid;
        }

        public ResponseDataResult getData() {
            return data;
        }

        @Override
        public String toString() {
            return new Gson().toJson(this);
        }
    }

    public static class ResponseDataResult {
        private ResponseDataTransResult result;

        public ResponseDataTransResult getResult() {
            return result;
        }

        @Override
        public String toString() {
            return new Gson().toJson(this);
        }
    }

    public static class ResponseDataTransResult {
        private String from;
        private String to;
        private TransResult trans_result;

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        public TransResult getTransresult() {
            return trans_result;
        }

        @Override
        public String toString() {
            return new Gson().toJson(this);
        }
    }

    public static class TransResult {
        private String dst;
        private String src;

        public String getDst() {
            return dst;
        }

        public String getSrc() {
            return src;
        }

        @Override
        public String toString() {
            return new Gson().toJson(this);
        }
    }
}
