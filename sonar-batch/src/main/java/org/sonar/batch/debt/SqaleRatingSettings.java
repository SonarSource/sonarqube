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

import org.sonar.api.BatchSide;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.Metric;

import javax.annotation.Nullable;

@BatchSide
public class SqaleRatingSettings {

  private final Settings settings;

  public SqaleRatingSettings(Settings settings) {
    this.settings = settings;
  }

  public double[] getRatingGrid() {
    try {
      String[] ratingGrades = settings.getStringArray(CoreProperties.RATING_GRID);
      double[] grid = new double[4];
      for (int i = 0; i < 4; i++) {
        grid[i] = Double.parseDouble(ratingGrades[i]);
      }
      return grid;
    } catch (Exception e) {
      throw new IllegalArgumentException("The SQALE rating grid is incorrect. Expected something similar to '"
        + CoreProperties.RATING_GRID_DEF_VALUES + "' and got '"
        + settings.getString(CoreProperties.RATING_GRID) + "'", e);
    }
  }

  public long getDevCost(@Nullable String languageKey) {
    try {
      if (languageKey != null) {
        LanguageSpecificConfiguration languageSpecificConfig = getSpecificParametersForLanguage(languageKey);
        if (languageSpecificConfig != null && languageSpecificConfig.getManDays() != null) {
          return Long.parseLong(languageSpecificConfig.getManDays());
        }
      }
      return Long.parseLong(settings.getString(CoreProperties.DEVELOPMENT_COST));
    } catch (Exception e) {
      throw new IllegalArgumentException("The value of the SQALE property '" + CoreProperties.DEVELOPMENT_COST
        + "' is incorrect. Expected long but got '" + settings.getString(CoreProperties.DEVELOPMENT_COST) + "'", e);
    }
  }

  public Metric getSizeMetric(@Nullable String languageKey, Metric[] metrics) {
    if (languageKey != null) {
      LanguageSpecificConfiguration languageSpecificConfig = getSpecificParametersForLanguage(languageKey);
      if (languageSpecificConfig != null && languageSpecificConfig.getMetric() != null) {
        return getMetricForKey(languageSpecificConfig.getMetric(), metrics);
      }
    }
    return getMetricForKey(settings.getString(CoreProperties.SIZE_METRIC), metrics);
  }

  private Metric getMetricForKey(String sizeMetricKey, Metric[] metrics) {
    for (Metric metric : metrics) {
      if (metric.getKey().equals(sizeMetricKey)) {
        return metric;
      }
    }
    throw new IllegalArgumentException("The metric key used to define the SQALE size metric is unknown : '" + sizeMetricKey + "'");
  }

  private LanguageSpecificConfiguration getSpecificParametersForLanguage(String languageKey) {
    String[] languageConfigIndexes = settings.getStringArray(CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS);
    for (String languageConfigIndex : languageConfigIndexes) {
      String languagePropertyKey = CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS + "." + languageConfigIndex + "." + CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS_LANGUAGE_KEY;
      if (languageKey.equals(settings.getString(languagePropertyKey))) {
        return LanguageSpecificConfiguration.create(settings, languageConfigIndex);
      }
    }
    return null;
  }

  private static class LanguageSpecificConfiguration {

    private final String language;
    private final String manDays;
    private final String metric;

    private LanguageSpecificConfiguration(String language, String manDays, String metric) {
      this.language = language;
      this.manDays = manDays;
      this.metric = metric;
    }

    static LanguageSpecificConfiguration create(Settings settings, String configurationId) {

      String configurationPrefix = CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS + "." + configurationId + ".";

      String language = settings.getString(configurationPrefix + CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS_LANGUAGE_KEY);
      String manDays = settings.getString(configurationPrefix + CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS_MAN_DAYS_KEY);
      String metric = settings.getString(configurationPrefix + CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS_SIZE_METRIC_KEY);

      return new LanguageSpecificConfiguration(language, manDays, metric);
    }

    String getLanguage() {
      return language;
    }

    String getManDays() {
      return manDays;
    }

    String getMetric() {
      return metric;
    }
  }
}
