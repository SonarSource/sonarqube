/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.ui;

import org.hamcrest.core.Is;
import org.junit.Test;
import org.sonar.api.i18n.I18n;
import org.sonar.core.i18n.GwtI18n;
import org.sonar.core.i18n.RuleI18nManager;

import java.util.Locale;

import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class JRubyI18nTest {
  @Test
  public void shouldConvertLocales() {
    assertThat(JRubyI18n.toLocale("fr"), Is.is(Locale.FRENCH));
    assertThat(JRubyI18n.toLocale("fr-CH"), Is.is(new Locale("fr", "CH")));
  }

  @Test
  public void shouldCacheLocales() {
    JRubyI18n i18n = new JRubyI18n(mock(I18n.class), mock(RuleI18nManager.class), mock(GwtI18n.class));
    assertThat(i18n.getLocalesByRubyKey().size(), Is.is(0));

    i18n.getLocale("fr");

    assertThat(i18n.getLocalesByRubyKey().size(), Is.is(1));
    assertThat(i18n.getLocalesByRubyKey().get("fr"), not(nullValue()));

  }
}
