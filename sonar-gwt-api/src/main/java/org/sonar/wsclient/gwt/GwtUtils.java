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
package org.sonar.wsclient.gwt;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import org.sonar.gwt.JsonUtils;
import org.sonar.wsclient.services.WSUtils;

import java.util.Date;
import java.util.Set;

public class GwtUtils extends WSUtils {
  @Override
  public String format(Date date, String format) {
    return DateTimeFormat.getFormat(format).format(date);
  }

  @Override
  public String encodeUrl(String url) {
    return com.google.gwt.http.client.URL.encode(url);
  }

  @Override
  public Object getField(Object json, String field) {
    return ((JSONObject) json).get(field);
  }

  @Override
  public String getString(Object json, String field) {
    return JsonUtils.getString((JSONObject) json, field);
  }

  @Override
  public Boolean getBoolean(Object json, String field) {
    return JsonUtils.getBoolean((JSONObject) json, field);
  }

  @Override
  public Integer getInteger(Object json, String field) {
    return JsonUtils.getInteger((JSONObject) json, field);
  }

  @Override
  public Double getDouble(Object json, String field) {
    return JsonUtils.getDouble((JSONObject) json, field);
  }

  @Override
  public Long getLong(Object json, String field) {
    Double d = JsonUtils.getDouble((JSONObject) json, field);
    if (d != null) {
      return d.longValue();
    }
    return null;
  }

  @Override
  public Date getDateTime(Object json, String field) {
    return JsonUtils.getDate((JSONObject) json, field);
  }

  @Override
  public int getArraySize(Object array) {
    return JsonUtils.getArraySize((JSONValue) array);
  }

  @Override
  public Object getArrayElement(Object array, int i) {
    return JsonUtils.getArray((JSONValue) array, i);
  }

  @Override
  public Object parse(String jsonStr) {
    return JSONParser.parse(jsonStr);
  }

  @Override
  public Set<String> getFields(Object json) {
    return ((JSONObject) json).keySet();
  }

}
