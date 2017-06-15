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

package it.organization;

import com.sonar.orchestrator.Orchestrator;
import it.Category6Suite;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.WsUsers;
import org.sonarqube.ws.client.organization.SearchWsRequest;
import util.OrganizationRule;
import util.user.UserRule;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.setServerProperty;

public class PersonalOrganizationTest {

  private static final String SETTING_CREATE_PERSONAL_ORG = "sonar.organizations.createPersonalOrg";

  private static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;
  private static OrganizationRule organizations = new OrganizationRule(orchestrator);
  private static UserRule users = new UserRule(orchestrator);

  @ClassRule
  public static TestRule chain = RuleChain.outerRule(orchestrator)
    .around(users)
    .around(organizations);

  @BeforeClass
  public static void setUp() {
    setServerProperty(orchestrator, SETTING_CREATE_PERSONAL_ORG, "true");
  }

  @AfterClass
  public static void tearDown() {
    setServerProperty(orchestrator, SETTING_CREATE_PERSONAL_ORG, null);
  }

  @Test
  public void personal_organizations_are_created_for_new_users() {
    WsUsers.CreateWsResponse.User user = users.createUser();

    List<Organizations.Organization> existing = organizations.getWsService().search(SearchWsRequest.builder().build()).getOrganizationsList();
    assertThat(existing)
      .filteredOn(o -> o.getGuarded())
      .filteredOn(o -> o.getKey().equals(user.getLogin()))
      .hasSize(1)
      .matches(l -> l.get(0).getName().equals(user.getName()));

    organizations.assertThatMemberOf(existing.get(0), user);
  }
}
