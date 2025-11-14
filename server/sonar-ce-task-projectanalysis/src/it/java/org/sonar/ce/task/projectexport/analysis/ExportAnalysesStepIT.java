/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.projectexport.analysis;

import com.sonarsource.governance.projectdump.protobuf.ProjectDump;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.event.Level;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.component.ComponentScopes;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectexport.component.ComponentRepositoryImpl;
import org.sonar.ce.task.projectexport.steps.DumpElement;
import org.sonar.ce.task.projectexport.steps.FakeDumpWriter;
import org.sonar.ce.task.projectexport.steps.ProjectHolder;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentDto.UUID_PATH_OF_ROOT;
import static org.sonar.db.component.ComponentDto.UUID_PATH_SEPARATOR;

@RunWith(DataProviderRunner.class)
public class ExportAnalysesStepIT {

  private static final String PROJECT_UUID = "PROJECT_UUID";
  private static final ComponentDto PROJECT = new ComponentDto()
    // no id yet
    .setScope(ComponentScopes.PROJECT)
    .setQualifier(ComponentQualifiers.PROJECT)
    .setKey("the_project")
    .setName("The Project")
    .setDescription("The project description")
    .setEnabled(true)
    .setUuid(PROJECT_UUID)
    .setUuidPath(UUID_PATH_OF_ROOT)
    .setBranchUuid(PROJECT_UUID);

  private static final String DIR_UUID = "DIR_UUID";
  private static final String UUID_PATH = UUID_PATH_OF_ROOT + UUID_PATH_SEPARATOR + DIR_UUID;
  private static final ComponentDto DIR = new ComponentDto()
    // no id yet
    .setScope(ComponentScopes.PROJECT)
    .setQualifier(ComponentQualifiers.DIRECTORY)
    .setKey("the_dir")
    .setName("The Dir")
    .setDescription("description of dir")
    .setEnabled(true)
    .setUuid(DIR_UUID)
    .setUuidPath(UUID_PATH)
    .setBranchUuid(PROJECT_UUID);

  private static final String FILE_UUID = "FILE_UUID";
  private static final ComponentDto FILE = new ComponentDto()
    // no id yet
    .setScope(ComponentScopes.FILE)
    .setQualifier(ComponentQualifiers.FILE)
    .setKey("the_file")
    .setName("The File")
    .setUuid(FILE_UUID)
    .setUuidPath(UUID_PATH + UUID_PATH_SEPARATOR + FILE_UUID)
    .setEnabled(true)
    .setBranchUuid(PROJECT_UUID);

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public LogTester logTester = new LogTester();

  private final ComponentRepositoryImpl componentRepository = new ComponentRepositoryImpl();
  private final FakeDumpWriter dumpWriter = new FakeDumpWriter();
  private final ProjectHolder projectHolder = mock(ProjectHolder.class);
  private final ExportAnalysesStep underTest = new ExportAnalysesStep(dbTester.getDbClient(), projectHolder, componentRepository, dumpWriter);

  @Before
  public void setUp() {
    logTester.setLevel(Level.DEBUG);
    ComponentDto projectDto = dbTester.components().insertPublicProject(PROJECT).getMainBranchComponent();
    componentRepository.register(1, projectDto.uuid(), false);
    dbTester.getDbClient().componentDao().insert(dbTester.getSession(), Set.of(DIR, FILE), true);
    dbTester.commit();
    when(projectHolder.projectDto()).thenReturn(dbTester.components().getProjectDtoByMainBranch(projectDto));
  }

  @Test
  public void getDescription_is_defined() {
    assertThat(underTest.getDescription()).isEqualTo("Export analyses");
  }

  @Test
  @UseDataProvider("versionAndBuildStringCombinations")
  public void export_analyses(@Nullable String version, @Nullable String buildString) {
    SnapshotDto firstAnalysis = newAnalysis("U_1", 1_450_000_000_000L, PROJECT.uuid(), "1.0", false, "1.0.2.3", 1_450_000_000_000L);
    SnapshotDto secondAnalysis = newAnalysis("U_4", 1_460_000_000_000L, PROJECT.uuid(), "1.1", true, "1.1.3.4", 1_460_000_000_000L);
    SnapshotDto thirdAnalysis = newAnalysis("U_7", 1_460_000_000_000L, PROJECT.uuid(), version, true, buildString, 1_470_000_000_000L);
    dbTester.getDbClient().snapshotDao().insert(dbTester.getSession(), firstAnalysis, secondAnalysis, thirdAnalysis);
    dbTester.commit();

    underTest.execute(new TestComputationStepContext());

    assertThat(logTester.logs(Level.DEBUG)).contains("3 analyses exported");
    List<ProjectDump.Analysis> analyses = dumpWriter.getWrittenMessagesOf(DumpElement.ANALYSES);
    assertThat(analyses).hasSize(3);
    assertAnalysis(analyses.get(0), PROJECT, firstAnalysis);
    assertAnalysis(analyses.get(1), PROJECT, secondAnalysis);
    assertAnalysis(analyses.get(2), PROJECT, thirdAnalysis);
  }

  @DataProvider
  public static Object[][] versionAndBuildStringCombinations() {
    String version = secure().nextAlphabetic(7);
    String buildString = secure().nextAlphabetic(12);
    return new Object[][] {
      {null, null},
      {version, null},
      {null, buildString},
      {version, buildString},
      {"", ""},
      {version, ""},
      {"", buildString},
    };
  }

  @Test
  public void export_analyses_by_ordering_by_technical_creation_date() {
    SnapshotDto firstAnalysis = newAnalysis("U_1", 1_450_000_000_000L, PROJECT.uuid(), "1.0", false, "1.0.2.3", 3_000_000_000_000L);
    SnapshotDto secondAnalysis = newAnalysis("U_4", 1_460_000_000_000L, PROJECT.uuid(), "1.1", true, "1.1.3.4", 1_000_000_000_000L);
    SnapshotDto thirdAnalysis = newAnalysis("U_7", 1_460_500_000_000L, PROJECT.uuid(), null, true, null, 2_000_000_000_000L);
    dbTester.getDbClient().snapshotDao().insert(dbTester.getSession(), firstAnalysis, secondAnalysis, thirdAnalysis);
    dbTester.commit();

    underTest.execute(new TestComputationStepContext());

    List<ProjectDump.Analysis> analyses = dumpWriter.getWrittenMessagesOf(DumpElement.ANALYSES);
    assertAnalysis(analyses.get(0), PROJECT, secondAnalysis);
    assertAnalysis(analyses.get(1), PROJECT, thirdAnalysis);
    assertAnalysis(analyses.get(2), PROJECT, firstAnalysis);
  }

  @Test
  public void export_provisioned_projects_without_any_analyses() {
    underTest.execute(new TestComputationStepContext());

    List<ProjectDump.Analysis> analyses = dumpWriter.getWrittenMessagesOf(DumpElement.ANALYSES);
    assertThat(analyses).isEmpty();
    assertThat(logTester.logs(Level.DEBUG)).contains("0 analyses exported");
  }

  @Test
  public void throws_ISE_if_error() {
    SnapshotDto firstAnalysis = newAnalysis("U_1", 1_450_000_000_000L, PROJECT.uuid(), "1.0", false, "1.0.2.3", 1);
    SnapshotDto secondAnalysis = newAnalysis("U_4", 1_460_000_000_000L, PROJECT.uuid(), "1.1", true, "1.1.3.4", 2);
    dbTester.getDbClient().snapshotDao().insert(dbTester.getSession(), firstAnalysis, secondAnalysis);
    dbTester.commit();
    dumpWriter.failIfMoreThan(1, DumpElement.ANALYSES);

    assertThatThrownBy(() -> underTest.execute(new TestComputationStepContext()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Analysis Export failed after processing 1 analyses successfully");
  }

  private static SnapshotDto newAnalysis(String uuid, long date, String componentUuid, @Nullable String version, boolean isLast, @Nullable String buildString, long analysisDate) {
    return new SnapshotDto()
      .setUuid(uuid)
      .setCreatedAt(date)
      .setRootComponentUuid(componentUuid)
      .setProjectVersion(version)
      .setBuildString(buildString)
      .setLast(isLast)
      .setStatus(SnapshotDto.STATUS_PROCESSED)
      .setAnalysisDate(analysisDate);
  }

  private static void assertAnalysis(ProjectDump.Analysis analysis, ComponentDto component, SnapshotDto dto) {
    assertThat(analysis.getUuid()).isEqualTo(dto.getUuid());
    assertThat(analysis.getComponentRef()).isOne();
    assertThat(analysis.getDate()).isEqualTo(dto.getCreatedAt());
    assertThat(analysis.getProjectVersion()).isEqualTo(defaultString(dto.getProjectVersion()));
    assertThat(analysis.getBuildString()).isEqualTo(defaultString(dto.getBuildString()));
  }
}
