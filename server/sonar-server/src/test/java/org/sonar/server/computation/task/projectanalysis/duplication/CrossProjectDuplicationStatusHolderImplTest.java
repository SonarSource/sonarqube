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
package org.sonar.server.computation.task.projectanalysis.duplication;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolderRule;

import static org.assertj.core.api.Assertions.assertThat;

public class CrossProjectDuplicationStatusHolderImplTest {

  private static String BRANCH = "origin/master";

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public LogTester logTester = new LogTester();
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();

  private CrossProjectDuplicationStatusHolderImpl underTest = new CrossProjectDuplicationStatusHolderImpl(analysisMetadataHolder);

  @Test
  public void cross_project_duplication_is_enabled_when_enabled_in_report_and_no_branch() throws Exception {
    analysisMetadataHolder
      .setCrossProjectDuplicationEnabled(true)
      .setBranch(null);
    underTest.start();

    assertThat(underTest.isEnabled()).isTrue();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).containsOnly("Cross project duplication is enabled");
  }

  @Test
  public void cross_project_duplication_is_disabled_when_not_enabled_in_report() throws Exception {
    analysisMetadataHolder
      .setCrossProjectDuplicationEnabled(false)
      .setBranch(null);
    underTest.start();

    assertThat(underTest.isEnabled()).isFalse();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).containsOnly("Cross project duplication is disabled because it's disabled in the analysis report");
  }

  @Test
  public void cross_project_duplication_is_disabled_when_branch_is_used() throws Exception {
    analysisMetadataHolder
      .setCrossProjectDuplicationEnabled(true)
      .setBranch(BRANCH);
    underTest.start();

    assertThat(underTest.isEnabled()).isFalse();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).containsOnly("Cross project duplication is disabled because of a branch is used");
  }

  @Test
  public void cross_project_duplication_is_disabled_when_not_enabled_in_report_and_when_branch_is_used() throws Exception {
    analysisMetadataHolder
      .setCrossProjectDuplicationEnabled(false)
      .setBranch(BRANCH);
    underTest.start();

    assertThat(underTest.isEnabled()).isFalse();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).containsOnly("Cross project duplication is disabled because it's disabled in the analysis report");
  }

  @Test
  public void flag_is_build_in_start() throws Exception {
    analysisMetadataHolder
      .setCrossProjectDuplicationEnabled(true)
      .setBranch(null);
    underTest.start();
    assertThat(underTest.isEnabled()).isTrue();

    // Change the boolean from the report. This can never happen, it's only to validate that the flag is build in the start method
    analysisMetadataHolder.setCrossProjectDuplicationEnabled(false);
    assertThat(underTest.isEnabled()).isTrue();
  }

  @Test
  public void isEnabled_throws_ISE_when_start_have_not_been_called_before() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Flag hasn't been initialized, the start() should have been called before");

    underTest.isEnabled();
  }
}
