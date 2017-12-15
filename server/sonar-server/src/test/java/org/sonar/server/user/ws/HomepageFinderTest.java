/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

package org.sonar.server.user.ws;

import java.util.UUID;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.tester.UserSessionRule;
import org.sonarqube.ws.Users.CurrentWsResponse.Homepage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarqube.ws.Users.CurrentWsResponse.HomepageType.ORGANIZATION;
import static org.sonarqube.ws.Users.CurrentWsResponse.HomepageType.PROJECT;

public class HomepageFinderTest {

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();

  @Test
  public void find_project_homepage_for_users_that_have_one() throws Exception {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = ComponentTesting.newPublicProjectDto(organization);
    dbTester.components().insertComponent(project);

    HomepageFinder underTest = new HomepageFinder(dbClient);

    Homepage homepage = underTest.findFor(dbSession, new UserDto().setHomepageType("PROJECT").setHomepageKey(project.uuid()));

    assertThat(homepage).isNotNull();
    assertThat(homepage.getType()).isEqualTo(PROJECT);
    assertThat(homepage.getValue()).isEqualTo(project.getKey());
  }

  @Test
  public void find_organization_homepage_for_users_that_have_one() throws Exception {
    OrganizationDto organization = dbTester.organizations().insert();

    HomepageFinder underTest = new HomepageFinder(dbClient);

    Homepage homepage = underTest.findFor(dbSession, new UserDto().setHomepageType("ORGANIZATION").setHomepageKey(organization.getUuid()));

    assertThat(homepage).isNotNull();
    assertThat(homepage.getType()).isEqualTo(ORGANIZATION);
    assertThat(homepage.getValue()).isEqualTo(organization.getKey());
  }


  @Test
  @Ignore // Default WIP implementation to be done in SONAR-10185
  public void default_if_homepage_project_key_does_not_match_a_project() throws Exception {
    String frozenUUID = UUID.randomUUID().toString();
    UserDto userDto = new UserDto().setHomepageType("PROJECT").setHomepageKey(frozenUUID);

    HomepageFinder underTest = new HomepageFinder(dbClient);

    Homepage homepage = underTest.findFor(dbSession, userDto);
    assertThat(homepage).isNotNull();
  }

  @Test
  @Ignore // Default WIP implementation to be done in SONAR-10185
  public void default_if_homepage_organization_key_does_not_match_an_organization() throws Exception {
    String frozenUUID = UUID.randomUUID().toString();
    UserDto userDto = new UserDto().setHomepageType("ORGANIZATION").setHomepageKey(frozenUUID);

    HomepageFinder underTest = new HomepageFinder(dbClient);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("No Organization found for homepage key " + frozenUUID);

    Homepage homepage = underTest.findFor(dbSession, userDto);
    assertThat(homepage).isNotNull();
  }


  @Test
  @Ignore // Default WIP implementation to be done in SONAR-10185
  public void find_default_homepage_when_users_does_not_have_one() throws Exception {

  }


}