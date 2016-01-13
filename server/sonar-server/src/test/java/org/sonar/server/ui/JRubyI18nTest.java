/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.ui;

import java.util.Date;
import java.util.Locale;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.i18n.I18n;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.Durations;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class JRubyI18nTest {
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  @Mock
  I18n i18n;

  @Mock
  Durations durations;

  JRubyI18n jRubyI18n;


  @Before
  public void setUp() {
    jRubyI18n = new JRubyI18n(i18n, durations, userSessionRule);
  }

  @Test
  public void convert_locales() {
    assertThat(JRubyI18n.toLocale("fr")).isEqualTo(Locale.FRENCH);
    assertThat(JRubyI18n.toLocale("fr-CH")).isEqualTo(new Locale("fr", "CH"));
  }

  @Test
  public void cache_locales() {
    assertThat(jRubyI18n.getLocalesByRubyKey()).isEmpty();

    jRubyI18n.getLocale("fr");

    assertThat(jRubyI18n.getLocalesByRubyKey()).hasSize(1);
    assertThat(jRubyI18n.getLocalesByRubyKey().get("fr")).isNotNull();
  }

  @Test
  public void default_locale_should_be_english() {
    assertThat(JRubyI18n.toLocale(null)).isEqualTo(Locale.ENGLISH);
  }

  @Test
  public void message() {
    jRubyI18n.message("en", "my.key", "default");
    verify(i18n).message(any(Locale.class), eq("my.key"), eq("default"));
  }

  @Test
  public void age_from_now() {
    Date date = new Date();
    jRubyI18n.ageFromNow(date);
    verify(i18n).ageFromNow(any(Locale.class), eq(date));
  }

  @Test
  public void format_work_duration() {
    jRubyI18n.formatDuration(Duration.create(10L), "SHORT");
    verify(durations).format(any(Locale.class), eq(Duration.create(10L)), eq(Durations.DurationFormat.SHORT));
  }

  @Test
  public void format_long_work_duration() {
    jRubyI18n.formatLongDuration(10L, "SHORT");
    verify(durations).format(any(Locale.class), eq(Duration.create(10L)), eq(Durations.DurationFormat.SHORT));
  }

  @Test
  public void format_date_time() {
    Date date = new Date();
    jRubyI18n.formatDateTime(date);
    verify(i18n).formatDateTime(any(Locale.class), eq(date));
  }

}
