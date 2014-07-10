/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.wsclient;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.sonar.wsclient.services.WSUtils;
import org.sonar.wsclient.unmarshallers.JsonUtils;

import javax.annotation.CheckForNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

public final class JdkUtils extends WSUtils {

  @Override
  public String format(Date date, String format) {
    SimpleDateFormat dateFormat = new SimpleDateFormat(format);
    return dateFormat.format(date);
  }

  @Override
  public String encodeUrl(String url) {
    try {
      return URLEncoder.encode(url, "UTF-8");

    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Encoding not supported", e);
    }
  }

  @Override
  public Object getField(Object json, String field) {
    return ((JSONObject) json).get(field);
  }

  @Override
  @CheckForNull
  public String getString(Object json, String field) {
    return JsonUtils.getString((JSONObject) json, field);
  }

  @Override
  @CheckForNull
  public Boolean getBoolean(Object json, String field) {
    return JsonUtils.getBoolean((JSONObject) json, field);
  }

  @Override
  @CheckForNull
  public Integer getInteger(Object json, String field) {
    return JsonUtils.getInteger((JSONObject) json, field);
  }

  @Override
  @CheckForNull
  public Double getDouble(Object json, String field) {
    return JsonUtils.getDouble((JSONObject) json, field);
  }

  @Override
  @CheckForNull
  public Long getLong(Object json, String field) {
    return JsonUtils.getLong((JSONObject) json, field);
  }

  @Override
  @CheckForNull
  public Date getDateTime(Object json, String field) {
    return JsonUtils.getDateTime((JSONObject) json, field);
  }

  @Override
  public int getArraySize(Object array) {
    return ((ArrayList) array).size();
  }

  @Override
  public Object getArrayElement(Object array, int i) {
    return ((ArrayList) array).get(i);
  }

  @Override
  public Object parse(String jsonStr) {
    return JSONValue.parse(jsonStr);
  }

  @Override
  public Set<String> getFields(Object json) {
    return ((JSONObject) json).keySet();
  }
}
