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
import java.util.Collections;
import javax.servlet.http.HttpServletRequest;
import org.jruby.RubyFile;
import org.junit.Test;
import org.sonar.server.plugins.MimeTypes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ServletRequestTest {

  HttpServletRequest source = mock(HttpServletRequest.class);

  @Test
  public void call_method() {
    ServletRequest request = new ServletRequest(source, Collections.<String, Object>emptyMap());
    request.method();
    verify(source).getMethod();
  }

  @Test
  public void getMediaType() throws Exception {
    when(source.getContentType()).thenReturn(MimeTypes.JSON);
    when(source.getRequestURI()).thenReturn("/path/to/resource/search");
    ServletRequest request = new ServletRequest(source, Collections.<String, Object>emptyMap());
    assertThat(request.getMediaType()).isEqualTo(MimeTypes.JSON);
  }

  @Test
  public void default_media_type_is_octet_stream() throws Exception {
    ServletRequest request = new ServletRequest(source, Collections.<String, Object>emptyMap());
    when(source.getRequestURI()).thenReturn("/path/to/resource/search");
    assertThat(request.getMediaType()).isEqualTo(MimeTypes.DEFAULT);
  }

  @Test
  public void media_type_taken_in_url_first() throws Exception {
    ServletRequest request = new ServletRequest(source, Collections.<String, Object>emptyMap());
    when(source.getContentType()).thenReturn(MimeTypes.JSON);
    when(source.getRequestURI()).thenReturn("/path/to/resource/search.protobuf");
    assertThat(request.getMediaType()).isEqualTo(MimeTypes.PROTOBUF);
  }

  @Test
  public void has_param_from_source() {
    when(source.getParameterMap()).thenReturn(ImmutableMap.of("param", new String[] {"value"}));
    ServletRequest request = new ServletRequest(source, Collections.<String, Object>emptyMap());
    assertThat(request.hasParam("param")).isTrue();
  }

  @Test
  public void has_param_from_params() {
    ServletRequest request = new ServletRequest(source, ImmutableMap.<String, Object>of("param", "value"));
    assertThat(request.hasParam("param")).isTrue();
  }

  @Test
  public void read_param_from_source() {
    when(source.getParameter("param")).thenReturn("value");
    ServletRequest request = new ServletRequest(source, Collections.<String, Object>emptyMap());
    assertThat(request.readParam("param")).isEqualTo("value");
  }

  @Test
  public void read_param_from_param() {
    ServletRequest request = new ServletRequest(source, ImmutableMap.<String, Object>of("param1", "value", "param2", 1));
    assertThat(request.readParam("param1")).isEqualTo("value");
    assertThat(request.readParam("param2")).isNull();
    assertThat(request.readParam("param3")).isNull();
  }

  @Test
  public void read_input_stream() {
    RubyFile file = mock(RubyFile.class);
    ServletRequest request = new ServletRequest(source, ImmutableMap.<String, Object>of("param1", file, "param2", "value"));
    request.readInputStreamParam("param1");
    verify(file).getInStream();

    assertThat(request.readInputStreamParam("param2")).isNull();
  }

  @Test
  public void to_string() {
    when(source.getRequestURL()).thenReturn(new StringBuffer("http:localhost:9000/api/issues"));
    ServletRequest request = new ServletRequest(source, Collections.<String, Object>emptyMap());
    assertThat(request.toString()).isEqualTo("http:localhost:9000/api/issues");

    when(source.getQueryString()).thenReturn("components=sonar");
    request = new ServletRequest(source, Collections.<String, Object>emptyMap());
    assertThat(request.toString()).isEqualTo("http:localhost:9000/api/issues?components=sonar");
  }
}
