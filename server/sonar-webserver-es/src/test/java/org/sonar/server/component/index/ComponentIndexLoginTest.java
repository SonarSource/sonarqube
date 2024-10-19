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
package org.sonar.server.component.index;

import org.junit.Test;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;

import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.db.user.UserTesting.newUserDto;

public class ComponentIndexLoginTest extends ComponentIndexTest {

  @Test
  public void should_filter_unauthorized_results() {
    indexer.index(newProject("sonarqube", "Quality Product"));

    // do not give any permissions to that project

    assertNoSearchResults("sonarqube");
    assertNoSearchResults("Quality Product");
  }

  @Test
  public void should_find_project_for_which_the_user_has_direct_permission() {
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
  public void should_find_project_for_which_the_user_has_indirect_permission_through_group() {
    GroupDto group = newGroupDto();
    userSession.logIn().setGroups(group);

    EntityDto project = newProject("sonarqube", "Quality Product");
    indexer.index(project);

    assertNoSearchResults("sonarqube");

    // give the user implicit access (though group)
    authorizationIndexerTester.allowOnlyGroup(project, group);
    assertSearchResults("sonarqube", project);
  }

}
