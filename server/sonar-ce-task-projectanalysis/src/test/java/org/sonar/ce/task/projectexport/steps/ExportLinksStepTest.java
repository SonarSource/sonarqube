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
package org.sonar.ce.task.projectexport.steps;

import com.sonarsource.governance.projectdump.protobuf.ProjectDump.Link;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.ce.task.projectexport.component.ComponentRepository;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectLinkDto;
import org.sonar.db.project.ProjectExportMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentDto.UUID_PATH_OF_ROOT;

public class ExportLinksStepTest {

  private static final String PROJECT_UUID = "project_uuid";
  private static final ComponentDto PROJECT = new ComponentDto()
    // no id yet
    .setScope(Scopes.PROJECT)
    .setQualifier(Qualifiers.PROJECT)
    .setKey("the_project")
    .setName("The Project")
    .setDescription("The project description")
    .setEnabled(true)
    .setUuid(PROJECT_UUID)
    .setRootUuid(PROJECT_UUID)
    .setUuidPath(UUID_PATH_OF_ROOT)
    .setModuleUuid(null)
    .setModuleUuidPath("." + PROJECT_UUID + ".")
    .setBranchUuid(PROJECT_UUID);

  @Rule
  public DbTester db = DbTester.createWithExtensionMappers(System2.INSTANCE, ProjectExportMapper.class);


  @Rule
  public LogTester logTester = new LogTester();

  private final FakeDumpWriter dumpWriter = new FakeDumpWriter();
  private final ComponentRepository componentRepository = mock(ComponentRepository.class);
  private final ProjectHolder projectHolder = mock(ProjectHolder.class);
  private final ExportLinksStep underTest = new ExportLinksStep(db.getDbClient(), componentRepository, projectHolder, dumpWriter);

  @Before
  public void setUp() {
    ComponentDto project = db.components().insertPublicProject(PROJECT);
    when(projectHolder.projectDto()).thenReturn(db.components().getProjectDto(project));
    when(componentRepository.getRef(PROJECT_UUID)).thenReturn(1L);
  }

  @Test
  public void export_zero_links() {
    underTest.execute(new TestComputationStepContext());

    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("0 links exported");
    assertThat(dumpWriter.getWrittenMessagesOf(DumpElement.LINKS)).isEmpty();
  }

  @Test
  public void export_links() {
    ProjectLinkDto link1 = db.componentLinks().insertCustomLink(PROJECT);
    ProjectLinkDto link2 = db.componentLinks().insertProvidedLink(PROJECT);
    db.componentLinks().insertCustomLink(db.components().insertPrivateProject());

    underTest.execute(new TestComputationStepContext());

    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("2 links exported");
    assertThat(dumpWriter.getWrittenMessagesOf(DumpElement.LINKS))
      .extracting(Link::getUuid, Link::getName, Link::getType, Link::getHref)
      .containsExactlyInAnyOrder(
        tuple(link1.getUuid(), link1.getName(), link1.getType(), link1.getHref()),
        tuple(link2.getUuid(), "", link2.getType(), link2.getHref()));
  }

  @Test
  public void throws_ISE_if_error() {
    db.componentLinks().insertCustomLink(PROJECT);
    db.componentLinks().insertProvidedLink(PROJECT);
    db.componentLinks().insertProvidedLink(PROJECT);
    db.componentLinks().insertCustomLink(db.components().insertPrivateProject());

    dumpWriter.failIfMoreThan(2, DumpElement.LINKS);

    assertThatThrownBy(() -> underTest.execute(new TestComputationStepContext()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Link export failed after processing 2 link(s) successfully");
  }

  @Test
  public void test_all_fields() {
    ProjectLinkDto link = db.componentLinks().insertCustomLink(PROJECT, l -> l.setName("name").setHref("href").setType("type"));

    underTest.execute(new TestComputationStepContext());

    Link reloaded = dumpWriter.getWrittenMessagesOf(DumpElement.LINKS).get(0);
    assertThat(reloaded.getUuid()).isEqualTo(link.getUuid());
    assertThat(reloaded.getName()).isEqualTo(link.getName());
    assertThat(reloaded.getHref()).isEqualTo(link.getHref());
    assertThat(reloaded.getType()).isEqualTo(link.getType());
  }

  @Test
  public void getDescription_is_defined() {
    assertThat(underTest.getDescription()).isEqualTo("Export links");
  }

}
