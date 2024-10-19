/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonarqube.ws.tester;

import com.google.common.collect.MoreCollectors;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.sonarqube.ws.UserTokens;
import org.sonarqube.ws.Users;
import org.sonarqube.ws.Users.CreateWsResponse.User;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsResponse;
import org.sonarqube.ws.client.usergroups.AddUserRequest;
import org.sonarqube.ws.client.users.ChangePasswordRequest;
import org.sonarqube.ws.client.users.CreateRequest;
import org.sonarqube.ws.client.users.SearchRequest;
import org.sonarqube.ws.client.users.UpdateIdentityProviderRequest;
import org.sonarqube.ws.client.users.UpdateRequest;
import org.sonarqube.ws.client.users.UsersService;
import org.sonarqube.ws.client.usertokens.GenerateRequest;

import static java.util.Arrays.stream;

public class UserTester {

  private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

  private final TesterSession session;

  UserTester(TesterSession session) {
    this.session = session;
  }

  void deleteAll() {
    session.wsClient().users().search(new SearchRequest()).getUsersList()
      .stream()
      .filter(u -> !"admin".equals(u.getLogin()))
      .forEach(u -> {
        PostRequest request = new PostRequest("api/users/deactivate").setParam("login", u.getLogin());
        try (final WsResponse response = session.wsClient().wsConnector().call(request)) {
          response.failIfNotSuccessful();
        }
      });
  }

  public final String generateToken(String login) {
    int id = ID_GENERATOR.getAndIncrement();
    String name = "token" + id;
    session.wsClient().userTokens().generate(new GenerateRequest().setLogin(login).setName(name));
    return name;
  }

  public final String generateToken(String login, String type, @Nullable String projectKey) {
    int id = ID_GENERATOR.getAndIncrement();
    String name = "token" + id;
    UserTokens.GenerateWsResponse response = session.wsClient().userTokens()
      .generate(new GenerateRequest().setLogin(login).setName(name).setType(type).setProjectKey(projectKey));
    return response.getToken();
  }

  public final String generateToken(String login, Consumer<GenerateRequest>... populators) {
    int id = ID_GENERATOR.getAndIncrement();
    String name = "token" + id;

    GenerateRequest generateRequest = new GenerateRequest()
      .setName(name)
      .setLogin(login);
    stream(populators).forEach(p -> p.accept(generateRequest));
    UserTokens.GenerateWsResponse response = session.wsClient().userTokens()
      .generate(generateRequest);
    return response.getToken();
  }

  @SafeVarargs
  public final User generate(Consumer<CreateRequest>... populators) {
    int id = ID_GENERATOR.getAndIncrement();
    String login = "login" + id;
    CreateRequest request = new CreateRequest()
      .setLogin(login)
      .setPassword(login)
      .setName("name" + id)
      .setEmail(id + "@test.com");
    stream(populators).forEach(p -> p.accept(request));
    return service().create(request).getUser();
  }

  @SafeVarargs
  public final User generateApplicationCreator(Consumer<CreateRequest>... populators) {
    User u = generate(populators);
    session.wsClient().permissions().addUser(
      new org.sonarqube.ws.client.permissions.AddUserRequest()
        .setLogin(u.getLogin())
        .setPermission("applicationcreator"));
    return u;
  }

  @SafeVarargs
  public final User generatePortfolioCreator(Consumer<CreateRequest>... populators) {
    User u = generate(populators);
    session.wsClient().permissions().addUser(
      new org.sonarqube.ws.client.permissions.AddUserRequest()
        .setLogin(u.getLogin())
        .setPermission("portfoliocreator"));
    session.wsClient().permissions().addUser(
      new org.sonarqube.ws.client.permissions.AddUserRequest()
        .setLogin(u.getLogin())
        .setPermission("admin"));
    return u;
  }

  /**
   * For standalone mode only
   */
  @SafeVarargs
  public final User generateAdministrator(Consumer<CreateRequest>... populators) {
    User user = generate(populators);
    session.wsClient().permissions().addUser(new org.sonarqube.ws.client.permissions.AddUserRequest().setLogin(user.getLogin()).setPermission("admin"));
    session.wsClient().userGroups().addUser(new AddUserRequest().setLogin(user.getLogin()).setName("sonar-administrators"));
    return user;
  }

  public UsersService service() {
    return session.wsClient().users();
  }

  public Optional<Users.SearchWsResponse.User> getByExternalLogin(String externalLogin) {
    return getAllUsers().stream()
      .filter(user -> user.getExternalIdentity().equals(externalLogin))
      .collect(MoreCollectors.toOptional());
  }

  public List<Users.SearchWsResponse.User> getAllUsers() {
    return service().search(new SearchRequest()).getUsersList();
  }

  public Optional<Users.SearchWsResponse.User> getDeactivatedUserByExternalLogin(String externalLogin) {
    return getAllDeactivatedUsers().stream()
      .filter(user -> user.getExternalIdentity().equals(externalLogin))
      .collect(MoreCollectors.toOptional());
  }

  public List<Users.SearchWsResponse.User> getAllDeactivatedUsers() {
    return service().search(new SearchRequest().setDeactivated(true)).getUsersList();
  }

  public Optional<Users.SearchWsResponse.User> getByLogin(String login) {
    return queryForUser(login, t -> t.getLogin().equals(login));
  }

  public Optional<Users.SearchWsResponse.User> getByEmail(String email) {
    return queryForUser(email, t -> t.getEmail().equals(email));
  }

  public Optional<Users.SearchWsResponse.User> getByName(String name) {
    return queryForUser(name, t -> t.getName().equals(name));
  }

  public List<Users.SearchWsResponse.User> getAllManagedUsers() {
    return service().search(new SearchRequest().setManaged(true)).getUsersList();
  }

  public void changePassword(String login, String previousPassword, String newPassword) {
    service().changePassword(new ChangePasswordRequest().setLogin(login).setPreviousPassword(previousPassword).setPassword(newPassword));
  }

  private Optional<Users.SearchWsResponse.User> queryForUser(String login, Predicate<Users.SearchWsResponse.User> predicate) {
    List<Users.SearchWsResponse.User> users = session.wsClient().users().search(new SearchRequest().setQ(login)).getUsersList().stream()
      .filter(predicate).toList();
    if (users.size() == 1) {
      return Optional.of(users.get(0));
    }
    return Optional.empty();
  }

  public final String generateLogin() {
    int id = ID_GENERATOR.getAndIncrement();
    return "login" + id;
  }

  public final String generateProviderId() {
    int id = ID_GENERATOR.getAndIncrement();
    return "providerId" + id;
  }

  public final String generateEmail() {
    int id = ID_GENERATOR.getAndIncrement();
    return "email" + id + "@test.com";
  }

  public void updateIdentityProvider(String login, String externalProvider, @Nullable String externalIdentity) {
    session.wsClient().users().updateIdentityProvider(
      new UpdateIdentityProviderRequest()
        .setLogin(login)
        .setNewExternalProvider(externalProvider)
        .setNewExternalIdentity(externalIdentity));
  }

  public void update(String login, Consumer<UpdateRequest>... updaters) {
    UpdateRequest updateRequest = new UpdateRequest();
    updateRequest.setLogin(login);
    stream(updaters).forEach(p -> p.accept(updateRequest));
    session.wsClient().users().update(updateRequest);
  }
}
