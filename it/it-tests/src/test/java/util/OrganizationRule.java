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
package util;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.http.HttpMethod;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.junit.rules.ExternalResource;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.WsUsers;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.organization.CreateWsRequest;
import org.sonarqube.ws.client.organization.OrganizationService;
import org.sonarqube.ws.client.organization.SearchMembersWsRequest;
import org.sonarqube.ws.client.organization.SearchWsRequest;
import org.sonarqube.ws.client.user.GroupsRequest;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;

public class OrganizationRule extends ExternalResource implements OrganizationSupport {

  private final Orchestrator orchestrator;
  private OrganizationSupport rootSupport;

  public OrganizationRule(Orchestrator orchestrator) {
    this.orchestrator = orchestrator;
  }

  @Override
  protected void before() {
    enableSupport();
    rootSupport = new OrganizationSupportImpl(newAdminWsClient(orchestrator));
  }

  @Override
  protected void after() {
    deleteOrganizations();
  }

  private void enableSupport() {
    orchestrator.getServer()
      .newHttpCall("api/organizations/enable_support")
      .setMethod(HttpMethod.POST)
      .setAdminCredentials()
      .execute();
  }

  public void deleteOrganizations() {
    rootSupport.getWsService().search(SearchWsRequest.builder().build()).getOrganizationsList()
      .stream()
      .filter(o -> !o.getKey().equals("default-organization"))
      .forEach(organization -> rootSupport.getWsService().delete(organization.getKey()));
  }

  public OrganizationSupport as(String login, String password) {
    return new OrganizationSupportImpl(ItUtils.newUserWsClient(orchestrator, login, password));
  }

  public OrganizationSupport asAnonymous() {
    return as(null, null);
  }

  @Override
  public OrganizationService getWsService() {
    return rootSupport.getWsService();
  }

  @Override
  public Organization create(Consumer<CreateWsRequest.Builder>... populators) {
    return rootSupport.create(populators);
  }

  @Override
  public OrganizationSupport delete(Organization organization) {
    return rootSupport.delete(organization);
  }

  public OrganizationRule assertThatOrganizationDoesNotExist(String organizationKey) {
    SearchWsRequest request = new SearchWsRequest.Builder().setOrganizations(organizationKey).build();
    Organizations.SearchWsResponse searchWsResponse = getWsService().search(request);
    assertThat(searchWsResponse.getOrganizationsList()).isEmpty();
    return this;
  }

  public OrganizationRule assertThatMemberOf(@Nullable Organization organization, WsUsers.CreateWsResponse.User user) {
    verifyOrganizationMembership(organization, user, true);
    verifyMembersGroupMembership(user, organization, true);
    return this;
  }

  public OrganizationRule assertThatNotMemberOf(@Nullable Organization organization, WsUsers.CreateWsResponse.User user) {
    verifyOrganizationMembership(organization, user, false);
    try {
      verifyMembersGroupMembership(user, organization, false);
    } catch (HttpException e) {
      // do not fail if user does not exist
      if (e.code() != 404) {
        throw e;
      }
    }
    return this;
  }

  private void verifyOrganizationMembership(@Nullable Organization organization, WsUsers.CreateWsResponse.User user, boolean isMember) {
    List<Organizations.User> users = getWsService().searchMembers(new SearchMembersWsRequest()
      .setQuery(user.getLogin())
      .setSelected("selected")
      .setOrganization(organization != null ? organization.getKey() : null))
      .getUsersList();
    assertThat(users).hasSize(isMember ? 1 : 0);
  }

  private void verifyMembersGroupMembership(WsUsers.CreateWsResponse.User user, @Nullable Organization organization, boolean isMember) {
    List<WsUsers.GroupsWsResponse.Group> groups = newAdminWsClient(orchestrator).users().groups(GroupsRequest.builder()
      .setLogin(user.getLogin())
      .setOrganization(organization != null ? organization.getKey() : null)
      .setQuery("Members")
      .setSelected("selected")
      .build())
      .getGroupsList();
    assertThat(groups).hasSize(isMember ? 1 : 0);
  }

  private static class OrganizationSupportImpl implements OrganizationSupport {
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger();
    private final WsClient wsClient;

    private OrganizationSupportImpl(WsClient wsClient) {
      this.wsClient = wsClient;
    }

    @Override
    public OrganizationService getWsService() {
      return wsClient.organizations();
    }

    @Override
    public Organization create(Consumer<CreateWsRequest.Builder>... populators) {
      int id = ID_GENERATOR.getAndIncrement();
      CreateWsRequest.Builder request = new CreateWsRequest.Builder()
        .setKey("org" + id)
        .setName("Org " + id);
      stream(populators).forEach(p -> p.accept(request));
      return getWsService().create(request.build()).getOrganization();
    }

    @Override
    public OrganizationSupport delete(Organization organization) {
      getWsService().delete(organization.getKey());
      return this;
    }
  }
}
