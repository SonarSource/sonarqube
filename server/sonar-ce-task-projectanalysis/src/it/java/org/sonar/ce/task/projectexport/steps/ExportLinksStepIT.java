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
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbTester;
import org.sonar.db.component.ProjectData;
import org.sonar.db.component.ProjectLinkDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.project.ProjectExportMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExportLinksStepIT {

  private static final String PROJECT_UUID = "project_uuid";

  private ProjectDto projectDto;

  @Rule
  public DbTester db = DbTester.createWithExtensionMappers(System2.INSTANCE, ProjectExportMapper.class);

  @Rule
  public LogTester logTester = new LogTester();

  private final FakeDumpWriter dumpWriter = new FakeDumpWriter();
  private final ProjectHolder projectHolder = mock(ProjectHolder.class);
  private final ExportLinksStep underTest = new ExportLinksStep(db.getDbClient(), projectHolder, dumpWriter);

  @Before
  public void setUp() {
    logTester.setLevel(Level.DEBUG);
    ProjectData project = db.components().insertPublicProject(PROJECT_UUID);
    this.projectDto = project.getProjectDto();
    when(projectHolder.projectDto()).thenReturn(projectDto);
  }

  @Test
  public void export_zero_links() {
    underTest.execute(new TestComputationStepContext());

    assertThat(logTester.logs(Level.DEBUG)).contains("0 links exported");
    assertThat(dumpWriter.getWrittenMessagesOf(DumpElement.LINKS)).isEmpty();
  }

  @Test
  public void export_links() {
    ProjectLinkDto link1 = db.projectLinks().insertCustomLink(projectDto);
    ProjectLinkDto link2 = db.projectLinks().insertProvidedLink(projectDto);
    db.projectLinks().insertCustomLink(db.components().insertPrivateProject().getProjectDto());

    underTest.execute(new TestComputationStepContext());

    assertThat(logTester.logs(Level.DEBUG)).contains("2 links exported");
    assertThat(dumpWriter.getWrittenMessagesOf(DumpElement.LINKS))
      .extracting(Link::getUuid, Link::getName, Link::getType, Link::getHref)
      .containsExactlyInAnyOrder(
        tuple(link1.getUuid(), link1.getName(), link1.getType(), link1.getHref()),
        tuple(link2.getUuid(), "", link2.getType(), link2.getHref()));
  }

  @Test
  public void throws_ISE_if_error() {
    db.projectLinks().insertCustomLink(projectDto);
    db.projectLinks().insertProvidedLink(projectDto);
    db.projectLinks().insertProvidedLink(projectDto);
    db.projectLinks().insertCustomLink(db.components().insertPrivateProject().getProjectDto());

    dumpWriter.failIfMoreThan(2, DumpElement.LINKS);

    assertThatThrownBy(() -> underTest.execute(new TestComputationStepContext()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Link export failed after processing 2 link(s) successfully");
  }

  @Test
  public void test_all_fields() {
    ProjectLinkDto link = db.projectLinks().insertCustomLink(projectDto, l -> l.setName("name").setHref("href").setType("type"));

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
