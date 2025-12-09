/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.http;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import org.sonar.api.server.http.Cookie;
import org.sonar.api.server.http.HttpResponse;

/**
 * Implementation of {@link HttpResponse} based on a delegate of {@link HttpServletResponse} from the Javax Servlet API.
 */
public class JakartaHttpResponse implements HttpResponse {

  private final HttpServletResponse delegate;

  public JakartaHttpResponse(HttpServletResponse delegate) {
    this.delegate = delegate;
  }

  public HttpServletResponse getDelegate() {
    return delegate;
  }

  @Override
  public void addHeader(String name, String value) {
    delegate.addHeader(name, value);
  }

  @Override
  public String getHeader(String name) {
    return delegate.getHeader(name);
  }

  @Override
  public Collection<String> getHeaders(String name) {
    return delegate.getHeaders(name);
  }

  @Override
  public void setStatus(int status) {
    delegate.setStatus(status);
  }

  @Override
  public int getStatus() {
    return delegate.getStatus();
  }

  @Override
  public void setContentType(String contentType) {
    delegate.setContentType(contentType);
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    return delegate.getWriter();
  }

  @Override
  public void setHeader(String name, String value) {
    delegate.setHeader(name, value);
  }

  @Override
  public void sendRedirect(String location) throws IOException {
    delegate.sendRedirect(location);
  }

  @Override
  public void addCookie(Cookie cookie) {
    jakarta.servlet.http.Cookie jakartaCookie = new jakarta.servlet.http.Cookie(cookie.getName(), cookie.getValue());
    jakartaCookie.setPath(cookie.getPath());
    jakartaCookie.setSecure(cookie.isSecure());
    jakartaCookie.setHttpOnly(cookie.isHttpOnly());
    jakartaCookie.setMaxAge(cookie.getMaxAge());
    delegate.addCookie(jakartaCookie);
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    return delegate.getOutputStream();
  }

  @Override
  public void setCharacterEncoding(String charset) {
    delegate.setCharacterEncoding(charset);
  }
}
