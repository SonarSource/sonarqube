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

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ReadListener;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.annotation.CheckForNull;

/**
 * Defaults of {@link HttpServletRequest} that local calls don't rely on: no body, no session, no cookies, no async
 * processing and a loopback connection.
 */
abstract class AbstractLocalHttpServletRequest implements HttpServletRequest {

  static final String LOCALHOST = "localhost";
  private static final String LOOPBACK_ADDRESS = "127.0.0.1";
  private static final String SESSION_NOT_SUPPORTED = "HTTP sessions are not supported by local calls";
  private static final String ASYNC_NOT_SUPPORTED = "Asynchronous processing is not supported by local calls";

  private final String requestId = UUID.randomUUID().toString();
  private String characterEncoding = StandardCharsets.UTF_8.name();

  @Override
  public String getCharacterEncoding() {
    return characterEncoding;
  }

  @Override
  public void setCharacterEncoding(String env) {
    this.characterEncoding = env;
  }

  @Override
  public int getContentLength() {
    return -1;
  }

  @Override
  public long getContentLengthLong() {
    return -1L;
  }

  @CheckForNull
  @Override
  public String getContentType() {
    return null;
  }

  @Override
  public ServletInputStream getInputStream() {
    return new ServletInputStream() {
      @Override
      public boolean isFinished() {
        return true;
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setReadListener(ReadListener readListener) {
        throw new IllegalStateException(ASYNC_NOT_SUPPORTED);
      }

      @Override
      public int read() {
        return -1;
      }
    };
  }

  @Override
  public BufferedReader getReader() {
    return new BufferedReader(new InputStreamReader(InputStream.nullInputStream(), StandardCharsets.UTF_8));
  }

  @Override
  public String getProtocol() {
    return "HTTP/1.1";
  }

  @Override
  public String getScheme() {
    return "http";
  }

  @Override
  public String getServerName() {
    return LOCALHOST;
  }

  @Override
  public int getServerPort() {
    return 80;
  }

  @Override
  public String getRemoteAddr() {
    return getLocalAddr();
  }

  @Override
  public String getRemoteHost() {
    return getLocalName();
  }

  @Override
  public int getRemotePort() {
    return 0;
  }

  @Override
  public String getLocalName() {
    return getServerName();
  }

  @Override
  public String getLocalAddr() {
    return LOOPBACK_ADDRESS;
  }

  @Override
  public int getLocalPort() {
    return 80;
  }

  @Override
  public Locale getLocale() {
    return Locale.ENGLISH;
  }

  @Override
  public Enumeration<Locale> getLocales() {
    return Collections.enumeration(List.of(Locale.ENGLISH));
  }

  @Override
  public boolean isSecure() {
    return false;
  }

  @CheckForNull
  @Override
  public RequestDispatcher getRequestDispatcher(String path) {
    return null;
  }

  @Override
  public AsyncContext startAsync() {
    throw new IllegalStateException(ASYNC_NOT_SUPPORTED);
  }

  @Override
  public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) {
    throw new IllegalStateException(ASYNC_NOT_SUPPORTED);
  }

  @Override
  public boolean isAsyncStarted() {
    return false;
  }

  @Override
  public boolean isAsyncSupported() {
    return false;
  }

  @Override
  public AsyncContext getAsyncContext() {
    throw new IllegalStateException(ASYNC_NOT_SUPPORTED);
  }

  @Override
  public DispatcherType getDispatcherType() {
    return DispatcherType.REQUEST;
  }

  @Override
  public String getRequestId() {
    return requestId;
  }

  @Override
  public String getProtocolRequestId() {
    return "";
  }

  @CheckForNull
  @Override
  public ServletConnection getServletConnection() {
    return null;
  }

  @CheckForNull
  @Override
  public String getAuthType() {
    return null;
  }

  @Override
  public Cookie[] getCookies() {
    return new Cookie[0];
  }

  @CheckForNull
  @Override
  public String getPathTranslated() {
    return null;
  }

  @CheckForNull
  @Override
  public String getRemoteUser() {
    return null;
  }

  @Override
  public boolean isUserInRole(String role) {
    return false;
  }

  @CheckForNull
  @Override
  public Principal getUserPrincipal() {
    return null;
  }

  @CheckForNull
  @Override
  public String getRequestedSessionId() {
    return null;
  }

  @CheckForNull
  @Override
  public HttpSession getSession(boolean create) {
    if (create) {
      throw new IllegalStateException(SESSION_NOT_SUPPORTED);
    }
    return null;
  }

  @Override
  public HttpSession getSession() {
    throw new IllegalStateException(SESSION_NOT_SUPPORTED);
  }

  @Override
  public String changeSessionId() {
    throw new IllegalStateException(SESSION_NOT_SUPPORTED);
  }

  @Override
  public boolean isRequestedSessionIdValid() {
    return false;
  }

  @Override
  public boolean isRequestedSessionIdFromCookie() {
    return false;
  }

  @Override
  public boolean isRequestedSessionIdFromURL() {
    return false;
  }

  @Override
  public boolean authenticate(HttpServletResponse response) {
    return false;
  }

  @Override
  public void login(String username, String password) {
    throw new UnsupportedOperationException("Login is not supported by local calls");
  }

  @Override
  public void logout() {
    // local calls reuse the user session of the caller
  }

  @Override
  public Collection<Part> getParts() {
    return List.of();
  }

  @CheckForNull
  @Override
  public Part getPart(String name) {
    return null;
  }

  @Override
  public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) {
    throw new UnsupportedOperationException("Protocol upgrade is not supported by local calls");
  }
}
