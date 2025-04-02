package org.example;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.FutureCallback;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

public class OfbHttp {

    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    private static final Map<String, ListenableFuture<OfbResponse>> requestMap = new ConcurrentHashMap<>();
    private static final Map<String, OfbResponse> cache = new ConcurrentHashMap<>();
    private static final Map<String, FutureCallback<OfbResponse>> callbackMap = new ConcurrentHashMap<>();


    public static ListenableFuture<OfbResponse> execute(OfbRequest request) {
        CompletableFuture<OfbResponse> completableFuture = new CompletableFuture<>();
        ListenableFuture<OfbResponse> listenableFuture = Futures.submit(() -> performRequest(request), executorService);

        requestMap.put(request.tag, listenableFuture);

        Futures.addCallback(listenableFuture, new ExtendedFutureCallback<OfbResponse>() {

            @Override
            public void onSuccess(OfbResponse result) {
                if (request.methodType.equalsIgnoreCase("GET")) {
                    cache.put(request.url, result);
                }
                completableFuture.complete(result);
            }

            @Override
            public void onFailure(Throwable t) {
                completableFuture.completeExceptionally(t);
            }
            
            @Override
            public void onCancel() {
                completableFuture.cancel(true);
            }
        }, executorService);

        callbackMap.put(request.tag, new ExtendedFutureCallback<OfbResponse>() {
            @Override
            public void onSuccess(OfbResponse result) {}

            @Override
            public void onFailure(Throwable t) {}

            @Override
            public void onCancel() {
                System.out.println("Request with tag " + request.tag + " was canceled.");
            }
        });

        return listenableFuture;
    }

    public static void cancel(String tag) {
        ListenableFuture<OfbResponse> future = requestMap.remove(tag);
        if (future != null) {
            future.cancel(true);

            // Retrieve and invoke the onCancel callback
            FutureCallback<OfbResponse> callback = callbackMap.remove(tag);
            if (callback instanceof ExtendedFutureCallback) {
                ((ExtendedFutureCallback<OfbResponse>) callback).onCancel();
            }
        }
    }

    private static OfbResponse performRequest(OfbRequest request) throws IOException {
        if (request.methodType.equalsIgnoreCase("GET") && cache.containsKey(request.url)) {
            return cache.get(request.url);
        }

        HttpURLConnection connection = null;
        try {
            URL url = new URL(request.url);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(request.methodType);

            if (request.headers != null) {
                for (Map.Entry<String, String> entry : request.headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            if (request.methodType.equalsIgnoreCase("POST") && request.body != null) {
                connection.setDoOutput(true);
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = request.body.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
            }

            int responseCode = connection.getResponseCode();
            InputStream inputStream;
            if (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
                inputStream = connection.getErrorStream();
            } else {
                inputStream = connection.getInputStream();
            }

            String responseBody = convertStreamToString(inputStream);

            return new OfbResponse(responseCode, responseBody);

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String convertStreamToString(InputStream inputStream) {
        java.util.Scanner s = new java.util.Scanner(inputStream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static void main(String[] args) {
        //GET Example
        String getUrl = "https://httpbin.org/get";
        Map<String, String> getHeaders = new HashMap<>();
        getHeaders.put("Content-Type", "application/json");
        String getTag = "getTag";

        ListenableFuture<OfbResponse> getResponseFuture = OfbHttp.execute(new OfbRequest("GET", getUrl, getHeaders, getTag));

            Futures.addCallback(getResponseFuture, new ExtendedFutureCallback<OfbResponse>() {
                @Override
                public void onSuccess(OfbResponse ofbResponse) {
                    System.out.println("GET Success: " + ofbResponse.getResponseBody());
                }

                @Override
                public void onFailure(Throwable thrown) {
                    System.err.println("GET Failure: " + thrown.getMessage());
                }

                @Override
                public void onCancel() {
                    System.err.println("GET Cancel");
                }
            }, executorService);

        //POST Example
        String postUrl = "https://httpbin.org/post";
        Map<String, String> postHeaders = new HashMap<>();
        postHeaders.put("Content-Type", "application/json");
        String postBody = "{\"key\":\"value\"}";
        String postTag = "postTag";

        ListenableFuture<OfbResponse> postResponseFuture = OfbHttp.execute(new OfbRequest("POST", postBody, postUrl, postHeaders, postTag));

        Futures.addCallback(postResponseFuture, new ExtendedFutureCallback<OfbResponse>() {
            @Override
            public void onSuccess(OfbResponse ofbResponse) {
                System.out.println("POST Success: " + ofbResponse.getResponseBody());
            }

            @Override
            public void onFailure(Throwable thrown) {
                System.err.println("POST Failure: " + thrown.getMessage());
            }
            @Override
            public void onCancel() {
                System.out.println("POST Cancel");
            }
        }, executorService);

        //Cancel Example
        executorService.submit(() -> {
            System.out.println("Cancelling GET request");
            OfbHttp.cancel(getTag);
        });

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        executorService.shutdownNow();
    }
}