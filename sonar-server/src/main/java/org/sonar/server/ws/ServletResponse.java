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

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.sonar.api.server.ws.Response;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.utils.text.XmlWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;

public class ServletResponse implements Response {

  private final HttpServletResponse source;

  public ServletResponse(HttpServletResponse hsr) {
    this.source = hsr;
  }

  @Override
  public JsonWriter newJsonWriter() {
    return JsonWriter.of(new Buffer(source));
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
  public OutputStream stream() {
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

  private static class Buffer extends StringWriter {
    private final HttpServletResponse httpResponse;

    public Buffer(HttpServletResponse httpResponse) {
      this.httpResponse = httpResponse;
    }

    @Override
    public void close() throws IOException {
      super.close();

      ServletOutputStream stream = null;
      try {
        stream = httpResponse.getOutputStream();
        IOUtils.copy(new ByteArrayInputStream(toString().getBytes(Charsets.UTF_8)), stream);
        stream.flush();
      } catch (IOException e) {
        throw new IllegalStateException("Fail to flush buffer", e);
      } finally {
        IOUtils.closeQuietly(stream);
      }
    }
  }
}
