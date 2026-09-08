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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.IntStream;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RootFilterTest {

  private static final String PAYLOAD = "payload";
  private static final String LB_INPUT_BUFFER = "LB_INPUT_BUFFER";

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
  public void doFilter_whenAccessLogPatternContainsRequestContent_populatesLbInputBuffer() throws Exception {
    RootFilter filter = new RootFilter();
    filter.init(filterConfigWithAccessLog(null, "\"%r\" %s \"%requestContent\""));
    byte[] payload = "{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8);
    HttpServletRequest request = mockRequestWithInputStream("POST", "application/json", payload);

    filter.doFilter(request, mock(HttpServletResponse.class), teeingChain());

    ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
    verify(request).setAttribute(eq(LB_INPUT_BUFFER), captor.capture());
    assertThat(captor.getValue()).isEqualTo(payload);
  }

  @Test
  public void doFilter_whenAccessLogPatternHasNoRequestContent_doesNotPopulateLbInputBuffer() throws Exception {
    RootFilter filter = new RootFilter();
    filter.init(filterConfigWithAccessLog(null, "\"%r\" %s"));
    byte[] payload = "{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8);
    HttpServletRequest request = mockRequestWithInputStream("POST", "application/json", payload);

    filter.doFilter(request, mock(HttpServletResponse.class), teeingChain());

    verify(request, never()).setAttribute(eq(LB_INPUT_BUFFER), any());
  }

  @Test
  public void doFilter_whenAccessLogDisabled_doesNotPopulateLbInputBufferEvenWithRequestContentInPattern() throws Exception {
    RootFilter filter = new RootFilter();
    filter.init(filterConfigWithAccessLog("false", "\"%r\" %s \"%requestContent\""));
    byte[] payload = "{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8);
    HttpServletRequest request = mockRequestWithInputStream("POST", "application/json", payload);

    filter.doFilter(request, mock(HttpServletResponse.class), teeingChain());

    verify(request, never()).setAttribute(eq(LB_INPUT_BUFFER), any());
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
    logTester.setLevel(Level.DEBUG);
    doThrow(new RuntimeException()).when(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    HttpServletResponse response = mockHttpResponse(true);
    underTest.doFilter(request("POST", "/context/service/call", "param=value"), response, chain);

    List<String> debugLogs = logTester.logs(Level.DEBUG);
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
    RootFilter.ServletRequestWrapper servletRequestWrapper = new RootFilter.ServletRequestWrapper(request, true);

    IntStream.range(0,3).forEach(i -> assertThat(readBody(servletRequestWrapper)).isEqualTo(PAYLOAD));
  }

  @Test
  public void getReader_whenIoExceptionThrown_rethrows() throws IOException {
    HttpServletRequest request = mockRequestWithBody();
    IOException ioException = new IOException();
    when(request.getReader()).thenThrow(ioException);

    RootFilter.ServletRequestWrapper servletRequestWrapper = new RootFilter.ServletRequestWrapper(request, true);

    assertThatExceptionOfType(RuntimeException.class)
      .isThrownBy(() -> readBody(servletRequestWrapper))
      .withCause(ioException);
  }

  @Test
  public void getInputStream_whenJsonPost_populatesLbInputBufferWithBody() throws IOException {
    byte[] payload = "{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8);
    HttpServletRequest request = mockRequestWithInputStream("POST", "application/json", payload);
    RootFilter.ServletRequestWrapper wrapper = new RootFilter.ServletRequestWrapper(request, true);

    readInputStreamFully(wrapper);

    ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
    verify(request).setAttribute(eq(LB_INPUT_BUFFER), captor.capture());
    assertThat(captor.getValue()).isEqualTo(payload);
  }

  @Test
  public void getInputStream_returnsBodyUnchanged() throws IOException {
    byte[] payload = "{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8);
    HttpServletRequest request = mockRequestWithInputStream("POST", "application/json", payload);
    RootFilter.ServletRequestWrapper wrapper = new RootFilter.ServletRequestWrapper(request, true);

    assertThat(readInputStreamFully(wrapper)).isEqualTo(payload);
  }

  @Test
  public void getInputStream_whenBodyIsNotTeeableJson_doesNotPopulateLbInputBuffer() throws IOException {
    // multipart uploads, form-urlencoded (V1) bodies and bodyless methods must never be captured
    assertDoesNotPopulateLbInputBuffer("POST", "multipart/form-data; boundary=xyz");
    assertDoesNotPopulateLbInputBuffer("POST", "application/x-www-form-urlencoded");
    assertDoesNotPopulateLbInputBuffer("GET", "application/json");
  }

  private static void assertDoesNotPopulateLbInputBuffer(String method, String contentType) throws IOException {
    HttpServletRequest request = mockRequestWithInputStream(method, contentType, PAYLOAD.getBytes(StandardCharsets.UTF_8));
    RootFilter.ServletRequestWrapper wrapper = new RootFilter.ServletRequestWrapper(request, true);

    readInputStreamFully(wrapper);

    verify(request, never()).setAttribute(eq(LB_INPUT_BUFFER), any());
  }

  @Test
  public void getInputStream_whenRequestContentNotLogged_doesNotPopulateLbInputBufferButBodyStaysReadable() throws IOException {
    byte[] payload = "{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8);
    HttpServletRequest request = mockRequestWithInputStream("POST", "application/json", payload);
    RootFilter.ServletRequestWrapper wrapper = new RootFilter.ServletRequestWrapper(request, false);

    assertThat(readInputStreamFully(wrapper)).isEqualTo(payload);
    verify(request, never()).setAttribute(eq(LB_INPUT_BUFFER), any());
  }

  @Test
  public void getInputStream_whenBodyExceedsCap_doesNotPopulateLbInputBufferButBodyStaysReadable() throws IOException {
    byte[] payload = new byte[100_001];
    HttpServletRequest request = mockRequestWithInputStream("POST", "application/json", payload);
    RootFilter.ServletRequestWrapper wrapper = new RootFilter.ServletRequestWrapper(request, true);

    assertThat(readInputStreamFully(wrapper)).isEqualTo(payload);
    verify(request, never()).setAttribute(eq(LB_INPUT_BUFFER), any());
  }

  @Test
  public void getInputStream_whenSingleLargeReadExceedsCap_doesNotBufferAndBodyStaysReadable() throws IOException {
    byte[] payload = new byte[100_001];
    HttpServletRequest request = mockRequestWithInputStream("POST", "application/json", payload);
    RootFilter.ServletRequestWrapper wrapper = new RootFilter.ServletRequestWrapper(request, true);

    // read the whole oversized body in one call: the cap must be enforced before the chunk is buffered
    ServletInputStream inputStream = wrapper.getInputStream();
    byte[] read = new byte[payload.length];
    int total = 0;
    int n;
    while (total < read.length && (n = inputStream.read(read, total, read.length - total)) != -1) {
      total += n;
    }

    assertThat(total).isEqualTo(payload.length);
    assertThat(read).isEqualTo(payload);
    verify(request, never()).setAttribute(eq(LB_INPUT_BUFFER), any());
  }

  @Test
  public void getInputStream_whenBodyExactlyAtCap_populatesLbInputBuffer() throws IOException {
    byte[] payload = new byte[100_000];
    HttpServletRequest request = mockRequestWithInputStream("POST", "application/json", payload);
    RootFilter.ServletRequestWrapper wrapper = new RootFilter.ServletRequestWrapper(request, true);

    assertThat(readInputStreamFully(wrapper)).isEqualTo(payload);
    ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
    verify(request).setAttribute(eq(LB_INPUT_BUFFER), captor.capture());
    assertThat(captor.getValue()).isEqualTo(payload);
  }

  @Test
  public void getInputStream_whenClosedBeforeEof_populatesLbInputBufferWithBytesRead() throws IOException {
    byte[] payload = "{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8);
    HttpServletRequest request = mockRequestWithInputStream("POST", "application/json", payload);
    RootFilter.ServletRequestWrapper wrapper = new RootFilter.ServletRequestWrapper(request, true);

    ServletInputStream inputStream = wrapper.getInputStream();
    // read exactly the body then close without reaching EOF, as Jackson does with AUTO_CLOSE_SOURCE
    byte[] read = inputStream.readNBytes(payload.length);
    inputStream.close();

    assertThat(read).isEqualTo(payload);
    ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
    verify(request).setAttribute(eq(LB_INPUT_BUFFER), captor.capture());
    assertThat(captor.getValue()).isEqualTo(payload);
  }

  @Test
  public void getInputStream_whenReadByteByByte_populatesLbInputBufferWithBody() throws IOException {
    byte[] payload = "{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8);
    HttpServletRequest request = mockRequestWithInputStream("POST", "application/json", payload);
    RootFilter.ServletRequestWrapper wrapper = new RootFilter.ServletRequestWrapper(request, true);

    ServletInputStream inputStream = wrapper.getInputStream();
    ByteArrayOutputStream readBack = new ByteArrayOutputStream();
    int b;
    while ((b = inputStream.read()) != -1) {
      readBack.write(b);
    }

    assertThat(readBack.toByteArray()).isEqualTo(payload);
    ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
    verify(request).setAttribute(eq(LB_INPUT_BUFFER), captor.capture());
    assertThat(captor.getValue()).isEqualTo(payload);
  }

  @Test
  public void getInputStream_delegatesAvailableReadyFinishedAndReadListener() throws IOException {
    ServletInputStream delegate = mock(ServletInputStream.class);
    when(delegate.isReady()).thenReturn(true);
    when(delegate.isFinished()).thenReturn(false);
    when(delegate.available()).thenReturn(42);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getMethod()).thenReturn("POST");
    when(request.getContentType()).thenReturn("application/json");
    when(request.getInputStream()).thenReturn(delegate);

    ServletInputStream inputStream = new RootFilter.ServletRequestWrapper(request, true).getInputStream();
    ReadListener readListener = mock(ReadListener.class);
    inputStream.setReadListener(readListener);

    assertThat(inputStream.isReady()).isTrue();
    assertThat(inputStream.isFinished()).isFalse();
    assertThat(inputStream.available()).isEqualTo(42);
    verify(delegate).setReadListener(readListener);
  }

  private static FilterConfig filterConfigWithAccessLog(String enable, String pattern) {
    FilterConfig filterConfig = mock(FilterConfig.class);
    ServletContext context = mock(ServletContext.class);
    when(context.getInitParameter("sonar.web.accessLogs.enable")).thenReturn(enable);
    when(context.getInitParameter("sonar.web.accessLogs.pattern")).thenReturn(pattern);
    when(filterConfig.getServletContext()).thenReturn(context);
    return filterConfig;
  }

  private static FilterChain teeingChain() {
    return (req, resp) -> ((HttpServletRequest) req).getInputStream().readAllBytes();
  }

  private static HttpServletRequest mockRequestWithInputStream(String method, String contentType, byte[] payload) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getMethod()).thenReturn(method);
    when(request.getContentType()).thenReturn(contentType);
    assertThatCode(() -> when(request.getInputStream()).thenReturn(new TestServletInputStream(payload)))
      .doesNotThrowAnyException();
    return request;
  }

  private static byte[] readInputStreamFully(HttpServletRequest request) throws IOException {
    return request.getInputStream().readAllBytes();
  }

  private static class TestServletInputStream extends ServletInputStream {
    private final InputStream delegate;

    private TestServletInputStream(byte[] payload) {
      this.delegate = new ByteArrayInputStream(payload);
    }

    @Override
    public int read() throws IOException {
      return delegate.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      return delegate.read(b, off, len);
    }

    @Override
    public boolean isFinished() {
      return false;
    }

    @Override
    public boolean isReady() {
      return true;
    }

    @Override
    public void setReadListener(ReadListener readListener) {
      // no-op for tests
    }
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
