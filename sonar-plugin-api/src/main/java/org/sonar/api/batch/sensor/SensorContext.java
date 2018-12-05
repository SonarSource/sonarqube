/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.io.Serializable;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.code.NewSignificantCode;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonar.api.batch.sensor.error.NewAnalysisError;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.batch.sensor.measure.NewMeasure;
import org.sonar.api.batch.sensor.rule.AdHocRule;
import org.sonar.api.batch.sensor.rule.NewAdHocRule;
import org.sonar.api.batch.sensor.symbol.NewSymbolTable;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.Settings;
import org.sonar.api.scanner.fs.InputProject;
import org.sonar.api.scanner.sensor.ProjectSensor;
import org.sonar.api.utils.Version;

/**
 * See {@link Sensor#execute(SensorContext)}
 * In order to write unit tests you can use {@link SensorContextTester}
 * @since 5.1
 */
public interface SensorContext {

  /**
   * @deprecated since 6.5 use {@link #config()}
   */
  @Deprecated
  Settings settings();

  /**
   * Get settings of the project.
   * @since 6.5
   */
  Configuration config();

  /**
   * Get filesystem of the project.
   */
  FileSystem fileSystem();

  /**
   * Get list of active rules.
   */
  ActiveRules activeRules();

  /**
   * @since 5.5
   * @deprecated since 7.6 modules are deprecated. Use {@link #project()} instead.
   * @throws UnsupportedOperationException for global {@link ProjectSensor}s
   */
  @Deprecated
  InputModule module();

  /**
   * The current project.
   * @since 7.6
   */
  InputProject project();

  /**
   * Version of API at runtime, not at compilation time. It's a shortcut on
   * {@code runtime().getApiVersion()} since 6.0.
   * @since 5.5
   * @see #runtime() since version 6.0.
   */
  Version getSonarQubeVersion();

  /**
   * Runtime information, mainly:
   * <ul>
   *   <li>to be able to have different behaviours between SonarQube and SonarLint</li>
   *   <li>to enable new features depending on version of API available at runtime</li>
   * </ul>
   * @since 6.0
   */
  SonarRuntime runtime();

  /**
   * Test if a cancellation of the analysis was requested. Sensors should periodically test this flag
   * and gracefully stop if value is {@code true}. For example it could be tested between each processed file.
   * @since 6.0
   */
  boolean isCancelled();

  // ----------- MEASURES --------------

  /**
   * Fluent builder to create a new {@link Measure}. Don't forget to call {@link NewMeasure#save()} once all parameters are provided.
   */
  <G extends Serializable> NewMeasure<G> newMeasure();

  // ----------- ISSUES --------------

  /**
   * Fluent builder to create a new {@link Issue}. Don't forget to call {@link NewIssue#save()} once all parameters are provided.
   */
  NewIssue newIssue();

  /**
   * Fluent builder to create a new {@link ExternalIssue}. Don't forget to call {@link NewExternalIssue#save()} once all parameters are provided.
   * @since 7.2
   */
  NewExternalIssue newExternalIssue();

  /**
   * Fluent builder to create a new {@link AdHocRule}. Don't forget to call {@link NewAdHocRule#save()} once all parameters are provided.
   * @since 7.4
   */
  NewAdHocRule newAdHocRule();

  // ------------ HIGHLIGHTING ------------

  /**
   * Builder to define highlighting of a file. Don't forget to call {@link NewHighlighting#save()} once all elements are provided.
   */
  NewHighlighting newHighlighting();

  // ------------ SYMBOL TABLE ------------

  /**
   * Builder to define symbol table of a file. Don't forget to call {@link NewSymbolTable#save()} once all symbols are provided.
   * @since 5.6
   */
  NewSymbolTable newSymbolTable();

  // ------------ COVERAGE ------------

  /**
   * Builder to define coverage in a file.
   * Don't forget to call {@link NewCoverage#save()}.
   */
  NewCoverage newCoverage();

  // ------------ CPD ------------

  /**
   * Builder to define CPD tokens in a file.
   * Don't forget to call {@link NewCpdTokens#save()}.
   * @since 5.5
   */
  NewCpdTokens newCpdTokens();

  // ------------ ANALYSIS ERROR ------------

  /**
   * Builder to declare errors that happened while processing a source file.
   * Don't forget to call {@link NewAnalysisError#save()}.
   * @since 6.0
   */
  NewAnalysisError newAnalysisError();

  /**
   * Builder to declare which parts of the code is significant code. 
   * Ranges that are not reported as significant code will be ignored and won't be considered when calculating which lines were modified.
   * 
   * If the significant code is not reported for a file, it is assumed that the entire file is significant code.
   * 
   * @since 7.2
   */
  NewSignificantCode newSignificantCode();

  /**
   * Add a property to the scanner context. This context is available
   * in Compute Engine when processing the report.
   * <br/>
   * The properties starting with {@code "sonar.analysis."} are included to the
   * payload of webhooks.
   *
   * @throws IllegalArgumentException if key or value parameter is null
   * @see org.sonar.api.ce.posttask.PostProjectAnalysisTask.ProjectAnalysis#getScannerContext()
   * @since 6.1
   */
  void addContextProperty(String key, String value);

  /**
   * Indicate that a file should be published in the report sent to SonarQube.
   * Files are automatically marked if any data is created for it (issues, highlighting, coverage, etc.).
   * @since 6.3
   */
  void markForPublishing(InputFile inputFile);

}
