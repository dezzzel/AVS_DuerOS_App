package com.wistron.demo.tool.teddybear.dcs.http.request;

import com.wistron.demo.tool.teddybear.dcs.http.exceptions.Exceptions;

import java.util.Map;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Created by ivanjlzhang on 17-9-23.
 */

public class PostStringRequest extends OkHttpRequest {
    private static MediaType MEDIA_TYPE_PLAIN = MediaType.parse("text/plain;charset=utf-8");
    private String content;
    private MediaType mediaType;

    public PostStringRequest(String url,
                             Object tag,
                             Map<String, String> params,
                             Map<String, String> headers,
                             String content,
                             MediaType mediaType,
                             int id) {
        super(url, tag, params, headers, id);
        this.content = content;
        this.mediaType = mediaType;
        if (this.content == null) {
            Exceptions.illegalArgument("the content can not be null !");
        }
        if (this.mediaType == null) {
            this.mediaType = MEDIA_TYPE_PLAIN;
        }
    }

    @Override
    protected RequestBody buildRequestBody() {
        return RequestBody.create(mediaType, content);
    }

    @Override
    protected Request buildRequest(RequestBody requestBody) {
        return builder.post(requestBody).build();
    }
}
