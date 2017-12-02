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
package org.sonarqube.qa.util;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.Users;
import org.sonarqube.ws.Users.CreateWsResponse.User;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.user.CreateRequest;
import org.sonarqube.ws.client.user.SearchRequest;
import org.sonarqube.ws.client.user.UsersService;
import org.sonarqube.ws.client.usergroups.AddUserRequest;

import static java.util.Arrays.stream;

public class UserTester {

  private static final AtomicInteger ID_GENERATOR = new AtomicInteger();
  private static final String DEFAULT_ORGANIZATION_KEY = "default-organization";

  private final TesterSession session;

  UserTester(TesterSession session) {
    this.session = session;
  }

  void deleteAll() {
    session.wsClient().users().search(SearchRequest.builder().build()).getUsersList()
      .stream()
      .filter(u -> !"admin".equals(u.getLogin()))
      .forEach(u -> {
        PostRequest request = new PostRequest("api/users/deactivate").setParam("login", u.getLogin());
        session.wsClient().wsConnector().call(request);
      });
  }

  @SafeVarargs
  public final User generate(Consumer<CreateRequest.Builder>... populators) {
    int id = ID_GENERATOR.getAndIncrement();
    String login = "login" + id;
    CreateRequest.Builder request = CreateRequest.builder()
      .setLogin(login)
      .setPassword(login)
      .setName("name" + id)
      .setEmail(id + "@test.com");
    stream(populators).forEach(p -> p.accept(request));
    return service().create(request.build()).getUser();
  }

  /**
   * For standalone mode only
   */
  @SafeVarargs
  public final User generateAdministrator(Consumer<CreateRequest.Builder>... populators) {
    User user = generate(populators);
    session.wsClient().permissionsOld().addUser(new org.sonarqube.ws.client.permission.AddUserRequest().setLogin(user.getLogin()).setPermission("admin"));
    session.wsClient().userGroups().addUser(new AddUserRequest().setLogin(user.getLogin()).setName("sonar-administrators"));
    return user;
  }

  @SafeVarargs
  public final User generateAdministrator(Organizations.Organization organization, Consumer<CreateRequest.Builder>... populators) {
    String organizationKey = organization.getKey();
    User user = generate(populators);
    session.wsClient().organizationsOld().addMember(organizationKey, user.getLogin());
    session.wsClient().userGroups().addUser(new AddUserRequest()
      .setOrganization(organizationKey)
      .setLogin(user.getLogin())
      .setName("Owners"));
    return user;
  }

  @SafeVarargs
  public final User generateAdministratorOnDefaultOrganization(Consumer<CreateRequest.Builder>... populators) {
    User user = generate(populators);
    session.wsClient().organizationsOld().addMember(DEFAULT_ORGANIZATION_KEY, user.getLogin());
    session.wsClient().userGroups().addUser(new AddUserRequest()
      .setOrganization(DEFAULT_ORGANIZATION_KEY)
      .setLogin(user.getLogin())
      .setName("sonar-administrators"));
    return user;
  }

  @SafeVarargs
  public final User generateMember(Organizations.Organization organization, Consumer<CreateRequest.Builder>... populators) {
    User user = generate(populators);
    session.wsClient().organizationsOld().addMember(organization.getKey(), user.getLogin());
    return user;
  }

  @SafeVarargs
  public final User generateMemberOfDefaultOrganization(Consumer<CreateRequest.Builder>... populators) {
    User user = generate(populators);
    session.wsClient().organizationsOld().addMember(DEFAULT_ORGANIZATION_KEY, user.getLogin());
    return user;
  }

  public UsersService service() {
    return session.wsClient().users();
  }

  public Optional<Users.SearchWsResponse.User> getByLogin(String login) {
    List<Users.SearchWsResponse.User> users = session.wsClient().users().search(SearchRequest.builder().setQuery(login).build()).getUsersList();
    if (users.size() == 1) {
      return Optional.of(users.get(0));
    }
    return Optional.empty();
  }
}
