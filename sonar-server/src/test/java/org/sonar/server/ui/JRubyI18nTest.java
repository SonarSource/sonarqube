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
package org.sonar.server.ui;

import org.junit.Test;
import org.sonar.api.i18n.I18n;
import org.sonar.core.i18n.GwtI18n;
import org.sonar.core.i18n.RuleI18nManager;

import java.util.Locale;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class JRubyI18nTest {
  @Test
  public void shouldConvertLocales() {
    assertThat(JRubyI18n.toLocale("fr")).isEqualTo(Locale.FRENCH);
    assertThat(JRubyI18n.toLocale("fr-CH")).isEqualTo(new Locale("fr", "CH"));
  }

  @Test
  public void shouldCacheLocales() {
    JRubyI18n i18n = new JRubyI18n(mock(I18n.class), mock(RuleI18nManager.class), mock(GwtI18n.class));
    assertThat(i18n.getLocalesByRubyKey()).isEmpty();

    i18n.getLocale("fr");

    assertThat(i18n.getLocalesByRubyKey()).hasSize(1);
    assertThat(i18n.getLocalesByRubyKey().get("fr")).isNotNull();
  }

  @Test
  public void default_locale_should_be_english() throws Exception {
    assertThat(JRubyI18n.toLocale(null)).isEqualTo(Locale.ENGLISH);

  }
}
