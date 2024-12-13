/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.auth.saml;

import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

/**
 * This class is needed only due to the fact that OneLogin Java SAML needs javax HttpServletRequest.
 * It wraps a jakarta.servlet.http.HttpServletRequest and adapts it to javax.servlet.http.HttpServletRequest.
 */
class JakartaToJavaxRequestWrapper implements javax.servlet.http.HttpServletRequest {
  public static final String NOT_IMPLEMENTED = "Not implemented";
  private final HttpServletRequest delegate;

  public JakartaToJavaxRequestWrapper(HttpServletRequest delegate) {
    this.delegate = delegate;
  }

  @Override
  public String getAuthType() {
    return delegate.getAuthType();
  }

  @Override
  public Cookie[] getCookies() {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public long getDateHeader(String s) {
    return delegate.getDateHeader(s);
  }

  @Override
  public String getHeader(String s) {
    return delegate.getHeader(s);
  }

  @Override
  public Enumeration<String> getHeaders(String s) {
    return delegate.getHeaders(s);
  }

  @Override
  public Enumeration<String> getHeaderNames() {
    return delegate.getHeaderNames();
  }

  @Override
  public int getIntHeader(String s) {
    return delegate.getIntHeader(s);
  }

  @Override
  public String getMethod() {
    return delegate.getMethod();
  }

  @Override
  public String getPathInfo() {
    return delegate.getPathInfo();
  }

  @Override
  public String getPathTranslated() {
    return delegate.getPathTranslated();
  }

  @Override
  public String getContextPath() {
    return delegate.getContextPath();
  }

  @Override
  public String getQueryString() {
    return delegate.getQueryString();
  }

  @Override
  public String getRemoteUser() {
    return delegate.getRemoteUser();
  }

  @Override
  public boolean isUserInRole(String s) {
    return delegate.isUserInRole(s);
  }

  @Override
  public Principal getUserPrincipal() {
    return delegate.getUserPrincipal();
  }

  @Override
  public String getRequestedSessionId() {
    return delegate.getSession().getId();
  }

  @Override
  public String getRequestURI() {
    return delegate.getRequestURI();
  }

  @Override
  public StringBuffer getRequestURL() {
    return delegate.getRequestURL();
  }

  @Override
  public String getServletPath() {
    return delegate.getServletPath();
  }

  @Override
  public HttpSession getSession(boolean b) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public HttpSession getSession() {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public String changeSessionId() {
    return delegate.changeSessionId();
  }

  @Override
  public boolean isRequestedSessionIdValid() {
    return delegate.isRequestedSessionIdValid();
  }

  @Override
  public boolean isRequestedSessionIdFromCookie() {
    return delegate.isRequestedSessionIdFromCookie();
  }

  @Override
  public boolean isRequestedSessionIdFromURL() {
    return delegate.isRequestedSessionIdFromURL();
  }

  @Override
  public boolean isRequestedSessionIdFromUrl() {
    return delegate.isRequestedSessionIdFromURL();
  }

  @Override
  public boolean authenticate(HttpServletResponse httpServletResponse) throws IOException, ServletException {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public void login(String s, String s1) throws ServletException {
    try {
      delegate.login(s, s1);
    } catch (jakarta.servlet.ServletException e) {
      throw new ServletException(e);
    }
  }

  @Override
  public void logout() throws ServletException {
    try {
      delegate.logout();
    } catch (jakarta.servlet.ServletException e) {
      throw new ServletException(e);
    }
  }

  @Override
  public Collection<Part> getParts() throws IOException, ServletException {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public Part getPart(String s) throws IOException, ServletException {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public <T extends HttpUpgradeHandler> T upgrade(Class<T> aClass) throws IOException, ServletException {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public Object getAttribute(String s) {
    return delegate.getAttribute(s);
  }

  @Override
  public Enumeration<String> getAttributeNames() {
    return delegate.getAttributeNames();
  }

  @Override
  public String getCharacterEncoding() {
    return delegate.getCharacterEncoding();
  }

  @Override
  public void setCharacterEncoding(String s) throws UnsupportedEncodingException {
    delegate.setCharacterEncoding(s);
  }

  @Override
  public int getContentLength() {
    return delegate.getContentLength();
  }

  @Override
  public long getContentLengthLong() {
    return delegate.getContentLengthLong();
  }

  @Override
  public String getContentType() {
    return delegate.getContentType();
  }

  @Override
  public ServletInputStream getInputStream() throws IOException {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public String getParameter(String s) {
    return delegate.getParameter(s);
  }

  @Override
  public Enumeration<String> getParameterNames() {
    return delegate.getParameterNames();
  }

  @Override
  public String[] getParameterValues(String s) {
    return delegate.getParameterValues(s);
  }

  @Override
  public Map<String, String[]> getParameterMap() {
    return delegate.getParameterMap();
  }

  @Override
  public String getProtocol() {
    return delegate.getProtocol();
  }

  @Override
  public String getScheme() {
    return delegate.getScheme();
  }

  @Override
  public String getServerName() {
    return delegate.getServerName();
  }

  @Override
  public int getServerPort() {
    return delegate.getServerPort();
  }

  @Override
  public BufferedReader getReader() throws IOException {
    return delegate.getReader();
  }

  @Override
  public String getRemoteAddr() {
    return delegate.getRemoteAddr();
  }

  @Override
  public String getRemoteHost() {
    return delegate.getRemoteHost();
  }

  @Override
  public void setAttribute(String s, Object o) {
    delegate.setAttribute(s, o);
  }

  @Override
  public void removeAttribute(String s) {
    delegate.removeAttribute(s);
  }

  @Override
  public Locale getLocale() {
    return delegate.getLocale();
  }

  @Override
  public Enumeration<Locale> getLocales() {
    return delegate.getLocales();
  }

  @Override
  public boolean isSecure() {
    return delegate.isSecure();
  }

  @Override
  public RequestDispatcher getRequestDispatcher(String s) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public String getRealPath(String s) {
    return delegate.getServletContext().getRealPath(s);
  }

  @Override
  public int getRemotePort() {
    return delegate.getRemotePort();
  }

  @Override
  public String getLocalName() {
    return delegate.getLocalName();
  }

  @Override
  public String getLocalAddr() {
    return delegate.getLocalAddr();
  }

  @Override
  public int getLocalPort() {
    return delegate.getLocalPort();
  }

  @Override
  public ServletContext getServletContext() {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public AsyncContext startAsync() throws IllegalStateException {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public boolean isAsyncStarted() {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public boolean isAsyncSupported() {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public AsyncContext getAsyncContext() {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public DispatcherType getDispatcherType() {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }
}
