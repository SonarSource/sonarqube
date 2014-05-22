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

package org.sonar.wsclient.test.internal;

import org.json.simple.JSONValue;
import org.sonar.wsclient.internal.EncodingUtils;
import org.sonar.wsclient.internal.HttpRequestFactory;
import org.sonar.wsclient.test.Coverage;
import org.sonar.wsclient.test.CoverageClient;
import org.sonar.wsclient.test.CoverageShowQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefaultCoverageClient implements CoverageClient {

  private final HttpRequestFactory requestFactory;

  public DefaultCoverageClient(HttpRequestFactory requestFactory) {
    this.requestFactory = requestFactory;
  }

  @Override
  public List<Coverage> show(CoverageShowQuery query) {
    Map<String, Object> params = EncodingUtils.toMap("key", query.key(), "from", query.from(), "to", query.to(), "type", query.type());
    String jsonResult = requestFactory.get("/api/coverage/show", params);

    List<Coverage> coverages = new ArrayList<Coverage>();
    Map jRoot = (Map) JSONValue.parse(jsonResult);
    List<List> jsonList = (List) jRoot.get("coverage");
    if (jsonList != null) {
      for (List json : jsonList) {
        coverages.add(new DefaultCoverage(json));
      }
    }
    return coverages;
  }

}
