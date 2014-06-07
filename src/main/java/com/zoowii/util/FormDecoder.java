package com.zoowii.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

public class FormDecoder {
    /**
     * 将a=b&c=d&c=e这种形式的form-encoded的字符串(Http form encoded内容就是这个形式)转成JSON { 'key': [value1, value2] }
     */
    public static JSONObject decodeFormString(String formString) throws UnsupportedEncodingException {
        JSONObject jsonObject = new JSONObject();
        List<String> strItems = ListUtil.filter(ListUtil.list(formString.split("&")), ListUtil.notEmptyString);
        for (String strItem : strItems) {
            List<String> itemPair = ListUtil.filter(ListUtil.list(strItem.split("=")), ListUtil.notEmptyString);
            if (itemPair.size() < 1) {
                continue;
            }
            String key = URLDecoder.decode(itemPair.get(0), "UTF-8");
            JSONArray valueList;
            if (jsonObject.containsKey(key)) {
                valueList = jsonObject.getJSONArray(key);
            } else {
                valueList = new JSONArray();
                jsonObject.put(key, valueList);
            }
            String value;
            if (itemPair.size() < 2) {
                value = "";
            } else {
                value = URLDecoder.decode(itemPair.get(1), "UTF-8");
            }
            valueList.add(value);
        }
        return jsonObject;
    }
}
