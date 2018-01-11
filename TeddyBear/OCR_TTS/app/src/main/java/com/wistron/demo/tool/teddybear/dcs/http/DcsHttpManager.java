package com.wistron.demo.tool.teddybear.dcs.http;

import com.wistron.demo.tool.teddybear.dcs.http.builder.GetBuilder;
import com.wistron.demo.tool.teddybear.dcs.http.builder.PostMultipartBuilder;
import com.wistron.demo.tool.teddybear.dcs.http.callback.DcsCallback;
import com.wistron.demo.tool.teddybear.dcs.http.intercepter.LoggingInterceptor;
import com.wistron.demo.tool.teddybear.dcs.http.request.RequestCall;
import com.wistron.demo.tool.teddybear.dcs.http.utils.Platform;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Response;

/**
 * Created by ivanjlzhang on 17-9-22.
 */

public class DcsHttpManager {
    // 默认时间，比如超时时间
    public static final long DEFAULT_MILLISECONDS = 60 * 1000L;
    private OkHttpClient mOkHttpClient;
    private Platform mPlatform;

    private static class DcsHttpManagerHolder {
        private static final DcsHttpManager INSTANCE = new DcsHttpManager();
    }

    public static DcsHttpManager getInstance() {
        return DcsHttpManagerHolder.INSTANCE;
    }

    private DcsHttpManager() {
        if (mOkHttpClient == null) {
            // http数据log，日志中打印出HTTP请求&响应数据
            // HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            // 包含header、body数据
            mOkHttpClient = new OkHttpClient.Builder()
                    .retryOnConnectionFailure(false)
                    .readTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS)
                    .writeTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS)
                    .connectTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS)
                    // .addInterceptor(new RetryInterceptor(3))
                    .addInterceptor(new LoggingInterceptor())
                    // .connectionPool(new ConnectionPool(0, 1, TimeUnit.NANOSECONDS))
                    .build();
        }
        mPlatform = Platform.get();
    }

    public OkHttpClient getOkHttpClient() {
        return mOkHttpClient;
    }

    public static GetBuilder get() {
        return new GetBuilder();
    }

    public static PostMultipartBuilder post() {
        return new PostMultipartBuilder();
    }

    public void execute(final RequestCall requestCall, DcsCallback dcsCallback) {

        if (dcsCallback == null) {
            dcsCallback = DcsCallback.backDefaultCallBack;
        }
        final DcsCallback finalDCSCallback = dcsCallback;
        final int id = requestCall.getOkHttpRequest().getId();

        requestCall.getCall().enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                sendFailResultCallback(call, e, finalDCSCallback, id);
            }

            @Override
            public void onResponse(final Call call, final Response response) {
                try {
                    if (call.isCanceled()) {
                        sendFailResultCallback(call, new IOException("Canceled!"), finalDCSCallback, id);
                        return;
                    }
                    if (!finalDCSCallback.validateResponse(response, id)) {
                        IOException exception = new IOException("request failed , response's code is : "
                                + response.code());
                        sendFailResultCallback(call, exception, finalDCSCallback, id);
                        return;
                    }
                    sendSuccessResultCallback(response, finalDCSCallback, id);
                    Object o = finalDCSCallback.parseNetworkResponse(response, id);
                } catch (Exception e) {
                    sendFailResultCallback(call, e, finalDCSCallback, id);
                } finally {
                    if (response.body() != null) {
                        response.body().close();
                    }
                }
            }
        });
    }

    /**
     * 网络请求失败处理
     *
     * @param call        okhttp的请求call
     * @param e           异常信息
     * @param dcsCallback 回调
     * @param id          请求标识
     */
    private void sendFailResultCallback(final Call call,
                                        final Exception e,
                                        final DcsCallback dcsCallback,
                                        final int id) {
        if (dcsCallback == null) {
            return;
        }
        mPlatform.execute(new Runnable() {
            @Override
            public void run() {
                dcsCallback.onError(call, e, id);
                dcsCallback.onAfter(id);
            }
        });
    }

    /**
     * 网络请求成功处理
     *
     * @param object      object 返回的对象
     * @param dcsCallback dcsCallback 回调
     * @param id          id 请求标识
     */
    private void sendSuccessResultCallback(final Object object,
                                           final DcsCallback dcsCallback,
                                           final int id) {
        if (dcsCallback == null) {
            return;
        }
        mPlatform.execute(new Runnable() {
            @Override
            public void run() {
                dcsCallback.onResponse(object, id);
                dcsCallback.onAfter(id);
            }
        });
    }

    /**
     * 取消请求
     *
     * @param tag 请求的时候设置的tag
     */
    public void cancelTag(Object tag) {
        for (Call call : mOkHttpClient.dispatcher().queuedCalls()) {
            if (tag.equals(call.request().tag())) {
                call.cancel();
            }
        }
        for (Call call : mOkHttpClient.dispatcher().runningCalls()) {
            if (tag.equals(call.request().tag())) {
                call.cancel();
            }
        }
    }
}
