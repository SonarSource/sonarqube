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
package org.sonar.api.batch.rule;

import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.SonarException;

import static org.assertj.core.api.Assertions.assertThat;

public class CheckFactoryTest {

  @org.junit.Rule
  public ExpectedException thrown = ExpectedException.none();

  ActiveRulesBuilder builder = new ActiveRulesBuilder();

  @Test
  public void no_checks_are_enabled() {
    CheckFactory checkFactory = new CheckFactory(builder.build());

    Checks checks = checkFactory.create("squid").addAnnotatedChecks(CheckWithoutProperties.class);

    assertThat(checks.all()).isEmpty();
  }

  @Test
  public void class_name_as_check_key() {
    RuleKey ruleKey = RuleKey.of("squid", "org.sonar.api.batch.rule.CheckWithoutProperties");
    NewActiveRule rule = new NewActiveRule.Builder()
      .setRuleKey(ruleKey)
      .build();
    builder.addRule(rule);
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
    NewActiveRule rule = new NewActiveRule.Builder()
      .setRuleKey(ruleKey)
      .setParam("pattern", "foo")
      .build();
    builder.addRule(rule);

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
    NewActiveRule rule = new NewActiveRule.Builder()
      .setRuleKey(ruleKey)
      .setParam("unknown", "foo")
      .build();
    builder.addRule(rule);

    CheckFactory checkFactory = new CheckFactory(builder.build());
    checkFactory.create("squid").addAnnotatedChecks(CheckWithStringProperty.class);
  }

  @Test
  public void param_as_primitive_fields() {
    RuleKey ruleKey = RuleKey.of("squid", "org.sonar.api.batch.rule.CheckWithPrimitiveProperties");
    NewActiveRule rule = new NewActiveRule.Builder()
      .setRuleKey(ruleKey)
      .setParam("max", "300")
      .setParam("ignore", "true")
      .build();
    builder.addRule(rule);

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
    NewActiveRule rule = new NewActiveRule.Builder()
      .setRuleKey(ruleKey)
      .setParam("max", "300")
      .build();
    builder.addRule(rule);

    CheckFactory checkFactory = new CheckFactory(builder.build());
    Checks checks = checkFactory.create("squid").addAnnotatedChecks(CheckWithPrimitiveProperties.class);

    Object check = checks.of(ruleKey);
    assertThat(check).isInstanceOf(CheckWithPrimitiveProperties.class);
    assertThat(((CheckWithPrimitiveProperties) check).getMax()).isEqualTo(300);
  }

  @Test
  public void use_template_rule_key() {
    RuleKey ruleKey = RuleKey.of("squid", "S0001_123");
    NewActiveRule rule = new NewActiveRule.Builder()
      .setRuleKey(ruleKey)
      .setTemplateRuleKey("S0001")
      .build();
    builder.addRule(rule);

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
    NewActiveRule rule = new NewActiveRule.Builder()
      .setRuleKey(ruleKey)
      .setParam("max", "300")
      .build();
    builder.addRule(rule);

    CheckFactory checkFactory = new CheckFactory(builder.build());
    checkFactory.create("squid").addAnnotatedChecks(CheckWithUnsupportedPropertyType.class);
  }

  @Test
  public void override_field_key() {
    RuleKey ruleKey = RuleKey.of("squid", "org.sonar.api.batch.rule.CheckWithOverriddenPropertyKey");
    NewActiveRule rule = new NewActiveRule.Builder()
      .setRuleKey(ruleKey)
      .setParam("maximum", "300")
      .build();
    builder.addRule(rule);

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
    NewActiveRule rule = new NewActiveRule.Builder()
      .setRuleKey(ruleKey)
      .setParam("pattern", "foo")
      .build();
    builder.addRule(rule);
    CheckFactory checkFactory = new CheckFactory(builder.build());

    CheckWithStringProperty check = new CheckWithStringProperty();
    Checks checks = checkFactory.create("squid").addAnnotatedChecks(check);

    Object createdCheck = checks.of(ruleKey);
    assertThat(createdCheck).isSameAs(check);
    assertThat(((CheckWithStringProperty) createdCheck).getPattern()).isEqualTo("foo");
  }
}
