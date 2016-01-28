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
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
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

  @Override
  protected void before() throws Throwable {
    adminWsClient = newAdminWsClient(orchestrator);
  }

  public void verifyUserExists(String login, String name, String email) {
    Optional<Users.User> user = getUserByLogin(login);
    assertThat(user).as("User with login '%s' hasn't been found", login).isPresent();
    Assertions.assertThat(user.get().getLogin()).isEqualTo(login);
    Assertions.assertThat(user.get().getName()).isEqualTo(name);
    Assertions.assertThat(user.get().getEmail()).isEqualTo(email);
  }

  public void verifyUserDoesNotExist(String login) {
    assertThat(getUserByLogin(login)).as("Unexpected user with login '%s' has been found", login).isAbsent();
  }

  public Optional<Users.User> getUserByLogin(String login) {
    return FluentIterable.from(getUsers().getUsers()).firstMatch(new MatchUserLogin(login));
  }

  public List<Users.User> getUsersByEmails(String... emails) {
    List<Users.User> foundUsers = new ArrayList<>();
    for (String email : emails) {
      Optional<Users.User> user = FluentIterable.from(getUsers().getUsers()).firstMatch(new MatchUserLogin(email));
      if (user.isPresent()) {
        foundUsers.add(user.get());
      }
    }
    return foundUsers;
  }

  public Users getUsers() {
    WsResponse response = adminWsClient.wsConnector().call(
      new GetRequest("api/users/search"));
    Assertions.assertThat(response.code()).isEqualTo(200);
    return Users.parse(response.content());
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

  public void deactivateUsers(List<Users.User> users) {
    for (Users.User user : users) {
      adminWsClient.wsConnector().call(
        new PostRequest("api/users/deactivate")
          .setParam("login", user.getLogin()));
    }
  }

  public void deactivateUsers(Users.User... users) {
    deactivateUsers(asList(users));
  }
}
