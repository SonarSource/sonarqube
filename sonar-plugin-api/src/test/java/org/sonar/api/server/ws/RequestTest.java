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

import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;

public class RequestTest {

  private static class SimpleRequest extends Request {

    private final Map<String, String> params = Maps.newHashMap();

    @Override
    public String method() {
      return "GET";
    }

    public SimpleRequest setParam(String key, @Nullable String value) {
      if (value != null) {
        params.put(key, value);
      }
      return this;
    }

    @Override
    protected String readParam(String key) {
      return params.get(key);
    }
  }

  private static class SimpleWebService implements WebService {

    @Override
    public void define(Context context) {
      NewController controller = context.createController("my_controller");
      NewAction action = controller.createAction("my_action");
      action.setHandler(mock(RequestHandler.class));
      action
        .createParam("required_param")
        .setRequired(true);

      action.createParam("a_string");
      action.createParam("a_boolean");
      action.createParam("a_number");

      action.createParam("a_required_string").setRequired(true);
      action.createParam("a_required_boolean").setRequired(true);
      action.createParam("a_required_number").setRequired(true);

      action.createParam("has_default_string").setDefaultValue("the_default_string");
      action.createParam("has_default_number").setDefaultValue("10");
      action.createParam("has_default_boolean").setDefaultValue("true");

      action.createParam("has_possible_values").setPossibleValues("foo", "bar");

      controller.done();
    }
  }

  SimpleRequest request = new SimpleRequest();

  @Before
  public void before() throws Exception {
    WebService.Context context = new WebService.Context();
    new SimpleWebService().define(context);
    request.setAction(context.controller("my_controller").action("my_action"));
  }

  @Test
  public void required_param_is_missing() throws Exception {
    try {
      request.mandatoryParam("required_param");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Parameter 'required_param' is missing");
    }
  }

  @Test
  public void required_param() throws Exception {
    request.setParam("a_required_string", "foo");
    request.setParam("a_required_number", "42");
    request.setParam("a_required_boolean", "true");

    assertThat(request.mandatoryParam("a_required_string")).isEqualTo("foo");
    assertThat(request.mandatoryParamAsBoolean("a_required_boolean")).isTrue();
    assertThat(request.mandatoryParamAsInt("a_required_number")).isEqualTo(42);
    assertThat(request.mandatoryParamAsLong("a_required_number")).isEqualTo(42L);
  }

  @Test
  public void required_param_as_strings() throws Exception {
    try {
      request.mandatoryParamAsStrings("a_required_string");
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Parameter 'a_required_string' is missing");
    }

    request.setParam("a_required_string", "foo,bar");
    assertThat(request.mandatoryParamAsStrings("a_required_string")).containsExactly("foo", "bar");
  }

  @Test
  public void default_value_of_optional_param() throws Exception {
    assertThat(request.param("has_default_string")).isEqualTo("the_default_string");
  }

  @Test
  public void param_as_string() throws Exception {
    assertThat(request.setParam("a_string", "foo").param("a_string")).isEqualTo("foo");
  }

  @Test
  public void null_param() throws Exception {
    assertThat(request.param("a_string")).isNull();
    assertThat(request.paramAsBoolean("a_boolean")).isNull();
    assertThat(request.paramAsInt("a_number")).isNull();
    assertThat(request.paramAsLong("a_number")).isNull();
  }

  @Test
  public void param_as_int() throws Exception {
    assertThat(request.setParam("a_number", "123").paramAsInt("a_number")).isEqualTo(123);
  }

  @Test
  public void param_as_long() throws Exception {
    assertThat(request.setParam("a_number", "123").paramAsLong("a_number")).isEqualTo(123L);
  }

  @Test
  public void param_as_boolean() throws Exception {
    assertThat(request.setParam("a_boolean", "true").paramAsBoolean("a_boolean")).isTrue();
    assertThat(request.setParam("a_boolean", "false").paramAsBoolean("a_boolean")).isFalse();
  }

  @Test
  public void param_as_strings() throws Exception {
    assertThat(request.paramAsStrings("a_string")).isNull();
    assertThat(request.setParam("a_string", "").paramAsStrings("a_string")).isEmpty();
    assertThat(request.setParam("a_string", "bar").paramAsStrings("a_string")).containsExactly("bar");
    assertThat(request.setParam("a_string", "bar,baz").paramAsStrings("a_string")).containsExactly("bar", "baz");
    assertThat(request.setParam("a_string", "bar , baz").paramAsStrings("a_string")).containsExactly("bar", "baz");
  }

  @Test
  public void fail_if_param_is_not_defined() throws Exception {
    try {
      request.param("unknown");
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("BUG - parameter 'unknown' is undefined for action 'my_action'");
    }
  }

  @Test
  public void verify_possible_values() throws Exception {
    request.setParam("has_possible_values", "foo");
    assertThat(request.param("has_possible_values")).isEqualTo("foo");

    try {
      request.setParam("has_possible_values", "not_possible");
      request.param("has_possible_values");
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Value of parameter 'has_possible_values' (not_possible) must be one of: [foo, bar]");
    }
  }
}
