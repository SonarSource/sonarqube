/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.core.i18n;

import java.util.Locale;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class RuleI18nManagerTest {

  @Test
  public void shouldGetName() {
    DefaultI18n i18n = mock(DefaultI18n.class);
    RuleI18nManager ruleI18n = new RuleI18nManager(i18n);

    ruleI18n.getName("checkstyle", "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck", Locale.ENGLISH);

    String propertyKey = "rule.checkstyle.com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.name";
    verify(i18n).message(Locale.ENGLISH, propertyKey, null /* no default value */);
    verifyNoMoreInteractions(i18n);
  }

  @Test
  public void shouldGetParamDescription() {
    DefaultI18n i18n = mock(DefaultI18n.class);
    RuleI18nManager ruleI18n = new RuleI18nManager(i18n);

    ruleI18n.getParamDescription("checkstyle", "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck", "pattern", Locale.ENGLISH);

    String propertyKey = "rule.checkstyle.com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.param.pattern";
    verify(i18n).message(Locale.ENGLISH, propertyKey, null /* no default value */);
    verifyNoMoreInteractions(i18n);
  }

  @Test
  public void shouldGetDescriptionFromFile() {
    String propertyKeyForName = "rule.checkstyle.com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.name";

    DefaultI18n i18n = mock(DefaultI18n.class);
    when(i18n.messageFromFile(Locale.ENGLISH, "rules/checkstyle/com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.html", propertyKeyForName)).thenReturn(
      "Description");

    RuleI18nManager ruleI18n = new RuleI18nManager(i18n);
    String description = ruleI18n.getDescription("checkstyle", "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck", Locale.ENGLISH);
    assertThat(description).isEqualTo("Description");

    verify(i18n).messageFromFile(Locale.ENGLISH, "rules/checkstyle/com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.html", propertyKeyForName);
    verifyNoMoreInteractions(i18n);
  }

  // see http://jira.sonarsource.com/browse/SONAR-3319
  @Test
  public void shouldGetDescriptionFromFileWithBackwardCompatibility() {
    String propertyKeyForName = "rule.checkstyle.com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.name";

    DefaultI18n i18n = mock(DefaultI18n.class);
    // this is the "old" way of storing HTML description files for rules (they are not in the "rules/<repo-key>" folder)
    when(i18n.messageFromFile(Locale.ENGLISH, "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.html", propertyKeyForName)).thenReturn("Description");

    RuleI18nManager ruleI18n = new RuleI18nManager(i18n);
    String description = ruleI18n.getDescription("checkstyle", "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck", Locale.ENGLISH);
    assertThat(description).isEqualTo("Description");

    verify(i18n).messageFromFile(Locale.ENGLISH, "rules/checkstyle/com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.html", propertyKeyForName);
    verify(i18n).messageFromFile(Locale.ENGLISH, "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.html", propertyKeyForName);
    verifyNoMoreInteractions(i18n);
  }

  @Test
  public void shoudlReturnNullIfMissingDescription() {
    DefaultI18n i18n = mock(DefaultI18n.class);
    RuleI18nManager ruleI18n = new RuleI18nManager(i18n);

    assertThat(ruleI18n.getDescription("checkstyle", "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck", Locale.ENGLISH)).isNull();
  }

  @Test
  public void shouldBeRuleKey() {
    assertThat(RuleI18nManager.isRuleProperty("rule.checkstyle.com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.name")).isTrue();
    assertThat(RuleI18nManager.isRuleProperty("rule.pmd.Header.name")).isTrue();
  }

  @Test
  public void shouldNotBeRuleKey() {
    // this is the parameter "name"
    assertThat(RuleI18nManager.isRuleProperty("rule.checkstyle.com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.param.name")).isFalse();
    assertThat(RuleI18nManager.isRuleProperty("by")).isFalse();
    assertThat(RuleI18nManager.isRuleProperty("something.else")).isFalse();
    assertThat(RuleI18nManager.isRuleProperty("checkstyle.page.name")).isFalse();
  }
}
