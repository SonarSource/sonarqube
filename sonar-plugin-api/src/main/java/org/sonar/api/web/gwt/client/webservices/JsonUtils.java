/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.web.gwt.client.webservices;

import java.util.Date;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.http.client.URL;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;

public final class JsonUtils {
  private static int requestId = 0;

  private JsonUtils() {

  }

  public interface JSONHandler {
    void onResponse(JavaScriptObject obj);

    void onTimeout();

    void onError(int errorCode, String errorMessage);
  }

  public static void requestJson(String url, JSONHandler handler) {
    if (!url.endsWith("&") && !url.endsWith("?")) {
      url += "&";
    }
    if (!url.contains("format=json")) {
      url += "format=json&";
    }
    if (!url.contains("callback=")) {
      //IMPORTANT : the url should ended with ?callback= or &callback= for JSONP calls
      url += "callback=";
    }
    makeJSONRequest(requestId++, URL.encode(url), handler);
  }

  public static native void makeJSONRequest(int requestId, String url, JSONHandler handler) /*-{
      var callback = "callback" + requestId;

      // create SCRIPT tag, and set SRC attribute equal to JSON feed URL + callback function name
      var script = document.createElement("script");
      script.setAttribute("src", url+callback);
      script.setAttribute("type", "text/javascript");

      window[callback] = function(jsonObj) {
        @org.sonar.api.web.gwt.client.webservices.JsonUtils::dispatchJSON(Lcom/google/gwt/core/client/JavaScriptObject;Lorg/sonar/api/web/gwt/client/webservices/JsonUtils$JSONHandler;)(jsonObj, handler);
        window[callback + "done"] = true;
      }

      setTimeout(function() {
        if (!window[callback + "done"]) {
          handler.@org.sonar.api.web.gwt.client.webservices.JsonUtils.JSONHandler::onTimeout();
        }

        // cleanup
        document.body.removeChild(script);
        if (window[callback]) {
          delete window[callback];
        }
        if (window[callback + "done"]) {
          delete window[callback + "done"];
        }
      }, 120000);

      document.body.appendChild(script);
    }-*/;

  public static void dispatchJSON(JavaScriptObject jsonObj, JSONHandler handler) {
    JSONObject obj = new JSONObject(jsonObj);
    if (obj.isObject() != null) {
      if (obj.containsKey("err_code")) {
        handler.onError(new Double(obj.get("err_code").isNumber().doubleValue()).intValue(),
            obj.get("err_msg").isString().stringValue());
        return;
      }
    }
    handler.onResponse(jsonObj);
  }

  public static String getString(JSONObject json, String field) {
    JSONValue jsonValue;
    JSONString jsonString;
    if ((jsonValue = json.get(field)) == null) {
      return null;
    }
    if ((jsonString = jsonValue.isString()) == null) {
      JSONNumber jsonNumber = jsonValue.isNumber();
      return jsonNumber != null ? jsonNumber.toString() : null;
    }
    return jsonString.stringValue();
  }
  
  public static Date getDate(JSONObject json, String field) {
    DateTimeFormat frmt = DateTimeFormat.getFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    String date = getString(json, field);
    if (date!=null && date.endsWith("Z") && date.length()>2) {
      // see SONAR-1182
      date = date.substring(0, date.length()-2) + "+00:00";
    }
    return frmt.parse(date);
  }
  
  public static Boolean getBoolean(JSONObject json, String field) {
    JSONValue jsonValue;
    JSONBoolean jsonBoolean;
    if ((jsonValue = json.get(field)) == null) {
      return null;
    }
    if ((jsonBoolean = jsonValue.isBoolean()) == null) {
      return null;
    }
    return jsonBoolean.booleanValue();
  }

  public static Double getDouble(JSONObject json, String field) {
    JSONValue jsonValue;
    JSONNumber jsonNumber;
    if ((jsonValue = json.get(field)) == null) {
      return null;
    }
    if ((jsonNumber = jsonValue.isNumber()) == null) {
      return null;
    }
    return jsonNumber.doubleValue();
  }

  public static Integer getInteger(JSONObject json, String field) {
    final Double d = getDouble(json, field);
    if (d != null) {
      return d.intValue();
    }
    return null;
  }

  public static JSONObject getArray(JSONValue json, int i) {
    if (json instanceof JSONArray) {
      return ((JSONArray) json).get(i).isObject();
    }
    if (json instanceof JSONObject) {
      return ((JSONObject) json).get(Integer.toString(i)).isObject();
    }
    throw new JavaScriptException("Not implemented");
  }

  public static int getArraySize(JSONValue array) {
    if (array instanceof JSONArray) {
      return ((JSONArray) array).size();
    }
    if (array instanceof JSONObject) {
      return ((JSONObject) array).size();
    }
    throw new JavaScriptException("Not implemented");
  }

}
