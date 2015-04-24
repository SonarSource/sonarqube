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
package org.sonar.core.i18n;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultI18nTest {

  @Mock
  System2 system2;

  DefaultI18n manager;

  @Before
  public void before() {
    PluginRepository pluginRepository = mock(PluginRepository.class);
    List<PluginInfo> plugins = Arrays.asList(newPlugin("core"), newPlugin("sqale"), newPlugin("frpack"), newPlugin("checkstyle"), newPlugin("other"));
    when(pluginRepository.getPluginInfos()).thenReturn(plugins);

    manager = new DefaultI18n(pluginRepository, system2);
    manager.doStart(getClass().getClassLoader());
  }

  @Test
  public void load_core_bundle_when_no_plugin() {
    DefaultI18n manager = new DefaultI18n(mock(PluginRepository.class), system2);
    manager.doStart(getClass().getClassLoader());

    assertThat(manager.getPropertyKeys().contains("any")).isTrue();
    assertThat(manager.getPropertyKeys().contains("assignee")).isTrue();
  }

  @Test
  public void introspect_all_available_properties() {
    assertThat(manager.getPropertyKeys().contains("any")).isTrue();
    // Only in english
    assertThat(manager.getPropertyKeys().contains("assignee")).isTrue();
    assertThat(manager.getPropertyKeys().contains("sqale.page")).isTrue();
    assertThat(manager.getPropertyKeys().contains("bla_bla_bla")).isFalse();
  }

  @Test
  public void get_english_labels() {
    assertThat(manager.message(Locale.ENGLISH, "any", null)).isEqualTo("Any");
    assertThat(manager.message(Locale.ENGLISH, "sqale.page", null)).isEqualTo("Sqale page title");
    assertThat(manager.message(Locale.ENGLISH, "checkstyle.rule1.name", null)).isEqualTo("Rule one");
  }

  // SONAR-2927
  @Test
  public void get_english_labels_when_default_locale_is_not_english() {
    Locale defaultLocale = Locale.getDefault();
    try {
      Locale.setDefault(Locale.FRENCH);
      assertThat(manager.message(Locale.ENGLISH, "any", null)).isEqualTo("Any");
      assertThat(manager.message(Locale.ENGLISH, "sqale.page", null)).isEqualTo("Sqale page title");
      assertThat(manager.message(Locale.ENGLISH, "checkstyle.rule1.name", null)).isEqualTo("Rule one");
    } finally {
      Locale.setDefault(defaultLocale);
    }
  }

  @Test
  public void get_labels_from_french_pack() {
    assertThat(manager.message(Locale.FRENCH, "checkstyle.rule1.name", null)).isEqualTo("Rule un");
    assertThat(manager.message(Locale.FRENCH, "any", null)).isEqualTo("Tous");

    // language pack
    assertThat(manager.message(Locale.FRENCH, "sqale.page", null)).isEqualTo("Titre de la page Sqale");
  }

  @Test
  public void get_french_label_if_swiss_country() {
    Locale swiss = new Locale("fr", "CH");
    assertThat(manager.message(swiss, "checkstyle.rule1.name", null)).isEqualTo("Rule un");
    assertThat(manager.message(swiss, "any", null)).isEqualTo("Tous");

    // language pack
    assertThat(manager.message(swiss, "sqale.page", null)).isEqualTo("Titre de la page Sqale");
  }

  @Test
  public void fallback_to_default_locale() {
    assertThat(manager.message(Locale.CHINA, "checkstyle.rule1.name", null)).isEqualTo("Rule one");
    assertThat(manager.message(Locale.CHINA, "any", null)).isEqualTo("Any");
    assertThat(manager.message(Locale.CHINA, "sqale.page", null)).isEqualTo("Sqale page title");
  }

  @Test
  public void return_default_value_if_missing_key() {
    assertThat(manager.message(Locale.ENGLISH, "bla_bla_bla", "default")).isEqualTo("default");
    assertThat(manager.message(Locale.FRENCH, "bla_bla_bla", "default")).isEqualTo("default");
  }

  @Test
  public void format_message_with_parameters() {
    assertThat(manager.message(Locale.ENGLISH, "name_too_long_x", null, "10")).isEqualTo("Name is too long (maximum is 10 characters)");
  }

  @Test
  public void use_default_locale_if_missing_value_in_localized_bundle() {
    assertThat(manager.message(Locale.FRENCH, "assignee", null)).isEqualTo("Assignee");
    assertThat(manager.message(Locale.CHINA, "assignee", null)).isEqualTo("Assignee");
  }

  @Test
  public void return_null_if_file_not_found() {
    String html = manager.messageFromFile(Locale.ENGLISH, "UnknownRule.html", "checkstyle.rule1.name");
    assertThat(html).isNull();
  }

  @Test
  public void return_null_if_rule_not_internationalized() {
    String html = manager.messageFromFile(Locale.ENGLISH, "UnknownRule.html", "foo.rule1.name");
    assertThat(html).isNull();
  }

  @Test
  public void get_age_with_duration() {
    assertThat(manager.age(Locale.ENGLISH, 10)).isEqualTo("less than a minute");
  }

  @Test
  public void get_age_with_dates() {
    assertThat(manager.age(Locale.ENGLISH, DateUtils.parseDate("2014-01-01"), DateUtils.parseDate("2014-01-02"))).isEqualTo("a day");
  }

  @Test
  public void get_age_from_now() {
    when(system2.now()).thenReturn(DateUtils.parseDate("2014-01-02").getTime());
    assertThat(manager.ageFromNow(Locale.ENGLISH, DateUtils.parseDate("2014-01-01"))).isEqualTo("a day");
  }

  @Test
  public void format_date_time() {
    TimeZone initialTz = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("GMT+1"));
    assertThat(manager.formatDateTime(Locale.ENGLISH, DateUtils.parseDateTime("2014-01-22T19:10:03+0100"))).startsWith("Jan 22, 2014");
    TimeZone.setDefault(initialTz);
  }

  @Test
  public void format_date() {
    TimeZone initialTz = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("GMT+1"));
    assertThat(manager.formatDate(Locale.ENGLISH, DateUtils.parseDateTime("2014-01-22T19:10:03+0100"))).isEqualTo("Jan 22, 2014");
    TimeZone.setDefault(initialTz);
  }

  @Test
  public void format_double() {
    assertThat(manager.formatDouble(Locale.FRENCH, 10.56)).isEqualTo("10,6");
    assertThat(manager.formatDouble(Locale.FRENCH, 10d)).isEqualTo("10,0");
  }

  @Test
  public void format_integer() {
    assertThat(manager.formatInteger(Locale.ENGLISH, 10)).isEqualTo("10");
    assertThat(manager.formatInteger(Locale.ENGLISH, 100000)).isEqualTo("100,000");
  }

  static URLClassLoader newCheckstyleClassloader() {
    return newClassLoader("/org/sonar/core/i18n/I18nClassloaderTest/");
  }

  /**
   * Example of plugin that embeds its own translations (English + French).
   */
  static URLClassLoader newSqaleClassloader() {
    return newClassLoader("/org/sonar/core/i18n/sqalePlugin/");
  }

  /**
   * "Language Pack" contains various translations for different plugins.
   */
  static URLClassLoader newFrenchPackClassloader() {
    return newClassLoader("/org/sonar/core/i18n/frenchPack/");
  }

  private static URLClassLoader newClassLoader(String... resourcePaths) {
    URL[] urls = new URL[resourcePaths.length];
    for (int index = 0; index < resourcePaths.length; index++) {
      urls[index] = DefaultI18nTest.class.getResource(resourcePaths[index]);
    }
    return new URLClassLoader(urls);
  }

  private PluginInfo newPlugin(String key) {
    PluginInfo plugin = mock(PluginInfo.class);
    when(plugin.getKey()).thenReturn(key);
    return plugin;
  }
}
