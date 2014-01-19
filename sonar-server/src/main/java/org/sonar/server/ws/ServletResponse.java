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

import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.utils.text.XmlWriter;
import org.sonar.api.web.ws.Response;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

public class ServletResponse implements Response {

  private final HttpServletResponse source;

  public ServletResponse(HttpServletResponse hsr) {
    this.source = hsr;
  }

  @Override
  public JsonWriter newJsonWriter() {
    try {
      return JsonWriter.of(source.getWriter());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public XmlWriter newXmlWriter() {
    try {
      return XmlWriter.of(source.getWriter());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public OutputStream output() {
    try {
      return source.getOutputStream();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public int status() {
    return source.getStatus();
  }

  @Override
  public Response setStatus(int httpStatus) {
    source.setStatus(httpStatus);
    return this;
  }
}
