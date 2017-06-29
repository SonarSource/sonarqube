/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.qualitymodel;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.MessageException;

import static java.lang.String.format;
import static org.sonar.api.CoreProperties.DEVELOPMENT_COST;
import static org.sonar.api.CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS;
import static org.sonar.api.CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS_LANGUAGE_KEY;
import static org.sonar.api.CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS_MAN_DAYS_KEY;
import static org.sonar.api.CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS_SIZE_METRIC_KEY;
import static org.sonar.api.CoreProperties.RATING_GRID;
import static org.sonar.api.CoreProperties.RATING_GRID_DEF_VALUES;

public class RatingSettings {

  private final Configuration config;
  private final Map<String, LanguageSpecificConfiguration> languageSpecificConfigurationByLanguageKey;

  public RatingSettings(Configuration config) {
    this.config = config;
    this.languageSpecificConfigurationByLanguageKey = buildLanguageSpecificConfigurationByLanguageKey(config);
  }

  private static Map<String, LanguageSpecificConfiguration> buildLanguageSpecificConfigurationByLanguageKey(Configuration config) {
    ImmutableMap.Builder<String, LanguageSpecificConfiguration> builder = ImmutableMap.builder();
    String[] languageConfigIndexes = config.getStringArray(LANGUAGE_SPECIFIC_PARAMETERS);
    for (String languageConfigIndex : languageConfigIndexes) {
      String languagePropertyKey = LANGUAGE_SPECIFIC_PARAMETERS + "." + languageConfigIndex + "." + LANGUAGE_SPECIFIC_PARAMETERS_LANGUAGE_KEY;
      String languageKey = config.get(languagePropertyKey)
        .orElseThrow(() -> MessageException.of("Technical debt configuration is corrupted. At least one language specific parameter has no Language key. " +
          "Contact your administrator to update this configuration in the global administration section of SonarQube."));
      builder.put(languageKey, LanguageSpecificConfiguration.create(config, languageConfigIndex));
    }
    return builder.build();
  }

  public RatingGrid getRatingGrid() {
    try {
      String[] ratingGrades = config.getStringArray(RATING_GRID);
      double[] grid = new double[4];
      for (int i = 0; i < 4; i++) {
        grid[i] = Double.parseDouble(ratingGrades[i]);
      }
      return new RatingGrid(grid);
    } catch (Exception e) {
      throw new IllegalArgumentException("The rating grid is incorrect. Expected something similar to '"
        + RATING_GRID_DEF_VALUES + "' and got '"
        + config.get(RATING_GRID).get() + "'", e);
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
      return Long.parseLong(config.get(DEVELOPMENT_COST).get());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("The value of the development cost property '" + DEVELOPMENT_COST
        + "' is incorrect. Expected long but got '" + config.get(DEVELOPMENT_COST).get() + "'", e);
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

    static LanguageSpecificConfiguration create(Configuration config, String configurationId) {

      String configurationPrefix = LANGUAGE_SPECIFIC_PARAMETERS + "." + configurationId + ".";

      String language = config.get(configurationPrefix + LANGUAGE_SPECIFIC_PARAMETERS_LANGUAGE_KEY).orElse(null);
      String manDays = config.get(configurationPrefix + LANGUAGE_SPECIFIC_PARAMETERS_MAN_DAYS_KEY).orElse(null);
      String metric = config.get(configurationPrefix + LANGUAGE_SPECIFIC_PARAMETERS_SIZE_METRIC_KEY).orElse(null);

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
