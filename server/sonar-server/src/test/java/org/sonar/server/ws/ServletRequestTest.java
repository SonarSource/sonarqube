/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.ws;

import com.google.common.collect.ImmutableMap;
import org.jruby.RubyFile;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ServletRequestTest {

  HttpServletRequest source = mock(HttpServletRequest.class);

  @Test
  public void call_method() throws Exception {
    ServletRequest request = new ServletRequest(source, Collections.<String, Object>emptyMap());
    request.method();
    verify(source).getMethod();
  }

  @Test
  public void has_param_from_source() throws Exception {
    when(source.getParameterMap()).thenReturn(ImmutableMap.of("param", new String[]{"value"}));
    ServletRequest request = new ServletRequest(source, Collections.<String, Object>emptyMap());
    assertThat(request.hasParam("param")).isTrue();
  }

  @Test
  public void has_param_from_params() throws Exception {
    ServletRequest request = new ServletRequest(source, ImmutableMap.<String, Object>of("param", "value"));
    assertThat(request.hasParam("param")).isTrue();
  }

  @Test
  public void read_param_from_source() throws Exception {
    when(source.getParameter("param")).thenReturn("value");
    ServletRequest request = new ServletRequest(source, Collections.<String, Object>emptyMap());
    assertThat(request.readParam("param")).isEqualTo("value");
  }

  @Test
  public void read_param_from_param() throws Exception {
    ServletRequest request = new ServletRequest(source, ImmutableMap.<String, Object>of("param1", "value", "param2", 1));
    assertThat(request.readParam("param1")).isEqualTo("value");
    assertThat(request.readParam("param2")).isNull();
    assertThat(request.readParam("param3")).isNull();
  }

  @Test
  public void read_input_stream() throws Exception {
    RubyFile file = mock(RubyFile.class);
    ServletRequest request = new ServletRequest(source, ImmutableMap.<String, Object>of("param1", file, "param2", "value"));
    request.readInputStreamParam("param1");
    verify(file).getInStream();

    assertThat(request.readInputStreamParam("param2")).isNull();
  }

  @Test
  public void to_string() throws Exception {
    when(source.getRequestURL()).thenReturn(new StringBuffer("http:localhost:9000/api/issues"));
    ServletRequest request = new ServletRequest(source, Collections.<String, Object>emptyMap());
    assertThat(request.toString()).isEqualTo("http:localhost:9000/api/issues");

    when(source.getQueryString()).thenReturn("components=sonar");
    request = new ServletRequest(source, Collections.<String, Object>emptyMap());
    assertThat(request.toString()).isEqualTo("http:localhost:9000/api/issues?components=sonar");
  }
}
