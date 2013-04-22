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
package org.sonar.core.i18n;

import com.google.common.collect.Sets;
import org.fest.assertions.Assertions;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.sonar.api.rules.Rule;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class RuleI18nManagerTest {

  @Test
  public void shouldGetName() {
    I18nManager i18n = mock(I18nManager.class);
    RuleI18nManager ruleI18n = new RuleI18nManager(i18n);

    ruleI18n.getName("checkstyle", "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck", Locale.ENGLISH);

    String propertyKey = "rule.checkstyle.com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.name";
    verify(i18n).message(Locale.ENGLISH, propertyKey, null /* no default value */);
    verifyNoMoreInteractions(i18n);
  }

  @Test
  public void shouldGetRuleNameIfNoLocalizationFound() {
    String propertyKey = "rule.checkstyle.com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.name";
    I18nManager i18n = mock(I18nManager.class);
    when(i18n.message(Locale.ENGLISH, propertyKey, null)).thenReturn(null);
    RuleI18nManager ruleI18n = new RuleI18nManager(i18n);

    String ruleName = "RULE_NAME";
    Rule rule = Rule.create("checkstyle", "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck", ruleName);
    String name = ruleI18n.getName(rule, Locale.ENGLISH);
    Assertions.assertThat(name).isEqualTo(ruleName);
  }

  @Test
  public void shouldGetParamDescription() {
    I18nManager i18n = mock(I18nManager.class);
    RuleI18nManager ruleI18n = new RuleI18nManager(i18n);

    ruleI18n.getParamDescription("checkstyle", "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck", "pattern", Locale.ENGLISH);

    String propertyKey = "rule.checkstyle.com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.param.pattern";
    verify(i18n).message(Locale.ENGLISH, propertyKey, null /* no default value */);
    verifyNoMoreInteractions(i18n);
  }

  @Test
  public void shouldGetDescriptionFromFile() {
    String propertyKeyForName = "rule.checkstyle.com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.name";

    I18nManager i18n = mock(I18nManager.class);
    when(i18n.messageFromFile(Locale.ENGLISH, "rules/checkstyle/com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.html", propertyKeyForName, true)).thenReturn("Description");

    RuleI18nManager ruleI18n = new RuleI18nManager(i18n);
    String description = ruleI18n.getDescription("checkstyle", "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck", Locale.ENGLISH);
    assertThat(description, is("Description"));

    verify(i18n).messageFromFile(Locale.ENGLISH, "rules/checkstyle/com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.html", propertyKeyForName, true);
    verifyNoMoreInteractions(i18n);
  }

  // see http://jira.codehaus.org/browse/SONAR-3319
  @Test
  public void shouldGetDescriptionFromFileWithBackwardCompatibility() {
    String propertyKeyForName = "rule.checkstyle.com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.name";

    I18nManager i18n = mock(I18nManager.class);
    // this is the "old" way of storing HTML description files for rules (they are not in the "rules/<repo-key>" folder)
    when(i18n.messageFromFile(Locale.ENGLISH, "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.html", propertyKeyForName, true)).thenReturn("Description");

    RuleI18nManager ruleI18n = new RuleI18nManager(i18n);
    String description = ruleI18n.getDescription("checkstyle", "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck", Locale.ENGLISH);
    assertThat(description, is("Description"));

    verify(i18n).messageFromFile(Locale.ENGLISH, "rules/checkstyle/com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.html", propertyKeyForName, true);
    verify(i18n).messageFromFile(Locale.ENGLISH, "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.html", propertyKeyForName, true);
    verifyNoMoreInteractions(i18n);
  }

  // see http://jira.codehaus.org/browse/SONAR-3319
  @Test
  public void shouldGetDescriptionFromFileWithBackwardCompatibilityWithSpecificLocale() {
    String propertyKeyForName = "rule.checkstyle.com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.name";

    I18nManager i18n = mock(I18nManager.class);
    // this is the "old" way of storing HTML description files for rules (they are not in the "rules/<repo-key>" folder)
    when(i18n.messageFromFile(Locale.ENGLISH, "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.html", propertyKeyForName, true)).thenReturn("Description");

    RuleI18nManager ruleI18n = new RuleI18nManager(i18n);
    String description = ruleI18n.getDescription("checkstyle", "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck", Locale.FRENCH);
    assertThat(description, is("Description"));

    verify(i18n).messageFromFile(Locale.FRENCH, "rules/checkstyle/com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.html", propertyKeyForName, true);
    verify(i18n).messageFromFile(Locale.FRENCH, "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.html", propertyKeyForName, true);
    verify(i18n).messageFromFile(Locale.ENGLISH, "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.html", propertyKeyForName, true);
    verifyNoMoreInteractions(i18n);
  }

  @Test
  public void shouldUseOnlyLanguage() {
    I18nManager i18n = mock(I18nManager.class);
    RuleI18nManager ruleI18n = new RuleI18nManager(i18n);

    ruleI18n.getDescription("checkstyle", "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck", new Locale("fr", "BE"));

    String propertyKeyForName = "rule.checkstyle.com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.name";
    verify(i18n).messageFromFile(new Locale("fr"), "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.html", propertyKeyForName, true);
  }

  @Test
  public void shoudlReturnNullIfMissingDescription() {
    I18nManager i18n = mock(I18nManager.class);
    RuleI18nManager ruleI18n = new RuleI18nManager(i18n);

    assertThat(ruleI18n.getDescription("checkstyle", "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck", Locale.ENGLISH), nullValue());
  }

  @Test
  public void shouldUseEnglishIfMissingLocale() {
    I18nManager i18n = mock(I18nManager.class);
    RuleI18nManager ruleI18n = new RuleI18nManager(i18n);

    ruleI18n.getDescription("checkstyle", "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck", Locale.FRENCH);

    String propertyKeyForName = "rule.checkstyle.com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.name";
    verify(i18n).messageFromFile(Locale.FRENCH, "rules/checkstyle/com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.html", propertyKeyForName, true);
    // check for backward compatibility in French
    verify(i18n).messageFromFile(Locale.FRENCH, "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.html", propertyKeyForName, true);
    // check for backward compatibility in English
    verify(i18n).messageFromFile(Locale.ENGLISH, "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.html", propertyKeyForName, true);
    // and finally get the default English bundle
    verify(i18n).messageFromFile(Locale.ENGLISH, "rules/checkstyle/com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.html", propertyKeyForName, true);
    verifyNoMoreInteractions(i18n);
  }

  @Test
  public void shouldBeRuleKey() {
    assertThat(RuleI18nManager.isRuleProperty("rule.checkstyle.com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.name"), Is.is(true));
    assertThat(RuleI18nManager.isRuleProperty("rule.pmd.Header.name"), Is.is(true));
  }

  @Test
  public void shouldNotBeRuleKey() {
    // this is the parameter "name"
    assertThat(RuleI18nManager.isRuleProperty("rule.checkstyle.com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.param.name"), Is.is(false));
    assertThat(RuleI18nManager.isRuleProperty("by"), Is.is(false));
    assertThat(RuleI18nManager.isRuleProperty("something.else"), Is.is(false));
    assertThat(RuleI18nManager.isRuleProperty("checkstyle.page.name"), Is.is(false));
  }

  @Test
  public void shouldExtractRuleKey() {
    RuleI18nManager.RuleKey ruleKey = RuleI18nManager.extractRuleKey("rule.checkstyle.com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.name");
    assertThat(ruleKey.getRepositoryKey(), Is.is("checkstyle"));
    assertThat(ruleKey.getKey(), Is.is("com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck"));
    assertThat(ruleKey.getNameProperty(), Is.is("rule.checkstyle.com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.name"));

    ruleKey = RuleI18nManager.extractRuleKey("rule.pmd.Header.name");
    assertThat(ruleKey.getRepositoryKey(), Is.is("pmd"));
    assertThat(ruleKey.getKey(), Is.is("Header"));
    assertThat(ruleKey.getNameProperty(), Is.is("rule.pmd.Header.name"));
  }

  @Test
  public void shouldRegisterRuleKeysAtStartup() {
    I18nManager i18n = mock(I18nManager.class);
    when(i18n.getPropertyKeys()).thenReturn(Sets.newHashSet(
        // rules
        "rule.pmd.Header.name", "rule.pmd.Header.param.pattern", "rule.checkstyle.com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.name",
        // other
        "by", "something.else"));
    RuleI18nManager ruleI18n = new RuleI18nManager(i18n);
    ruleI18n.start();

    List<RuleI18nManager.RuleKey> keys = Arrays.asList(ruleI18n.getRuleKeys());
    assertThat(keys.size(), Is.is(2));
    assertThat(keys, hasItem(new RuleI18nManager.RuleKey("pmd", "Header")));
    assertThat(keys, hasItem(new RuleI18nManager.RuleKey("checkstyle", "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck")));
  }

  @Test
  public void shouldSearchEnglishNames() {
    I18nManager i18n = mock(I18nManager.class);
    when(i18n.getPropertyKeys()).thenReturn(Sets.newHashSet("rule.pmd.Header.name", "rule.checkstyle.AnnotationUseStyleCheck.name"));
    when(i18n.message(Locale.ENGLISH, "rule.pmd.Header.name", null)).thenReturn("HEADER PMD CHECK");
    when(i18n.message(Locale.ENGLISH, "rule.checkstyle.AnnotationUseStyleCheck.name", null)).thenReturn("check annotation style");

    RuleI18nManager ruleI18n = new RuleI18nManager(i18n);
    ruleI18n.start();

    List<RuleI18nManager.RuleKey> result = ruleI18n.searchNames("ANNOTATion", Locale.ENGLISH);
    assertThat(result.size(), Is.is(1));
    assertThat(result.get(0).getRepositoryKey(), Is.is("checkstyle"));

    result = ruleI18n.searchNames("bibopaloula", Locale.ENGLISH);
    assertThat(result.size(), Is.is(0));
  }

  @Test
  public void shouldSearchLocalizedNames() {
    I18nManager i18n = mock(I18nManager.class);
    when(i18n.getPropertyKeys()).thenReturn(Sets.newHashSet("rule.pmd.Header.name", "rule.checkstyle.AnnotationUseStyleCheck.name"));
    when(i18n.message(Locale.ENGLISH, "rule.pmd.Header.name", null)).thenReturn("HEADER PMD CHECK");
    when(i18n.message(Locale.ENGLISH, "rule.checkstyle.AnnotationUseStyleCheck.name", null)).thenReturn("check annotation style");
    when(i18n.message(Locale.FRENCH, "rule.checkstyle.AnnotationUseStyleCheck.name", null)).thenReturn("vérifie le style des annotations");

    RuleI18nManager ruleI18n = new RuleI18nManager(i18n);
    ruleI18n.start();

    List<RuleI18nManager.RuleKey> result = ruleI18n.searchNames("annotation", Locale.FRENCH);
    assertThat(result.size(), Is.is(1));
    assertThat(result.get(0).getKey(), Is.is("AnnotationUseStyleCheck"));

    // search only in the french bundle
    result = ruleI18n.searchNames("vérifie", Locale.FRENCH);
    assertThat(result.size(), Is.is(1));
    assertThat(result.get(0).getKey(), Is.is("AnnotationUseStyleCheck"));
  }
}
