/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectexport.component;

import com.google.common.collect.ImmutableSet;
import com.sonarsource.governance.projectdump.protobuf.ProjectDump;
import java.util.Date;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectexport.steps.DumpElement;
import org.sonar.ce.task.projectexport.steps.FakeDumpWriter;
import org.sonar.ce.task.projectexport.steps.ProjectHolder;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentDto.UUID_PATH_OF_ROOT;
import static org.sonar.db.component.ComponentDto.UUID_PATH_SEPARATOR;

public class ExportComponentsStepIT {

  private static final String PROJECT_UUID = "PROJECT_UUID";
  private static final ComponentDto PROJECT = new ComponentDto()
    // no id yet
    .setScope(Scopes.PROJECT)
    .setQualifier(Qualifiers.PROJECT)
    .setKey("the_project")
    .setName("The Project")
    .setDescription("The project description")
    .setEnabled(true)
    .setUuid(PROJECT_UUID)
    .setUuidPath(UUID_PATH_OF_ROOT)
    .setCreatedAt(new Date(1596749115856L))
    .setBranchUuid(PROJECT_UUID);

  private static final String FILE_UUID = "FILE_UUID";
  private static final String FILE_UUID_PATH = PROJECT_UUID + FILE_UUID + UUID_PATH_SEPARATOR;
  private static final ComponentDto FILE = new ComponentDto()
    // no id yet
    .setScope(Scopes.FILE)
    .setQualifier(Qualifiers.FILE)
    .setKey("the_file")
    .setName("The File")
    .setUuid(FILE_UUID)
    .setUuidPath(FILE_UUID_PATH)
    .setEnabled(true)
    .setCreatedAt(new Date(1596749148406L))
    .setBranchUuid(PROJECT_UUID);

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public LogTester logTester = new LogTester();

  private final FakeDumpWriter dumpWriter = new FakeDumpWriter();
  private final ProjectHolder projectHolder = mock(ProjectHolder.class);
  private final MutableComponentRepository componentRepository = new ComponentRepositoryImpl();
  private final ExportComponentsStep underTest = new ExportComponentsStep(dbTester.getDbClient(), projectHolder, componentRepository, dumpWriter);

  @Before
  public void before() {
    logTester.setLevel(Level.DEBUG);
  }

  @After
  public void tearDown() {
    dbTester.getSession().close();
  }

  @Test
  public void export_components_including_project() {
    dbTester.components().insertPublicProject(PROJECT).getMainBranchComponent();
    dbTester.getDbClient().componentDao().insert(dbTester.getSession(), FILE, true);
    dbTester.commit();
    when(projectHolder.projectDto()).thenReturn(dbTester.components().getProjectDtoByMainBranch(PROJECT));

    underTest.execute(new TestComputationStepContext());

    assertThat(logTester.logs(Level.DEBUG)).contains("2 components exported");
    List<ProjectDump.Component> components = dumpWriter.getWrittenMessagesOf(DumpElement.COMPONENTS);
    assertThat(components).extracting(ProjectDump.Component::getQualifier, ProjectDump.Component::getUuid, ProjectDump.Component::getUuidPath)
      .containsExactlyInAnyOrder(
        tuple(Qualifiers.FILE, FILE_UUID, FILE_UUID_PATH),
        tuple(Qualifiers.PROJECT, PROJECT_UUID, UUID_PATH_OF_ROOT));
  }

  @Test
  public void execute_register_all_components_uuids_as_their_id_in_ComponentRepository() {
    dbTester.components().insertPublicProject(PROJECT).getMainBranchComponent();
    dbTester.getDbClient().componentDao().insert(dbTester.getSession(), FILE, true);
    dbTester.commit();
    when(projectHolder.projectDto()).thenReturn(dbTester.components().getProjectDtoByMainBranch(PROJECT));

    underTest.execute(new TestComputationStepContext());

    assertThat(ImmutableSet.of(
      componentRepository.getRef(PROJECT.uuid()),
      componentRepository.getRef(FILE.uuid()))).containsExactlyInAnyOrder(1L, 2L);
  }

  @Test
  public void throws_ISE_if_error() {
    dbTester.components().insertPublicProject(PROJECT).getMainBranchComponent();
    dbTester.getDbClient().componentDao().insert(dbTester.getSession(), FILE, true);
    dbTester.commit();
    when(projectHolder.projectDto()).thenReturn(dbTester.components().getProjectDtoByMainBranch(PROJECT));
    dumpWriter.failIfMoreThan(1, DumpElement.COMPONENTS);

    assertThatThrownBy(() -> underTest.execute(new TestComputationStepContext()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Component Export failed after processing 1 components successfully");
  }

  @Test
  public void getDescription_is_defined() {
    assertThat(underTest.getDescription()).isEqualTo("Export components");
  }

}
