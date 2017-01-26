/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.util.Collections;
import org.junit.Test;
import org.sonar.db.component.ComponentDto;

import static java.util.Collections.emptyList;

public class ComponentIndexLoginTest extends ComponentIndexTest {

  @Test
  public void should_respect_confidentiallity() {
    indexer.index(newProject("sonarqube", "Quality Product"));

    // do not give any permissions to that project

    assertNoSearchResults("sonarqube");
    assertNoSearchResults("Quality Product");
  }

  @Test
  public void should_find_project_for_which_the_user_has_direct_permission() {
    login();

    ComponentDto project = newProject("sonarqube", "Quality Product");
    indexer.index(project);

    // give the user explicit access
    authorizationIndexerTester.indexProjectPermission(project.uuid(),
      emptyList(),
      Collections.singletonList((long) TEST_USER_ID));

    assertSearchResults("sonarqube", project);
  }

  @Test
  public void should_find_project_for_which_the_user_has_indirect_permission_through_group() {
    login();

    ComponentDto project = newProject("sonarqube", "Quality Product");
    indexer.index(project);

    // give the user implicit access (though group)
    authorizationIndexerTester.indexProjectPermission(project.uuid(),
      Collections.singletonList(TEST_USER_GROUP),
      emptyList());

    assertSearchResults("sonarqube", project);
  }

  protected void login() {
    userSession.login("john").setUserId(TEST_USER_ID).setUserGroups(TEST_USER_GROUP);
  }
}
