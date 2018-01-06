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
package org.sonarqube.qa.util;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.assertj.core.api.Assertions;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.Users;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.organizations.AddMemberRequest;
import org.sonarqube.ws.client.organizations.CreateRequest;
import org.sonarqube.ws.client.organizations.DeleteRequest;
import org.sonarqube.ws.client.organizations.OrganizationsService;
import org.sonarqube.ws.client.organizations.SearchMembersRequest;
import org.sonarqube.ws.client.organizations.SearchRequest;
import org.sonarqube.ws.client.users.GroupsRequest;

import static java.util.Arrays.stream;

public class OrganizationTester {

  private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

  private final TesterSession session;

  OrganizationTester(TesterSession session) {
    this.session = session;
  }

  void enableSupport() {
    session.wsClient().wsConnector().call(new PostRequest("api/organizations/enable_support"));
  }

  void deleteNonGuardedOrganizations() {
    service().search(new SearchRequest()).getOrganizationsList()
      .stream()
      .filter(o -> !o.getKey().equals("default-organization"))
      .forEach(organization -> service().delete(new DeleteRequest().setOrganization(organization.getKey())));
  }

  @SafeVarargs
  public final Organizations.Organization generate(Consumer<CreateRequest>... populators) {
    int id = ID_GENERATOR.getAndIncrement();
    CreateRequest request = new CreateRequest()
      .setKey("org" + id)
      .setName("Org " + id)
      .setDescription("Description " + id)
      .setUrl("http://test" + id);
    stream(populators).forEach(p -> p.accept(request));
    return service().create(request).getOrganization();
  }

  public OrganizationTester addMember(Organizations.Organization organization, Users.CreateWsResponse.User user) {
    service().addMember(new AddMemberRequest().setOrganization(organization.getKey()).setLogin(user.getLogin()));
    return this;
  }

  public Organizations.Organization getDefaultOrganization() {
    return service().search(new SearchRequest()).getOrganizationsList()
      .stream()
      .filter(o -> o.getKey().equals("default-organization"))
      .findFirst().orElseThrow(() -> new IllegalStateException("Can't find default organization"));
  }

  public OrganizationTester assertThatOrganizationDoesNotExist(String organizationKey) {
    SearchRequest request = new SearchRequest().setOrganizations(Collections.singletonList(organizationKey));
    Organizations.SearchWsResponse searchWsResponse = service().search(request);
    Assertions.assertThat(searchWsResponse.getOrganizationsList()).isEmpty();
    return this;
  }

  public OrganizationTester assertThatMemberOf(Organizations.Organization organization, Users.CreateWsResponse.User user) {
    return assertThatMemberOf(organization, user.getLogin());
  }

  public OrganizationTester assertThatMemberOf(Organizations.Organization organization, String userLogin) {
    verifyOrganizationMembership(organization, userLogin, true);
    verifyMembersGroupMembership(userLogin, organization, true);
    return this;
  }

  public OrganizationTester assertThatNotMemberOf(Organizations.Organization organization, Users.CreateWsResponse.User user) {
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
    List<Organizations.User> users = service().searchMembers(new SearchMembersRequest()
      .setQ(userLogin)
      .setSelected("selected")
      .setOrganization(organization != null ? organization.getKey() : null))
      .getUsersList();
    Assertions.assertThat(users).hasSize(isMember ? 1 : 0);
  }

  private void verifyMembersGroupMembership(String userLogin, @Nullable Organizations.Organization organization, boolean isMember) {
    List<Users.GroupsWsResponse.Group> groups = session.wsClient().users().groups(new GroupsRequest()
      .setLogin(userLogin)
      .setOrganization(organization != null ? organization.getKey() : null)
      .setQ("Members")
      .setSelected("selected"))
      .getGroupsList();
    Assertions.assertThat(groups).hasSize(isMember ? 1 : 0);
  }

  public OrganizationsService service() {
    return session.wsClient().organizations();
  }
}
