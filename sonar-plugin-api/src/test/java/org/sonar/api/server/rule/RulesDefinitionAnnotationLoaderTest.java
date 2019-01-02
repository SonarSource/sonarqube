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
package org.sonar.api.server.rule;

import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RulesDefinition.NewRule;
import org.sonar.check.Priority;

import static org.assertj.core.api.Assertions.assertThat;

public class RulesDefinitionAnnotationLoaderTest {

  @org.junit.Rule
  public final ExpectedException thrown = ExpectedException.none();

  RulesDefinitionAnnotationLoader annotationLoader = new RulesDefinitionAnnotationLoader();

  @Test
  public void rule_with_property() {
    RulesDefinition.Repository repository = load(RuleWithProperty.class);
    assertThat(repository.rules()).hasSize(1);
    RulesDefinition.Rule rule = repository.rules().get(0);
    assertThat(rule.key()).isEqualTo("foo");
    assertThat(rule.status()).isEqualTo(RuleStatus.BETA);
    assertThat(rule.name()).isEqualTo("bar");
    assertThat(rule.htmlDescription()).isEqualTo("Foo Bar");
    assertThat(rule.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(rule.params()).hasSize(1);
    assertThat(rule.tags()).isEmpty();

    RulesDefinition.Param prop = rule.param("property");
    assertThat(prop.key()).isEqualTo("property");
    assertThat(prop.description()).isEqualTo("Ignore ?");
    assertThat(prop.defaultValue()).isEqualTo("false");
    assertThat(prop.type()).isEqualTo(RuleParamType.STRING);
  }

  @Test
  public void override_annotation_programmatically() {
    RulesDefinition.Context context = new RulesDefinition.Context();
    RulesDefinition.NewRepository newRepository = context.createRepository("squid", "java");
    NewRule newRule = annotationLoader.loadRule(newRepository, RuleWithProperty.class);
    newRule.setName("Overridden name");
    newRule.param("property").setDefaultValue("true");
    newRule.param("property").setDescription("Overridden");
    newRepository.done();

    RulesDefinition.Repository repository = context.repository("squid");
    assertThat(repository.rules()).hasSize(1);
    RulesDefinition.Rule rule = repository.rules().get(0);
    assertThat(rule.key()).isEqualTo("foo");
    assertThat(rule.status()).isEqualTo(RuleStatus.BETA);
    assertThat(rule.name()).isEqualTo("Overridden name");
    assertThat(rule.htmlDescription()).isEqualTo("Foo Bar");
    assertThat(rule.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(rule.params()).hasSize(1);

    RulesDefinition.Param prop = rule.param("property");
    assertThat(prop.key()).isEqualTo("property");
    assertThat(prop.description()).isEqualTo("Overridden");
    assertThat(prop.defaultValue()).isEqualTo("true");
    assertThat(prop.type()).isEqualTo(RuleParamType.STRING);
  }

  @Test
  public void rule_with_integer_property() {
    RulesDefinition.Repository repository = load(RuleWithIntegerProperty.class);

    RulesDefinition.Param prop = repository.rules().get(0).param("property");
    assertThat(prop.description()).isEqualTo("Max");
    assertThat(prop.defaultValue()).isEqualTo("12");
    assertThat(prop.type()).isEqualTo(RuleParamType.INTEGER);
  }

  @Test
  public void rule_with_text_property() {
    RulesDefinition.Repository repository = load(RuleWithTextProperty.class);

    RulesDefinition.Param prop = repository.rules().get(0).param("property");
    assertThat(prop.description()).isEqualTo("text");
    assertThat(prop.defaultValue()).isEqualTo("Long text");
    assertThat(prop.type()).isEqualTo(RuleParamType.TEXT);
  }

  @Test
  public void should_recognize_type() {
    assertThat(RulesDefinitionAnnotationLoader.guessType(Integer.class)).isEqualTo(RuleParamType.INTEGER);
    assertThat(RulesDefinitionAnnotationLoader.guessType(int.class)).isEqualTo(RuleParamType.INTEGER);
    assertThat(RulesDefinitionAnnotationLoader.guessType(Float.class)).isEqualTo(RuleParamType.FLOAT);
    assertThat(RulesDefinitionAnnotationLoader.guessType(float.class)).isEqualTo(RuleParamType.FLOAT);
    assertThat(RulesDefinitionAnnotationLoader.guessType(Boolean.class)).isEqualTo(RuleParamType.BOOLEAN);
    assertThat(RulesDefinitionAnnotationLoader.guessType(boolean.class)).isEqualTo(RuleParamType.BOOLEAN);
    assertThat(RulesDefinitionAnnotationLoader.guessType(String.class)).isEqualTo(RuleParamType.STRING);
    assertThat(RulesDefinitionAnnotationLoader.guessType(Object.class)).isEqualTo(RuleParamType.STRING);
  }

  @Test
  public void use_classname_when_missing_key() {
    RulesDefinition.Repository repository = load(RuleWithoutKey.class);
    assertThat(repository.rules()).hasSize(1);
    RulesDefinition.Rule rule = repository.rules().get(0);
    assertThat(rule.key()).isEqualTo(RuleWithoutKey.class.getCanonicalName());
    assertThat(rule.name()).isEqualTo("foo");
  }

  @Test
  public void rule_with_tags() {
    RulesDefinition.Repository repository = load(RuleWithTags.class);
    assertThat(repository.rules()).hasSize(1);
    RulesDefinition.Rule rule = repository.rules().get(0);
    assertThat(rule.tags()).containsOnly("misra", "clumsy");
  }

  @Test
  public void overridden_class() {
    RulesDefinition.Repository repository = load(OverridingRule.class);
    assertThat(repository.rules()).hasSize(1);
    RulesDefinition.Rule rule = repository.rules().get(0);
    assertThat(rule.key()).isEqualTo("overriding_foo");
    assertThat(rule.name()).isEqualTo("Overriding Foo");
    assertThat(rule.severity()).isEqualTo(Severity.MAJOR);
    assertThat(rule.htmlDescription()).isEqualTo("Desc of Overriding Foo");
    assertThat(rule.params()).hasSize(2);
  }

  private RulesDefinition.Repository load(Class annotatedClass) {
    RulesDefinition.Context context = new RulesDefinition.Context();
    RulesDefinition.NewExtendedRepository newRepository = context.createRepository("squid", "java");
    annotationLoader.load(newRepository, annotatedClass);
    newRepository.done();
    return context.repository("squid");
  }

  @org.sonar.check.Rule(name = "foo", description = "Foo")
  static class RuleWithoutKey {
  }

  @org.sonar.check.Rule(key = "foo", name = "bar", description = "Foo Bar", priority = Priority.BLOCKER, status = "BETA")
  static class RuleWithProperty {
    @org.sonar.check.RuleProperty(description = "Ignore ?", defaultValue = "false")
    private String property;
  }

  @org.sonar.check.Rule(key = "overriding_foo", name = "Overriding Foo", description = "Desc of Overriding Foo")
  static class OverridingRule extends RuleWithProperty {
    @org.sonar.check.RuleProperty
    private String additionalProperty;
  }

  @org.sonar.check.Rule(key = "foo", name = "bar", description = "Foo Bar", priority = Priority.BLOCKER)
  static class RuleWithIntegerProperty {
    @org.sonar.check.RuleProperty(description = "Max", defaultValue = "12")
    private Integer property;
  }

  @org.sonar.check.Rule(key = "foo", name = "bar", description = "Foo Bar", priority = Priority.BLOCKER)
  static class RuleWithTextProperty {
    @org.sonar.check.RuleProperty(description = "text", defaultValue = "Long text", type = "TEXT")
    protected String property;
  }

  @org.sonar.check.Rule(key = "foo", name = "bar", description = "Foo Bar", priority = Priority.BLOCKER)
  static class RuleWithInvalidPropertyType {
    @org.sonar.check.RuleProperty(description = "text", defaultValue = "Long text", type = "INVALID")
    public String property;
  }

  @org.sonar.check.Rule(key = "foo", name = "bar", description = "Bar", tags = {"misra", "clumsy"})
  static class RuleWithTags {
  }
}
