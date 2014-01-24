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

  private class ServletStream implements Stream {

    @Override
    public Stream setMediaType(String s) {
      source.setContentType(s);
      return this;
    }

    @Override
    public OutputStream output() {
      try {
        return source.getOutputStream();
      } catch (IOException e) {
        throw new IllegalStateException("Fail to get servlet output stream", e);
      }
    }
  }

  private final HttpServletResponse source;
  private int httpStatus = 200;

  public ServletResponse(HttpServletResponse hsr) {
    this.source = hsr;
  }

  @Override
  public JsonWriter newJsonWriter() {
    return JsonWriter.of(new Buffer("application/json"));
  }

  @Override
  public XmlWriter newXmlWriter() {
    return XmlWriter.of(new Buffer("application/xml"));
  }

  @Override
  public Stream stream() {
    return new ServletStream();
  }

  @Override
  public int status() {
    return source.getStatus();
  }

  @Override
  public Response setStatus(int httpStatus) {
    this.httpStatus = httpStatus;
    return this;
  }

  @Override
  public void noContent() {
    setStatus(204);
    try {
      this.source.flushBuffer();
    } catch(IOException ioex) {
      throw new IllegalStateException("Fail to send 'no content' to client");
    }
  }

  private class Buffer extends StringWriter {
    private final String mediaType;

    private Buffer(String mediaType) {
      this.mediaType = mediaType;
    }

    @Override
    public void close() throws IOException {
      super.close();

      source.setStatus(httpStatus);
      source.setContentType(mediaType);
      ServletOutputStream stream = null;
      try {
        stream = source.getOutputStream();
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
