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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.web.FilterChain;
import org.sonar.server.http.JavaxHttpRequest;
import org.sonar.server.http.JavaxHttpResponse;
import org.sonar.server.ws.ServletRequest;
import org.sonar.server.ws.ServletResponse;
import org.sonar.server.ws.WebServiceEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WebServiceReroutingFilterTest {

  private final WebServiceEngine webServiceEngine = mock(WebServiceEngine.class);

  private final HttpServletRequest request = mock(HttpServletRequest.class);
  private final HttpServletResponse response = mock(HttpServletResponse.class);
  private final FilterChain chain = mock(FilterChain.class);
  private final ArgumentCaptor<ServletRequest> servletRequestCaptor = ArgumentCaptor.forClass(ServletRequest.class);

  private final WebServiceReroutingFilter underTest = new WebServiceReroutingFilter(webServiceEngine);

  @Before
  public void setUp() {
    when(request.getContextPath()).thenReturn("/sonarqube");
  }

  @Test
  public void do_get_pattern() {
    assertThat(underTest.doGetPattern().matches("/api/components/update_key")).isTrue();
    assertThat(underTest.doGetPattern().matches("/api/components/bulk_update_key")).isTrue();
    assertThat(underTest.doGetPattern().matches("/api/projects/update_key")).isFalse();
  }

  @Test
  public void redirect_components_update_key() {
    when(request.getServletPath()).thenReturn("/api/components/update_key");
    when(request.getMethod()).thenReturn("POST");

    underTest.doFilter(new JavaxHttpRequest(request), new JavaxHttpResponse(response), chain);

    assertRedirection("/api/projects/update_key", "POST");
  }

  private void assertRedirection(String path, String method) {
    verify(webServiceEngine).execute(servletRequestCaptor.capture(), any(ServletResponse.class));
    assertThat(servletRequestCaptor.getValue().getPath()).isEqualTo(path);
    assertThat(servletRequestCaptor.getValue().method()).isEqualTo(method);
  }
}
