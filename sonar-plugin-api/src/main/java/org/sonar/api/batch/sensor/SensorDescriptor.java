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
package org.sonar.api.batch.sensor;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.measure.Metric;

/**
 * Describe what an {@link Sensor} is doing. Information may be used by the platform
 * to log interesting information or perform some optimization.
 * See {@link Sensor#describe(SensorDescriptor)}
 * @since 5.0
 */
public interface SensorDescriptor {

  /**
   * Name of the {@link Sensor}. Will be displayed in logs.
   */
  SensorDescriptor name(String name);

  /**
   * List {@link Metric} this {@link Sensor} depends on. Will be used to execute sensors in correct order.
   */
  SensorDescriptor dependsOn(Metric<?>... metrics);

  /**
   * List {@link Metric} this {@link Sensor} provides. Will be used to execute sensors in correct order.
   */
  SensorDescriptor provides(Metric<?>... metrics);

  /**
   * List languages this {@link Sensor} work on. May be used by the platform to skip execution of the {@link Sensor} when
   * no file for given languages are present in the project.
   * If no language is provided then it will be executed for all languages.
   */
  SensorDescriptor workOnLanguages(String... languageKeys);

  /**
   * List {@link InputFile.Type} this {@link Sensor} work on. May be used by the platform to skip execution of the {@link Sensor} when
   * no file for given type are present in the project.
   * If you don't call this method then it means sensor is working on all input file types.
   */
  SensorDescriptor workOnFileTypes(InputFile.Type... types);

  /**
   * List {@link InputFile.Type} this {@link Sensor} work on. May be used by the platform to skip execution of the {@link Sensor} when
   * no file for given type are present in the project.
   * If not type is provided then it will be executed for all types.
   */
  SensorDescriptor createIssuesForRuleRepositories(String... repositoryKeys);

}
