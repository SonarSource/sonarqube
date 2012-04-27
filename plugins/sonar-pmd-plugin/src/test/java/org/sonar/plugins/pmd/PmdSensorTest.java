/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.pmd;

import com.google.common.collect.Iterators;
import net.sourceforge.pmd.IRuleViolation;
import net.sourceforge.pmd.Report;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.Violation;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class PmdSensorTest {
  PmdSensor pmdSensor;

  Project project = mock(Project.class, RETURNS_DEEP_STUBS);
  RulesProfile profile = mock(RulesProfile.class, RETURNS_DEEP_STUBS);
  PmdExecutor executor = mock(PmdExecutor.class);
  PmdViolationToRuleViolation pmdViolationToRuleViolation = mock(PmdViolationToRuleViolation.class);
  SensorContext sensorContext = mock(SensorContext.class);
  Violation violation = mock(Violation.class);

  @Before
  public void setUpPmdSensor() {
    pmdSensor = new PmdSensor(profile, executor, pmdViolationToRuleViolation);
  }

  @Test
  public void should_execute_on_project_with_main_files_and_active_rules() {
    boolean shouldExecute = pmdSensor.shouldExecuteOnProject(project);

    assertThat(shouldExecute).isTrue();
  }

  @Test
  public void should_not_execute_on_project_without_files() {
    when(project.getFileSystem().mainFiles(Java.KEY).isEmpty()).thenReturn(true);
    when(project.getFileSystem().testFiles(Java.KEY).isEmpty()).thenReturn(true);

    boolean shouldExecute = pmdSensor.shouldExecuteOnProject(project);

    assertThat(shouldExecute).isFalse();
  }

  @Test
  public void should_not_execute_on_project_without_active_rules() {
    when(profile.getActiveRulesByRepository(PmdConstants.REPOSITORY_KEY).isEmpty()).thenReturn(true);
    when(profile.getActiveRulesByRepository(PmdConstants.TEST_REPOSITORY_KEY).isEmpty()).thenReturn(true);

    boolean shouldExecute = pmdSensor.shouldExecuteOnProject(project);

    assertThat(shouldExecute).isFalse();
  }

  @Test
  public void should_report_violations() {
    IRuleViolation pmdViolation = violation();
    Report report = report(pmdViolation);
    when(executor.execute()).thenReturn(report);
    when(pmdViolationToRuleViolation.toViolation(pmdViolation, sensorContext)).thenReturn(violation);

    pmdSensor.analyse(project, sensorContext);

    verify(sensorContext).saveViolation(violation);
  }

  @Test
  public void shouldnt_report_zero_violation() {
    Report report = report();
    when(executor.execute()).thenReturn(report);

    pmdSensor.analyse(project, sensorContext);

    verifyZeroInteractions(sensorContext);
  }

  @Test
  public void shouldnt_report_invalid_violation() {
    IRuleViolation pmdViolation = violation();
    Report report = report(pmdViolation);
    when(executor.execute()).thenReturn(report);
    when(report.iterator()).thenReturn(Iterators.forArray(pmdViolation));
    when(pmdViolationToRuleViolation.toViolation(pmdViolation, sensorContext)).thenReturn(null);

    pmdSensor.analyse(project, sensorContext);

    verifyZeroInteractions(sensorContext);
  }

  static IRuleViolation violation() {
    return mock(IRuleViolation.class);
  }

  static Report report(IRuleViolation... violations) {
    Report report = mock(Report.class);
    when(report.iterator()).thenReturn(Iterators.forArray(violations));
    return report;
  }
}
