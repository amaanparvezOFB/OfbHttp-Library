package org.example;

import java.util.Map;

public class OfbRequest {

    String methodType;

    String url;

    Map<String, String> headers;

    String tag;

    String body;

    // Constructor for GET
    public OfbRequest(String methodType, String url, Map<String, String> headers, String tag) {
        this.methodType = methodType;
        this.url = url;
        this.headers = headers;
        this.tag = tag;
    }

    // Constructor for POST
    public OfbRequest(String methodType, String body, String url, Map<String, String> headers, String tag) {
        this.methodType = methodType;
        this.body = body;
        this.url = url;
        this.headers = headers;
        this.tag = tag;
    }
}