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
package org.sonar.api.rules;

import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.PropertyType;
import org.sonar.api.utils.SonarException;
import org.sonar.check.Priority;

import static org.assertj.core.api.Assertions.assertThat;

public class AnnotationRuleParserTest {

  @org.junit.Rule
  public final ExpectedException exception = ExpectedException.none();

  @Test
  public void rule_with_property() {
    List<Rule> rules = parseAnnotatedClass(RuleWithProperty.class);
    assertThat(rules).hasSize(1);
    Rule rule = rules.get(0);
    assertThat(rule.getKey()).isEqualTo("foo");
    assertThat(rule.getName()).isEqualTo("bar");
    assertThat(rule.getDescription()).isEqualTo("Foo Bar");
    assertThat(rule.getSeverity()).isEqualTo(RulePriority.BLOCKER);
    assertThat(rule.getStatus()).isEqualTo(Rule.STATUS_READY);
    assertThat(rule.getParams()).hasSize(1);

    RuleParam prop = rule.getParam("property");
    assertThat(prop.getKey()).isEqualTo("property");
    assertThat(prop.getDescription()).isEqualTo("Ignore ?");
    assertThat(prop.getDefaultValue()).isEqualTo("false");
    assertThat(prop.getType()).isEqualTo(PropertyType.STRING.name());
  }

  @Test
  public void rule_with_integer_property() {
    List<Rule> rules = parseAnnotatedClass(RuleWithIntegerProperty.class);

    RuleParam prop = rules.get(0).getParam("property");
    assertThat(prop.getDescription()).isEqualTo("Max");
    assertThat(prop.getDefaultValue()).isEqualTo("12");
    assertThat(prop.getType()).isEqualTo(PropertyType.INTEGER.name());
  }

  @Test
  public void rule_with_text_property() {
    List<Rule> rules = parseAnnotatedClass(RuleWithTextProperty.class);

    RuleParam prop = rules.get(0).getParam("property");
    assertThat(prop.getDescription()).isEqualTo("text");
    assertThat(prop.getDefaultValue()).isEqualTo("Long text");
    assertThat(prop.getType()).isEqualTo(PropertyType.TEXT.name());
  }

  @Test
  public void should_reject_invalid_property_types() {
    exception.expect(SonarException.class);
    exception.expectMessage("Invalid property type [INVALID]");

    parseAnnotatedClass(RuleWithInvalidPropertyType.class);
  }

  @Test
  public void should_recognize_type() {
    assertThat(AnnotationRuleParser.guessType(Integer.class)).isEqualTo(PropertyType.INTEGER);
    assertThat(AnnotationRuleParser.guessType(int.class)).isEqualTo(PropertyType.INTEGER);
    assertThat(AnnotationRuleParser.guessType(Float.class)).isEqualTo(PropertyType.FLOAT);
    assertThat(AnnotationRuleParser.guessType(float.class)).isEqualTo(PropertyType.FLOAT);
    assertThat(AnnotationRuleParser.guessType(Boolean.class)).isEqualTo(PropertyType.BOOLEAN);
    assertThat(AnnotationRuleParser.guessType(boolean.class)).isEqualTo(PropertyType.BOOLEAN);
    assertThat(AnnotationRuleParser.guessType(String.class)).isEqualTo(PropertyType.STRING);
    assertThat(AnnotationRuleParser.guessType(Object.class)).isEqualTo(PropertyType.STRING);
  }

  @Test
  public void rule_without_name_nor_description() {
    List<Rule> rules = parseAnnotatedClass(RuleWithoutNameNorDescription.class);
    assertThat(rules).hasSize(1);
    Rule rule = rules.get(0);
    assertThat(rule.getKey()).isEqualTo("foo");
    assertThat(rule.getSeverity()).isEqualTo(RulePriority.MAJOR);
    assertThat(rule.getName()).isNull();
    assertThat(rule.getDescription()).isNull();
  }

  @Test
  public void rule_without_key() {
    List<Rule> rules = parseAnnotatedClass(RuleWithoutKey.class);
    assertThat(rules).hasSize(1);
    Rule rule = rules.get(0);
    assertThat(rule.getKey()).isEqualTo(RuleWithoutKey.class.getCanonicalName());
    assertThat(rule.getName()).isEqualTo("foo");
    assertThat(rule.getDescription()).isNull();
    assertThat(rule.getSeverity()).isEqualTo(RulePriority.MAJOR);
  }

  @Test
  public void overridden_rule() {
    List<Rule> rules = parseAnnotatedClass(OverridingRule.class);
    assertThat(rules).hasSize(1);
    Rule rule = rules.get(0);
    assertThat(rule.getKey()).isEqualTo("overriding_foo");
    assertThat(rule.getName()).isEqualTo("Overriding Foo");
    assertThat(rule.getDescription()).isNull();
    assertThat(rule.getSeverity()).isEqualTo(RulePriority.MAJOR);
    assertThat(rule.getParams()).hasSize(2);
  }

  private List<Rule> parseAnnotatedClass(Class annotatedClass) {
    return new AnnotationRuleParser().parse("repo", Collections.singleton(annotatedClass));
  }

  @org.sonar.check.Rule(name = "foo")
  static class RuleWithoutKey {
  }

  @org.sonar.check.Rule(key = "foo")
  static class RuleWithoutNameNorDescription {
  }

  @org.sonar.check.Rule(key = "foo", name = "bar", description = "Foo Bar", status = Rule.STATUS_READY, priority = Priority.BLOCKER)
  static class RuleWithProperty {
    @org.sonar.check.RuleProperty(description = "Ignore ?", defaultValue = "false")
    private String property;
  }

  @org.sonar.check.Rule(key = "overriding_foo", name = "Overriding Foo")
  static class OverridingRule extends RuleWithProperty {
    @org.sonar.check.RuleProperty
    private String additionalProperty;
  }

  @org.sonar.check.Rule(key = "foo", name = "bar", description = "Foo Bar", status = Rule.STATUS_READY, priority = Priority.BLOCKER)
  static class RuleWithIntegerProperty {
    @org.sonar.check.RuleProperty(description = "Max", defaultValue = "12")
    private Integer property;
  }

  @org.sonar.check.Rule(key = "foo", name = "bar", description = "Foo Bar", status = Rule.STATUS_READY, priority = Priority.BLOCKER)
  static class RuleWithTextProperty {
    @org.sonar.check.RuleProperty(description = "text", defaultValue = "Long text", type = "TEXT")
    protected String property;
  }

  @org.sonar.check.Rule(key = "foo", name = "bar", description = "Foo Bar", status = Rule.STATUS_READY, priority = Priority.BLOCKER)
  static class RuleWithInvalidPropertyType {
    @org.sonar.check.RuleProperty(description = "text", defaultValue = "Long text", type = "INVALID")
    public String property;
  }
}
