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
package org.sonar.ce.task.projectexport.steps;

import com.sonarsource.governance.projectdump.protobuf.ProjectDump;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.component.ComponentScopes;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectexport.component.ComponentRepositoryImpl;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.event.EventDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.component.ComponentDto.UUID_PATH_OF_ROOT;
import static org.sonar.db.component.SnapshotDto.STATUS_PROCESSED;

public class ExportEventsStepIT {

  private static final long NOW = 1_450_000_000_000L;
  private static final long IN_THE_PAST = 1_440_000_000_000L;

  private static final String PROJECT_UUID = "project_uuid";
  private static final ComponentDto PROJECT = new ComponentDto()
    .setUuid(PROJECT_UUID)
    .setUuidPath(UUID_PATH_OF_ROOT)
    .setBranchUuid(PROJECT_UUID)
    .setScope(ComponentScopes.PROJECT)
    .setQualifier(ComponentQualifiers.PROJECT)
    .setKey("the_project")
    .setEnabled(true);


  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public LogTester logTester = new LogTester();

  private FakeDumpWriter dumpWriter = new FakeDumpWriter();
  private MutableProjectHolder projectHolder = new MutableProjectHolderImpl();
  private ComponentRepositoryImpl componentRepository = new ComponentRepositoryImpl();
  private ExportEventsStep underTest = new ExportEventsStep(dbTester.getDbClient(), projectHolder, componentRepository, dumpWriter);

  @Before
  public void setUp() {
    logTester.setLevel(Level.DEBUG);
    ComponentDto projectDto = dbTester.components().insertPublicProject(PROJECT).getMainBranchComponent();
    componentRepository.register(1, projectDto.uuid(), false);
    projectHolder.setProjectDto(dbTester.components().getProjectDtoByMainBranch(projectDto));
  }

  @Test
  public void export_zero_events() {
    underTest.execute(new TestComputationStepContext());

    assertThat(logTester.logs(Level.DEBUG)).contains("0 events exported");
    List<ProjectDump.Event> events = dumpWriter.getWrittenMessagesOf(DumpElement.EVENTS);
    assertThat(events).isEmpty();
  }

  @Test
  public void export_events() {
    SnapshotDto snapshot = insertSnapshot();
    insertEvent(snapshot, "E1", "one");
    insertEvent(snapshot, "E2", "two");

    underTest.execute(new TestComputationStepContext());

    assertThat(logTester.logs(Level.DEBUG)).contains("2 events exported");
    List<ProjectDump.Event> events = dumpWriter.getWrittenMessagesOf(DumpElement.EVENTS);
    assertThat(events).hasSize(2);
    assertThat(events).extracting(ProjectDump.Event::getUuid).containsOnly("E1", "E2");
  }

  @Test
  public void throws_ISE_if_error() {
    SnapshotDto snapshot = insertSnapshot();
    insertEvent(snapshot, "E1", "one");
    insertEvent(snapshot, "E2", "two");
    dumpWriter.failIfMoreThan(1, DumpElement.EVENTS);

    assertThatThrownBy(() -> underTest.execute(new TestComputationStepContext()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Event Export failed after processing 1 events successfully");
  }

  @Test
  public void export_all_fields() {
    SnapshotDto snapshot = insertSnapshot();
    dbTester.getDbClient().eventDao().insert(dbTester.getSession(), new EventDto()
      .setUuid("E1")
      .setAnalysisUuid(snapshot.getUuid())
      .setComponentUuid(snapshot.getRootComponentUuid())
      .setDate(IN_THE_PAST)
      .setCreatedAt(NOW)
      .setData("data")
      .setName("name")
      .setCategory("categ")
      .setDescription("desc"));
    dbTester.commit();

    underTest.execute(new TestComputationStepContext());

    ProjectDump.Event event = dumpWriter.getWrittenMessagesOf(DumpElement.EVENTS).get(0);
    assertThat(event.getUuid()).isEqualTo("E1");
    assertThat(event.getName()).isEqualTo("name");
    assertThat(event.getData()).isEqualTo("data");
    assertThat(event.getCategory()).isEqualTo("categ");
    assertThat(event.getDescription()).isEqualTo("desc");
    assertThat(event.getDate()).isEqualTo(IN_THE_PAST);
    assertThat(event.getAnalysisUuid()).isEqualTo(snapshot.getUuid());
    assertThat(event.getComponentRef()).isOne();
  }

  @Test
  public void getDescription_is_defined() {
    assertThat(underTest.getDescription()).isEqualTo("Export events");
  }

  private void insertEvent(SnapshotDto snapshot, String uuid, String name) {
    dbTester.getDbClient().eventDao().insert(dbTester.getSession(), new EventDto()
      .setUuid(uuid)
      .setAnalysisUuid(snapshot.getUuid())
      .setComponentUuid(snapshot.getRootComponentUuid())
      .setDate(IN_THE_PAST)
      .setCreatedAt(NOW)
      .setName(name));
    dbTester.commit();
  }

  private SnapshotDto insertSnapshot() {
    SnapshotDto snapshot = new SnapshotDto()
      .setUuid("U1")
      .setRootComponentUuid(PROJECT.uuid())
      .setStatus(STATUS_PROCESSED)
      .setLast(false);
    dbTester.getDbClient().snapshotDao().insert(dbTester.getSession(), snapshot);
    dbTester.commit();
    return snapshot;
  }
}
