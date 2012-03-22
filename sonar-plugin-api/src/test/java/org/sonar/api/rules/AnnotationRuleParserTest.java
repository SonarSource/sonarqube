/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.rules;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.sonar.check.IsoCategory;
import org.sonar.check.Priority;

public class AnnotationRuleParserTest {

  @Test
  public void ruleWithProperty() {
    List<Rule> rules = parseAnnotatedClass(RuleWithProperty.class);
    assertThat(rules.size(), is(1));
    Rule rule = rules.get(0);
    assertThat(rule.getKey(), is("foo"));
    assertThat(rule.getName(), is("bar"));
    assertThat(rule.getDescription(), is("Foo Bar"));
    assertThat(rule.getSeverity(), is(RulePriority.BLOCKER));
    assertThat(rule.getParams().size(), is(1));
    RuleParam prop = rule.getParam("property");
    assertThat(prop.getKey(), is("property"));
    assertThat(prop.getDescription(), is("Ignore ?"));
    assertThat(prop.getDefaultValue(), is("false"));
  }

  @Test
  public void ruleWithoutNameNorDescription() {
    List<Rule> rules = parseAnnotatedClass(RuleWithoutNameNorDescription.class);
    assertThat(rules.size(), is(1));
    Rule rule = rules.get(0);
    assertThat(rule.getKey(), is("foo"));
    assertThat(rule.getSeverity(), is(RulePriority.MAJOR));
    assertThat(rule.getName(), is(nullValue()));
    assertThat(rule.getDescription(), is(nullValue()));
  }

  @Test
  public void ruleWithoutKey() {
    List<Rule> rules = parseAnnotatedClass(RuleWithoutKey.class);
    assertThat(rules.size(), is(1));
    Rule rule = rules.get(0);
    assertThat(rule.getKey(), is(RuleWithoutKey.class.getCanonicalName()));
    assertThat(rule.getName(), is("foo"));
    assertThat(rule.getDescription(), is(nullValue()));
    assertThat(rule.getSeverity(), is(RulePriority.MAJOR));
  }

  @Test
  public void supportDeprecatedAnnotations() {
    List<Rule> rules = parseAnnotatedClass(Check.class);
    assertThat(rules.size(), is(1));
    Rule rule = rules.get(0);
    assertThat(rule.getKey(), is(Check.class.getCanonicalName()));
    assertThat(rule.getName(), is(Check.class.getCanonicalName()));
    assertThat(rule.getDescription(), is("Deprecated check"));
    assertThat(rule.getSeverity(), is(RulePriority.BLOCKER));
  }

  private List<Rule> parseAnnotatedClass(Class annotatedClass) {
    return new AnnotationRuleParser().parse("repo", Collections.singleton(annotatedClass));
  }

  @org.sonar.check.Rule(name = "foo")
  private class RuleWithoutKey {
  }

  @org.sonar.check.Rule(key = "foo")
  private class RuleWithoutNameNorDescription {
  }

  @org.sonar.check.Rule(key = "foo", name = "bar", description = "Foo Bar", priority = Priority.BLOCKER)
  private class RuleWithProperty {
    @org.sonar.check.RuleProperty(description = "Ignore ?", defaultValue = "false")
    String property;
  }

  @org.sonar.check.Check(description = "Deprecated check", priority = Priority.BLOCKER, isoCategory = IsoCategory.Maintainability)
  private class Check {
  }

}
