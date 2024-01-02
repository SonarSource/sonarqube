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
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.ce.task.projectexport.component.ComponentRepositoryImpl;
import org.sonar.ce.task.projectexport.component.MutableComponentRepository;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.project.ProjectExportMapper;
import org.sonar.db.property.PropertyDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.component.ComponentDto.UUID_PATH_OF_ROOT;

public class ExportSettingsStepTest {

  private static final ComponentDto GLOBAL = null;
  private static final ComponentDto PROJECT = new ComponentDto()
    .setUuid("project_uuid")
    .setUuidPath(UUID_PATH_OF_ROOT)
    .setRootUuid("project_uuid")
    .setBranchUuid("project_uuid")
    .setKey("the_project");
  private static final ComponentDto ANOTHER_PROJECT = new ComponentDto()
    .setUuid("another_project_uuid")
    .setUuidPath(UUID_PATH_OF_ROOT)
    .setRootUuid("another_project_uuid")
    .setBranchUuid("another_project_uuid")
    .setKey("another_project");

  @Rule
  public LogTester logTester = new LogTester();
  @Rule
  public DbTester dbTester = DbTester.createWithExtensionMappers(System2.INSTANCE, ProjectExportMapper.class);
  private MutableComponentRepository componentRepository = new ComponentRepositoryImpl();
  private MutableProjectHolder projectHolder = new MutableProjectHolderImpl();
  private FakeDumpWriter dumpWriter = new FakeDumpWriter();
  private ExportSettingsStep underTest = new ExportSettingsStep(dbTester.getDbClient(), projectHolder, componentRepository, dumpWriter);

  @Before
  public void setUp() {
    dbTester.components().insertPublicProject(PROJECT);
    dbTester.components().insertPublicProject(ANOTHER_PROJECT);
    dbTester.commit();
    projectHolder.setProjectDto(dbTester.components().getProjectDto(PROJECT));
    componentRepository.register(1, PROJECT.uuid(), false);
  }

  @Test
  public void export_zero_settings() {
    underTest.execute(new TestComputationStepContext());

    assertThat(dumpWriter.getWrittenMessagesOf(DumpElement.SETTINGS)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("0 settings exported");
  }

  @Test
  public void export_only_project_settings() {
    PropertyDto projectProperty1 = newDto("p1", "v1", PROJECT);
    PropertyDto projectProperty2 = newDto("p2", "v2", PROJECT);
    // the following properties are not exported
    PropertyDto propOnOtherProject = newDto("p3", "v3", ANOTHER_PROJECT);
    PropertyDto globalProperty = newDto("p4", "v4", GLOBAL);
    insertProperties(PROJECT.getKey(), PROJECT.name(), projectProperty1, projectProperty2);
    insertProperties(ANOTHER_PROJECT.getKey(), ANOTHER_PROJECT.name(), propOnOtherProject);
    insertProperties(null, null, globalProperty);

    underTest.execute(new TestComputationStepContext());

    List<ProjectDump.Setting> exportedProps = dumpWriter.getWrittenMessagesOf(DumpElement.SETTINGS);
    assertThat(exportedProps).hasSize(2);
    assertThat(exportedProps).extracting(ProjectDump.Setting::getKey).containsOnly("p1", "p2");
    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("2 settings exported");
  }

  @Test
  public void exclude_properties_specific_to_environment() {
    insertProperties(PROJECT.getKey(), PROJECT.name(), newDto("sonar.issues.defaultAssigneeLogin", null, PROJECT));

    underTest.execute(new TestComputationStepContext());

    assertThat(dumpWriter.getWrittenMessagesOf(DumpElement.SETTINGS)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("0 settings exported");
  }

  @Test
  public void test_exported_fields() {
    PropertyDto dto = newDto("p1", "v1", PROJECT);
    insertProperties(PROJECT.getKey(), PROJECT.name(), dto);

    underTest.execute(new TestComputationStepContext());

    ProjectDump.Setting exportedProp = dumpWriter.getWrittenMessagesOf(DumpElement.SETTINGS).get(0);
    assertThat(exportedProp.getKey()).isEqualTo(dto.getKey());
    assertThat(exportedProp.getValue()).isEqualTo(dto.getValue());
  }

  @Test
  public void property_can_have_empty_value() {
    insertProperties(PROJECT.getKey(), PROJECT.name(), newDto("p1", null, PROJECT));

    underTest.execute(new TestComputationStepContext());

    ProjectDump.Setting exportedProp = dumpWriter.getWrittenMessagesOf(DumpElement.SETTINGS).get(0);
    assertThat(exportedProp.getKey()).isEqualTo("p1");
    assertThat(exportedProp.getValue()).isEmpty();
  }

  @Test
  public void throws_ISE_if_error() {
    dumpWriter.failIfMoreThan(1, DumpElement.SETTINGS);
    insertProperties(PROJECT.getKey(), PROJECT.name(), newDto("p1", null, PROJECT),
      newDto("p2", null, PROJECT));

    assertThatThrownBy(() -> underTest.execute(new TestComputationStepContext()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Settings Export failed after processing 1 settings successfully");
  }

  @Test
  public void test_getDescription() {
    assertThat(underTest.getDescription()).isEqualTo("Export settings");
  }

  private static PropertyDto newDto(String key, @Nullable String value, @Nullable ComponentDto project) {
    PropertyDto dto = new PropertyDto().setKey(key).setValue(value);
    if (project != null) {
      dto.setComponentUuid(project.uuid());
    }
    return dto;
  }

  private void insertProperties(@Nullable String componentKey, @Nullable String componentName, PropertyDto... dtos) {
    for (PropertyDto dto : dtos) {
      dbTester.getDbClient().propertiesDao().saveProperty(dbTester.getSession(), dto, null, componentKey, componentName, Qualifiers.VIEW);
    }
    dbTester.commit();
  }
}
