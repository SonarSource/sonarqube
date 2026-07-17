/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.issue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ComponentImpl;
import org.sonar.ce.task.projectanalysis.component.ReportAttributes;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.AnticipatedTransitionDto;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.project.CreationMethod;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;

public class AnticipatedTransitionRepositoryImplTest {

  @Rule
  public final DbTester db = DbTester.create(System2.INSTANCE);

  private final DbClient dbClient = db.getDbClient();

  private final AnticipatedTransitionRepository underTest = new AnticipatedTransitionRepositoryImpl(dbClient);

  @Before
  public void cleanUpDb() {

  }

  @Test
  public void giveAnticipatedTransitionsForFile_shouldBeReturnedCorrectly() {
    //given
    String projectKey = "projectKey1";
    String projectUuid = "projectUuid1";
    ProjectDto projectDto = getProjectDto(projectUuid, projectKey);
    dbClient.projectDao().insert(db.getSession(), projectDto);
    String authorizedUserUuid = insertUserWithIssueAdminPermission(projectDto);

    insertAnticipatedTransition(projectUuid, "file1.js", authorizedUserUuid);
    insertAnticipatedTransition(projectUuid, "file2.js", authorizedUserUuid);
    insertAnticipatedTransition(projectUuid, "file2.js", authorizedUserUuid);

    db.getSession().commit();

    String componentUuid = "componentUuid";
    Component file = getFileComponent(componentUuid, projectKey, "file1.js");
    var anticipatedTransitions = underTest.getAnticipatedTransitionByComponent(file);
    assertThat(anticipatedTransitions).hasSize(1);

    file = getFileComponent(componentUuid, projectKey, "file2.js");
    anticipatedTransitions = underTest.getAnticipatedTransitionByComponent(file);
    assertThat(anticipatedTransitions).hasSize(2);

    file = getFileComponent(componentUuid, projectKey, "file3.js");
    anticipatedTransitions = underTest.getAnticipatedTransitionByComponent(file);
    assertThat(anticipatedTransitions).isEmpty();
  }

  @Test
  public void giveProjectBranchAvailable_projectUuidShouldBeCalculatedFromThere() {
    //given
    String projectKey = "projectKey2";
    String projectUuid = "projectUuid2";
    String mainFile = "file1.js";
    ProjectDto projectDto = getProjectDto(projectUuid, projectKey);
    dbClient.projectDao().insert(db.getSession(), projectDto);
    String authorizedUserUuid = insertUserWithIssueAdminPermission(projectDto);

    BranchDto branchDto = getBranchDto(projectUuid, "branch");
    dbClient.branchDao().insert(db.getSession(), branchDto);

    ComponentDto fileDto = getComponentDto(projectKey + ":" + mainFile, branchDto.getUuid());
    dbClient.componentDao().insertWithAudit(db.getSession(), fileDto);

    insertAnticipatedTransition(projectUuid, mainFile, authorizedUserUuid);
    insertAnticipatedTransition(projectUuid, "file2.js", authorizedUserUuid);
    insertAnticipatedTransition(projectUuid, "file2.js", authorizedUserUuid);

    db.getSession().commit();

    Component file = getFileComponent(fileDto.uuid(), projectKey, mainFile);
    var anticipatedTransitions = underTest.getAnticipatedTransitionByComponent(file);
    assertThat(anticipatedTransitions).hasSize(1);
  }

  @Test
  public void giveAnticipatedTransitionFromUserWhoLostIssueAdminPermission_shouldBeFilteredOut() {
    //given
    String projectKey = "projectKey3";
    String projectUuid = "projectUuid3";
    ProjectDto projectDto = getProjectDto(projectUuid, projectKey);
    dbClient.projectDao().insert(db.getSession(), projectDto);

    String authorizedUserUuid = insertUserWithIssueAdminPermission(projectDto);
    UserDto unauthorizedUser = db.users().insertUser();

    insertAnticipatedTransition(projectUuid, "file1.js", authorizedUserUuid);
    insertAnticipatedTransition(projectUuid, "file1.js", unauthorizedUser.getUuid());

    db.getSession().commit();

    String componentUuid = "componentUuid3";
    Component file = getFileComponent(componentUuid, projectKey, "file1.js");
    var anticipatedTransitions = underTest.getAnticipatedTransitionByComponent(file);

    assertThat(anticipatedTransitions).hasSize(1);
    assertThat(anticipatedTransitions.iterator().next().getUserUuid()).isEqualTo(authorizedUserUuid);
  }

  private String insertUserWithIssueAdminPermission(ProjectDto projectDto) {
    UserDto user = db.users().insertUser();
    db.users().insertProjectPermissionOnUser(user, ProjectPermission.ISSUE_ADMIN, projectDto);
    return user.getUuid();
  }

  private void insertAnticipatedTransition(String projectUuid, String filename, String userUuid) {
    var anticipatedTransition = getAnticipatedTransition(projectUuid, filename, userUuid);
    dbClient.anticipatedTransitionDao().insert(db.getSession(), anticipatedTransition);
  }

  private ComponentDto getComponentDto(String componentKey, String branchUuid) {
    ComponentDto componentDto = new ComponentDto();
    componentDto.setQualifier("FIL")
      .setUuid(Uuids.createFast())
      .setKey(componentKey)
      .setBranchUuid(branchUuid)
      .setUuidPath(Uuids.createFast());
    return componentDto;
  }


  private BranchDto getBranchDto(String projectUuid, String key) {
    BranchDto branchDto = new BranchDto();
    branchDto.setProjectUuid(projectUuid)
      .setUuid(Uuids.createFast())
      .setIsMain(true)
      .setBranchType(BranchType.BRANCH)
      .setKey(key);
    return branchDto;
  }

  private ProjectDto getProjectDto(String projectUuid, String projectKey) {
    ProjectDto projectDto = new ProjectDto();
    projectDto.setKey(projectKey);
    projectDto.setUuid(projectUuid);
    projectDto.setQualifier("TRK");
    projectDto.setName("project");
    projectDto.setCreationMethod(CreationMethod.LOCAL_API);
    projectDto.setPrivate(true);
    return projectDto;
  }

  private Component getFileComponent(String componenUuid, String projectKey, String filename) {
    return ComponentImpl.builder(FILE)
      .setUuid(componenUuid)
      .setKey(String.format("%s:%s", projectKey, filename))
      .setName(filename)
      .setStatus(Component.Status.ADDED)
      .setShortName(filename)
      .setReportAttributes(mock(ReportAttributes.class)).build();
  }

  private AnticipatedTransitionDto getAnticipatedTransition(String projectUuid, String filename, String userUuid) {
    return new AnticipatedTransitionDto(Uuids.createFast(), projectUuid, userUuid, "wontfix", null, null, null, null, "rule:key", filename, 1_704_067_200_000L);
  }

}
