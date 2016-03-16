/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package util.user;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.sonar.orchestrator.Orchestrator;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.assertj.core.api.Assertions;
import org.junit.rules.ExternalResource;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsResponse;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static org.assertj.guava.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;

public class UserRule extends ExternalResource {

  private final Orchestrator orchestrator;

  private WsClient adminWsClient;

  private UserRule(Orchestrator orchestrator) {
    this.orchestrator = orchestrator;
  }

  public static UserRule from(Orchestrator orchestrator) {
    return new UserRule(requireNonNull(orchestrator, "Orchestrator instance can not be null"));
  }

  public void resetUsers() {
    for (Users.User user : getUsers().getUsers()) {
      String userLogin = user.getLogin();
      if (!userLogin.equals("admin")) {
        deactivateUsers(userLogin);
      }
    }
  }

  private WsClient adminWsClient() {
    if (adminWsClient == null) {
      adminWsClient = newAdminWsClient(orchestrator);
    }
    return adminWsClient;
  }

  public Users.User verifyUserExists(String login, String name, @Nullable String email) {
    Optional<Users.User> user = getUserByLogin(login);
    assertThat(user).as("User with login '%s' hasn't been found", login).isPresent();
    Assertions.assertThat(user.get().getLogin()).isEqualTo(login);
    Assertions.assertThat(user.get().getName()).isEqualTo(name);
    Assertions.assertThat(user.get().getEmail()).isEqualTo(email);
    return user.get();
  }

  public void verifyUserExists(String login, String name, @Nullable String email, boolean local) {
    Users.User user = verifyUserExists(login, name, email);
    Assertions.assertThat(user.isLocal()).isEqualTo(local);
  }

  public void verifyUserDoesNotExist(String login) {
    assertThat(getUserByLogin(login)).as("Unexpected user with login '%s' has been found", login).isAbsent();
  }

  public void createUser(String login, String name, @Nullable String email, String password) {
    adminWsClient().wsConnector().call(
      new PostRequest("api/users/create")
        .setParam("login", login)
        .setParam("name", name)
        .setParam("email", email)
        .setParam("password", password));
  }

  public void createUser(String login, String password) {
    createUser(login, login, null, password);
  }

  public Optional<Users.User> getUserByLogin(String login) {
    return FluentIterable.from(getUsers().getUsers()).firstMatch(new MatchUserLogin(login));
  }

  public Users getUsers() {
    WsResponse response = adminWsClient().wsConnector().call(
      new GetRequest("api/users/search"));
    Assertions.assertThat(response.code()).isEqualTo(200);
    return Users.parse(response.content());
  }

  public void deactivateUsers(List<String> userLogins) {
    for (String userLogin : userLogins) {
      if (getUserByLogin(userLogin).isPresent()) {
        adminWsClient().wsConnector().call(
          new PostRequest("api/users/deactivate")
            .setParam("login", userLogin));
      }
    }
  }

  public void deactivateUsers(String... userLogins) {
    deactivateUsers(asList(userLogins));
  }

  private class MatchUserLogin implements Predicate<Users.User> {
    private final String login;

    private MatchUserLogin(String login) {
      this.login = login;
    }

    @Override
    public boolean apply(@Nonnull Users.User user) {
      String login = user.getLogin();
      return login != null && login.equals(this.login) && user.isActive();
    }
  }
}
