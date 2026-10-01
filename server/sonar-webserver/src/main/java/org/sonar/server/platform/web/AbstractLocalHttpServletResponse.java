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

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import javax.annotation.Nullable;

/**
 * Defaults of {@link HttpServletResponse} that local calls don't rely on: cookies, URL encoding, redirects, buffering
 * and locale.
 */
abstract class AbstractLocalHttpServletResponse implements HttpServletResponse {

  private Locale locale = Locale.ENGLISH;

  @Override
  public void sendRedirect(String location, int sc, boolean clearBuffer) throws IOException {
    if (clearBuffer) {
      resetBuffer();
    }
    setStatus(sc);
    setHeader("Location", location);
    flushBuffer();
  }

  @Override
  public void addCookie(Cookie cookie) {
    // cookies are meaningless for local calls
  }

  @Override
  public String encodeURL(String url) {
    return url;
  }

  @Override
  public String encodeRedirectURL(String url) {
    return url;
  }

  @Override
  public void setBufferSize(int size) {
    // output is fully buffered in memory
  }

  @Override
  public int getBufferSize() {
    return 0;
  }

  @Override
  public void setLocale(@Nullable Locale loc) {
    if (loc != null) {
      this.locale = loc;
    }
  }

  @Override
  public Locale getLocale() {
    return locale;
  }
}
