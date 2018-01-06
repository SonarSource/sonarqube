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
package org.sonar.api.batch.sensor;

import java.util.function.Predicate;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.config.Configuration;

/**
 * Describe what a {@link Sensor} is doing. Information may be used by the platform
 * to log interesting information or perform some optimization.
 * See {@link Sensor#describe(SensorDescriptor)}
 * @since 5.1
 */
public interface SensorDescriptor {

  /**
   * Displayable name of the {@link Sensor}. Will be displayed in logs.
   */
  SensorDescriptor name(String sensorName);

  /**
   * Language this {@link Sensor} work on. Used by the platform to skip execution of the {@link Sensor} when
   * no file for given languages are present in the project.
   * Default is to execute sensor for all languages.
   */
  SensorDescriptor onlyOnLanguage(String languageKey);

  /**
   * List languages this {@link Sensor} work on. Used by the platform to skip execution of the {@link Sensor} when
   * no file for given languages are present in the project.
   * Default is to execute sensor for all languages.
   */
  SensorDescriptor onlyOnLanguages(String... languageKeys);

  /**
   * {@link InputFile.Type} this {@link Sensor} work on. Used by the platform to skip execution of the {@link Sensor} when
   * no file for given type are present in the project.
   * Default is to execute sensor whatever are the available file types.
   */
  SensorDescriptor onlyOnFileType(InputFile.Type type);

  /**
   * Rule repository this {@link Sensor} create issues for. Used by the platform to skip execution of the {@link Sensor} when
   * no rule is activated for the given repository.
   */
  SensorDescriptor createIssuesForRuleRepository(String... repositoryKey);

  /**
   * List rule repositories this {@link Sensor} create issues for. Used by the platform to skip execution of the {@link Sensor} when
   * no rule is activated for the given repositories.
   */
  SensorDescriptor createIssuesForRuleRepositories(String... repositoryKeys);

  /**
   * Property this {@link Sensor} depends on. Used by the platform to skip execution of the {@link Sensor} when
   * property is not set.
   * @deprecated since 6.5 use {@link #onlyWhenConfiguration(Predicate)}
   */
  @Deprecated
  SensorDescriptor requireProperty(String... propertyKey);

  /**
   * List properties this {@link Sensor} depends on. Used by the platform to skip execution of the {@link Sensor} when
   * property is not set.
   * @deprecated since 6.5 use {@link #onlyWhenConfiguration(Predicate)}
   */
  @Deprecated
  SensorDescriptor requireProperties(String... propertyKeys);

  /**
   * This sensor should be executed at the project level, instead of per-module.
   * @since 6.4
   */
  SensorDescriptor global();

  /**
   * Predicate that will be evaluated on current module/project {@link Configuration} by the platform to decide if execution of the {@link Sensor} should be skipped.
   * @since 6.5
   */
  SensorDescriptor onlyWhenConfiguration(Predicate<Configuration> predicate);
}
