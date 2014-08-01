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

import com.google.common.annotations.Beta;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.duplication.DuplicationBuilder;
import org.sonar.api.batch.sensor.duplication.DuplicationGroup;
import org.sonar.api.batch.sensor.duplication.DuplicationTokenBuilder;
import org.sonar.api.batch.sensor.highlighting.HighlightingBuilder;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.IssueBuilder;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.batch.sensor.measure.MeasureBuilder;
import org.sonar.api.batch.sensor.symbol.SymbolTableBuilder;
import org.sonar.api.config.Settings;

import javax.annotation.CheckForNull;

import java.io.Serializable;
import java.util.List;

/**
 * @since 4.4
 */
@Beta
public interface SensorContext {

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
   * Builder to create a new {@link Measure}.
   */
  <G extends Serializable> MeasureBuilder<G> measureBuilder();

  /**
   * Find a project measure.
   */
  @CheckForNull
  Measure getMeasure(String metricKey);

  /**
   * Find a project measure.
   */
  @CheckForNull
  <G extends Serializable> Measure<G> getMeasure(Metric<G> metric);

  /**
   * Find a file measure.
   */
  @CheckForNull
  Measure getMeasure(InputFile file, String metricKey);

  /**
   * Find a file measure.
   */
  @CheckForNull
  <G extends Serializable> Measure<G> getMeasure(InputFile file, Metric<G> metric);

  /**
   * Add a measure. Use {@link #measureBuilder()} to create the new measure.
   * A measure for a given metric can only be saved once for the same resource.
   */
  void addMeasure(Measure<?> measure);

  // ----------- ISSUES --------------

  /**
   * Builder to create a new {@link Issue}.
   */
  IssueBuilder issueBuilder();

  /**
   * Add an issue. Use {@link #issueBuilder()} to create the new issue.
   * @return <code>true</code> if the new issue is registered, <code>false</code> if:
   * <ul>
   * <li>the rule does not exist</li>
   * <li>the rule is disabled in the Quality profile</li>
   * </ul>
   */
  boolean addIssue(Issue issue);

  // ------------ HIGHLIGHTING ------------

  /**
   * Builder to define highlighting of a file.
   * @since 4.5
   */
  HighlightingBuilder highlightingBuilder(InputFile inputFile);

  // ------------ SYMBOL REFERENCES ------------

  /**
   * Builder to define symbol references in a file.
   * @since 4.5
   */
  SymbolTableBuilder symbolTableBuilder(InputFile inputFile);

  // ------------ DUPLICATIONS ------------

  /**
   * Builder to define tokens in a file. Tokens are used to compute duplication by the core.
   * @since 4.5
   */
  DuplicationTokenBuilder duplicationTokenBuilder(InputFile inputFile);

  /**
   * Builder to manually define duplications in a file. When duplication are manually computed then
   * no need to use {@link #duplicationTokenBuilder(InputFile)}.
   * @since 4.5
   */
  DuplicationBuilder duplicationBuilder(InputFile inputFile);

  /**
   * Register all duplications of an {@link InputFile}. Use {@link #duplicationBuilder(InputFile)} to create
   * list of duplications.
   */
  void saveDuplications(InputFile inputFile, List<DuplicationGroup> duplications);

}
