/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import javax.servlet.FilterChain;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.server.ws.ServletRequest;
import org.sonar.server.ws.ServletResponse;
import org.sonar.server.ws.WebServiceEngine;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeprecatedPropertiesWsFilterTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private WebServiceEngine webServiceEngine = mock(WebServiceEngine.class);

  private HttpServletRequest request = mock(HttpServletRequest.class);
  private HttpServletResponse response = mock(HttpServletResponse.class);
  private FilterChain chain = mock(FilterChain.class);
  private ArgumentCaptor<ServletRequest> servletRequestCaptor = ArgumentCaptor.forClass(ServletRequest.class);

  private DeprecatedPropertiesWsFilter underTest = new DeprecatedPropertiesWsFilter(webServiceEngine);

  @Before
  public void setUp() {
    when(request.getContextPath()).thenReturn("/sonarqube");
  }

  @Test
  public void do_get_pattern() {
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
    when(request.getMethod()).thenReturn("GET");

    underTest.doFilter(request, response, chain);

    assertRedirection("api/properties/index", "GET");
    assertNoParam("key", "component", "value", "values");
  }

  @Test
  public void redirect_api_properties_to_api_properties_index_when_no_property() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/properties/");
    when(request.getMethod()).thenReturn("GET");

    underTest.doFilter(request, response, chain);

    assertRedirection("api/properties/index", "GET");
    assertNoParam("key", "component", "value", "values");
  }

  @Test
  public void redirect_api_properties_with_property_key() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/properties/my.property");
    when(request.getMethod()).thenReturn("GET");

    underTest.doFilter(request, response, chain);

    assertRedirection("api/properties/index", "GET");
    assertParam("id", "my.property");
    assertNoParam("component", "value", "values");
  }

  @Test
  public void redirect_api_properties_with_property_id() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/properties");
    when(request.getParameter("id")).thenReturn("my.property");
    when(request.getMethod()).thenReturn("GET");

    underTest.doFilter(request, response, chain);

    assertRedirection("api/properties/index", "GET");
    assertParam("id", "my.property");
    assertNoParam("component", "value", "values");
  }

  @Test
  public void redirect_api_properties_when_url_ands_with_slash() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/properties/");
    when(request.getParameter("id")).thenReturn("my.property");
    when(request.getMethod()).thenReturn("GET");

    underTest.doFilter(request, response, chain);

    assertRedirection("api/properties/index", "GET");
    assertParam("id", "my.property");
    assertNoParam("component", "value", "values");
  }

  @Test
  public void redirect_api_properties_when_resource() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/properties/my.property");
    when(request.getParameter("resource")).thenReturn("my_project");
    when(request.getMethod()).thenReturn("GET");

    underTest.doFilter(request, response, chain);

    assertRedirection("api/properties/index", "GET");
    assertParam("id", "my.property");
    assertParam("resource", "my_project");
    assertNoParam("component", "value", "values");
  }

  @Test
  public void redirect_api_properties_when_format() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/properties/my.property");
    when(request.getParameter("format")).thenReturn("json");
    when(request.getMethod()).thenReturn("GET");

    underTest.doFilter(request, response, chain);

    assertRedirection("api/properties/index", "GET");
    assertParam("id", "my.property");
    assertParam("format", "json");
    assertNoParam("component", "value", "values");
  }

  @Test
  public void redirect_put_api_properties_to_api_settings_set() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/properties/my.property");
    when(request.getParameter("value")).thenReturn("my_value");
    when(request.getParameter("resource")).thenReturn("my_project");
    when(request.getMethod()).thenReturn("PUT");

    underTest.doFilter(request, response, chain);

    assertRedirection("api/settings/set", "POST");
    assertParam("key", "my.property");
    assertParam("value", "my_value");
    assertParam("component", "my_project");
    assertNoParam("values");
  }

  @Test
  public void redirect_post_api_properties_to_api_settings_set() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/properties/my.property");
    when(request.getParameter("value")).thenReturn("my_value");
    when(request.getParameter("resource")).thenReturn("my_project");
    when(request.getMethod()).thenReturn("POST");

    underTest.doFilter(request, response, chain);

    assertRedirection("api/settings/set", "POST");
    assertParam("key", "my.property");
    assertParam("value", "my_value");
    assertParam("component", "my_project");
    assertNoParam("values");
  }

  @Test
  public void redirect_post_api_properties_to_api_settings_set_when_value_is_in_body() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/properties/my.property");
    when(request.getMethod()).thenReturn("POST");
    when(request.getInputStream()).thenReturn(new TestInputStream("my_value"));

    underTest.doFilter(request, response, chain);

    assertRedirection("api/settings/set", "POST");
    assertParam("key", "my.property");
    assertParam("value", "my_value");
    assertNoParam("values", "component");
  }

  @Test
  public void redirect_post_api_properties_to_api_settings_set_when_multi_values() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/properties/my.property");
    when(request.getParameter("value")).thenReturn("value1,value2,value3");
    when(request.getMethod()).thenReturn("POST");

    underTest.doFilter(request, response, chain);

    assertRedirection("api/settings/set", "POST");
    assertParam("key", "my.property");
    assertNoParam("value");
    assertThat(servletRequestCaptor.getValue().hasParam("values")).as("Parameter '%s' hasn't been found", "values").isTrue();
    assertThat(servletRequestCaptor.getValue().readMultiParam("values")).containsOnly("value1", "value2", "value3");
  }

  @Test
  public void redirect_post_api_properties_to_api_settings_reset_when_no_value() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/properties/my.property");
    when(request.getParameter("resource")).thenReturn("my_project");
    when(request.getMethod()).thenReturn("POST");

    underTest.doFilter(request, response, chain);

    assertRedirection("api/settings/reset", "POST");
    assertParam("keys", "my.property");
    assertParam("component", "my_project");
    assertNoParam("value", "values");
  }

  @Test
  public void redirect_post_api_properties_to_api_settings_reset_when_empty_value() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/properties/my.property");
    when(request.getParameter("value")).thenReturn("");
    when(request.getParameter("resource")).thenReturn("my_project");
    when(request.getMethod()).thenReturn("POST");

    underTest.doFilter(request, response, chain);

    assertRedirection("api/settings/reset", "POST");
    assertParam("keys", "my.property");
    assertParam("component", "my_project");
    assertNoParam("value", "values");
  }

  @Test
  public void redirect_delete_api_properties_to_api_settings_reset() {
    when(request.getRequestURI()).thenReturn("/api/properties/my.property");
    when(request.getParameter("resource")).thenReturn("my_project");
    when(request.getMethod()).thenReturn("DELETE");

    underTest.doFilter(request, response, chain);

    assertRedirection("api/settings/reset", "POST");
    assertParam("keys", "my.property");
    assertParam("component", "my_project");
    assertNoParam("value", "values");
  }

  private void assertRedirection(String path, String method) {
    verify(webServiceEngine).execute(servletRequestCaptor.capture(), any(ServletResponse.class));
    assertThat(servletRequestCaptor.getValue().getPath()).isEqualTo(path);
    assertThat(servletRequestCaptor.getValue().method()).isEqualTo(method);
  }

  private void assertParam(String key, String value) {
    assertThat(servletRequestCaptor.getValue().hasParam(key)).as("Parameter '%s' hasn't been found", key).isTrue();
    assertThat(servletRequestCaptor.getValue().readParam(key)).isEqualTo(value);
  }

  private void assertNoParam(String... keys) {
    Arrays.stream(keys).forEach(key -> {
      assertThat(servletRequestCaptor.getValue().hasParam(key)).as(key).isFalse();
      assertThat(servletRequestCaptor.getValue().readParam(key)).as(key).isNull();
    });
  }

  private static class TestInputStream extends ServletInputStream {

    private final ByteArrayInputStream byteArrayInputStream;

    TestInputStream(String value) {
      this.byteArrayInputStream = new ByteArrayInputStream(value.getBytes(UTF_8));
    }

    @Override
    public boolean isFinished() {
      return false;
    }

    @Override
    public boolean isReady() {
      return false;
    }

    @Override
    public void setReadListener(ReadListener listener) {
      throw new UnsupportedOperationException("Not available");
    }

    @Override
    public int read() {
      return byteArrayInputStream.read();
    }
  }
}
