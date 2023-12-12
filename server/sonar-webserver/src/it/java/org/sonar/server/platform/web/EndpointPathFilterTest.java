/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EndpointPathFilterTest {

  private static final String ENDPOINT_PATH = "/api/system/status";
  private static final String ENTRYPOINT_MDC_KEY = "ENTRYPOINT";

  private final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
  private final HttpServletResponse servletResponse = mock(HttpServletResponse.class);
  private final FilterChain filterChain = mock(FilterChain.class);
  private final EndpointPathFilter endpointPathFilter = new EndpointPathFilter();

  @Before
  public void setUp() {
    when(servletRequest.getRequestURI()).thenReturn(ENDPOINT_PATH);
  }

  @Test
  public void doFilter_shouldPutEndpointToMDCAndRemoveItAfterChainExecution() throws ServletException, IOException {
    doAnswer(invocation -> assertThat(MDC.get("ENTRYPOINT")).isEqualTo(ENDPOINT_PATH))
      .when(filterChain)
      .doFilter(servletRequest, servletResponse);

    endpointPathFilter.doFilter(servletRequest, servletResponse, filterChain);

    assertThat(MDC.get(ENTRYPOINT_MDC_KEY)).isNull();
  }

  @Test
  public void doFilter_whenChainFails_shouldPutInMDCAndRemoveItAfter() throws IOException, ServletException {
    RuntimeException exception = new RuntimeException("Simulating chain failing");
    doAnswer(invocation -> {
      assertThat(MDC.get(ENTRYPOINT_MDC_KEY)).isEqualTo(ENDPOINT_PATH);
      throw exception;
    })
      .when(filterChain)
      .doFilter(servletRequest, servletResponse);

    assertThatThrownBy(() -> endpointPathFilter.doFilter(servletRequest, servletResponse, filterChain)).isEqualTo(exception);
    assertThat(MDC.get(ENTRYPOINT_MDC_KEY)).isNull();
  }

  @Test
  public void doFilter_whenNotHttpServletRequest_shouldAddEmptyPath() throws ServletException, IOException {
    doAnswer(invocation -> assertThat(MDC.get("ENTRYPOINT")).isEqualTo("-"))
      .when(filterChain)
      .doFilter(servletRequest, servletResponse);

    endpointPathFilter.doFilter(mock(ServletRequest.class), servletResponse, filterChain);

    assertThat(MDC.get(ENTRYPOINT_MDC_KEY)).isNull();
  }

}
