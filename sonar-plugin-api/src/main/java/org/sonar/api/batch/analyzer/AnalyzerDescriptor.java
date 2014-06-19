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
package org.sonar.api.batch.analyzer;

import org.sonar.api.batch.measure.Metric;

import com.google.common.annotations.Beta;
import org.sonar.api.batch.fs.InputFile;

/**
 * Describe what an {@link Analyzer} is doing. Information may be used by the platform
 * to log interesting information or perform some optimization.
 * @since 4.4
 */
@Beta
public interface AnalyzerDescriptor {

  /**
   * Name of the {@link Analyzer}. Will be displayed in logs.
   */
  AnalyzerDescriptor name(String name);

  /**
   * List {@link Metric} this {@link Analyzer} depends on. Will be used to execute Analyzers in correct order.
   */
  AnalyzerDescriptor dependsOn(Metric<?>... metrics);

  /**
   * List {@link Metric} this {@link Analyzer} provides. Will be used to execute Analyzers in correct order.
   */
  AnalyzerDescriptor provides(Metric<?>... metrics);

  /**
   * List languages this {@link Analyzer} work on. May be used by the platform to skip execution of the {@link Analyzer} when
   * no file for given languages are present in the project.
   * If no language is provided then it will be executed for all languages.
   */
  AnalyzerDescriptor runOnLanguages(String... languageKeys);

  /**
   * List {@link InputFile.Type} this {@link Analyzer} work on. May be used by the platform to skip execution of the {@link Analyzer} when
   * no file for given type are present in the project.
   * If not type is provided then t will be executed for all types.
   */
  AnalyzerDescriptor runOnTypes(InputFile.Type... types);

}
