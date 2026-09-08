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

import com.google.common.annotations.VisibleForTesting;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.ProcessProperties.Property;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

/**
 * <p>Profile HTTP requests using platform profiling utility.</p>
 * <p>To avoid profiling of requests for static resources, the <code>staticDirs</code>
 * filter parameter can be set in the servlet context descriptor. This parameter should
 * contain a comma-separated list of paths, starting at the context root;
 * requests on subpaths of these paths will not be profiled.</p>
 *
 * @since 4.1
 */
public class RootFilter implements Filter {

  private static final Logger LOGGER = LoggerFactory.getLogger(RootFilter.class);

  /**
   * Name of the request attribute read by logback-access to resolve the {@code %requestContent} pattern for request
   * bodies that are not form-urlencoded (e.g. the JSON payloads sent to Web API V2).
   */
  private static final String LB_INPUT_BUFFER = "LB_INPUT_BUFFER";

  /**
   * Logback-access pattern fragment that logs the request body. Request bodies are only buffered when this fragment is
   * part of the configured access-log pattern.
   */
  private static final String REQUEST_CONTENT_PATTERN = "%requestContent";

  /**
   * Maximum size of a request body kept in memory to feed {@code %requestContent}. Bodies larger than this are not
   * logged, so that large payloads (e.g. report uploads) do not put pressure on the heap.
   */
  private static final int MAX_BUFFERED_BODY_BYTES = 100_000;

  /**
   * Whether the access log is enabled and its pattern contains {@code %requestContent}. When it does not, there is no
   * point buffering request bodies, so the extra copying and allocations are skipped for normal traffic.
   */
  private boolean requestContentLogged = false;

  @Override
  public void init(FilterConfig filterConfig) {
    // TomcatContexts.configure() copies every raw property into the webapp context init parameters, so the access-log
    // configuration is readable here. This mirrors TomcatAccessLog: access logs are enabled unless explicitly disabled,
    // and an unset pattern falls back to a default that does not contain %requestContent.
    String enable = filterConfig.getServletContext().getInitParameter(Property.WEB_ACCESSLOGS_ENABLE.getKey());
    String pattern = filterConfig.getServletContext().getInitParameter(Property.WEB_ACCESSLOGS_PATTERN.getKey());
    boolean accessLogsEnabled = !"false".equalsIgnoreCase(enable);
    requestContentLogged = accessLogsEnabled && pattern != null && pattern.contains(REQUEST_CONTENT_PATTERN);
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    if (request instanceof HttpServletRequest httpRequest) {
      HttpServletResponse httpResponse = (HttpServletResponse) response;
      try {
        chain.doFilter(new ServletRequestWrapper(httpRequest, requestContentLogged), httpResponse);
      } catch (Throwable e) {
        if (httpResponse.isCommitted()) {
          // Request has been aborted by the client, nothing can been done as Tomcat has committed the response
          LOGGER.debug(format("Processing of request %s failed", toUrl(httpRequest)), e);
          return;
        }
        LOGGER.error(format("Processing of request %s failed", toUrl(httpRequest)), e);
        httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
    } else {
      // Not an HTTP request, not profiled
      chain.doFilter(request, response);
    }
  }

  private static String toUrl(HttpServletRequest request) {
    String requestURI = request.getRequestURI();
    String queryString = request.getQueryString();
    if (queryString == null) {
      return requestURI;
    }
    return requestURI + '?' + queryString;
  }

  @Override
  public void destroy() {
    // Nothing
  }

  @VisibleForTesting
  static class ServletRequestWrapper extends HttpServletRequestWrapper {

    private final boolean requestContentLogged;
    private String body;

    ServletRequestWrapper(HttpServletRequest request, boolean requestContentLogged) {
      super(request);
      this.requestContentLogged = requestContentLogged;
    }

    @Override
    public HttpSession getSession(boolean create) {
      if (!create) {
        return null;
      }
      throw notSupported();
    }

    @Override
    public HttpSession getSession() {
      throw notSupported();
    }

    private static UnsupportedOperationException notSupported() {
      return new UnsupportedOperationException("Sessions are disabled so that web server is stateless");
    }

    @Override
    public BufferedReader getReader() throws IOException {
      if (body == null) {
        body = getBodyInternal((HttpServletRequest) getRequest());
      }
      return new BufferedReader(new StringReader(body));
    }

    private static String getBodyInternal(HttpServletRequest request) throws IOException {
      return request.getReader().lines().collect(joining(System.lineSeparator()));
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
      HttpServletRequest request = (HttpServletRequest) getRequest();
      ServletInputStream delegate = request.getInputStream();
      if (!shouldTee(request)) {
        return delegate;
      }
      return new CachingServletInputStream(delegate, request);
    }

    /**
     * Bodies are only captured when the access log is configured to log them (see {@link #init(FilterConfig)}), so normal
     * traffic is not buffered when {@code %requestContent} is not in use.
     * <p>
     * Web API V1 endpoints read their body as form parameters, which logback-access can already reconstruct for
     * {@code %requestContent}. Only the bodies consumed through {@link #getInputStream()} (e.g. the JSON payloads of
     * Web API V2 endpoints) need to be captured into the {@code LB_INPUT_BUFFER} attribute. Multipart and
     * form-urlencoded requests are left untouched.
     */
    private boolean shouldTee(HttpServletRequest request) {
      if (!requestContentLogged) {
        return false;
      }
      String method = request.getMethod();
      if (!"POST".equals(method) && !"PUT".equals(method) && !"PATCH".equals(method) && !"DELETE".equals(method)) {
        return false;
      }
      String contentType = request.getContentType();
      if (contentType == null) {
        return true;
      }
      String lowerCaseContentType = contentType.toLowerCase(Locale.ENGLISH);
      return !lowerCaseContentType.startsWith("multipart/") && !lowerCaseContentType.startsWith("application/x-www-form-urlencoded");
    }
  }

  /**
   * Copies the bytes read from the wrapped stream into an in-memory buffer and, once the body has been fully consumed,
   * exposes it through the {@code LB_INPUT_BUFFER} request attribute so that logback-access can log it. Bytes are only
   * copied as they are read by the application, so the request body stays fully available to the endpoint.
   */
  private static final class CachingServletInputStream extends ServletInputStream {
    private final ServletInputStream delegate;
    private final HttpServletRequest request;
    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private boolean overflow;
    private boolean finished;

    private CachingServletInputStream(ServletInputStream delegate, HttpServletRequest request) {
      this.delegate = delegate;
      this.request = request;
    }

    @Override
    public int read() throws IOException {
      int read = delegate.read();
      if (read == -1) {
        finish();
      } else {
        append(read);
      }
      return read;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      int read = delegate.read(b, off, len);
      if (read == -1) {
        finish();
      } else {
        append(b, off, read);
      }
      return read;
    }

    private void append(int singleByte) {
      if (overflow) {
        return;
      }
      if (buffer.size() + 1 > MAX_BUFFERED_BODY_BYTES) {
        markOverflow();
        return;
      }
      buffer.write(singleByte);
    }

    private void append(byte[] b, int off, int len) {
      if (overflow) {
        return;
      }
      // Bound the write to the remaining allowance so a single large read cannot allocate far beyond the cap.
      if (buffer.size() + len > MAX_BUFFERED_BODY_BYTES) {
        markOverflow();
        return;
      }
      buffer.write(b, off, len);
    }

    private void markOverflow() {
      overflow = true;
      // Drop the reference so the (possibly grown) backing array can be garbage collected instead of retained.
      buffer = null;
    }

    private void finish() {
      if (finished) {
        return;
      }
      finished = true;
      if (!overflow) {
        request.setAttribute(LB_INPUT_BUFFER, buffer.toByteArray());
      }
    }

    @Override
    public void close() throws IOException {
      finish();
      delegate.close();
    }

    @Override
    public int available() throws IOException {
      return delegate.available();
    }

    @Override
    public boolean isFinished() {
      return delegate.isFinished();
    }

    @Override
    public boolean isReady() {
      return delegate.isReady();
    }

    @Override
    public void setReadListener(ReadListener readListener) {
      delegate.setReadListener(readListener);
    }
  }
}
