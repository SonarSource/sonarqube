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
package org.sonarqube.tests;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.WsUsers;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.organization.CreateWsRequest;
import org.sonarqube.ws.client.organization.OrganizationService;
import org.sonarqube.ws.client.organization.SearchMembersWsRequest;
import org.sonarqube.ws.client.organization.SearchWsRequest;
import org.sonarqube.ws.client.user.GroupsRequest;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;

public class OrganizationTester {

  private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

  private final Session session;

  OrganizationTester(Session session) {
    this.session = session;
  }

  void enableSupport() {
    session.wsClient().wsConnector().call(new PostRequest("api/organizations/enable_support"));
  }

  void deleteNonGuardedOrganizations() {
    service().search(SearchWsRequest.builder().build()).getOrganizationsList()
      .stream()
      .filter(o -> !o.getKey().equals("default-organization"))
      .forEach(organization -> service().delete(organization.getKey()));
  }

  @SafeVarargs
  public final Organizations.Organization generate(Consumer<CreateWsRequest.Builder>... populators) {
    int id = ID_GENERATOR.getAndIncrement();
    CreateWsRequest.Builder request = new CreateWsRequest.Builder()
      .setKey("org" + id)
      .setName("Org " + id)
      .setDescription("Description " + id)
      .setUrl("http://test" + id);
    stream(populators).forEach(p -> p.accept(request));
    return service().create(request.build()).getOrganization();
  }

  public OrganizationTester addMember(Organizations.Organization organization, WsUsers.CreateWsResponse.User user) {
    service().addMember(organization.getKey(), user.getLogin());
    return this;
  }

  public OrganizationTester assertThatOrganizationDoesNotExist(String organizationKey) {
    SearchWsRequest request = new SearchWsRequest.Builder().setOrganizations(organizationKey).build();
    Organizations.SearchWsResponse searchWsResponse = service().search(request);
    assertThat(searchWsResponse.getOrganizationsList()).isEmpty();
    return this;
  }

  public OrganizationTester assertThatMemberOf(Organizations.Organization organization, WsUsers.CreateWsResponse.User user) {
    return assertThatMemberOf(organization, user.getLogin());
  }

  public OrganizationTester assertThatMemberOf(Organizations.Organization organization, String userLogin) {
    verifyOrganizationMembership(organization, userLogin, true);
    verifyMembersGroupMembership(userLogin, organization, true);
    return this;
  }

  public OrganizationTester assertThatNotMemberOf(Organizations.Organization organization, WsUsers.CreateWsResponse.User user) {
    return assertThatNotMemberOf(organization, user.getLogin());
  }

  public OrganizationTester assertThatNotMemberOf(Organizations.Organization organization, String userLogin) {
    verifyOrganizationMembership(organization, userLogin, false);
    try {
      verifyMembersGroupMembership(userLogin, organization, false);
    } catch (HttpException e) {
      // do not fail if user does not exist
      if (e.code() != 404) {
        throw e;
      }
    }
    return this;
  }

  private void verifyOrganizationMembership(@Nullable Organizations.Organization organization, String userLogin, boolean isMember) {
    List<Organizations.User> users = service().searchMembers(new SearchMembersWsRequest()
      .setQuery(userLogin)
      .setSelected("selected")
      .setOrganization(organization != null ? organization.getKey() : null))
      .getUsersList();
    assertThat(users).hasSize(isMember ? 1 : 0);
  }

  private void verifyMembersGroupMembership(String userLogin, @Nullable Organizations.Organization organization, boolean isMember) {
    List<WsUsers.GroupsWsResponse.Group> groups = session.wsClient().users().groups(GroupsRequest.builder()
      .setLogin(userLogin)
      .setOrganization(organization != null ? organization.getKey() : null)
      .setQuery("Members")
      .setSelected("selected")
      .build())
      .getGroupsList();
    assertThat(groups).hasSize(isMember ? 1 : 0);
  }

  public OrganizationService service() {
    return session.wsClient().organizations();
  }
}
