/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

package org.sonar.server.authentication;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import org.sonar.api.web.ServletFilter;
import org.sonar.server.user.UserSession;

/**
 * This filter creates web session when user authenticates on URL "/sessions/login".
 * The generated session is server stateless.
 */
public class GenerateJwtTokenFilter extends ServletFilter {

  private static final String POST = "POST";

  private final JwtHttpHandler jwtHttpHandler;
  private final UserSession userSession;

  public GenerateJwtTokenFilter(JwtHttpHandler jwtHttpHandler, UserSession userSession) {
    this.jwtHttpHandler = jwtHttpHandler;
    this.userSession = userSession;
  }

  @Override
  public UrlPattern doGetPattern() {
    return UrlPattern.create("/sessions/login");
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;

    if (request.getMethod().equals(POST)) {
      BufferResponseWrapper wrapper = new BufferResponseWrapper(response);
      chain.doFilter(request, wrapper);
      if (userSession.isLoggedIn()) {
        jwtHttpHandler.generateToken(userSession.getLogin(), response);
      }
      response.getOutputStream().write(wrapper.getWrapperBytes());
    } else {
      chain.doFilter(request, response);
    }
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // Nothing to do
  }

  @Override
  public void destroy() {
    // Nothing to do
  }

  /**
   * As the RackFilter is executed before this filter, the reponse is commited and it's not possible anymore to add cookie.
   * So we're create a buffer response wrapper that will buffer the dat that should be send to the browser in order to not commit the response.
   * It's then possible to add cookie before flushing data to the browser.
   *
   * See <a href="http://stackoverflow.com/questions/11025605/response-is-committing-and-dofilter-chain-is-broken">
   *
   * Note : this must be removed when authentication will not use rails anymore
   */
  private static final class BufferResponseWrapper extends HttpServletResponseWrapper {

    private BufferServletOutputStream stream = new BufferServletOutputStream();

    BufferResponseWrapper(HttpServletResponse httpServletResponse) {
      super(httpServletResponse);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
      return stream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
      return new PrintWriter(stream);
    }

    byte[] getWrapperBytes() {
      return stream.getBytes();
    }
  }

  private static final class BufferServletOutputStream extends ServletOutputStream {
    private ByteArrayOutputStream out = new ByteArrayOutputStream();

    @Override
    public void write(int b) throws IOException {
      out.write(b);
    }

    byte[] getBytes() {
      return out.toByteArray();
    }

    @Override
    public boolean isReady() {
      return false;
    }

    @Override
    public void setWriteListener(WriteListener listener) {
      // Nothing to do
    }
  }
}
