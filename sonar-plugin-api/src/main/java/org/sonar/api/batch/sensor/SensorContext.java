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
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.duplication.DuplicationBuilder;
import org.sonar.api.batch.sensor.duplication.DuplicationGroup;
import org.sonar.api.batch.sensor.duplication.DuplicationTokenBuilder;
import org.sonar.api.batch.sensor.highlighting.HighlightingBuilder;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.IssueBuilder;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.batch.sensor.symbol.SymbolTableBuilder;
import org.sonar.api.batch.sensor.test.TestCase;
import org.sonar.api.batch.sensor.test.TestCaseBuilder;
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
  <G extends Serializable> Measure<G> newMeasure();

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
   * Builder to define tokens in a file. Tokens are used to compute duplication using default SonarQube engine.
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
   * @since 4.5
   */
  void saveDuplications(InputFile inputFile, List<DuplicationGroup> duplications);

  // ------------ TESTS ------------

  /**
   * Create a new test case for the given test file.
   * @param testFile An {@link InputFile} with type {@link InputFile.Type#TEST}
   * @param testCaseName name of the test case
   * @since 5.0
   */
  TestCaseBuilder testCaseBuilder(InputFile testFile, String testCaseName);

  /**
   * Add a new test case.
   * Use {@link #testCaseBuilder(InputFile, String)} to create a new {@link TestCase}
   * @throws IllegalArgumentException if a test case with same name was already added on the same file.
   * @since 5.0
   */
  void addTestCase(TestCase testCase);

  /**
   * Get a {@link TestCase} that has been previously added to the context with {@link #addTestCase(TestCase)}.
   * @since 5.0
   */
  @CheckForNull
  TestCase getTestCase(InputFile testFile, String testCaseName);

  /**
   * Register coverage of a given test case on another main file. TestCase should have been registered using {@link #testPlanBuilder(InputFile)}
   * @param testFile test file containing the test case
   * @param testCaseName name of the test case
   * @param coveredFile main file that is covered
   * @param coveredLines list of covered lines
   * @since 5.0
   */
  void saveCoveragePerTest(TestCase testCase, InputFile coveredFile, List<Integer> coveredLines);

  // ------------ DEPENDENCIES ------------

  /**
   * Declare a dependency between 2 files.
   * @param weight Weight of the dependency
   * @since 5.0
   */
  void saveDependency(InputFile from, InputFile to, int weight);

}
