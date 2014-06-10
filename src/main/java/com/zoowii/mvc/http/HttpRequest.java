package com.zoowii.mvc.http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zoowii.util.FileUtil;
import com.zoowii.util.FormDecoder;
import com.zoowii.util.Pair;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class HttpRequest {
    private HttpServletRequest httpServletRequest;
    private List<Pair<String, Object>> params = new ArrayList<Pair<String, Object>>();
    private String cachedStringBody = null;
    private JSON cachedJsonBody = null;
    private JSONObject cachedForm = null;

    public List<Pair<String, Object>> getParams() {
        return params;
    }

    public Object getParam(String name) {
        for (Pair<String, Object> pair : params) {
            if (pair.getLeft().equals(name)) {
                return pair.getRight();
            }
        }
        return null;
    }

    public String getStringBody() {
        if (cachedStringBody == null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                FileUtil.writeFullyStream(getInputStream(), byteArrayOutputStream);
                cachedStringBody = new String(byteArrayOutputStream.toByteArray(), "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
                cachedStringBody = "";
            }
        }
        return cachedStringBody;
    }

    public JSON getJSONBody() {
        if (cachedJsonBody == null) {
            try {
                String bodyStr = getStringBody();
                cachedJsonBody = (JSON) JSON.parse(bodyStr);
            } catch (Exception e) {
                e.printStackTrace();
                cachedJsonBody = new JSONObject();
            }
        }
        return cachedJsonBody;
    }

    public JSONObject getFormData() {
        if (cachedForm == null) {
            try {
                String bodyStr = getStringBody();
                cachedForm = FormDecoder.decodeFormString(bodyStr);
            } catch (Exception e) {
                e.printStackTrace();
                cachedForm = new JSONObject();
            }
        }
        return cachedForm;
    }

    public List<String> getPostParamArray(String name) {
        JSONObject formData = getFormData();
        if (!formData.containsKey(name)) {
            return null;
        }
        JSONArray values = formData.getJSONArray(name);
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < values.size(); ++i) {
            result.add(values.getString(i));
        }
        return result;
    }

    public String getPostParam(String name) {
        List<String> values = getPostParamArray(name);
        if (values == null) {
            return null;
        }
        if (values.size() > 0) {
            return values.get(0);
        } else {
            return null;
        }
    }

    public Boolean getBoolPostParam(String name, Boolean defaultValue) {
        String val = getPostParam(name);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Boolean.valueOf(val);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public String getStringParam(String name, String defaultValue) {
        Object val = getParam(name);
        return val != null ? val.toString() : defaultValue;
    }

    public String getStringParam(String name) {
        return getStringParam(name, null);
    }

    public Integer getIntParam(String name, Long defaultValue) {
        return getIntParam(name, defaultValue.intValue());
    }

    public Integer getIntParam(String name, Integer defaultValue) {
        String val = getStringParam(name);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Integer.valueOf(val);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public Integer getIntParam(String name) {
        return getIntParam(name, (Integer) null);
    }

    public void setParams(List<Pair<String, Object>> params) {
        this.params = params;
    }

    public HttpRequest(HttpServletRequest httpServletRequest) {
        this.httpServletRequest = httpServletRequest;
    }

    public HttpServletRequest getHttpServletRequest() {
        return httpServletRequest;
    }

    public void setHttpServletRequest(HttpServletRequest httpServletRequest) {
        this.httpServletRequest = httpServletRequest;
    }

    public AsyncContext startAsync() {
        return this.getHttpServletRequest().startAsync();
    }

    /**
     * get parameter from query string
     */
    public String getParameter(String name) {
        return getHttpServletRequest().getParameter(name);
    }

    public InputStream getInputStream() throws IOException {
        return getHttpServletRequest().getInputStream();
    }

    public String getHeader(String name) {
        return getHttpServletRequest().getHeader(name);
    }

    public HttpSession session() {
        return getHttpServletRequest().getSession();
    }

    public Object session(String key) {
        return session().getAttribute(key);
    }

    public void session(String key, Object val) {
        session().setAttribute(key, val);
    }

    public void clearSession() {
        session().invalidate();
    }

    /**
     * 获取用户打开的http://domain[:port]
     */
    public String getFinalHostWithPort() {
        String reqUrl = getHttpServletRequest().getRequestURL().toString();
        try {
            URI uri = new URI(reqUrl);
            String url = uri.getScheme() + "://" + uri.getHost();
            if (uri.getPort() != 80) {
                url += ":" + uri.getPort();
            }
            return url;
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return reqUrl; // FIXME
        }
    }

}
