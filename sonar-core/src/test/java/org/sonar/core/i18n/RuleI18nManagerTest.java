/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.core.i18n;

import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class RuleI18nManagerTest {
  @Test
  public void shouldGetName() {
    I18nManager i18n = mock(I18nManager.class);
    RuleI18nManager ruleI18n = new RuleI18nManager(i18n);

    ruleI18n.getName("checkstyle", "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck", Locale.ENGLISH);

    String propertyKey = "rule.checkstyle.com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.name";
    verify(i18n).message(Locale.ENGLISH, propertyKey, "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck" /* default value is the key */);
    verifyNoMoreInteractions(i18n);
  }

  @Test
  public void shouldGetParamDescription() {
    I18nManager i18n = mock(I18nManager.class);
    RuleI18nManager ruleI18n = new RuleI18nManager(i18n);

    ruleI18n.getParamDescription("checkstyle", "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck", "pattern", Locale.ENGLISH);

    String propertyKey = "rule.checkstyle.com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.param.pattern";
    verify(i18n).message(Locale.ENGLISH, propertyKey, "" /* default value must be blank */);
    verifyNoMoreInteractions(i18n);
  }

  @Test
  public void shouldGetDescriptionFromFile() {
    I18nManager i18n = mock(I18nManager.class);
    RuleI18nManager ruleI18n = new RuleI18nManager(i18n);

    ruleI18n.getDescription("checkstyle", "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck", Locale.ENGLISH);

    String propertyKeyForName = "rule.checkstyle.com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.name";
    verify(i18n).messageFromFile(Locale.ENGLISH, "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.html", propertyKeyForName);
    verifyNoMoreInteractions(i18n);
  }

  @Test
  public void shoudlReturnBlankIfMissingDescription() {
    I18nManager i18n = mock(I18nManager.class);
    RuleI18nManager ruleI18n = new RuleI18nManager(i18n);

    assertThat(ruleI18n.getDescription("checkstyle", "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck", Locale.ENGLISH), Is.is(""));
  }

  @Test
  public void shouldUseEnglishIfMissingLocale() {
    I18nManager i18n = mock(I18nManager.class);
    RuleI18nManager ruleI18n = new RuleI18nManager(i18n);

    ruleI18n.getDescription("checkstyle", "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck", Locale.FRENCH);

    String propertyKeyForName = "rule.checkstyle.com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.name";
    verify(i18n).messageFromFile(Locale.FRENCH, "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.html", propertyKeyForName);
    verify(i18n).messageFromFile(Locale.ENGLISH, "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck.html", propertyKeyForName);
    verifyNoMoreInteractions(i18n);
  }
}
