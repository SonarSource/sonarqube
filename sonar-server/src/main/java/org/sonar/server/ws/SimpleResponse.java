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

import javax.servlet.http.HttpServletResponse;
import java.io.StringWriter;
import java.io.Writer;

public class SimpleResponse extends Response {
  private int httpStatus = HttpServletResponse.SC_OK;
  private final Writer writer = new StringWriter();

  @Override
  public JsonWriter newJsonWriter() {
    return JsonWriter.of(writer);
  }

  @Override
  public XmlWriter newXmlWriter() {
    return XmlWriter.of(writer);
  }

  @Override
  public Writer writer() {
    return writer;
  }

  @Override
  public int status() {
    return httpStatus;
  }

  @Override
  public Response setStatus(int httpStatus) {
    this.httpStatus = httpStatus;
    return this;
  }
}
