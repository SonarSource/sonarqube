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
package org.sonar.server.ws;

import javax.servlet.http.HttpServletRequest;

import java.util.Map;

public class ServletRequest extends InternalRequest {

  private final HttpServletRequest source;
  private final Map<String, String> params;

  public ServletRequest(HttpServletRequest source, Map<String, String> params) {
    this.source = source;
    this.params = params;
  }

  @Override
  public String method() {
    return source.getMethod();
  }

  @Override
  public String param(String key) {
    String parameter = source.getParameter(key);
    if (parameter == null) {
      parameter = params.get(key);
    }
    return parameter;
  }

}
