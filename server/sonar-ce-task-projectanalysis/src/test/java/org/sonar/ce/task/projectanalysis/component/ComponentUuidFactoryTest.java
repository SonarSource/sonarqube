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

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentUuidFactoryTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Test
  public void load_uuids_from_existing_components_in_db() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto module = db.components().insertComponent(ComponentTesting
      .newModuleDto(project).setPath("module1"));

    ComponentUuidFactory underTest = new ComponentUuidFactory(db.getDbClient(), db.getSession(), project.getDbKey());

    assertThat(underTest.getOrCreateForKey(project.getDbKey())).isEqualTo(project.uuid());
    assertThat(underTest.getOrCreateForKey(module.getDbKey())).isNotEqualTo(module.uuid());
  }

  @Test
  public void migrate_project_with_modules() {
    ComponentDto project = db.components().insertPrivateProject(dto -> dto.setDbKey("project"));
    ComponentDto module1 = db.components().insertComponent(ComponentTesting.newModuleDto(project)
      .setDbKey("project:module1")
      .setPath("module1_path"));
    ComponentDto module2 = db.components().insertComponent(ComponentTesting.newModuleDto(module1)
      .setDbKey("project:module1:module2")
      .setPath("module2_path"));
    ComponentDto file1 = db.components().insertComponent(ComponentTesting.newFileDto(project)
      .setDbKey("project:file1")
      .setPath("file1_path"));
    ComponentDto file2 = db.components().insertComponent(ComponentTesting.newFileDto(module2)
      .setDbKey("project:module1:module2:file2")
      .setPath("file2_path"));

    assertThat(file2.moduleUuidPath()).isEqualTo("." + project.uuid() + "." + module1.uuid() + "." + module2.uuid() + ".");

    ComponentUuidFactory underTest = new ComponentUuidFactory(db.getDbClient(), db.getSession(), project.getDbKey());

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
      .setEnabled(false)
      .setPath("module1_path"));
    ComponentDto file1 = db.components().insertComponent(ComponentTesting.newFileDto(module1)
      .setDbKey("project:file1")
      .setEnabled(false)
      .setPath("file1_path"));

    ComponentUuidFactory underTest = new ComponentUuidFactory(db.getDbClient(), db.getSession(), project.getDbKey());

    // migrated files
    assertThat(underTest.getOrCreateForKey("project:module1_path/file1_path")).isEqualTo(file1.uuid());
  }

  @Test
  public void migrate_project_having_modules_without_paths() {
    ComponentDto project = db.components().insertPrivateProject(dto -> dto.setDbKey("project"));
    ComponentDto module = db.components().insertComponent(ComponentTesting.newModuleDto(project)
      .setDbKey("project:module")
      .setPath(null));
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(module)
      .setDbKey("project:module:file")
      .setPath("file_path"));

    assertThat(file.moduleUuidPath()).isEqualTo("." + project.uuid() + "." + module.uuid() + ".");

    ComponentUuidFactory underTest = new ComponentUuidFactory(db.getDbClient(), db.getSession(), project.getDbKey());

    // file will have this key since the module has a null path
    assertThat(underTest.getOrCreateForKey("project:file_path")).isEqualTo(file.uuid());

    // migrated module
    // TODO!!
    //assertThat(underTest.getOrCreateForKey("project:module")).isEqualTo(module.uuid());

    // project remains the same
    //assertThat(underTest.getOrCreateForKey(project.getDbKey())).isEqualTo(project.uuid());

    // old keys with modules don't exist
    assertThat(underTest.getOrCreateForKey(module.getDbKey())).isNotEqualTo(module.uuid());
    assertThat(underTest.getOrCreateForKey(file.getDbKey())).isNotEqualTo(file.uuid());
  }

  @Test
  public void generate_uuid_if_it_does_not_exist_in_db() {
    ComponentUuidFactory underTest = new ComponentUuidFactory(db.getDbClient(), db.getSession(), "theProjectKey");

    String generatedKey = underTest.getOrCreateForKey("foo");
    assertThat(generatedKey).isNotEmpty();

    // uuid is kept in memory for further calls with same key
    assertThat(underTest.getOrCreateForKey("foo")).isEqualTo(generatedKey);
  }

}
