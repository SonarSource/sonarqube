/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

public class RequestTest {

  static class SimpleRequest extends Request {

    private final Map<String, String> params = new HashMap<String, String>();

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


  SimpleRequest request = new SimpleRequest();

  @Test
  public void required_param_is_missing() throws Exception {
    try {
      request.requiredParam("foo");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Parameter 'foo' is missing");
    }
  }

  @Test
  public void required_param_is_set() throws Exception {
    String value = request.setParam("foo", "bar").requiredParam("foo");
    assertThat(value).isEqualTo("bar");
  }

  @Test
  public void default_value_of_optional_param() throws Exception {
    String value = request.param("foo", "bar");
    assertThat(value).isEqualTo("bar");
  }

  @Test
  public void string_param() throws Exception {
    String value = request.setParam("foo", "bar").param("foo", "default");
    assertThat(value).isEqualTo("bar");
  }

  @Test
  public void int_param() throws Exception {
    assertThat(request.setParam("foo", "123").intParam("foo")).isEqualTo(123);
    assertThat(request.setParam("foo", "123").intParam("xxx")).isNull();
    assertThat(request.setParam("foo", "123").intParam("foo", 456)).isEqualTo(123);
    assertThat(request.setParam("foo", "123").intParam("xxx", 456)).isEqualTo(456);
  }

  @Test
  public void boolean_param() throws Exception {
    assertThat(request.setParam("foo", "true").booleanParam("foo")).isTrue();
    assertThat(request.setParam("foo", "false").booleanParam("foo")).isFalse();
    assertThat(request.setParam("foo", "true").booleanParam("xxx")).isNull();

    assertThat(request.setParam("foo", "true").booleanParam("foo", true)).isTrue();
    assertThat(request.setParam("foo", "true").booleanParam("foo", false)).isTrue();
    assertThat(request.setParam("foo", "true").booleanParam("xxx", true)).isTrue();
    assertThat(request.setParam("foo", "true").booleanParam("xxx", false)).isFalse();
  }
}
