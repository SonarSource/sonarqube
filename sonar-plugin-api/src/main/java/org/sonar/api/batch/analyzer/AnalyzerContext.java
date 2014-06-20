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

import com.google.common.annotations.Beta;
import org.sonar.api.batch.analyzer.issue.AnalyzerIssue;
import org.sonar.api.batch.analyzer.issue.AnalyzerIssueBuilder;
import org.sonar.api.batch.analyzer.measure.AnalyzerMeasure;
import org.sonar.api.batch.analyzer.measure.AnalyzerMeasureBuilder;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.config.Settings;

import javax.annotation.CheckForNull;

import java.io.Serializable;

/**
 * @since 4.4
 */
@Beta
public interface AnalyzerContext {

  /**
   * Get settings of the current project.
   */
  Settings settings();

  /**
   * Get filesystem of the current project.
   */
  FileSystem fileSystem();

  /**
   * Get list of active rules.
   */
  ActiveRules activeRules();

  // ----------- MEASURES --------------

  /**
   * Builder to create a new {@link AnalyzerMeasure}.
   */
  <G extends Serializable> AnalyzerMeasureBuilder<G> measureBuilder();

  /**
   * Find a project measure.
   */
  @CheckForNull
  AnalyzerMeasure getMeasure(String metricKey);

  /**
   * Find a project measure.
   */
  @CheckForNull
  <G extends Serializable> AnalyzerMeasure<G> getMeasure(Metric<G> metric);

  /**
   * Find a file measure.
   */
  @CheckForNull
  AnalyzerMeasure getMeasure(InputFile file, String metricKey);

  /**
   * Find a file measure.
   */
  @CheckForNull
  <G extends Serializable> AnalyzerMeasure<G> getMeasure(InputFile file, Metric<G> metric);

  /**
   * Add a measure. Use {@link #measureBuilder()} to create the new measure.
   */
  void addMeasure(AnalyzerMeasure<?> measure);

  // ----------- ISSUES --------------

  /**
   * Builder to create a new {@link AnalyzerIssue}.
   */
  AnalyzerIssueBuilder issueBuilder();

  /**
   * Add an issue. Use {@link #issueBuilder()} to create the new issue.
   * @return true if the new issue is registered, false if the related rule does not exist or is disabled in the Quality profile.
   */
  boolean addIssue(AnalyzerIssue issue);

}
