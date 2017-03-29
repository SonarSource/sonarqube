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
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.organization.CreateWsRequest;
import org.sonarqube.ws.client.permission.AddUserWsRequest;
import util.user.UserRule;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Locale.ENGLISH;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.newUserWsClient;
import static util.ItUtils.setServerProperty;
import static util.ItUtils.xooPlugin;

public class OrganizationMembershipTest {

  @ClassRule
  public static final Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(xooPlugin())
    .build();

  @ClassRule
  public static UserRule userRule = UserRule.from(orchestrator);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static WsClient adminClient;

  @BeforeClass
  public static void setUp() throws Exception {
    adminClient = newAdminWsClient(orchestrator);
    orchestrator.getServer().post("api/organizations/enable_support", emptyMap());
    setServerProperty(orchestrator, "sonar.organizations.anyoneCanCreate", "true");
  }

  @Test
  public void add_and_remove_member() throws Exception {
    String organizationKey = createOrganization();
    String login = createUser();
    adminClient.organizations().addMember(organizationKey, login);
    verifyMembership(login, organizationKey, true);

    adminClient.organizations().removeMember(organizationKey, login);
    verifyMembership(login, organizationKey, false);
  }

  @Test
  public void remove_organization_admin_member() throws Exception {
    String organizationKey = createOrganization();
    String login = createUser();
    adminClient.organizations().addMember(organizationKey, login);
    adminClient.permissions().addUser(new AddUserWsRequest().setLogin(login).setPermission("admin").setOrganization(organizationKey));
    verifyMembership(login, organizationKey, true);

    adminClient.organizations().removeMember(organizationKey, login);
    verifyMembership(login, organizationKey, false);
  }

  @Test
  public void fail_to_remove_organization_admin_member_when_last_admin() throws Exception {
    String organizationKey = createOrganization();
    String login = createUser();
    adminClient.organizations().addMember(organizationKey, login);
    adminClient.permissions().addUser(new AddUserWsRequest().setLogin(login).setPermission("admin").setOrganization(organizationKey));
    verifyMembership(login, organizationKey, true);
    // Admin is the creator of the organization so he was granted with admin permission
    adminClient.organizations().removeMember(organizationKey, "admin");

    expectedException.expect(HttpException.class);
    expectedException.expectMessage("The last administrator member cannot be removed");

    adminClient.organizations().removeMember(organizationKey, login);
  }

  @Test
  public void remove_user_remove_its_membership() throws Exception {
    String organizationKey = createOrganization();
    String login = createUser();
    adminClient.organizations().addMember(organizationKey, login);
    verifyMembership(login, organizationKey, true);

    userRule.deactivateUsers(login);
    verifyMembership(login, organizationKey, false);
  }

  @Test
  public void user_creating_an_organization_becomes_member_of_this_organization() throws Exception {
    String keyAndName = newOrganizationKey();
    String login = createUser();

    newUserWsClient(orchestrator, login, login).organizations().create(
      new CreateWsRequest.Builder().setKey(keyAndName).setName(keyAndName).build()).getOrganization();

    verifyMembership(login, keyAndName, true);
  }

  private void verifyMembership(String login, String organizationKey, boolean isMember) {
    // TODO replace with search member WS
    int count = orchestrator.getDatabase().countSql(format("SELECT COUNT(1) FROM organization_members om " +
      "INNER JOIN users u ON u.id=om.user_id AND u.login='%s' " +
      "INNER JOIN organizations o ON o.uuid=om.organization_uuid AND o.kee='%s'", login, organizationKey));
    assertThat(count).isEqualTo(isMember ? 1 : 0);
  }

  private static String createOrganization() {
    String keyAndName = newOrganizationKey();
    adminClient.organizations().create(new CreateWsRequest.Builder().setKey(keyAndName).setName(keyAndName).build()).getOrganization();
    return keyAndName;
  }

  private static String createUser() {
    String login = randomAlphabetic(10);
    userRule.createUser(login, login);
    return login;
  }

  private static String newOrganizationKey() {
    return randomAlphabetic(32).toLowerCase(ENGLISH);
  }

}
