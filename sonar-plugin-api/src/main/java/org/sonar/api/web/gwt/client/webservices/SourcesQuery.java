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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import org.sonar.api.web.gwt.client.Utils;

import java.util.Map;
import java.util.TreeMap;

public final class SourcesQuery extends AbstractResourceQuery<FileSource> {

  private Integer from;
  private Integer length;
  private boolean color;

  public static SourcesQuery get(String resourceKey) {
    return new SourcesQuery(resourceKey);
  }

  private SourcesQuery(String resourceKey) {
    super(resourceKey);
  }

  public SourcesQuery setFrom(Integer from) {
    this.from = from;
    return this;
  }

  public SourcesQuery setLength(Integer length) {
    this.length = length;
    return this;
  }

  public SourcesQuery setColor(boolean color) {
    this.color = color;
    return this;
  }

  @Override
  public String toString() {
    String url = Utils.getServerApiUrl() + "/sources?resource=" + getResourceKey() + "&";
    if (length > 0) {
      url += "from=" + from + "&to=" + (from + length) + "&";
    }
    if (color) {
      url += "color=true&";
    }
    return url;
  }

  @Override
  public void execute(QueryCallBack<FileSource> callback) {
    JsonUtils.requestJson(this.toString(), new JSONHandlerDispatcher<FileSource>(callback) {
      @Override
      public FileSource parseResponse(JavaScriptObject obj) {
        return parseLines(obj);
      }
    });
  }

  private FileSource parseLines(JavaScriptObject obj) {
    Map<Integer, String> sourceLines = new TreeMap<Integer, String>();
    FileSource src = new FileSource(sourceLines);
    JSONArray jsonArray = new JSONArray(obj);
    if (jsonArray.size() == 0) return src;
    JSONObject sources = jsonArray.get(0).isObject();
    if (sources.size() == 0) return src;
    int maxSize = new Double(Math.pow(2, 16)).intValue();
    int currentLine = from == 0 ? 1 : from;
    while (currentLine < maxSize) {
      JSONValue line = sources.get(Integer.toString(currentLine));
      if (line == null) {
        break;
      }
      sourceLines.put(currentLine++, line.isString().stringValue());
    }
    return src;
  }

}
