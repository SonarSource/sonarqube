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

package org.sonar.server.ws;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeprecatedRestWebServiceFilterTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private WebServiceEngine webServiceEngine = mock(WebServiceEngine.class);

  private HttpServletRequest request = mock(HttpServletRequest.class);
  private HttpServletResponse response = mock(HttpServletResponse.class);
  private FilterChain chain = mock(FilterChain.class);
  private ArgumentCaptor<ServletRequest> servletRequestCaptor = ArgumentCaptor.forClass(ServletRequest.class);

  private DeprecatedRestWebServiceFilter underTest = new DeprecatedRestWebServiceFilter(webServiceEngine);

  @Before
  public void setUp() throws Exception {
    when(request.getContextPath()).thenReturn("/sonarqube");
  }

  @Test
  public void do_get_pattern() throws Exception {
    assertThat(underTest.doGetPattern().matches("/api/properties")).isTrue();
    assertThat(underTest.doGetPattern().matches("/api/properties/")).isTrue();
    assertThat(underTest.doGetPattern().matches("/api/properties/my.property")).isTrue();
    assertThat(underTest.doGetPattern().matches("/api/properties/my_property")).isTrue();

    assertThat(underTest.doGetPattern().matches("/api/issues/search")).isFalse();
    assertThat(underTest.doGetPattern().matches("/batch/index")).isFalse();
    assertThat(underTest.doGetPattern().matches("/foo")).isFalse();
  }

  @Test
  public void redirect_api_properties_to_api_properties_index() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/properties");

    underTest.doFilter(request, response, chain);

    verify(webServiceEngine).execute(servletRequestCaptor.capture(), any(ServletResponse.class));
    assertThat(servletRequestCaptor.getValue().getPath()).isEqualTo("api/properties/index");
    assertThat(servletRequestCaptor.getValue().hasParam("key")).isFalse();
  }

  @Test
  public void redirect_api_properties_to_api_properties_index_when_no_property() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/properties/");

    underTest.doFilter(request, response, chain);

    verify(webServiceEngine).execute(servletRequestCaptor.capture(), any(ServletResponse.class));
    assertThat(servletRequestCaptor.getValue().getPath()).isEqualTo("api/properties/index");
    assertThat(servletRequestCaptor.getValue().hasParam("key")).isFalse();
  }

  @Test
  public void redirect_api_properties_with_property_key() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/properties/my.property");

    underTest.doFilter(request, response, chain);

    verify(webServiceEngine).execute(servletRequestCaptor.capture(), any(ServletResponse.class));
    assertThat(servletRequestCaptor.getValue().getPath()).isEqualTo("api/properties/index");
    assertThat(servletRequestCaptor.getValue().hasParam("key")).isTrue();
    assertThat(servletRequestCaptor.getValue().readParam("key")).isEqualTo("my.property");
  }

}
