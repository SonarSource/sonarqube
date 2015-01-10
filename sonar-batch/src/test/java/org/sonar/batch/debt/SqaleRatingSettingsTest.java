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

package org.sonar.batch.debt;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;

import static org.assertj.core.api.Assertions.assertThat;


public class SqaleRatingSettingsTest {

  private static final Metric[] metrics = {CoreMetrics.NCLOC, CoreMetrics.COMPLEXITY};

  private Settings settings;

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  @Before
  public void setUp() {
    settings = new Settings();
  }

  @Test
  public void load_rating_grid() throws Exception {
    settings.setProperty(CoreProperties.RATING_GRID, "1,3.4,8,50");
    SqaleRatingSettings configurationLoader = new SqaleRatingSettings(settings);

    double[] grid = configurationLoader.getRatingGrid();
    assertThat(grid).hasSize(4);
    assertThat(grid[0]).isEqualTo(1.0);
    assertThat(grid[1]).isEqualTo(3.4);
    assertThat(grid[2]).isEqualTo(8.0);
    assertThat(grid[3]).isEqualTo(50.0);
  }

  @Test
  public void load_work_units_for_language() throws Exception {
    settings.setProperty(CoreProperties.DEVELOPMENT_COST, "50");
    SqaleRatingSettings configurationLoader = new SqaleRatingSettings(settings);

    assertThat(configurationLoader.getDevCost("defaultLanguage")).isEqualTo(50L);
  }

  @Test
  public void load_size_metric_for_language() throws Exception {
    settings.setProperty(CoreProperties.SIZE_METRIC, "complexity");
    SqaleRatingSettings configurationLoader = new SqaleRatingSettings(settings);

    assertThat(configurationLoader.getSizeMetric("defaultLanguage", metrics)).isEqualTo(CoreMetrics.COMPLEXITY);
  }

  @Test
  public void load_overridden_values_for_language() throws Exception {

    String aLanguage = "aLanguage";
    String anotherLanguage = "anotherLanguage";

    settings.setProperty(CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS, "0,1");
    settings.setProperty(CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS + "." + "0" + "." + CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS_LANGUAGE_KEY, aLanguage);
    settings.setProperty(CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS + "." + "0" + "." + CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS_MAN_DAYS_KEY, "30");
    settings.setProperty(CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS + "." + "0" + "." + CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS_SIZE_METRIC_KEY, CoreMetrics.NCLOC_KEY);
    settings.setProperty(CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS + "." + "1" + "." + CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS_LANGUAGE_KEY, anotherLanguage);
    settings.setProperty(CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS + "." + "1" + "." + CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS_MAN_DAYS_KEY, "40");
    settings.setProperty(CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS + "." + "1" + "." + CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS_SIZE_METRIC_KEY, CoreMetrics.COMPLEXITY_KEY);

    SqaleRatingSettings configurationLoader = new SqaleRatingSettings(settings);

    assertThat(configurationLoader.getSizeMetric(aLanguage, metrics)).isEqualTo(CoreMetrics.NCLOC);
    assertThat(configurationLoader.getSizeMetric(anotherLanguage, metrics)).isEqualTo(CoreMetrics.COMPLEXITY);
    assertThat(configurationLoader.getDevCost(aLanguage)).isEqualTo(30L);
    assertThat(configurationLoader.getDevCost(anotherLanguage)).isEqualTo(40L);
  }

  @Test
  public void fail_on_invalid_rating_grid_configuration() throws Exception {

    throwable.expect(IllegalArgumentException.class);
    settings.setProperty(CoreProperties.RATING_GRID, "a b c");
    SqaleRatingSettings configurationLoader = new SqaleRatingSettings(settings);

    configurationLoader.getRatingGrid();
  }
  
  @Test
  public void fail_on_invalid_work_unit_value() throws Exception {
    throwable.expect(IllegalArgumentException.class);
    settings.setProperty(CoreProperties.DEVELOPMENT_COST, "a");
    SqaleRatingSettings configurationLoader = new SqaleRatingSettings(settings);

    configurationLoader.getSizeMetric("aLanguage", metrics);
  }

  @Test
  public void fail_on_unknown_metric_key() throws Exception {
    throwable.expect(IllegalArgumentException.class);
    settings.setProperty(CoreProperties.SIZE_METRIC, "unknown");
    SqaleRatingSettings configurationLoader = new SqaleRatingSettings(settings);

    configurationLoader.getSizeMetric("aLanguage", metrics);
  }

  @Test
  public void use_generic_value_when_specific_setting_is_missing() throws Exception {
    String aLanguage = "aLanguage";

    settings.setProperty(CoreProperties.SIZE_METRIC, "complexity");
    settings.setProperty(CoreProperties.DEVELOPMENT_COST, "30");
    settings.setProperty(CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS, "0");
    settings.setProperty(CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS + "." + "0" + "." + CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS_LANGUAGE_KEY, aLanguage);
    settings.setProperty(CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS + "." + "0" + "." + CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS_MAN_DAYS_KEY, "40");

    SqaleRatingSettings configurationLoader = new SqaleRatingSettings(settings);

    assertThat(configurationLoader.getSizeMetric(aLanguage, metrics)).isEqualTo(CoreMetrics.COMPLEXITY);
    assertThat(configurationLoader.getDevCost(aLanguage)).isEqualTo(40L);
  }
}
