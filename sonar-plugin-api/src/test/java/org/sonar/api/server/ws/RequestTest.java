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
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.ws.internal.ValidatingRequest;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.SonarException;

import javax.annotation.Nullable;

import java.io.InputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class RequestTest {

  private static class SimpleRequest extends ValidatingRequest {

    private final Map<String, String> params = Maps.newHashMap();

    @Override
    public String method() {
      return "GET";
    }

    @Override
    public boolean hasParam(String key) {
      return params.keySet().contains(key);
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

    @Override
    protected InputStream readInputStreamParam(String key) {
      String param = readParam(key);

      return param == null ? null : IOUtils.toInputStream(param);
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
      action.createParam("a_enum");
      action.createParam("a_date");
      action.createParam("a_datetime");

      action.createParam("a_required_string").setRequired(true);
      action.createParam("a_required_boolean").setRequired(true);
      action.createParam("a_required_number").setRequired(true);
      action.createParam("a_required_enum").setRequired(true);

      action.createParam("has_default_string").setDefaultValue("the_default_string");
      action.createParam("has_default_number").setDefaultValue("10");
      action.createParam("has_default_boolean").setDefaultValue("true");

      action.createParam("has_possible_values").setPossibleValues("foo", "bar");

      action.createParam("new_param").setDeprecatedKey("deprecated_param");
      action.createParam("new_param_with_default_value").setDeprecatedKey("deprecated_new_param_with_default_value").setDefaultValue("the_default_string");

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
  public void has_param() throws Exception {
    request.setParam("a_required_string", "foo");

    assertThat(request.hasParam("a_required_string")).isTrue();
    assertThat(request.hasParam("unknown")).isFalse();
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
    request.setParam("a_required_enum", "BETA");

    assertThat(request.mandatoryParam("a_required_string")).isEqualTo("foo");
    assertThat(request.mandatoryParamAsBoolean("a_required_boolean")).isTrue();
    assertThat(request.mandatoryParamAsInt("a_required_number")).isEqualTo(42);
    assertThat(request.mandatoryParamAsLong("a_required_number")).isEqualTo(42L);
    assertThat(request.mandatoryParamAsEnum("a_required_enum", RuleStatus.class)).isEqualTo(RuleStatus.BETA);
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
    assertThat(request.setParam("a_number", "123").paramAsLong("a_number", 42L)).isEqualTo(123L);
    assertThat(request.setParam("a_number", null).paramAsLong("a_number", 42L)).isEqualTo(123L);
  }

  @Test
  public void param_as_boolean() throws Exception {
    assertThat(request.setParam("a_boolean", "true").paramAsBoolean("a_boolean")).isTrue();
    assertThat(request.setParam("a_boolean", "yes").paramAsBoolean("a_boolean")).isTrue();
    assertThat(request.setParam("a_boolean", "false").paramAsBoolean("a_boolean")).isFalse();
    assertThat(request.setParam("a_boolean", "no").paramAsBoolean("a_boolean")).isFalse();
    try {
      request.setParam("a_boolean", "oui").paramAsBoolean("a_boolean");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Property a_boolean is not a boolean value: oui");
    }
  }

  @Test
  public void param_as_enum() throws Exception {
    assertThat(request.setParam("a_enum", "BETA").paramAsEnum("a_enum", RuleStatus.class)).isEqualTo(RuleStatus.BETA);
  }

  @Test
  public void param_as_enums() throws Exception {
    assertThat(request.setParam("a_enum", "BETA,READY").paramAsEnums("a_enum", RuleStatus.class)).containsOnly(
      RuleStatus.BETA, RuleStatus.READY);
  }

  @Test
  public void param_as_date() throws Exception {
    assertThat(request.setParam("a_date", "2014-05-27").paramAsDate("a_date")).isEqualTo(DateUtils.parseDate("2014-05-27"));
  }

  @Test
  public void param_as_datetime() throws Exception {
    assertThat(request.setParam("a_datetime", "2014-05-27T15:50:45+0100").paramAsDateTime("a_datetime")).isEqualTo(DateUtils.parseDateTime("2014-05-27T15:50:45+0100"));
    assertThat(request.setParam("a_datetime", "2014-05-27").paramAsDateTime("a_datetime")).isEqualTo(DateUtils.parseDate("2014-05-27"));
    try {
      request.setParam("a_datetime", "polop").paramAsDateTime("a_datetime");
    } catch (SonarException error) {
      assertThat(error.getMessage()).isEqualTo("'polop' cannot be parsed as either a date or date+time");
    }
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
  public void deprecated_key() throws Exception {
    assertThat(request.setParam("deprecated_param", "bar").param("new_param")).isEqualTo("bar");
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

  @Test
  public void param_as_input_stream() throws Exception {
    assertThat(request.paramAsInputStream("a_string")).isNull();
    assertThat(IOUtils.toString(request.setParam("a_string", "").paramAsInputStream("a_string"))).isEmpty();
    assertThat(IOUtils.toString(request.setParam("a_string", "foo").paramAsInputStream("a_string"))).isEqualTo("foo");
  }
}
