/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.apache.commons.io.IOUtils;
import org.sonar.api.server.ws.Response;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.utils.text.XmlWriter;

public class DumbResponse implements Response {
  private InMemoryStream stream;

  private final ByteArrayOutputStream output = new ByteArrayOutputStream();

  private Map<String, String> headers = Maps.newHashMap();

  public class InMemoryStream implements Response.Stream {
    private String mediaType;

    private int status = 200;

    @CheckForNull
    public String mediaType() {
      return mediaType;
    }

    public int status() {
      return status;
    }

    @Override
    public Response.Stream setMediaType(String s) {
      this.mediaType = s;
      return this;
    }

    @Override
    public Response.Stream setStatus(int i) {
      this.status = i;
      return this;
    }

    @Override
    public OutputStream output() {
      return output;
    }

    public String outputAsString() {
      return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
  }

  @Override
  public JsonWriter newJsonWriter() {
    return JsonWriter.of(new OutputStreamWriter(output, StandardCharsets.UTF_8));
  }

  @Override
  public XmlWriter newXmlWriter() {
    return XmlWriter.of(new OutputStreamWriter(output, StandardCharsets.UTF_8));
  }

  @Override
  public InMemoryStream stream() {
    if (stream == null) {
      stream = new InMemoryStream();
    }
    return stream;
  }

  @Override
  public Response noContent() {
    stream().setStatus(HttpURLConnection.HTTP_NO_CONTENT);
    IOUtils.closeQuietly(output);
    return this;
  }

  public String outputAsString() {
    return new String(output.toByteArray(), StandardCharsets.UTF_8);
  }

  @Override
  public Response setHeader(String name, String value) {
    headers.put(name, value);
    return this;
  }

  public Collection<String> getHeaderNames() {
    return headers.keySet();
  }

  @CheckForNull
  public String getHeader(String name){
    return headers.get(name);
  }

  public byte[] getFlushedOutput() {
    try {
      output.flush();
      return output.toByteArray();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
}
