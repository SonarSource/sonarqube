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
package org.sonar.server.computation;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.ZipUtils;
import org.sonar.api.utils.internal.JUnitTempFolder;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.computation.db.AnalysisReportDto.Status;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.activity.Activity;
import org.sonar.server.activity.ActivityService;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.component.db.SnapshotDao;
import org.sonar.server.computation.step.ComputationStep;
import org.sonar.server.computation.step.ComputationSteps;
import org.sonar.server.db.DbClient;
import org.sonar.server.properties.ProjectSettingsFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class ComputationServiceTest {

  private static final long ANY_SNAPSHOT_ID = 54987654231L;
  @ClassRule
  public static DbTester dbTester = new DbTester();
  @Rule
  public JUnitTempFolder tempFolder = new JUnitTempFolder();
  @Rule
  public LogTester logTester = new LogTester();
  ComputationStep projectStep1 = mockStep(Qualifiers.PROJECT);
  ComputationStep projectStep2 = mockStep(Qualifiers.PROJECT);
  ComputationStep viewStep = mockStep(Qualifiers.VIEW);
  ComputationSteps steps = mock(ComputationSteps.class);
  ActivityService activityService = mock(ActivityService.class);
  System2 system = mock(System2.class);
  ComputationService sut;
  ProjectSettingsFactory settingsFactory = mock(ProjectSettingsFactory.class, Mockito.RETURNS_DEEP_STUBS);

  @Before
  public void setUp() throws Exception {
    DbClient dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new ComponentDao(), new SnapshotDao(system));
    sut = new ComputationService(dbClient, steps, activityService, settingsFactory, tempFolder, system);

    // db contains project with key "P1"
    dbTester.prepareDbUnit(getClass(), "shared.xml");
  }

  @Test
  public void process_project() throws Exception {
    logTester.setLevel(LoggerLevel.INFO);

    // view step is not supposed to be executed
    when(steps.orderedSteps()).thenReturn(Arrays.asList(projectStep1, projectStep2, viewStep));
    AnalysisReportDto dto = newDefaultReport();
    File zip = generateZip();

    sut.process(new ReportQueue.Item(dto, zip));

    // report is integrated -> status is set to SUCCESS
    assertThat(dto.getStatus()).isEqualTo(Status.SUCCESS);
    assertThat(dto.getFinishedAt()).isNotNull();

    // one info log at the end
    assertThat(logTester.logs(LoggerLevel.INFO)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.INFO).get(0)).startsWith("Analysis of project P1 (report 1) (done) | time=");

    // execute only the steps supporting the project qualifier
    verify(projectStep1).execute(any(ComputationContext.class));
    verify(projectStep2).execute(any(ComputationContext.class));
    verify(viewStep, never()).execute(any(ComputationContext.class));
    verify(activityService).save(any(Activity.class));
  }

  private AnalysisReportDto newDefaultReport() {
    return AnalysisReportDto.newForTests(1L).setProjectKey("P1").setUuid("U1").setStatus(Status.PENDING);
  }

  @Test
  public void debug_logs() throws Exception {
    logTester.setLevel(LoggerLevel.DEBUG);

    AnalysisReportDto dto = newDefaultReport();
    File zip = generateZip();
    sut.process(new ReportQueue.Item(dto, zip));

    assertThat(logTester.logs(LoggerLevel.DEBUG)).isNotEmpty();
  }

  @Test
  public void fail_if_corrupted_zip() throws Exception {
    AnalysisReportDto dto = newDefaultReport();
    File zip = tempFolder.newFile();
    FileUtils.write(zip, "not a file");

    try {
      sut.process(new ReportQueue.Item(dto, zip));
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).startsWith("Fail to unzip " + zip.getAbsolutePath() + " into ");
      assertThat(dto.getStatus()).isEqualTo(Status.FAILED);
      assertThat(dto.getFinishedAt()).isNotNull();
    }
  }

  @Test
  public void step_error() throws Exception {
    when(steps.orderedSteps()).thenReturn(Arrays.asList(projectStep1));
    doThrow(new IllegalStateException("pb")).when(projectStep1).execute(any(ComputationContext.class));

    AnalysisReportDto dto = newDefaultReport();
    File zip = generateZip();

    try {
      sut.process(new ReportQueue.Item(dto, zip));
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("pb");
      assertThat(dto.getStatus()).isEqualTo(Status.FAILED);
      assertThat(dto.getFinishedAt()).isNotNull();
    }
  }

  @Test
  public void analysis_cancelled_when_snapshot_not_found() throws Exception {
    AnalysisReportDto report = newDefaultReport();
    File zip = generateZip(ANY_SNAPSHOT_ID);
    logTester.setLevel(LoggerLevel.DEBUG);

    sut.process(new ReportQueue.Item(report, zip));

    assertThat(report.getStatus()).isEqualTo(Status.CANCELLED);
    assertThat(logTester.logs()).contains(
      String.format("Processing of report #%s is canceled because it was submitted while another report of the same project was already being processed.", report.getId()));
    assertThat(logTester.logs()).contains(String.format("The snapshot ID #%s provided by the report #%s does not exist anymore.", ANY_SNAPSHOT_ID, report.getId()));
  }

  private ComputationStep mockStep(String... qualifiers) {
    ComputationStep step = mock(ComputationStep.class);
    when(step.supportedProjectQualifiers()).thenReturn(qualifiers);
    when(step.getDescription()).thenReturn(RandomStringUtils.randomAscii(5));
    return step;
  }

  private File generateZip() throws IOException {
    return generateZip(110L);
  }

  private File generateZip(long snapshotId) throws IOException {
    File dir = tempFolder.newDir();
    BatchReportWriter writer = new BatchReportWriter(dir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setProjectKey("PROJECT_KEY")
      .setAnalysisDate(150000000L)
      .setSnapshotId(snapshotId)
      .build());
    File zip = tempFolder.newFile();
    ZipUtils.zipDir(dir, zip);
    return zip;
  }
}
