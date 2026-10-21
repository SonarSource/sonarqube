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
package org.sonar.server.component.index;

import org.junit.jupiter.api.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.component.ProjectData;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTokenDto;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.user.TokenUserSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.db.user.TokenType.PROJECT_ANALYSIS_TOKEN;
import static org.sonar.db.user.UserTesting.newUserDto;

class ComponentIndexLoginTest extends ComponentIndexTest {

  @Test
  void should_filter_unauthorized_results() {
    indexer.index(newProject("sonarqube", "Quality Product"));

    // do not give any permissions to that project

    assertNoSearchResults("sonarqube");
    assertNoSearchResults("Quality Product");
  }

  @Test
  void should_find_project_for_which_the_user_has_direct_permission() {
    UserDto user = newUserDto();
    userSession.logIn(user);

    EntityDto project = newProject("sonarqube", "Quality Product");
    indexer.index(project);

    assertNoSearchResults("sonarqube");

    // give the user explicit access
    authorizationIndexerTester.allowOnlyUser(project, user);
    assertSearchResults("sonarqube", project);
  }

  @Test
  void should_find_project_for_which_the_user_has_indirect_permission_through_group() {
    GroupDto group = newGroupDto();
    userSession.logIn().setGroups(group);

    EntityDto project = newProject("sonarqube", "Quality Product");
    indexer.index(project);

    assertNoSearchResults("sonarqube");

    // give the user implicit access (though group)
    authorizationIndexerTester.allowOnlyGroup(project, group);
    assertSearchResults("sonarqube", project);
  }

  @Test
  void project_scoped_analysis_token_does_not_find_a_different_project_the_owner_can_browse() {
    UserDto owner = db.users().insertUser();

    ProjectData projectA = db.components().insertPrivateProject(p -> p.setKey("proj-a").setName("Project A"));
    ProjectData projectB = db.components().insertPrivateProject(p -> p.setKey("proj-b").setName("Project B"));
    ProjectDto projectDtoA = projectA.getProjectDto();
    ProjectDto projectDtoB = projectB.getProjectDto();

    indexer.indexAll();
    authorizationIndexerTester.allowOnlyUser(projectDtoA, owner);
    authorizationIndexerTester.allowOnlyUser(projectDtoB, owner);

    UserTokenDto tokenScopedToProjectA = new UserTokenDto()
      .setType(PROJECT_ANALYSIS_TOKEN.name())
      .setName("ci")
      .setUserUuid(owner.getUuid())
      .setProjectKey(projectDtoA.getKey())
      .setProjectName(projectDtoA.getName())
      .setProjectUuid(projectDtoA.getUuid());

    TokenUserSession tokenSession = new TokenUserSession(db.getDbClient(), owner, tokenScopedToProjectA);
    ThreadLocalUserSession threadLocalUserSession = new ThreadLocalUserSession();
    threadLocalUserSession.set(tokenSession);
    ComponentIndex searchAsToken = new ComponentIndex(es.client(), new WebAuthorizationTypeSupport(threadLocalUserSession), System2.INSTANCE);

    SearchIdResult<String> result = searchAsToken.searchV2(ComponentQuery.builder().build(), new SearchOptions());

    assertThat(result.getUuids()).containsExactly(projectDtoA.getUuid());
  }
}
