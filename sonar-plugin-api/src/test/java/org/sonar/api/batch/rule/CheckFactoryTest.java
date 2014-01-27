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
package org.sonar.api.batch.rule;

import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.rule.internal.ModuleRulesBuilder;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.SonarException;

import static org.fest.assertions.Assertions.assertThat;

public class CheckFactoryTest {

  @org.junit.Rule
  public ExpectedException thrown = ExpectedException.none();

  ModuleRulesBuilder builder = new ModuleRulesBuilder();

  @Test
  public void no_checks_are_enabled() {
    CheckFactory checkFactory = new CheckFactory(builder.build());

    Checks checks = checkFactory.create("squid").addAnnotatedChecks(CheckWithoutProperties.class);

    assertThat(checks.all()).isEmpty();
  }

  @Test
  public void class_name_as_check_key() {
    RuleKey ruleKey = RuleKey.of("squid", "org.sonar.api.batch.rule.CheckWithoutProperties");
    builder.activate(ruleKey);
    CheckFactory checkFactory = new CheckFactory(builder.build());

    Checks checks = checkFactory.create("squid").addAnnotatedChecks(CheckWithoutProperties.class);

    Object check = checks.of(ruleKey);
    assertThat(check).isInstanceOf(CheckWithoutProperties.class);
    assertThat(checks.all()).containsOnly(check);
    assertThat(checks.ruleKey(check)).isEqualTo(ruleKey);
  }

  @Test
  public void param_as_string_field() {
    RuleKey ruleKey = RuleKey.of("squid", "org.sonar.api.batch.rule.CheckWithStringProperty");
    builder.activate(ruleKey).setParam("pattern", "foo");

    CheckFactory checkFactory = new CheckFactory(builder.build());
    Checks checks = checkFactory.create("squid").addAnnotatedChecks(CheckWithStringProperty.class);

    Object check = checks.of(ruleKey);
    assertThat(check).isInstanceOf(CheckWithStringProperty.class);

    assertThat(((CheckWithStringProperty) check).getPattern()).isEqualTo("foo");
  }

  @Test
  public void fail_if_missing_field() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("The field 'unknown' does not exist or is not annotated with @RuleProperty in the class org.sonar.api.batch.rule.CheckWithStringProperty");

    RuleKey ruleKey = RuleKey.of("squid", "org.sonar.api.batch.rule.CheckWithStringProperty");
    builder.activate(ruleKey).setParam("unknown", "foo");

    CheckFactory checkFactory = new CheckFactory(builder.build());
    checkFactory.create("squid").addAnnotatedChecks(CheckWithStringProperty.class);
  }

  @Test
  public void param_as_primitive_fields() {
    RuleKey ruleKey = RuleKey.of("squid", "org.sonar.api.batch.rule.CheckWithPrimitiveProperties");
    builder.activate(ruleKey).setParam("max", "300").setParam("ignore", "true");

    CheckFactory checkFactory = new CheckFactory(builder.build());
    Checks checks = checkFactory.create("squid").addAnnotatedChecks(CheckWithPrimitiveProperties.class);

    Object check = checks.of(ruleKey);
    assertThat(check).isInstanceOf(CheckWithPrimitiveProperties.class);
    assertThat(((CheckWithPrimitiveProperties) check).getMax()).isEqualTo(300);
    assertThat(((CheckWithPrimitiveProperties) check).isIgnore()).isTrue();
  }

  /**
   * SONAR-3164
   */
  @Test
  public void param_as_inherited_field() {
    RuleKey ruleKey = RuleKey.of("squid", "org.sonar.api.batch.rule.CheckWithPrimitiveProperties");
    builder.activate(ruleKey).setParam("max", "300");

    CheckFactory checkFactory = new CheckFactory(builder.build());
    Checks checks = checkFactory.create("squid").addAnnotatedChecks(CheckWithPrimitiveProperties.class);

    Object check = checks.of(ruleKey);
    assertThat(check).isInstanceOf(CheckWithPrimitiveProperties.class);
    assertThat(((CheckWithPrimitiveProperties) check).getMax()).isEqualTo(300);
  }

  @Test
  public void use_engine_key() {
    RuleKey ruleKey = RuleKey.of("squid", "One");
    builder.activate(ruleKey).setEngineKey("S0001");

    CheckFactory checkFactory = new CheckFactory(builder.build());
    Checks checks = checkFactory.create("squid").addAnnotatedChecks(CheckWithKey.class);

    Object check = checks.of(ruleKey);
    assertThat(check).isInstanceOf(CheckWithKey.class);
    assertThat(checks.of(ruleKey)).isSameAs(check);
    assertThat(checks.ruleKey(check)).isEqualTo(ruleKey);
    assertThat(checks.all()).containsOnly(check);
  }

  @Test
  public void fail_if_field_type_is_not_supported() {
    thrown.expect(SonarException.class);

    RuleKey ruleKey = RuleKey.of("squid", "org.sonar.api.batch.rule.CheckWithUnsupportedPropertyType");
    builder.activate(ruleKey).setParam("max", "300");

    CheckFactory checkFactory = new CheckFactory(builder.build());
    checkFactory.create("squid").addAnnotatedChecks(CheckWithUnsupportedPropertyType.class);
  }

  @Test
  public void override_field_key() {
    RuleKey ruleKey = RuleKey.of("squid", "org.sonar.api.batch.rule.CheckWithOverriddenPropertyKey");
    builder.activate(ruleKey).setParam("maximum", "300");

    CheckFactory checkFactory = new CheckFactory(builder.build());
    Checks checks = checkFactory.create("squid").addAnnotatedChecks(CheckWithOverriddenPropertyKey.class);

    Object check = checks.of(ruleKey);
    assertThat(check).isInstanceOf(CheckWithOverriddenPropertyKey.class);
    assertThat(((CheckWithOverriddenPropertyKey) check).getMax()).isEqualTo(300);
  }

  /**
   * SONAR-2900
   */
  @Test
  public void checks_as_objects() {
    RuleKey ruleKey = RuleKey.of("squid", "org.sonar.api.batch.rule.CheckWithStringProperty");
    builder.activate(ruleKey).setParam("pattern", "foo");
    CheckFactory checkFactory = new CheckFactory(builder.build());

    CheckWithStringProperty check = new CheckWithStringProperty();
    Checks checks = checkFactory.create("squid").addAnnotatedChecks(check);

    Object createdCheck = checks.of(ruleKey);
    assertThat(createdCheck).isSameAs(check);
    assertThat(((CheckWithStringProperty) createdCheck).getPattern()).isEqualTo("foo");
  }
}
