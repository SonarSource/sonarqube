/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.api.server.ws;

import com.google.common.collect.Maps;

import javax.annotation.CheckForNull;
import java.util.Map;

public class SimpleRequest extends Request {

  private String method = "GET";
  private Map<String, String> params = Maps.newHashMap();

  @Override
  public String method() {
    return method;
  }

  public SimpleRequest setMethod(String s) {
    this.method = s;
    return this;
  }

  public SimpleRequest setParams(Map<String, String> m) {
    this.params = m;
    return this;
  }

  public SimpleRequest setParam(String key, @CheckForNull String value) {
    if (value != null) {
      params.put(key, value);
    }
    return this;
  }

  @Override
  @CheckForNull
  public String param(String key) {
    return params.get(key);
  }
}
