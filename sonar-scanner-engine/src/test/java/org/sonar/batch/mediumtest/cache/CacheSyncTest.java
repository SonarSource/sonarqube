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
package org.sonar.batch.mediumtest.cache;

import org.junit.rules.TemporaryFolder;

import org.sonar.batch.mediumtest.TaskResult;
import org.sonar.batch.mediumtest.BatchMediumTester.TaskBuilder;
import org.sonar.batch.mediumtest.LogOutputRecorder;
import org.sonar.batch.repository.FileData;
import com.google.common.collect.ImmutableMap;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.CoreProperties;
import org.sonar.batch.mediumtest.BatchMediumTester;
import org.sonar.xoo.XooPlugin;
import org.sonar.xoo.rule.XooRulesDefinition;

public class CacheSyncTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private BatchMediumTester tester;

  @After
  public void stop() {
    if (tester != null) {
      tester.stop();
      tester = null;
    }
  }

  @Test
  public void testExecuteTask() {
    LogOutputRecorder logOutput = new LogOutputRecorder();

    tester = BatchMediumTester.builder()
      .bootstrapProperties(ImmutableMap.of(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_ISSUES,
        "sonar.verbose", "true"))
      .registerPlugin("xoo", new XooPlugin())
      .addRules(new XooRulesDefinition())
      .addQProfile("lang", "name")
      .addActiveRule("xoo", "OneIssuePerLine", null, "One issue per line", "MAJOR", "my/internal/key", "xoo")
      .setPreviousAnalysisDate(new Date())
      .addFileData("test-project", "file1", new FileData("hash", "123456789"))
      .setLogOutput(logOutput)
      .build();

    tester.start();
    executeTask(tester.newTask());
    assertThat(logOutput.getAsString()).contains("Cache for project [key] not found, synchronizing");
  }

  @Test
  public void testSyncFirstTime() {
    LogOutputRecorder logOutput = new LogOutputRecorder();

    tester = BatchMediumTester.builder()
      .bootstrapProperties(ImmutableMap.of(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_ISSUES,
        "sonar.verbose", "true"))
      .registerPlugin("xoo", new XooPlugin())
      .addRules(new XooRulesDefinition())
      .addQProfile("lang", "name")
      .addActiveRule("xoo", "OneIssuePerLine", null, "One issue per line", "MAJOR", "my/internal/key", "xoo")
      .setPreviousAnalysisDate(new Date())
      .addFileData("test-project", "file1", new FileData("hash", "123456789"))
      .setLogOutput(logOutput)
      .build();

    tester.start();
    tester.syncProject("test-project");
    assertThat(logOutput.getAsString()).contains("Cache for project [test-project] not found");
  }

  @Test
  public void testSyncTwice() {
    LogOutputRecorder logOutput = new LogOutputRecorder();

    tester = BatchMediumTester.builder()
      .bootstrapProperties(ImmutableMap.of(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_ISSUES,
        "sonar.verbose", "true"))
      .registerPlugin("xoo", new XooPlugin())
      .addRules(new XooRulesDefinition())
      .addQProfile("lang", "name")
      .addActiveRule("xoo", "OneIssuePerLine", null, "One issue per line", "MAJOR", "my/internal/key", "xoo")
      .setPreviousAnalysisDate(new Date())
      .addFileData("test-project", "file1", new FileData("hash", "123456789"))
      .setLogOutput(logOutput)
      .build();

    tester.start();
    tester.syncProject("test-project");
    tester.syncProject("test-project");
    assertThat(logOutput.getAsString()).contains("-- Found project [test-project]");
    assertThat(logOutput.getAsString()).contains("not found, synchronizing data");
    assertThat(logOutput.getAsString()).contains("], synchronizing data (forced)..");
  }

  @Test
  public void testNonAssociated() {
    LogOutputRecorder logOutput = new LogOutputRecorder();

    tester = BatchMediumTester.builder()
      .bootstrapProperties(ImmutableMap.of(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_ISSUES))
      .registerPlugin("xoo", new XooPlugin())
      .addRules(new XooRulesDefinition())
      .addQProfile("lang", "name")
      .addActiveRule("xoo", "OneIssuePerLine", null, "One issue per line", "MAJOR", "my/internal/key", "xoo")
      .setPreviousAnalysisDate(new Date())
      .addFileData("test-project", "file1", new FileData("hash", "123456789"))
      .setLogOutput(logOutput)
      .build();

    tester.start();
    tester.syncProject(null);

    assertThat(logOutput.getAsString()).contains("Cache not found, synchronizing data");
  }

  private TaskResult executeTask(TaskBuilder builder) {
    builder.property("sonar.projectKey", "key");
    builder.property("sonar.projectVersion", "1.0");
    builder.property("sonar.projectName", "key");
    builder.property("sonar.projectBaseDir", temp.getRoot().getAbsolutePath());
    builder.property("sonar.sources", temp.getRoot().getAbsolutePath());
    return builder.start();
  }

}
