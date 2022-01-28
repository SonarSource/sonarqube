/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.exceptions.NotFoundException;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.component.BranchType.PULL_REQUEST;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.server.component.ComponentFinder.ParamNames.ID_AND_KEY;

public class ComponentFinderTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final DbSession dbSession = db.getSession();
  private final ComponentFinder underTest = TestComponentFinder.from(db);

  @Test
  public void fail_when_the_uuid_and_key_are_null() {
    assertThatThrownBy(() -> underTest.getByUuidOrKey(dbSession, null, null, ID_AND_KEY))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Either 'id' or 'key' must be provided");
  }

  @Test
  public void fail_when_the_uuid_and_key_are_provided() {
    assertThatThrownBy(() -> underTest.getByUuidOrKey(dbSession, "project-uuid", "project-key", ID_AND_KEY))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Either 'id' or 'key' must be provided");
  }

  @Test
  public void fail_when_the_uuid_is_empty() {
    assertThatThrownBy(() -> underTest.getByUuidOrKey(dbSession, "", null, ID_AND_KEY))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'id' parameter must not be empty");
  }

  @Test
  public void fail_when_the_key_is_empty() {
    assertThatThrownBy(() -> underTest.getByUuidOrKey(dbSession, null, "", ID_AND_KEY))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'key' parameter must not be empty");
  }

  @Test
  public void fail_when_component_uuid_not_found() {
    assertThatThrownBy(() -> underTest.getByUuidOrKey(dbSession, "project-uuid", null, ID_AND_KEY))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Component id 'project-uuid' not found");
  }

  @Test
  public void fail_when_component_key_not_found() {
    assertThatThrownBy(() -> underTest.getByUuidOrKey(dbSession, null, "project-key", ID_AND_KEY))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Component key 'project-key' not found");
  }

  @Test
  public void fail_to_getByUuidOrKey_when_using_branch_uuid() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto branch = db.components().insertProjectBranch(project);

    String branchUuid = branch.uuid();
    assertThatThrownBy(() -> underTest.getByUuidOrKey(dbSession, branchUuid, null, ID_AND_KEY))
      .isInstanceOf(NotFoundException.class)
      .hasMessage(format("Component id '%s' not found", branchUuid));
  }

  @Test
  public void fail_to_getByUuidOrKey_when_using_branch_key() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto branch = db.components().insertProjectBranch(project);

    String branchDbKey = branch.getDbKey();
    assertThatThrownBy(() -> underTest.getByUuidOrKey(dbSession, null, branchDbKey, ID_AND_KEY))
      .isInstanceOf(NotFoundException.class)
      .hasMessage(format("Component key '%s' not found", branch.getDbKey()));
  }

  @Test
  public void fail_when_component_uuid_is_removed() {
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto());
    db.components().insertComponent(newFileDto(project, null, "file-uuid").setEnabled(false));

    assertThatThrownBy(() -> underTest.getByUuid(dbSession, "file-uuid"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Component id 'file-uuid' not found");
  }

  @Test
  public void fail_to_getByUuid_on_branch() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto branch = db.components().insertProjectBranch(project);

    String branchUuid = branch.uuid();
    assertThatThrownBy(() -> underTest.getByUuid(dbSession, branchUuid))
      .isInstanceOf(NotFoundException.class)
      .hasMessage(format("Component id '%s' not found", branchUuid));
  }

  @Test
  public void fail_when_component_key_is_removed() {
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto());
    db.components().insertComponent(newFileDto(project).setDbKey("file-key").setEnabled(false));

    assertThatThrownBy(() -> underTest.getByKey(dbSession, "file-key"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Component key 'file-key' not found");
  }

  @Test
  public void fail_getByKey_on_branch() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto branch = db.components().insertProjectBranch(project);

    String branchDbKey = branch.getDbKey();
    assertThatThrownBy(() -> underTest.getByKey(dbSession, branchDbKey))
      .isInstanceOf(NotFoundException.class)
      .hasMessage(format("Component key '%s' not found", branchDbKey));
  }

  @Test
  public void get_component_by_uuid() {
    db.components().insertComponent(newPrivateProjectDto("project-uuid"));

    ComponentDto component = underTest.getByUuidOrKey(dbSession, "project-uuid", null, ID_AND_KEY);

    assertThat(component.uuid()).isEqualTo("project-uuid");
  }

  @Test
  public void get_component_by_key() {
    db.components().insertComponent(newPrivateProjectDto().setDbKey("project-key"));

    ComponentDto component = underTest.getByUuidOrKey(dbSession, null, "project-key", ID_AND_KEY);

    assertThat(component.getDbKey()).isEqualTo("project-key");
  }

  @Test
  public void get_by_key_and_branch() {
    ComponentDto project = db.components().insertPublicProject();
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
  public void get_by_key_and_pull_request() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("pr-123").setBranchType(PULL_REQUEST).setMergeBranchUuid(project.uuid()));
    ComponentDto module = db.components().insertComponent(newModuleDto(branch));
    ComponentDto directory = db.components().insertComponent(newDirectory(module, "scr"));
    ComponentDto file = db.components().insertComponent(newFileDto(module));

    assertThat(underTest.getByKeyAndOptionalBranchOrPullRequest(dbSession, project.getKey(), null, "pr-123").uuid()).isEqualTo(branch.uuid());
    assertThat(underTest.getByKeyAndOptionalBranchOrPullRequest(dbSession, module.getKey(), null, "pr-123").uuid()).isEqualTo(module.uuid());
    assertThat(underTest.getByKeyAndOptionalBranchOrPullRequest(dbSession, file.getKey(), null, "pr-123").uuid()).isEqualTo(file.uuid());
    assertThat(underTest.getByKeyAndOptionalBranchOrPullRequest(dbSession, directory.getKey(), null, "pr-123").uuid()).isEqualTo(directory.uuid());
  }

  @Test
  public void fail_when_pull_request_branch_provided() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto pullRequest = db.components().insertProjectBranch(project, b -> b.setKey("pr-123").setBranchType(PULL_REQUEST));

    String projectKey = project.getKey();
    assertThatThrownBy(() -> underTest.getByKeyAndOptionalBranchOrPullRequest(dbSession, projectKey, "pr-123", "pr-123"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Either branch or pull request can be provided, not both");
  }

  @Test
  public void get_by_key_and_branch_accept_main_branch() {
    ComponentDto project = db.components().insertPublicProject();

    assertThat(underTest.getByKeyAndBranch(dbSession, project.getKey(), "master").uuid()).isEqualTo(project.uuid());
  }

  @Test
  public void fail_to_get_by_key_and_branch_when_branch_does_not_exist() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("my_branch"));
    ComponentDto file = db.components().insertComponent(newFileDto(branch));

    String fileKey = file.getKey();
    assertThatThrownBy(() -> underTest.getByKeyAndBranch(dbSession, fileKey, "other_branch"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage(format("Component '%s' on branch 'other_branch' not found", fileKey));
  }
}
