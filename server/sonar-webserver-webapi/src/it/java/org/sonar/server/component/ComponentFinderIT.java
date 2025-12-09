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
package org.sonar.server.component;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.exceptions.NotFoundException;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;
import static org.sonar.db.component.BranchType.PULL_REQUEST;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.server.component.ComponentFinder.ParamNames.ID_AND_KEY;

public class ComponentFinderIT {

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
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(project);

    String branchUuid = branch.uuid();
    assertThatThrownBy(() -> underTest.getByUuidOrKey(dbSession, branchUuid, null, ID_AND_KEY))
      .isInstanceOf(NotFoundException.class)
      .hasMessage(format("Component id '%s' not found", branchUuid));
  }

  @Test
  public void fail_when_component_uuid_is_removed() {
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto());
    db.components().insertComponent(newFileDto(project, null, "file-uuid").setEnabled(false));

    assertThatThrownBy(() -> underTest.getByUuidFromMainBranch(dbSession, "file-uuid"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Component id 'file-uuid' not found");
  }

  @Test
  public void fail_to_getByUuid_on_branch() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(project);

    String branchUuid = branch.uuid();
    assertThatThrownBy(() -> underTest.getByUuidFromMainBranch(dbSession, branchUuid))
      .isInstanceOf(NotFoundException.class)
      .hasMessage(format("Component id '%s' not found", branchUuid));
  }

  @Test
  public void fail_when_component_key_is_removed() {
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto());
    db.components().insertComponent(newFileDto(project).setKey("file-key").setEnabled(false));

    assertThatThrownBy(() -> underTest.getByKey(dbSession, "file-key"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Component key 'file-key' not found");
  }

  @Test
  public void get_component_by_uuid() {
    db.components().insertComponent(newPrivateProjectDto("project-uuid"));

    ComponentDto component = underTest.getByUuidOrKey(dbSession, "project-uuid", null, ID_AND_KEY);

    assertThat(component.uuid()).isEqualTo("project-uuid");
  }

  @Test
  public void get_component_by_key() {
    db.components().insertPrivateProject(c -> c.setKey("project-key")).getMainBranchComponent();

    ComponentDto component = underTest.getByUuidOrKey(dbSession, null, "project-key", ID_AND_KEY);

    assertThat(component.getKey()).isEqualTo("project-key");
  }

  @Test
  public void get_by_key_and_branch() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("my_branch"));
    ComponentDto directory = db.components().insertComponent(newDirectory(branch, "scr"));
    ComponentDto file = db.components().insertComponent(newFileDto(branch, project.uuid()));

    assertThat(underTest.getByKeyAndBranch(dbSession, project.getKey(), "my_branch").uuid()).isEqualTo(branch.uuid());
    assertThat(underTest.getByKeyAndBranch(dbSession, file.getKey(), "my_branch").uuid()).isEqualTo(file.uuid());
    assertThat(underTest.getByKeyAndBranch(dbSession, directory.getKey(), "my_branch").uuid()).isEqualTo(directory.uuid());
  }

  @Test
  public void get_by_key_and_pull_request() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("pr-123").setBranchType(PULL_REQUEST).setMergeBranchUuid(project.uuid()));
    ComponentDto directory = db.components().insertComponent(newDirectory(branch, "scr"));
    ComponentDto file = db.components().insertComponent(newFileDto(branch, project.uuid()));

    assertThat(underTest.getByKeyAndOptionalBranchOrPullRequest(dbSession, project.getKey(), null, "pr-123").uuid()).isEqualTo(branch.uuid());
    assertThat(underTest.getByKeyAndOptionalBranchOrPullRequest(dbSession, file.getKey(), null, "pr-123").uuid()).isEqualTo(file.uuid());
    assertThat(underTest.getByKeyAndOptionalBranchOrPullRequest(dbSession, directory.getKey(), null, "pr-123").uuid()).isEqualTo(directory.uuid());
  }

  @Test
  public void get_optional_by_key_and_optional_branch_or_pull_request() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto pr = db.components().insertProjectBranch(project, b -> b.setKey("pr").setBranchType(PULL_REQUEST));
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("branch"));
    ComponentDto branchFile = db.components().insertComponent(newFileDto(branch, project.uuid()));

    assertThat(underTest.getOptionalByKeyAndOptionalBranchOrPullRequest(dbSession, project.getKey(), null, null)).isPresent();
    assertThat(underTest.getOptionalByKeyAndOptionalBranchOrPullRequest(dbSession, project.getKey(), null, null).get().uuid())
      .isEqualTo(project.uuid());

    assertThat(underTest.getOptionalByKeyAndOptionalBranchOrPullRequest(dbSession, branch.getKey(), "branch", null)).isPresent();
    assertThat(underTest.getOptionalByKeyAndOptionalBranchOrPullRequest(dbSession, branch.getKey(), "branch", null).get().uuid())
      .isEqualTo(branch.uuid());
    assertThat(underTest.getOptionalByKeyAndOptionalBranchOrPullRequest(dbSession, branchFile.getKey(), "branch", null)).isPresent();
    assertThat(underTest.getOptionalByKeyAndOptionalBranchOrPullRequest(dbSession, branchFile.getKey(), "branch", null).get().uuid())
      .isEqualTo(branchFile.uuid());

    assertThat(underTest.getOptionalByKeyAndOptionalBranchOrPullRequest(dbSession, pr.getKey(), null, "pr")).isPresent();
    assertThat(underTest.getOptionalByKeyAndOptionalBranchOrPullRequest(dbSession, pr.getKey(), null, "pr").get().uuid())
      .isEqualTo(pr.uuid());

    assertThat(underTest.getOptionalByKeyAndOptionalBranchOrPullRequest(dbSession, "unknown", null, null)).isEmpty();

  }

  @Test
  public void fail_when_pull_request_branch_provided() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto pullRequest = db.components().insertProjectBranch(project, b -> b.setKey("pr-123").setBranchType(PULL_REQUEST));

    String projectKey = project.getKey();
    assertThatThrownBy(() -> underTest.getByKeyAndOptionalBranchOrPullRequest(dbSession, projectKey, "pr-123", "pr-123"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Either branch or pull request can be provided, not both");
  }

  @Test
  public void get_by_key_and_branch_accept_main_branch() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();

    assertThat(underTest.getByKeyAndBranch(dbSession, project.getKey(), DEFAULT_MAIN_BRANCH_NAME).uuid()).isEqualTo(project.uuid());
  }

  @Test
  public void fail_to_get_by_key_and_branch_when_branch_does_not_exist() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("my_branch"));
    ComponentDto file = db.components().insertComponent(newFileDto(branch, project.uuid()));

    String fileKey = file.getKey();
    assertThatThrownBy(() -> underTest.getByKeyAndBranch(dbSession, fileKey, "other_branch"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage(format("Component '%s' on branch 'other_branch' not found", fileKey));
  }

  @Test
  public void get_main_branch_name_when_selecting_any_branch() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();

    // Copy project ComponentDto and rename it to be used by the branch
    ComponentDto project2 = project.copy();
    project2.setName("projectName_branch");
    project2.setLongName("projectLongName_branch");
    db.components().insertProjectBranch(project2, b -> b.setKey("my_branch"));

    ComponentDto retrievedProject = underTest.getByKeyAndOptionalBranchOrPullRequest(dbSession, project.getKey(), "my_branch", null);
    assertThat(retrievedProject.name()).isEqualTo(project.name());
    assertThat(retrievedProject.longName()).isEqualTo(project.name());
  }

  @Test
  public void ignore_component_parent_name_when_not_branch() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto directory = db.components().insertComponent(newDirectory(project, "src"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, directory)
      .setKey("org.struts:struts-core:src/org/struts/RequestContext.java")
      .setName("RequestContext.java")
      .setLongName("org.struts.RequestContext")
      .setLanguage("java")
      .setPath("src/RequestContext.java"));

    ComponentDto retrievedFile = underTest.getByKeyAndOptionalBranchOrPullRequest(dbSession, file.getKey(), null, null);
    assertThat(retrievedFile.name()).isEqualTo(file.name());
    assertThat(retrievedFile.longName()).isEqualTo(file.longName());
  }

  @Test
  public void getMainBranch_whenMainBranchExist_shouldReturnMainBranchForProject() {
    ProjectDto projectDto = db.components().insertPrivateProject().getProjectDto();

    BranchDto mainBranch = underTest.getMainBranch(dbSession, projectDto);

    assertThat(mainBranch).isNotNull();
    assertThat(mainBranch.isMain()).isTrue();
  }

  @Test
  public void getMainBranch_whenMainBranchDoesNotExist_shouldThrowException() {
    ProjectDto projectDto = new ProjectDto();
    projectDto.setUuid("uuid");

    assertThatThrownBy(() -> underTest.getMainBranch(dbSession, projectDto))
      .isInstanceOf(IllegalStateException.class);
  }
}
