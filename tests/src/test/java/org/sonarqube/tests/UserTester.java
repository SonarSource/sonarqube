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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.WsUsers;
import org.sonarqube.ws.WsUsers.CreateWsResponse.User;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.user.CreateRequest;
import org.sonarqube.ws.client.user.SearchRequest;
import org.sonarqube.ws.client.user.UsersService;
import org.sonarqube.ws.client.usergroup.AddUserWsRequest;

import static java.util.Arrays.stream;

public class UserTester {

  private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

  private final Session session;

  UserTester(Session session) {
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

  @SafeVarargs
  public final User generateAdministrator(Consumer<CreateRequest.Builder>... populators) {
    User user = generate(populators);
    session.wsClient().permissions().addUser(new org.sonarqube.ws.client.permission.AddUserWsRequest().setLogin(user.getLogin()).setPermission("admin"));
    session.wsClient().userGroups().addUser(org.sonarqube.ws.client.usergroup.AddUserWsRequest.builder().setLogin(user.getLogin()).setName("sonar-administrators").build());
    return user;
  }

  @SafeVarargs
  public final User generateAdministrator(Organizations.Organization organization, Consumer<CreateRequest.Builder>... populators) {
    User user = generate(populators);
    session.wsClient().organizations().addMember(organization.getKey(), user.getLogin());
    session.wsClient().userGroups().addUser(AddUserWsRequest.builder()
      .setOrganization(organization.getKey())
      .setLogin(user.getLogin())
      .setName("Owners")
      .build());
    return user;
  }

  @SafeVarargs
  public final User generateMember(Organizations.Organization organization, Consumer<CreateRequest.Builder>... populators) {
    User user = generate(populators);
    session.wsClient().organizations().addMember(organization.getKey(), user.getLogin());
    return user;
  }

  public UsersService service() {
    return session.wsClient().users();
  }

  public Optional<WsUsers.SearchWsResponse.User> getByLogin(String login) {
    List<WsUsers.SearchWsResponse.User> users = session.wsClient().users().search(SearchRequest.builder().setQuery(login).build()).getUsersList();
    if (users.size() == 1) {
      return Optional.of(users.get(0));
    }
    return Optional.empty();
  }
}
