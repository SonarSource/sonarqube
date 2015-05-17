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
package org.sonar.api.checks;

import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.utils.SonarException;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class AnnotationCheckFactoryTest {

  @org.junit.Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void createCheckWithoutProperties() {
    RulesProfile profile = RulesProfile.create("repo", "java");
    ActiveRule activeRule = profile.activateRule(Rule.create("repo", "org.sonar.api.checks.CheckWithoutProperties", ""), null);
    AnnotationCheckFactory factory = AnnotationCheckFactory.create(profile, "repo", Arrays.<Class>asList(CheckWithoutProperties.class));

    Object check = factory.getCheck(activeRule);
    assertNotNull(check);
    assertThat(check).isInstanceOf(CheckWithoutProperties.class);
  }

  @Test
  public void createCheckWithStringProperty() {
    RulesProfile profile = RulesProfile.create("repo", "java");
    Rule rule = Rule.create("repo", "org.sonar.api.checks.CheckWithStringProperty", "");
    rule.createParameter("pattern");

    ActiveRule activeRule = profile.activateRule(rule, null);
    activeRule.setParameter("pattern", "foo");
    AnnotationCheckFactory factory = AnnotationCheckFactory.create(profile, "repo", Arrays.<Class>asList(CheckWithStringProperty.class));

    Object check = factory.getCheck(activeRule);
    assertNotNull(check);
    assertThat(check).isInstanceOf(CheckWithStringProperty.class);
    assertThat(((CheckWithStringProperty) check).getPattern()).isEqualTo("foo");
  }

  @Test
  public void failIfMissingProperty() {
    thrown.expect(SonarException.class);

    RulesProfile profile = RulesProfile.create("repo", "java");
    Rule rule = Rule.create("repo", "org.sonar.api.checks.CheckWithStringProperty", "");
    rule.createParameter("unknown");

    ActiveRule activeRule = profile.activateRule(rule, null);
    activeRule.setParameter("unknown", "bar");
    AnnotationCheckFactory.create(profile, "repo", Arrays.<Class>asList(CheckWithStringProperty.class));
  }

  @Test
  public void createCheckWithPrimitiveProperties() {
    RulesProfile profile = RulesProfile.create("repo", "java");
    Rule rule = Rule.create("repo", "org.sonar.api.checks.CheckWithPrimitiveProperties", "");
    rule.createParameter("max");
    rule.createParameter("ignore");

    ActiveRule activeRule = profile.activateRule(rule, null);
    activeRule.setParameter("max", "300");
    activeRule.setParameter("ignore", "true");
    AnnotationCheckFactory factory = AnnotationCheckFactory.create(profile, "repo", Arrays.<Class>asList(CheckWithPrimitiveProperties.class));

    Object check = factory.getCheck(activeRule);
    assertThat(((CheckWithPrimitiveProperties) check).getMax()).isEqualTo(300);
    assertThat(((CheckWithPrimitiveProperties) check).isIgnore()).isTrue();
  }

  @Test
  public void createCheckWithIntegerProperty() {
    RulesProfile profile = RulesProfile.create("repo", "java");
    Rule rule = Rule.create("repo", "org.sonar.api.checks.CheckWithIntegerProperty", "");
    rule.createParameter("max");

    ActiveRule activeRule = profile.activateRule(rule, null);
    activeRule.setParameter("max", "300");
    AnnotationCheckFactory factory = AnnotationCheckFactory.create(profile, "repo", Arrays.<Class>asList(CheckWithIntegerProperty.class));

    Object check = factory.getCheck(activeRule);
    assertThat(((CheckWithIntegerProperty) check).getMax()).isEqualTo(300);
  }

  /**
   * SONAR-3164
   */
  @Test
  public void setValueOfInheritedField() {
    RulesProfile profile = RulesProfile.create("repo", "java");
    Rule rule = Rule.create("repo", "org.sonar.api.checks.ImplementedCheck", "");
    rule.createParameter("max");

    ActiveRule activeRule = profile.activateRule(rule, null);
    activeRule.setParameter("max", "300");
    AnnotationCheckFactory factory = AnnotationCheckFactory.create(profile, "repo", Arrays.<Class>asList(ImplementedCheck.class));

    Object check = factory.getCheck(activeRule);
    assertThat(((ImplementedCheck) check).getMax()).isEqualTo(300);
  }

  @Test
  public void failIfPropertyTypeIsNotSupported() {
    thrown.expect(SonarException.class);

    RulesProfile profile = RulesProfile.create("repo", "java");
    Rule rule = Rule.create("repo", "org.sonar.api.checks.CheckWithUnsupportedPropertyType", "");
    rule.createParameter("max");

    ActiveRule activeRule = profile.activateRule(rule, null);
    activeRule.setParameter("max", "300");
    AnnotationCheckFactory.create(profile, "repo", Arrays.<Class>asList(CheckWithUnsupportedPropertyType.class));
  }

  @Test
  public void shouldOverridePropertyKey() {
    RulesProfile profile = RulesProfile.create("repo", "java");
    Rule rule = Rule.create("repo", "org.sonar.api.checks.CheckWithOverriddenPropertyKey", "");
    rule.createParameter("maximum");

    ActiveRule activeRule = profile.activateRule(rule, null);
    activeRule.setParameter("maximum", "300");
    AnnotationCheckFactory factory = AnnotationCheckFactory.create(profile, "repo", Arrays.<Class>asList(CheckWithOverriddenPropertyKey.class));

    Object check = factory.getCheck(activeRule);
    assertThat(((CheckWithOverriddenPropertyKey) check).getMax()).isEqualTo(300);
  }

  @Test
  public void shouldWorkWithClonedRules() {
    RulesProfile profile = RulesProfile.create("repo", "java");
    Rule rule = Rule.create("repo", "CheckWithKey", "");
    Rule clonedRule = Rule.create("repo", "CheckWithKey_2", "").setConfigKey("CheckWithKey").setTemplate(rule);

    profile.activateRule(rule, null);
    profile.activateRule(clonedRule, null);
    AnnotationCheckFactory factory = AnnotationCheckFactory.create(profile, "repo", Arrays.<Class>asList(CheckWithKey.class));

    assertThat(factory.getChecks()).doesNotContain(new Object[] {null});
  }

  /**
   * SONAR-2900
   */
  @Test
  public void create_accept_objects() {
    RulesProfile profile = RulesProfile.create("repo", "java");
    ActiveRule activeRule = profile.activateRule(Rule.create("repo", "org.sonar.api.checks.CheckWithoutProperties", ""), null);
    CheckWithoutProperties checkInstance = new CheckWithoutProperties();
    AnnotationCheckFactory factory = AnnotationCheckFactory.create(profile, "repo", Arrays.asList(checkInstance));

    Object check = factory.getCheck(activeRule);
    assertNotNull(check);
    assertSame(check, checkInstance);
  }

  @Test
  public void create_instance_with_string_property() {
    RulesProfile profile = RulesProfile.create("repo", "java");
    Rule rule = Rule.create("repo", "org.sonar.api.checks.CheckWithStringProperty", "");
    rule.createParameter("pattern");

    ActiveRule activeRule = profile.activateRule(rule, null);
    activeRule.setParameter("pattern", "foo");
    CheckWithStringProperty checkInstance = new CheckWithStringProperty();
    AnnotationCheckFactory factory = AnnotationCheckFactory.create(profile, "repo", Arrays.asList(checkInstance));

    Object check = factory.getCheck(activeRule);
    assertNotNull(check);
    assertSame(check, checkInstance);
    assertThat(checkInstance.getPattern()).isEqualTo("foo");
  }
}
