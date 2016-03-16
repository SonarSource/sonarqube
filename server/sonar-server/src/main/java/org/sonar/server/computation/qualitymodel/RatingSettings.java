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
package org.sonar.server.computation.qualitymodel;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.config.Settings;

import static java.lang.String.format;
import static org.sonar.api.CoreProperties.DEVELOPMENT_COST;
import static org.sonar.api.CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS;
import static org.sonar.api.CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS_LANGUAGE_KEY;
import static org.sonar.api.CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS_MAN_DAYS_KEY;
import static org.sonar.api.CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS_SIZE_METRIC_KEY;
import static org.sonar.api.CoreProperties.RATING_GRID;
import static org.sonar.api.CoreProperties.RATING_GRID_DEF_VALUES;

public class RatingSettings {

  private final Settings settings;
  private final Map<String, LanguageSpecificConfiguration> languageSpecificConfigurationByLanguageKey;

  public RatingSettings(Settings settings) {
    this.settings = settings;
    this.languageSpecificConfigurationByLanguageKey = buildLanguageSpecificConfigurationByLanguageKey(settings);
  }

  private static Map<String, LanguageSpecificConfiguration> buildLanguageSpecificConfigurationByLanguageKey(Settings settings) {
    ImmutableMap.Builder<String, LanguageSpecificConfiguration> builder = ImmutableMap.builder();
    String[] languageConfigIndexes = settings.getStringArray(LANGUAGE_SPECIFIC_PARAMETERS);
    for (String languageConfigIndex : languageConfigIndexes) {
      String languagePropertyKey = LANGUAGE_SPECIFIC_PARAMETERS + "." + languageConfigIndex + "." + LANGUAGE_SPECIFIC_PARAMETERS_LANGUAGE_KEY;
      builder.put(settings.getString(languagePropertyKey), LanguageSpecificConfiguration.create(settings, languageConfigIndex));
    }
    return builder.build();
  }

  public RatingGrid getRatingGrid() {
    try {
      String[] ratingGrades = settings.getStringArray(RATING_GRID);
      double[] grid = new double[4];
      for (int i = 0; i < 4; i++) {
        grid[i] = Double.parseDouble(ratingGrades[i]);
      }
      return new RatingGrid(grid);
    } catch (Exception e) {
      throw new IllegalArgumentException("The rating grid is incorrect. Expected something similar to '"
        + RATING_GRID_DEF_VALUES + "' and got '"
        + settings.getString(RATING_GRID) + "'", e);
    }
  }

  public long getDevCost(@Nullable String languageKey) {
    if (languageKey != null) {
      try {
        LanguageSpecificConfiguration languageSpecificConfig = getSpecificParametersForLanguage(languageKey);
        if (languageSpecificConfig != null && languageSpecificConfig.getManDays() != null) {
          return Long.parseLong(languageSpecificConfig.getManDays());
        }
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(format("The manDays for language %s is not a valid long number", languageKey), e);
      }
    }

    return getDefaultDevelopmentCost();
  }

  private long getDefaultDevelopmentCost() {
    try {
      return Long.parseLong(settings.getString(DEVELOPMENT_COST));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("The value of the development cost property '" + DEVELOPMENT_COST
        + "' is incorrect. Expected long but got '" + settings.getString(DEVELOPMENT_COST) + "'", e);
    }
  }

  @CheckForNull
  private LanguageSpecificConfiguration getSpecificParametersForLanguage(String languageKey) {
    return languageSpecificConfigurationByLanguageKey.get(languageKey);
  }

  @Immutable
  private static class LanguageSpecificConfiguration {
    private final String language;
    private final String manDays;
    private final String metricKey;

    private LanguageSpecificConfiguration(String language, String manDays, String metricKey) {
      this.language = language;
      this.manDays = manDays;
      this.metricKey = metricKey;
    }

    static LanguageSpecificConfiguration create(Settings settings, String configurationId) {

      String configurationPrefix = LANGUAGE_SPECIFIC_PARAMETERS + "." + configurationId + ".";

      String language = settings.getString(configurationPrefix + LANGUAGE_SPECIFIC_PARAMETERS_LANGUAGE_KEY);
      String manDays = settings.getString(configurationPrefix + LANGUAGE_SPECIFIC_PARAMETERS_MAN_DAYS_KEY);
      String metric = settings.getString(configurationPrefix + LANGUAGE_SPECIFIC_PARAMETERS_SIZE_METRIC_KEY);

      return new LanguageSpecificConfiguration(language, manDays, metric);
    }

    String getLanguage() {
      return language;
    }

    String getManDays() {
      return manDays;
    }

    String getMetricKey() {
      return metricKey;
    }
  }
}
