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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultI18nTest {

  private TestSystem2 system2 = new TestSystem2();

  DefaultI18n underTest;

  @Before
  public void before() {
    PluginRepository pluginRepository = mock(PluginRepository.class);
    List<PluginInfo> plugins = Arrays.asList(newPlugin("sqale"), newPlugin("frpack"), newPlugin("checkstyle"), newPlugin("other"));
    when(pluginRepository.getPluginInfos()).thenReturn(plugins);

    underTest = new DefaultI18n(pluginRepository, system2);
    underTest.doStart(getClass().getClassLoader());
  }

  @Test
  public void load_core_bundle() {
    assertThat(underTest.message(Locale.ENGLISH, "any", null)).isEqualTo("Any");
  }

  @Test
  public void introspect_all_available_properties() {
    assertThat(underTest.getPropertyKeys().contains("any")).isTrue();
    // Only in english
    assertThat(underTest.getPropertyKeys().contains("assignee")).isTrue();
    assertThat(underTest.getPropertyKeys().contains("sqale.page")).isTrue();
    assertThat(underTest.getPropertyKeys().contains("bla_bla_bla")).isFalse();
  }

  @Test
  public void all_core_metrics_are_in_core_bundle() {
    List<Metric> coreMetrics = CoreMetrics.getMetrics();
    List<String> incorrectMetricDefinitions = new ArrayList<>();
    for (Metric metric : coreMetrics) {
      if (metric.isHidden()) {
        continue;
      }
      String metricNamePropertyKey = "metric." + metric.getKey() + ".name";
      String l10nMetricName = underTest.message(Locale.ENGLISH, metricNamePropertyKey, null);
      if (l10nMetricName == null) {
        incorrectMetricDefinitions.add(metricNamePropertyKey + "=" + metric.getName());
      } else if (!l10nMetricName.equals(metric.getName())) {
        incorrectMetricDefinitions.add(metricNamePropertyKey + " is not consistent in core bundle and CoreMetrics");
      }

      String metricDescriptionPropertyKey = "metric." + metric.getKey() + ".description";
      String l10nMetricDescription = underTest.message(Locale.ENGLISH, metricDescriptionPropertyKey, null);
      if (l10nMetricDescription == null) {
        incorrectMetricDefinitions.add(metricDescriptionPropertyKey + "=" + metric.getDescription());
      } else if (!l10nMetricDescription.equals(metric.getDescription())) {
        incorrectMetricDefinitions.add(metricDescriptionPropertyKey + " is not consistent in core bundle and CoreMetrics");
      }
    }

    assertThat(incorrectMetricDefinitions).as("Metric definitions to fix in core bundle", incorrectMetricDefinitions.size()).isEmpty();
  }

  @Test
  public void get_english_labels() {
    assertThat(underTest.message(Locale.ENGLISH, "any", null)).isEqualTo("Any");
    assertThat(underTest.message(Locale.ENGLISH, "sqale.page", null)).isEqualTo("Sqale page title");
    assertThat(underTest.message(Locale.ENGLISH, "checkstyle.rule1.name", null)).isEqualTo("Rule one");
  }

  // SONAR-2927
  @Test
  public void get_english_labels_when_default_locale_is_not_english() {
    Locale defaultLocale = Locale.getDefault();
    try {
      Locale.setDefault(Locale.FRENCH);
      assertThat(underTest.message(Locale.ENGLISH, "any", null)).isEqualTo("Any");
      assertThat(underTest.message(Locale.ENGLISH, "sqale.page", null)).isEqualTo("Sqale page title");
      assertThat(underTest.message(Locale.ENGLISH, "checkstyle.rule1.name", null)).isEqualTo("Rule one");
    } finally {
      Locale.setDefault(defaultLocale);
    }
  }

  @Test
  public void get_labels_from_french_pack() {
    assertThat(underTest.message(Locale.FRENCH, "checkstyle.rule1.name", null)).isEqualTo("Rule un");
    assertThat(underTest.message(Locale.FRENCH, "any", null)).isEqualTo("Tous");

    // language pack
    assertThat(underTest.message(Locale.FRENCH, "sqale.page", null)).isEqualTo("Titre de la page Sqale");
  }

  @Test
  public void get_french_label_if_swiss_country() {
    Locale swiss = new Locale("fr", "CH");
    assertThat(underTest.message(swiss, "checkstyle.rule1.name", null)).isEqualTo("Rule un");
    assertThat(underTest.message(swiss, "any", null)).isEqualTo("Tous");

    // language pack
    assertThat(underTest.message(swiss, "sqale.page", null)).isEqualTo("Titre de la page Sqale");
  }

  @Test
  public void fallback_to_default_locale() {
    assertThat(underTest.message(Locale.CHINA, "checkstyle.rule1.name", null)).isEqualTo("Rule one");
    assertThat(underTest.message(Locale.CHINA, "any", null)).isEqualTo("Any");
    assertThat(underTest.message(Locale.CHINA, "sqale.page", null)).isEqualTo("Sqale page title");
    assertThat(underTest.getEffectiveLocale(Locale.CHINA)).isEqualTo(Locale.ENGLISH);
  }

  @Test
  public void return_default_value_if_missing_key() {
    assertThat(underTest.message(Locale.ENGLISH, "bla_bla_bla", "default")).isEqualTo("default");
    assertThat(underTest.message(Locale.FRENCH, "bla_bla_bla", "default")).isEqualTo("default");
  }

  @Test
  public void format_message_with_parameters() {
    assertThat(underTest.message(Locale.ENGLISH, "x_results", null, "10")).isEqualTo("10 results");
  }

  @Test
  public void use_default_locale_if_missing_value_in_localized_bundle() {
    assertThat(underTest.message(Locale.FRENCH, "assignee", null)).isEqualTo("Assignee");
    assertThat(underTest.message(Locale.CHINA, "assignee", null)).isEqualTo("Assignee");
  }

  @Test
  public void return_null_if_file_not_found() {
    String html = underTest.messageFromFile(Locale.ENGLISH, "UnknownRule.html", "checkstyle.rule1.name");
    assertThat(html).isNull();
  }

  @Test
  public void return_null_if_rule_not_internationalized() {
    String html = underTest.messageFromFile(Locale.ENGLISH, "UnknownRule.html", "foo.rule1.name");
    assertThat(html).isNull();
  }

  @Test
  public void get_age_with_duration() {
    assertThat(underTest.age(Locale.ENGLISH, 10)).isEqualTo("less than a minute");
  }

  @Test
  public void get_age_with_dates() {
    assertThat(underTest.age(Locale.ENGLISH, DateUtils.parseDate("2014-01-01"), DateUtils.parseDate("2014-01-02"))).isEqualTo("a day");
  }

  @Test
  public void get_age_from_now() {
    system2.setNow(DateUtils.parseDate("2014-01-02").getTime());
    assertThat(underTest.ageFromNow(Locale.ENGLISH, DateUtils.parseDate("2014-01-01"))).isEqualTo("a day");
  }

  @Test
  public void format_date_time() {
    TimeZone initialTz = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("GMT+1"));
    assertThat(underTest.formatDateTime(Locale.ENGLISH, DateUtils.parseDateTime("2014-01-22T19:10:03+0100"))).startsWith("Jan 22, 2014");
    TimeZone.setDefault(initialTz);
  }

  @Test
  public void format_date() {
    TimeZone initialTz = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("GMT+1"));
    assertThat(underTest.formatDate(Locale.ENGLISH, DateUtils.parseDateTime("2014-01-22T19:10:03+0100"))).isEqualTo("Jan 22, 2014");
    TimeZone.setDefault(initialTz);
  }

  @Test
  public void format_double() {
    assertThat(underTest.formatDouble(Locale.FRENCH, 10.56)).isEqualTo("10,6");
    assertThat(underTest.formatDouble(Locale.FRENCH, 10d)).isEqualTo("10,0");
  }

  @Test
  public void format_integer() {
    assertThat(underTest.formatInteger(Locale.ENGLISH, 10)).isEqualTo("10");
    assertThat(underTest.formatInteger(Locale.ENGLISH, 100000)).isEqualTo("100,000");
  }

  private PluginInfo newPlugin(String key) {
    PluginInfo plugin = mock(PluginInfo.class);
    when(plugin.getKey()).thenReturn(key);
    return plugin;
  }
}
