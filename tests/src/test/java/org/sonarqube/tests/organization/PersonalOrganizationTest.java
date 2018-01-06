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
package org.sonarqube.tests.organization;

import com.sonar.orchestrator.Orchestrator;
import java.util.List;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.Users;
import org.sonarqube.ws.client.organizations.SearchRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class PersonalOrganizationTest {

  private static final String SETTING_CREATE_PERSONAL_ORG = "sonar.organizations.createPersonalOrg";

  @ClassRule
  public static Orchestrator orchestrator = OrganizationSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Before
  public void setUp() {
    tester.settings().setGlobalSettings(SETTING_CREATE_PERSONAL_ORG, "true");
  }

  @Test
  public void personal_organizations_are_created_for_new_users() {
    Users.CreateWsResponse.User user = tester.users().generate();

    List<Organizations.Organization> existing = tester.wsClient().organizations().search(new SearchRequest()).getOrganizationsList();
    assertThat(existing)
      .filteredOn(Organizations.Organization::getGuarded)
      .filteredOn(o -> o.getKey().equals(user.getLogin()))
      .hasSize(1)
      .matches(l -> l.get(0).getName().equals(user.getName()));

    tester.organizations().assertThatMemberOf(existing.get(0), user);
  }
}
