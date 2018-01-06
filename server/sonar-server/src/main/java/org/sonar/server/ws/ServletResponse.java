/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.ws;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import javax.servlet.http.HttpServletResponse;
import org.sonar.api.server.ws.Response;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.utils.text.XmlWriter;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonarqube.ws.MediaTypes.JSON;
import static org.sonarqube.ws.MediaTypes.XML;

public class ServletResponse implements Response {

  private final ServletStream stream;

  public ServletResponse(HttpServletResponse response) {
    stream = new ServletStream(response);
  }

  public static class ServletStream implements Stream {
    private final HttpServletResponse response;

    public ServletStream(HttpServletResponse response) {
      this.response = response;
      this.response.setStatus(200);
      // SONAR-6964 WS should not be cached by browser
      this.response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    }

    @Override
    public ServletStream setMediaType(String s) {
      this.response.setContentType(s);
      return this;
    }

    @Override
    public ServletStream setStatus(int httpStatus) {
      this.response.setStatus(httpStatus);
      return this;
    }

    @Override
    public OutputStream output() {
      try {
        return response.getOutputStream();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    HttpServletResponse response() {
      return response;
    }

    public ServletStream reset() {
      response.reset();
      return this;
    }
  }

  @Override
  public JsonWriter newJsonWriter() {
    stream.setMediaType(JSON);
    return JsonWriter.of(new CacheWriter(new OutputStreamWriter(stream.output(), StandardCharsets.UTF_8)));
  }

  @Override
  public XmlWriter newXmlWriter() {
    stream.setMediaType(XML);
    return XmlWriter.of(new OutputStreamWriter(stream.output(), UTF_8));
  }

  @Override
  public ServletStream stream() {
    return stream;
  }

  @Override
  public Response noContent() {
    stream.setStatus(204);
    return this;
  }

  @Override
  public Response setHeader(String name, String value) {
    stream.response().setHeader(name, value);
    return this;
  }

  @Override
  public Collection<String> getHeaderNames() {
    return stream.response().getHeaderNames();
  }

  @Override
  public String getHeader(String name) {
    return stream.response().getHeader(name);
  }
}
