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

package org.sonar.wsclient.source.internal;

import org.json.simple.JSONValue;
import org.sonar.wsclient.internal.EncodingUtils;
import org.sonar.wsclient.internal.HttpRequestFactory;
import org.sonar.wsclient.source.Scm;
import org.sonar.wsclient.source.Source;
import org.sonar.wsclient.source.SourceClient;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefaultSourceClient implements SourceClient {

  private final HttpRequestFactory requestFactory;

  public DefaultSourceClient(HttpRequestFactory requestFactory) {
    this.requestFactory = requestFactory;
  }

  @Override
  public List<Source> show(String key, @Nullable String from, @Nullable String to) {
    Map<String, Object> params = EncodingUtils.toMap("key", key, "from", from, "to", to);
    String json = requestFactory.get("/api/sources/show", params);

    List<Source> sources = new ArrayList<Source>();
    Map jRoot = (Map) JSONValue.parse(json);
    List<List> jsonSources = (List) jRoot.get("sources");
    if (jsonSources != null) {
      for (List jSource : jsonSources) {
        sources.add(new DefaultSource(jSource));
      }
    }
    return sources;
  }

  @Override
  public List<Scm> scm(String key, @Nullable String from, @Nullable String to, @Nullable Boolean groupCommits) {
    Map<String, Object> params = EncodingUtils.toMap("key", key, "from", from, "to", to);
    params.put("group_commits", groupCommits);
    String json = requestFactory.get("/api/sources/scm", params);

    List<Scm> result = new ArrayList<Scm>();
    Map jRoot = (Map) JSONValue.parse(json);
    List<List> jsonResult = (List) jRoot.get("scm");
    if (jsonResult != null) {
      for (List line : jsonResult) {
        result.add(new DefaultScm(line));
      }
    }
    return result;
  }

}
