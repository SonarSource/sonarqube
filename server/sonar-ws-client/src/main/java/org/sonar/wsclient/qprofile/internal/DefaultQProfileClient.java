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

package org.sonar.wsclient.qprofile.internal;

import org.json.simple.JSONValue;
import org.sonar.wsclient.internal.HttpRequestFactory;
import org.sonar.wsclient.qprofile.QProfileClient;
import org.sonar.wsclient.qprofile.QProfileResult;

import java.util.Collections;
import java.util.Map;

public class DefaultQProfileClient implements QProfileClient {

  private final HttpRequestFactory requestFactory;

  public DefaultQProfileClient(HttpRequestFactory requestFactory) {
    this.requestFactory = requestFactory;
  }

  @Override
  public QProfileResult restoreDefault(String language) {
    String json = requestFactory.post("/api/qualityprofiles/restore_default", Collections.singletonMap("language", (Object) language));
    Map jsonRoot = (Map) JSONValue.parse(json);
    return new DefaultQProfileResult(jsonRoot);
  }

}
