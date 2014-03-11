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
package org.sonar.api.server.ws;

import org.junit.Test;

import javax.annotation.CheckForNull;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;

public class RequestTest {

  static class SimpleRequest extends Request {

    private final Map<String, String> params = new HashMap<String, String>();
    private final WebService.Action action;

    private SimpleRequest(WebService.Action action) {
      this.action = action;
    }

    @Override
    public WebService.Action action() {
      return action;
    }

    @Override
    public String method() {
      return "GET";
    }

    public SimpleRequest setParam(String key, @CheckForNull String value) {
      if (value != null) {
        params.put(key, value);
      }
      return this;
    }

    @Override
    @CheckForNull
    public String param(String key) {
      return params.get(key);
    }
  }


  SimpleRequest request = new SimpleRequest(mock(WebService.Action.class));

  @Test
  public void mandatory_param_is_missing() throws Exception {
    try {
      request.mandatoryParam("foo");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Parameter 'foo' is missing");
    }
  }

  @Test
  public void mandatory_param_is_set() throws Exception {
    request.setParam("a_string", "foo");
    request.setParam("a_long", "42");
    request.setParam("a_int", "42");
    request.setParam("a_boolean", "true");

    assertThat(request.mandatoryParam("a_string")).isEqualTo("foo");
    assertThat(request.mandatoryParamAsBoolean("a_boolean")).isTrue();
    assertThat(request.mandatoryParamAsInt("a_int")).isEqualTo(42);
    assertThat(request.mandatoryParamAsLong("a_long")).isEqualTo(42L);
  }

  @Test
  public void default_value_of_optional_param() throws Exception {
    String value = request.param("foo", "bar");
    assertThat(value).isEqualTo("bar");
  }

  @Test
  public void param_as_string() throws Exception {
    String value = request.setParam("foo", "bar").param("foo", "default");
    assertThat(value).isEqualTo("bar");
  }

  @Test
  public void param_as_int() throws Exception {
    assertThat(request.setParam("foo", "123").paramAsInt("foo")).isEqualTo(123);
    assertThat(request.setParam("foo", "123").paramAsInt("xxx")).isNull();
    assertThat(request.setParam("foo", "123").paramAsInt("foo", 456)).isEqualTo(123);
    assertThat(request.setParam("foo", "123").paramAsInt("xxx", 456)).isEqualTo(456);
  }

  @Test
  public void param_as_long() throws Exception {
    assertThat(request.setParam("foo", "123").paramAsLong("foo")).isEqualTo(123L);
    assertThat(request.setParam("foo", "123").paramAsLong("xxx")).isNull();
    assertThat(request.setParam("foo", "123").paramAsLong("foo", 456L)).isEqualTo(123L);
    assertThat(request.setParam("foo", "123").paramAsLong("xxx", 456L)).isEqualTo(456L);
  }

  @Test
  public void param_as_boolean() throws Exception {
    assertThat(request.setParam("foo", "true").paramAsBoolean("foo")).isTrue();
    assertThat(request.setParam("foo", "false").paramAsBoolean("foo")).isFalse();
    assertThat(request.setParam("foo", "true").paramAsBoolean("xxx")).isNull();

    assertThat(request.setParam("foo", "true").paramAsBoolean("foo", true)).isTrue();
    assertThat(request.setParam("foo", "true").paramAsBoolean("foo", false)).isTrue();
    assertThat(request.setParam("foo", "true").paramAsBoolean("xxx", true)).isTrue();
    assertThat(request.setParam("foo", "true").paramAsBoolean("xxx", false)).isFalse();
  }
}
