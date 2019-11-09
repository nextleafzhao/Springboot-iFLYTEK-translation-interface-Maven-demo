package com.nl.example.mydemo.pojo;

import com.google.gson.Gson;

public class TransRequests {
    /**
     * 		"result": {
     * 			"from": "cn",
     * 			"to": "en",
     * 			"trans_result": {
     * 				"dst": "The People's Republic of China was founded in 1949. ",
     * 				"src": "中华人民共和国于1949年成立"
     *               }
     *          }
     */

    //源语种
    private String from;
    //目标语种
    private String to;
    //翻译文本
    private String source;

    public TransRequests(String from, String to, String source) {
        this.from = from;
        this.to = to;
        this.source = source;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
    @Override
    public String toString() {
        return  new Gson().toJson(this);
    }
}
