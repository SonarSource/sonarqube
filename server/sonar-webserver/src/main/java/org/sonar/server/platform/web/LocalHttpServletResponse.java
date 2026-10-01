/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.web;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.LocalConnector;

import static org.apache.commons.lang3.StringUtils.substringBefore;

/**
 * In-memory {@link HttpServletResponse} filled by the Web API v2 servlet and returned as a {@link LocalConnector.LocalResponse}.
 */
class LocalHttpServletResponse extends AbstractLocalHttpServletResponse implements LocalConnector.LocalResponse {

  private static final String CONTENT_TYPE_HEADER = "Content-Type";

  private final ByteArrayOutputStream output = new ByteArrayOutputStream();
  private final Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
  private int status = SC_OK;
  private String characterEncoding = StandardCharsets.UTF_8.name();
  private boolean committed = false;
  private PrintWriter writer = null;
  private ServletOutputStream outputStream = null;

  @CheckForNull
  @Override
  public String getMediaType() {
    String contentType = getContentType();
    return contentType == null ? null : substringBefore(contentType, ";").trim();
  }

  @Override
  public byte[] getBytes() {
    if (writer != null) {
      writer.flush();
    }
    return output.toByteArray();
  }

  @Override
  public int getStatus() {
    return status;
  }

  @Override
  public void setStatus(int sc) {
    if (!committed) {
      this.status = sc;
    }
  }

  @Override
  public void sendError(int sc, String msg) {
    sendError(sc);
  }

  @Override
  public void sendError(int sc) {
    checkNotCommitted();
    resetBuffer();
    this.status = sc;
    this.committed = true;
  }

  @CheckForNull
  @Override
  public String getHeader(String name) {
    List<String> values = headers.get(name);
    return values == null || values.isEmpty() ? null : values.get(0);
  }

  @Override
  public Collection<String> getHeaders(String name) {
    return new ArrayList<>(headers.getOrDefault(name, List.of()));
  }

  @Override
  public Collection<String> getHeaderNames() {
    return new ArrayList<>(headers.keySet());
  }

  @Override
  public boolean containsHeader(String name) {
    return headers.containsKey(name);
  }

  @Override
  public void setHeader(String name, @Nullable String value) {
    if (value == null) {
      headers.remove(name);
      return;
    }
    List<String> values = new ArrayList<>();
    values.add(value);
    headers.put(name, values);
  }

  @Override
  public void addHeader(String name, @Nullable String value) {
    if (value != null) {
      headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
    }
  }

  @Override
  public void setDateHeader(String name, long date) {
    setHeader(name, String.valueOf(date));
  }

  @Override
  public void addDateHeader(String name, long date) {
    addHeader(name, String.valueOf(date));
  }

  @Override
  public void setIntHeader(String name, int value) {
    setHeader(name, String.valueOf(value));
  }

  @Override
  public void addIntHeader(String name, int value) {
    addHeader(name, String.valueOf(value));
  }

  @Override
  public String getCharacterEncoding() {
    return characterEncoding;
  }

  @Override
  public void setCharacterEncoding(@Nullable String charset) {
    if (writer == null && charset != null) {
      this.characterEncoding = charset;
    }
  }

  @CheckForNull
  @Override
  public String getContentType() {
    return getHeader(CONTENT_TYPE_HEADER);
  }

  @Override
  public void setContentType(String type) {
    setHeader(CONTENT_TYPE_HEADER, type);
  }

  @Override
  public void setContentLength(int len) {
    setContentLengthLong(len);
  }

  @Override
  public void setContentLengthLong(long len) {
    setHeader("Content-Length", String.valueOf(len));
  }

  @Override
  public ServletOutputStream getOutputStream() {
    if (writer != null) {
      throw new IllegalStateException("getWriter() has already been called");
    }
    if (outputStream == null) {
      outputStream = new ServletOutputStream() {
        @Override
        public boolean isReady() {
          return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
          throw new IllegalStateException("Asynchronous processing is not supported by local calls");
        }

        @Override
        public void write(int b) {
          output.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) {
          output.write(b, off, len);
        }
      };
    }
    return outputStream;
  }

  @Override
  public PrintWriter getWriter() {
    if (outputStream != null) {
      throw new IllegalStateException("getOutputStream() has already been called");
    }
    if (writer == null) {
      writer = new PrintWriter(new OutputStreamWriter(output, Charset.forName(characterEncoding)));
    }
    return writer;
  }

  @Override
  public void flushBuffer() {
    if (writer != null) {
      writer.flush();
    }
    committed = true;
  }

  @Override
  public void resetBuffer() {
    checkNotCommitted();
    if (writer != null) {
      writer.flush();
    }
    output.reset();
  }

  @Override
  public boolean isCommitted() {
    return committed;
  }

  @Override
  public void reset() {
    resetBuffer();
    headers.clear();
    status = SC_OK;
  }

  private void checkNotCommitted() {
    if (committed) {
      throw new IllegalStateException("Response is already committed");
    }
  }
}
