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
package org.sonar.db.jira.dao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ProjectData;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.user.UserDto;

import static org.assertj.core.api.Assertions.assertThat;

class JiraPermissionDaoTest {

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final DbSession dbSession = db.getSession();
  private final JiraPermissionDao underTest = new JiraPermissionDao();

  @Test
  void findProjectsAdministeredByCurrentUserAsAdministrator_shouldReturnZero_whenUserUuidIsNull() {
    var result = underTest.findProjectsAdministeredByCurrentUser(dbSession, null);

    assertThat(result).isEmpty();
  }

  @Test
  void findProjectsAdministeredByCurrentUserAsAdministrator_shouldReturnZero_whenUserHasNoPermissions() {
    var user = db.users().insertUser();
    var result = underTest.findProjectsAdministeredByCurrentUser(dbSession, user.getUuid());

    assertThat(result).isEmpty();
  }

  @Test
  void findProjectsAdministeredByCurrentUserAsAdministrator_shouldReturnCorrectCount_whenUserHasDirectPermissions() {
    UserDto user = db.users().insertUser();
    var project1 = db.components().insertPrivateProject();
    var project2 = db.components().insertPrivateProject();
    var project3 = db.components().insertPrivateProject();

    // Grant admin permission on project1 and project2 to the user directly
    db.users().insertProjectPermissionOnUser(user, ProjectPermission.ADMIN, project1.getProjectDto());
    db.users().insertProjectPermissionOnUser(user, ProjectPermission.ADMIN, project2.getProjectDto());
    
    // Grant a different permission on project3 (should not be counted)
    db.users().insertProjectPermissionOnUser(user, ProjectPermission.USER, project3.getProjectDto());

    var result = underTest.findProjectsAdministeredByCurrentUser(dbSession, user.getUuid());

    assertThat(result).containsExactlyInAnyOrder(project1.projectUuid(), project2.projectUuid());
  }

  @Test
  void findProjectsAdministeredByCurrentUserAsAdministrator_shouldReturnCorrectCount_whenUserHasGroupPermissions() {
    var user = db.users().insertUser();
    var group1 = db.users().insertGroup();
    var group2 = db.users().insertGroup();
    
    db.users().insertMember(group1, user);
    db.users().insertMember(group2, user);
    
    var project1 = db.components().insertPrivateProject();
    var project2 = db.components().insertPrivateProject();
    var project3 = db.components().insertPrivateProject();

    // Grant admin permission on project1 and project2 via groups
    db.users().insertEntityPermissionOnGroup(group1, ProjectPermission.ADMIN, project1.getProjectDto());
    db.users().insertEntityPermissionOnGroup(group2, ProjectPermission.ADMIN, project2.getProjectDto());
    
    // Grant a different permission on project3 (should not be counted)
    db.users().insertEntityPermissionOnGroup(group1, ProjectPermission.USER, project3.getProjectDto());

    var found = underTest.findProjectsAdministeredByCurrentUser(dbSession, user.getUuid());

    assertThat(found).containsExactlyInAnyOrder(project1.projectUuid(), project2.projectUuid());
  }

  @Test
  void findProjectsAdministeredByCurrentUserAsAdministrator_shouldReturnCorrectCount_whenUserHasBothDirectAndGroupPermissions() {
    var user = db.users().insertUser();
    var group = db.users().insertGroup();
    
    // Add user to group
    db.users().insertMember(group, user);

    var project1 = db.components().insertPrivateProject();
    var project2 = db.components().insertPrivateProject();
    var project3 = db.components().insertPrivateProject();
    db.components().insertPrivateProject();

    // Grant admin permission on project1 via group
    db.users().insertEntityPermissionOnGroup(group, ProjectPermission.ADMIN, project1.getProjectDto());
    
    // Grant admin permission on project2 directly to user
    db.users().insertProjectPermissionOnUser(user, ProjectPermission.ADMIN, project2.getProjectDto());
    
    // Grant admin permission on project3 via both group AND direct permission
    db.users().insertEntityPermissionOnGroup(group, ProjectPermission.ADMIN, project3.getProjectDto());
    db.users().insertProjectPermissionOnUser(user, ProjectPermission.ADMIN, project3.getProjectDto());

    var found = underTest.findProjectsAdministeredByCurrentUser(dbSession, user.getUuid());

    assertThat(found).containsExactlyInAnyOrder(project1.projectUuid(), project2.projectUuid(), project3.projectUuid(), project3.projectUuid());
  }

  @Test
  void findProjectsAdministeredByCurrentUser_shouldNotCountProjectsWhereUserIsNotMemberOfGroup() {
    var user1 = db.users().insertUser();
    var user2 = db.users().insertUser();
    var group = db.users().insertGroup();
    
    // Add only user2 to group, not user1
    db.users().insertMember(group, user2);
    
    ProjectData project = db.components().insertPrivateProject();

    // Grant admin permission on project via group
    db.users().insertEntityPermissionOnGroup(group, ProjectPermission.ADMIN, project.getProjectDto());

    var user1Projects = underTest.findProjectsAdministeredByCurrentUser(dbSession, user1.getUuid());
    var user2Projects = underTest.findProjectsAdministeredByCurrentUser(dbSession, user2.getUuid());

    assertThat(user1Projects).isEmpty();
    assertThat(user2Projects).containsExactly(project.projectUuid());
  }
}
