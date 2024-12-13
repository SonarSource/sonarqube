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

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;

/**
 * This class is needed only due to the fact that OneLogin Java SAML needs javax HttpServletResponse.
 * It wraps a jakarta.servlet.http.HttpServletResponse and adapts it to javax.servlet.http.HttpServletResponse.
 */
class JakartaToJavaxResponseWrapper implements javax.servlet.http.HttpServletResponse {
  public static final String NOT_IMPLEMENTED = "Not implemented";
  private final HttpServletResponse delegate;

  public JakartaToJavaxResponseWrapper(HttpServletResponse delegate) {
    this.delegate = delegate;
  }

  @Override
  public void addCookie(Cookie cookie) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public boolean containsHeader(String s) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public String encodeURL(String s) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public String encodeRedirectURL(String s) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public String encodeUrl(String s) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public String encodeRedirectUrl(String s) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public void sendError(int i, String s) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public void sendError(int i) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public void sendRedirect(String s) throws IOException {
    delegate.sendRedirect(s);
  }

  @Override
  public void setDateHeader(String s, long l) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public void addDateHeader(String s, long l) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public void setHeader(String s, String s1) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public void addHeader(String s, String s1) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public void setIntHeader(String s, int i) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public void addIntHeader(String s, int i) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public void setStatus(int i) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public void setStatus(int i, String s) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public int getStatus() {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public String getHeader(String s) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public Collection<String> getHeaders(String s) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public Collection<String> getHeaderNames() {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public String getCharacterEncoding() {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public String getContentType() {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public ServletOutputStream getOutputStream() {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public void setCharacterEncoding(String s) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public void setContentLength(int i) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public void setContentLengthLong(long l) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public void setContentType(String s) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public void setBufferSize(int i) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public int getBufferSize() {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public void flushBuffer() {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public void resetBuffer() {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public boolean isCommitted() {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public void reset() {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public void setLocale(Locale locale) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }

  @Override
  public Locale getLocale() {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED);
  }
}
