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
package org.sonar.db.component;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.project.ProjectDto;

import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationProjectsDaoTest {
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private UuidFactoryFast uuids = UuidFactoryFast.getInstance();
  private TestSystem2 system2 = new TestSystem2();
  private DbSession dbSession = db.getSession();
  private ApplicationProjectsDao underTest = new ApplicationProjectsDao(system2, uuids);

  @Before
  public void before() {
    system2.setNow(1000L);
  }

  @Test
  public void select_projects() {
    insertApplicationProject("uuid2", "p1");
    insertApplicationProject("uuid2", "p2");

    assertThat(underTest.selectProjects(dbSession, "uuid")).isEmpty();
    assertThat(underTest.selectProjects(dbSession, "uuid2")).extracting(ProjectDto::getUuid).containsOnly("p1", "p2");
  }

  @Test
  public void select_projects_from_non_existing_app_is_empty() {
    insertApplicationProject("uuid", "p1");
    assertThat(underTest.selectProjects(dbSession, "does_not_exist")).isEmpty();
  }

  @Test
  public void add_project() {
    insertProject("p1");
    underTest.addProject(dbSession, "uuid", "p1");
    assertThat(underTest.selectProjects(dbSession, "uuid")).extracting(ProjectDto::getUuid).containsOnly("p1");
  }

  @Test
  public void add_project_branch_to_application_branch() {
    insertProject("p1");
    insertBranch("p1", "b1");
    insertApplication("app1");
    insertBranch("app1", "app-b1");
    underTest.addProjectBranchToAppBranch(dbSession, "app1", "app-b1", "p1", "b1");
    assertThat(underTest.selectProjectBranchesFromAppBranchUuid(dbSession, "app-b1")).extracting(BranchDto::getUuid).containsOnly("b1");
  }

  @Test
  public void select_project_branches_from_application_branch() {
    var project = db.components().insertPublicProjectDto(p -> p.setKey("project"));
    var projectBranch = db.components().insertProjectBranch(project, b -> b.setKey("project-branch"));
    var app = db.components().insertPrivateApplicationDto(a -> a.setKey("app1"));
    var appBranch = db.components().insertProjectBranch(app, b -> b.setKey("app-branch"));
    db.components().addApplicationProject(app, project);
    underTest.addProjectBranchToAppBranch(dbSession, app.getUuid(), appBranch.getUuid(), project.getUuid(), projectBranch.getUuid());
    assertThat(underTest.selectProjectBranchesFromAppBranchUuid(dbSession, appBranch.getUuid())).extracting(BranchDto::getKey).containsOnly("project-branch");
    assertThat(underTest.selectProjectBranchesFromAppBranchKey(dbSession, app.getUuid(), appBranch.getKey())).extracting(BranchDto::getKey).containsOnly("project-branch");
  }

  @Test
  public void remove_project() {
    insertApplicationProject("uuid", "p1");
    insertApplicationProject("uuid", "p2");
    assertThat(underTest.selectProjects(dbSession, "uuid")).extracting(ProjectDto::getUuid).contains("p1");
    underTest.removeApplicationProjectsByApplicationAndProject(dbSession, "uuid", "p1");
    assertThat(underTest.selectProjects(dbSession, "uuid")).extracting(ProjectDto::getUuid).containsOnly("p2");
  }

  @Test
  public void remove_project_from_non_existing_app_is_no_op() {
    insertApplicationProject("uuid", "p1");
    underTest.removeApplicationProjectsByApplicationAndProject(dbSession, "non_existing", "p1");
    assertThat(underTest.selectProjects(dbSession, "uuid")).extracting(ProjectDto::getUuid).containsOnly("p1");
  }

  @Test
  public void remove_non_existing_project_from_app_is_no_op() {
    insertApplicationProject("uuid", "p1");
    underTest.removeApplicationProjectsByApplicationAndProject(dbSession, "uuid", "non_existing");
    assertThat(underTest.selectProjects(dbSession, "uuid")).extracting(ProjectDto::getUuid).containsOnly("p1");
  }

  @Test
  public void remove() {
    insertApplicationProject("uuid", "p1");
    insertApplicationProject("uuid", "p2");

    underTest.remove(dbSession, "uuid");
    assertThat(underTest.selectProjects(dbSession, "uuid")).isEmpty();
  }

  private String insertApplicationProject(String applicationUuid, String projectUuid) {
    String uuid = uuids.create();
    db.executeInsert(
      "app_projects",
      "uuid", uuid,
      "application_uuid", applicationUuid,
      "project_uuid", projectUuid,
      "created_at", 1000L);
    insertProject(projectUuid);
    return uuid;
  }

  private void insertProject(String projectUuid) {
    db.executeInsert("projects",
      "uuid", projectUuid,
      "kee", projectUuid,
      "qualifier", "TRK",
      "private", true,
      "updated_at", 1000L,
      "created_at", 1000L);
  }

  private void insertApplication(String appUuid) {
    db.executeInsert("projects",
      "uuid", appUuid,
      "kee", appUuid,
      "qualifier", "APP",
      "private", true,
      "updated_at", 1000L,
      "created_at", 1000L);
  }

  private void insertBranch(String projectUuid, String branchKey) {
    db.executeInsert("project_branches",
      "uuid", branchKey,
      "branch_type", "BRANCH",
      "project_uuid", projectUuid,
      "kee", branchKey,
      "NEED_ISSUE_SYNC", true,
      "updated_at", 1000L,
      "created_at", 1000L);
  }

}
