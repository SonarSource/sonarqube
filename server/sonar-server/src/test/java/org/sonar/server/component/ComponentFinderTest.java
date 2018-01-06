/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.component;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.exceptions.NotFoundException;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.server.component.ComponentFinder.ParamNames.ID_AND_KEY;

public class ComponentFinderTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final DbSession dbSession = db.getSession();
  private ComponentFinder underTest = TestComponentFinder.from(db);

  @Test
  public void fail_when_the_uuid_and_key_are_null() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Either 'id' or 'key' must be provided");

    underTest.getByUuidOrKey(dbSession, null, null, ID_AND_KEY);
  }

  @Test
  public void fail_when_the_uuid_and_key_are_provided() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Either 'id' or 'key' must be provided");

    underTest.getByUuidOrKey(dbSession, "project-uuid", "project-key", ID_AND_KEY);
  }

  @Test
  public void fail_when_the_uuid_is_empty() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'id' parameter must not be empty");

    underTest.getByUuidOrKey(dbSession, "", null, ID_AND_KEY);
  }

  @Test
  public void fail_when_the_key_is_empty() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'key' parameter must not be empty");

    underTest.getByUuidOrKey(dbSession, null, "", ID_AND_KEY);
  }

  @Test
  public void fail_when_component_uuid_not_found() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component id 'project-uuid' not found");

    underTest.getByUuidOrKey(dbSession, "project-uuid", null, ID_AND_KEY);
  }

  @Test
  public void fail_when_component_key_not_found() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component key 'project-key' not found");

    underTest.getByUuidOrKey(dbSession, null, "project-key", ID_AND_KEY);
  }

  @Test
  public void fail_to_getByUuidOrKey_when_using_branch_uuid() {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Component id '%s' not found", branch.uuid()));

    underTest.getByUuidOrKey(dbSession, branch.uuid(), null, ID_AND_KEY);
  }

  @Test
  public void fail_to_getByUuidOrKey_when_using_branch_key() {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Component key '%s' not found", branch.getDbKey()));

    underTest.getByUuidOrKey(dbSession, null, branch.getDbKey(), ID_AND_KEY);
  }

  @Test
  public void fail_when_component_uuid_is_removed() {
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto(db.getDefaultOrganization()));
    db.components().insertComponent(newFileDto(project, null, "file-uuid").setEnabled(false));

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component id 'file-uuid' not found");

    underTest.getByUuid(dbSession, "file-uuid");
  }

  @Test
  public void fail_to_getByUuid_on_branch() {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Component id '%s' not found", branch.uuid()));

    underTest.getByUuid(dbSession, branch.uuid());
  }

  @Test
  public void fail_when_component_key_is_removed() {
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto(db.getDefaultOrganization()));
    db.components().insertComponent(newFileDto(project).setDbKey("file-key").setEnabled(false));

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component key 'file-key' not found");

    underTest.getByKey(dbSession, "file-key");
  }

  @Test
  public void fail_getByKey_on_branch() {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Component key '%s' not found", branch.getDbKey()));

    underTest.getByKey(dbSession, branch.getDbKey());
  }

  @Test
  public void get_component_by_uuid() {
    db.components().insertComponent(newPrivateProjectDto(db.organizations().insert(), "project-uuid"));

    ComponentDto component = underTest.getByUuidOrKey(dbSession, "project-uuid", null, ID_AND_KEY);

    assertThat(component.uuid()).isEqualTo("project-uuid");
  }

  @Test
  public void get_component_by_key() {
    db.components().insertComponent(newPrivateProjectDto(db.getDefaultOrganization()).setDbKey("project-key"));

    ComponentDto component = underTest.getByUuidOrKey(dbSession, null, "project-key", ID_AND_KEY);

    assertThat(component.getDbKey()).isEqualTo("project-key");
  }

  @Test
  public void get_by_key_and_branch() {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("my_branch"));
    ComponentDto module = db.components().insertComponent(newModuleDto(branch));
    ComponentDto directory = db.components().insertComponent(newDirectory(module, "scr"));
    ComponentDto file = db.components().insertComponent(newFileDto(module));

    assertThat(underTest.getByKeyAndBranch(dbSession, project.getKey(), "my_branch").uuid()).isEqualTo(branch.uuid());
    assertThat(underTest.getByKeyAndBranch(dbSession, module.getKey(), "my_branch").uuid()).isEqualTo(module.uuid());
    assertThat(underTest.getByKeyAndBranch(dbSession, file.getKey(), "my_branch").uuid()).isEqualTo(file.uuid());
    assertThat(underTest.getByKeyAndBranch(dbSession, directory.getKey(), "my_branch").uuid()).isEqualTo(directory.uuid());
  }

  @Test
  public void get_by_key_and_branch_accept_main_branch() {
    ComponentDto project = db.components().insertMainBranch();

    assertThat(underTest.getByKeyAndBranch(dbSession, project.getKey(), "master").uuid()).isEqualTo(project.uuid());
  }

  @Test
  public void fail_to_get_by_key_and_branch_when_branch_does_not_exist() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("my_branch"));
    ComponentDto file = db.components().insertComponent(newFileDto(branch));

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Component '%s' on branch 'other_branch' not found", file.getKey()));

    underTest.getByKeyAndBranch(dbSession, file.getKey(), "other_branch");
  }
}
