/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform.web.requestid;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.server.platform.Platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RequestIdFilterTest {
  private Platform platform = mock(Platform.class);
  private RequestIdGenerator requestIdGenerator = mock(RequestIdGenerator.class);
  private ServletRequest servletRequest = mock(ServletRequest.class);
  private ServletResponse servletResponse = mock(ServletResponse.class);
  private FilterChain filterChain = mock(FilterChain.class);
  private RequestIdFilter underTest = new RequestIdFilter(platform);

  @Before
  public void setUp() throws Exception {
    ComponentContainer container = new ComponentContainer();
    container.add(requestIdGenerator);
    when(platform.getContainer()).thenReturn(container);
  }

  @Test
  public void filter_put_id_in_MDC_and_remove_it_after_chain_has_executed() throws IOException, ServletException {
    String requestId = "request id";
    when(requestIdGenerator.generate()).thenReturn(requestId);
    doAnswer(invocation -> assertThat(MDC.get("HTTP_REQUEST_ID")).isEqualTo(requestId))
      .when(filterChain)
      .doFilter(servletRequest, servletResponse);

    underTest.doFilter(servletRequest, servletResponse, filterChain);

    assertThat(MDC.get("HTTP_REQUEST_ID")).isNull();
  }

  @Test
  public void filter_put_id_in_MDC_and_remove_it_after_chain_throws_exception() throws IOException, ServletException {
    RuntimeException exception = new RuntimeException("Simulating chain failing");
    String requestId = "request id";
    when(requestIdGenerator.generate()).thenReturn(requestId);
    doAnswer(invocation -> {
      assertThat(MDC.get("HTTP_REQUEST_ID")).isEqualTo(requestId);
      throw exception;
    })
      .when(filterChain)
      .doFilter(servletRequest, servletResponse);

    try {
      underTest.doFilter(servletRequest, servletResponse, filterChain);
      fail("A runtime exception should have been raised");
    } catch (RuntimeException e) {
      assertThat(e).isEqualTo(exception);
    } finally {
      assertThat(MDC.get("HTTP_REQUEST_ID")).isNull();
    }
  }

  @Test
  public void filter_adds_requestId_to_request_passed_on_to_chain() throws IOException, ServletException {
    String requestId = "request id";
    when(requestIdGenerator.generate()).thenReturn(requestId);

    underTest.doFilter(servletRequest, servletResponse, filterChain);

    verify(servletRequest).setAttribute("ID", requestId);
  }

  @Test
  public void filter_does_not_fail_when_there_is_no_RequestIdGenerator_in_container() throws IOException, ServletException {
    Platform platform = mock(Platform.class);
    when(platform.getContainer()).thenReturn(new ComponentContainer());
    RequestIdFilter underTest = new RequestIdFilter(platform);

    underTest.doFilter(servletRequest, servletResponse, filterChain);
  }

  @Test
  public void filter_does_not_add_requestId_to_request_passed_on_to_chain_when_there_is_no_RequestIdGenerator_in_container() throws IOException, ServletException {
    Platform platform = mock(Platform.class);
    when(platform.getContainer()).thenReturn(new ComponentContainer());
    RequestIdFilter underTest = new RequestIdFilter(platform);

    underTest.doFilter(servletRequest, servletResponse, filterChain);

    verify(servletRequest, times(0)).setAttribute(anyString(), anyString());
  }
}
