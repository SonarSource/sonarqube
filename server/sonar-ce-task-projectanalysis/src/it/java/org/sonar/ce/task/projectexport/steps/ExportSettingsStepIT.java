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

import com.sonarsource.governance.projectdump.protobuf.ProjectDump;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.project.ProjectExportMapper;
import org.sonar.db.property.PropertyDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.component.ComponentDto.UUID_PATH_OF_ROOT;

public class ExportSettingsStepIT {

  private static final ComponentDto PROJECT = new ComponentDto()
    .setUuid("project_uuid")
    .setUuidPath(UUID_PATH_OF_ROOT)
    .setBranchUuid("project_uuid")
    .setKey("the_project");
  private static final ComponentDto ANOTHER_PROJECT = new ComponentDto()
    .setUuid("another_project_uuid")
    .setUuidPath(UUID_PATH_OF_ROOT)
    .setBranchUuid("another_project_uuid")
    .setKey("another_project");

  @Rule
  public LogTester logTester = new LogTester();
  @Rule
  public DbTester dbTester = DbTester.createWithExtensionMappers(System2.INSTANCE, ProjectExportMapper.class);
  private final MutableProjectHolder projectHolder = new MutableProjectHolderImpl();
  private final FakeDumpWriter dumpWriter = new FakeDumpWriter();
  private final ExportSettingsStep underTest = new ExportSettingsStep(dbTester.getDbClient(), projectHolder, dumpWriter);
  private ProjectDto project;
  private ProjectDto anotherProject;

  @Before
  public void setUp() {
    logTester.setLevel(Level.DEBUG);
    project = dbTester.components().insertPublicProject(PROJECT).getProjectDto();
    anotherProject = dbTester.components().insertPublicProject(ANOTHER_PROJECT).getProjectDto();
    dbTester.commit();
    projectHolder.setProjectDto(project);
  }

  @Test
  public void export_zero_settings() {
    underTest.execute(new TestComputationStepContext());

    assertThat(dumpWriter.getWrittenMessagesOf(DumpElement.SETTINGS)).isEmpty();
    assertThat(logTester.logs(Level.DEBUG)).contains("0 settings exported");
  }

  @Test
  public void export_only_project_settings() {
    PropertyDto projectProperty1 = newDto("p1", "v1", project);
    PropertyDto projectProperty2 = newDto("p2", "v2", project);
    // the following properties are not exported
    PropertyDto propOnOtherProject = newDto("p3", "v3", anotherProject);
    PropertyDto globalProperty = newDto("p4", "v4", null);
    insertProperties(project.getKey(), project.getName(), projectProperty1, projectProperty2);
    insertProperties(anotherProject.getKey(), anotherProject.getName(), propOnOtherProject);
    insertProperties(null, null, globalProperty);

    underTest.execute(new TestComputationStepContext());

    List<ProjectDump.Setting> exportedProps = dumpWriter.getWrittenMessagesOf(DumpElement.SETTINGS);
    assertThat(exportedProps).hasSize(2);
    assertThat(exportedProps).extracting(ProjectDump.Setting::getKey).containsOnly("p1", "p2");
    assertThat(logTester.logs(Level.DEBUG)).contains("2 settings exported");
  }

  @Test
  public void exclude_properties_specific_to_environment() {
    insertProperties(project.getKey(), project.getName(), newDto("sonar.issues.defaultAssigneeLogin", null, project));

    underTest.execute(new TestComputationStepContext());

    assertThat(dumpWriter.getWrittenMessagesOf(DumpElement.SETTINGS)).isEmpty();
    assertThat(logTester.logs(Level.DEBUG)).contains("0 settings exported");
  }

  @Test
  public void test_exported_fields() {
    PropertyDto dto = newDto("p1", "v1", project);
    insertProperties(project.getKey(), project.getName(), dto);

    underTest.execute(new TestComputationStepContext());

    ProjectDump.Setting exportedProp = dumpWriter.getWrittenMessagesOf(DumpElement.SETTINGS).get(0);
    assertThat(exportedProp.getKey()).isEqualTo(dto.getKey());
    assertThat(exportedProp.getValue()).isEqualTo(dto.getValue());
  }

  @Test
  public void property_can_have_empty_value() {
    insertProperties(project.getKey(), project.getName(), newDto("p1", null, project));

    underTest.execute(new TestComputationStepContext());

    ProjectDump.Setting exportedProp = dumpWriter.getWrittenMessagesOf(DumpElement.SETTINGS).get(0);
    assertThat(exportedProp.getKey()).isEqualTo("p1");
    assertThat(exportedProp.getValue()).isEmpty();
  }

  @Test
  public void throws_ISE_if_error() {
    dumpWriter.failIfMoreThan(1, DumpElement.SETTINGS);
    insertProperties(project.getKey(), project.getName(), newDto("p1", null, project),
      newDto("p2", null, project));

    assertThatThrownBy(() -> underTest.execute(new TestComputationStepContext()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Settings Export failed after processing 1 settings successfully");
  }

  @Test
  public void test_getDescription() {
    assertThat(underTest.getDescription()).isEqualTo("Export settings");
  }

  private static PropertyDto newDto(String key, @Nullable String value, @Nullable EntityDto project) {
    PropertyDto dto = new PropertyDto().setKey(key).setValue(value);
    if (project != null) {
      dto.setEntityUuid(project.getUuid());
    }
    return dto;
  }

  private void insertProperties(@Nullable String entityKey, @Nullable String entityName, PropertyDto... dtos) {
    for (PropertyDto dto : dtos) {
      dbTester.getDbClient().propertiesDao().saveProperty(dbTester.getSession(), dto, null, entityKey, entityName, Qualifiers.VIEW);
    }
    dbTester.commit();
  }
}
