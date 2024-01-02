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
package org.sonar.server.platform.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.stream.IntStream;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RootFilterTest {

  private static final String PAYLOAD = "payload";

  @Rule
  public LogTester logTester = new LogTester();

  private FilterChain chain = mock(FilterChain.class);
  private RootFilter underTest;

  @Before
  public void initialize() {
    FilterConfig filterConfig = mock(FilterConfig.class);
    ServletContext context = mock(ServletContext.class);
    when(context.getContextPath()).thenReturn("/context");
    when(filterConfig.getServletContext()).thenReturn(context);
    underTest = new RootFilter();
    underTest.init(filterConfig);
  }

  @Test
  public void throwable_in_doFilter_is_caught_and_500_error_returned_if_response_is_not_committed() throws Exception {
    doThrow(new RuntimeException()).when(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    HttpServletResponse response = mockHttpResponse(false);
    underTest.doFilter(request("POST", "/context/service/call", "param=value"), response, chain);

    verify(response).sendError(500);
  }

  @Test
  public void throwable_in_doFilter_is_caught_but_no_500_response_is_sent_if_response_already_committed() throws Exception {
    doThrow(new RuntimeException()).when(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    HttpServletResponse response = mockHttpResponse(true);
    underTest.doFilter(request("POST", "/context/service/call", "param=value"), response, chain);

    verify(response, never()).sendError(500);
  }

  @Test
  public void throwable_in_doFilter_is_logged_in_debug_if_response_is_already_committed() throws Exception {
    doThrow(new RuntimeException()).when(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    HttpServletResponse response = mockHttpResponse(true);
    underTest.doFilter(request("POST", "/context/service/call", "param=value"), response, chain);

    List<String> debugLogs = logTester.logs(LoggerLevel.DEBUG);
    assertThat(debugLogs.size()).isOne();
    assertThat(debugLogs.get(0)).contains("Processing of request", "failed");
  }


  @Test
  public void request_used_in_chain_do_filter_is_a_servlet_wrapper_when_static_resource() throws Exception {
    underTest.doFilter(request("GET", "/context/static/image.png", null), mock(HttpServletResponse.class), chain);
    ArgumentCaptor<ServletRequest> requestArgumentCaptor = ArgumentCaptor.forClass(ServletRequest.class);

    verify(chain).doFilter(requestArgumentCaptor.capture(), any(HttpServletResponse.class));

    assertThat(requestArgumentCaptor.getValue()).isInstanceOf(RootFilter.ServletRequestWrapper.class);
  }

  @Test
  public void request_used_in_chain_do_filter_is_a_servlet_wrapper_when_service_call() throws Exception {
    underTest.doFilter(request("POST", "/context/service/call", "param=value"), mock(HttpServletResponse.class), chain);
    ArgumentCaptor<ServletRequest> requestArgumentCaptor = ArgumentCaptor.forClass(ServletRequest.class);

    verify(chain).doFilter(requestArgumentCaptor.capture(), any(HttpServletResponse.class));

    assertThat(requestArgumentCaptor.getValue()).isInstanceOf(RootFilter.ServletRequestWrapper.class);
  }

  @Test
  public void fail_to_get_session_from_request() throws Exception {
    underTest.doFilter(request("GET", "/context/static/image.png", null), mock(HttpServletResponse.class), chain);
    ArgumentCaptor<ServletRequest> requestArgumentCaptor = ArgumentCaptor.forClass(ServletRequest.class);
    verify(chain).doFilter(requestArgumentCaptor.capture(), any(ServletResponse.class));

    ServletRequest actualServletRequest = requestArgumentCaptor.getValue();
    assertThatThrownBy(() -> ((HttpServletRequest) actualServletRequest).getSession())
      .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void fail_to_get_session_with_create_from_request() throws Exception {
    underTest.doFilter(request("GET", "/context/static/image.png", null), mock(HttpServletResponse.class), chain);
    ArgumentCaptor<ServletRequest> requestArgumentCaptor = ArgumentCaptor.forClass(ServletRequest.class);
    verify(chain).doFilter(requestArgumentCaptor.capture(), any(ServletResponse.class));

    ServletRequest actualServletRequest = requestArgumentCaptor.getValue();
    assertThatThrownBy(() -> ((HttpServletRequest) actualServletRequest).getSession(true))
      .isInstanceOf(UnsupportedOperationException.class);
  }

  private HttpServletRequest request(String method, String path, String query) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getMethod()).thenReturn(method);
    when(request.getRequestURI()).thenReturn(path);
    when(request.getQueryString()).thenReturn(query);
    return request;
  }

  private static HttpServletResponse mockHttpResponse(boolean committed) {
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.isCommitted()).thenReturn(committed);
    return response;
  }

  @Test
  public void body_can_be_read_several_times() {
    HttpServletRequest request = mockRequestWithBody();
    RootFilter.ServletRequestWrapper servletRequestWrapper = new RootFilter.ServletRequestWrapper(request);

    IntStream.range(0,3).forEach(i -> assertThat(readBody(servletRequestWrapper)).isEqualTo(PAYLOAD));
  }

  @Test
  public void getReader_whenIoExceptionThrown_rethrows() throws IOException {
    HttpServletRequest request = mockRequestWithBody();
    IOException ioException = new IOException();
    when(request.getReader()).thenThrow(ioException);

    RootFilter.ServletRequestWrapper servletRequestWrapper = new RootFilter.ServletRequestWrapper(request);

    assertThatExceptionOfType(RuntimeException.class)
      .isThrownBy(() -> readBody(servletRequestWrapper))
      .withCause(ioException);
  }

  private static HttpServletRequest mockRequestWithBody() {
    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    try {
      StringReader stringReader = new StringReader(PAYLOAD);
      BufferedReader bufferedReader = new BufferedReader(stringReader);
      when(httpServletRequest.getReader()).thenReturn(bufferedReader);
    } catch (IOException e) {
      fail("mockRequest threw an exception: " + e.getMessage());
    }
    return httpServletRequest;
  }

  private static String readBody(HttpServletRequest request) {
    try {
      return request.getReader().lines().collect(joining(System.lineSeparator()));
    } catch (IOException e) {
      throw new RuntimeException("unexpected failure", e);
    }
  }
}
