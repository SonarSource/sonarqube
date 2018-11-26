/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ComponentUuidFactoryTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private ReportModulesPath reportModulesPath = mock(ReportModulesPath.class);

  @Test
  public void load_uuids_from_existing_components_in_db() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto module = db.components().insertComponent(ComponentTesting
      .newModuleDto(project));
    when(reportModulesPath.get()).thenReturn(Collections.singletonMap(module.getKey(), "module1_path"));
    ComponentUuidFactory underTest = new ComponentUuidFactory(db.getDbClient(), db.getSession(), project.getDbKey(), reportModulesPath);

    assertThat(underTest.getOrCreateForKey(project.getDbKey())).isEqualTo(project.uuid());
    assertThat(underTest.getOrCreateForKey(module.getDbKey())).isNotEqualTo(module.uuid());
  }

  @Test
  public void migrate_project_with_modules() {
    ComponentDto project = db.components().insertPrivateProject(dto -> dto.setDbKey("project"));
    ComponentDto module1 = db.components().insertComponent(ComponentTesting.newModuleDto(project)
      .setDbKey("project:module1"));
    ComponentDto module2 = db.components().insertComponent(ComponentTesting.newModuleDto(module1)
      .setDbKey("project:module1:module2"));
    ComponentDto file1 = db.components().insertComponent(ComponentTesting.newFileDto(project)
      .setDbKey("project:file1")
      .setPath("file1_path"));
    ComponentDto file2 = db.components().insertComponent(ComponentTesting.newFileDto(module2)
      .setDbKey("project:module1:module2:file2")
      .setPath("file2_path"));

    assertThat(file2.moduleUuidPath()).isEqualTo("." + project.uuid() + "." + module1.uuid() + "." + module2.uuid() + ".");
    Map<String, String> modulesRelativePaths = new HashMap<>();
    modulesRelativePaths.put("project:module1", "module1_path");
    modulesRelativePaths.put("project:module1:module2", "module1_path/module2_path");
    when(reportModulesPath.get()).thenReturn(modulesRelativePaths);
    ComponentUuidFactory underTest = new ComponentUuidFactory(db.getDbClient(), db.getSession(), project.getDbKey(), reportModulesPath);

    // migrated files
    assertThat(underTest.getOrCreateForKey("project:file1_path")).isEqualTo(file1.uuid());
    assertThat(underTest.getOrCreateForKey("project:module1_path/module2_path/file2_path")).isEqualTo(file2.uuid());

    // project remains the same
    assertThat(underTest.getOrCreateForKey(project.getDbKey())).isEqualTo(project.uuid());

    // old keys with modules don't exist
    assertThat(underTest.getOrCreateForKey(module1.getDbKey())).isNotEqualTo(module1.uuid());
    assertThat(underTest.getOrCreateForKey(module2.getDbKey())).isNotEqualTo(module2.uuid());
    assertThat(underTest.getOrCreateForKey(file1.getDbKey())).isNotEqualTo(file1.uuid());
    assertThat(underTest.getOrCreateForKey(file2.getDbKey())).isNotEqualTo(file2.uuid());
  }

  @Test
  public void migrate_project_with_disabled_modules() {
    ComponentDto project = db.components().insertPrivateProject(dto -> dto.setDbKey("project"));
    ComponentDto module1 = db.components().insertComponent(ComponentTesting.newModuleDto(project)
      .setDbKey("project:module1")
      .setEnabled(false));
    ComponentDto file1 = db.components().insertComponent(ComponentTesting.newFileDto(module1)
      .setDbKey("project:file1")
      .setEnabled(false)
      .setPath("file1_path"));
    when(reportModulesPath.get()).thenReturn(Collections.singletonMap("project:module1", "module1_path"));

    ComponentUuidFactory underTest = new ComponentUuidFactory(db.getDbClient(), db.getSession(), project.getDbKey(), reportModulesPath);

    // migrated files
    assertThat(underTest.getOrCreateForKey("project:module1_path/file1_path")).isEqualTo(file1.uuid());
  }

  @Test
  public void generate_uuid_if_it_does_not_exist_in_db() {
    when(reportModulesPath.get()).thenReturn(Collections.emptyMap());
    ComponentUuidFactory underTest = new ComponentUuidFactory(db.getDbClient(), db.getSession(), "theProjectKey", reportModulesPath);

    String generatedKey = underTest.getOrCreateForKey("foo");
    assertThat(generatedKey).isNotEmpty();

    // uuid is kept in memory for further calls with same key
    assertThat(underTest.getOrCreateForKey("foo")).isEqualTo(generatedKey);
  }

}
