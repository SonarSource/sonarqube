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

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.sonarqube.ws.UserGroups;
import org.sonarqube.ws.Users;
import org.sonarqube.ws.Users.GroupsWsResponse.Group;
import org.sonarqube.ws.client.usergroups.AddUserRequest;
import org.sonarqube.ws.client.usergroups.CreateRequest;
import org.sonarqube.ws.client.usergroups.DeleteRequest;
import org.sonarqube.ws.client.usergroups.SearchRequest;
import org.sonarqube.ws.client.users.GroupsRequest;

import static java.util.Arrays.stream;

public class GroupTester {

  private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

  private final TesterSession session;

  GroupTester(TesterSession session) {
    this.session = session;
  }

  @SafeVarargs
  public final UserGroups.Group generate(Consumer<CreateRequest>... populators) {
    int id = ID_GENERATOR.getAndIncrement();
    CreateRequest request = new CreateRequest()
      .setName("Group" + id)
      .setDescription("Description " + id);
    stream(populators).forEach(p -> p.accept(request));
    return session.wsClient().userGroups().create(request).getGroup();
  }

  public List<UserGroups.Group> getGroups(String partialGroupName) {
    SearchRequest searchRequest = new SearchRequest();
    searchRequest.setQ(partialGroupName);
    return session.wsClient().userGroups().search(searchRequest).getGroupsList();
  }

  public List<Group> getGroupsOfUser(String userLogin) {
    GroupsRequest request = new GroupsRequest().setLogin(userLogin);
    Users.GroupsWsResponse response = session.users().service().groups(request);
    return response.getGroupsList();
  }

  public GroupTester addMemberToGroups(String userLogin, String... groups) {
    for (String group : groups) {
      AddUserRequest request = new AddUserRequest()
        .setLogin(userLogin)
        .setName(group);
      session.wsClient().userGroups().addUser(request);
    }
    return this;
  }

  public GroupTester assertThatUserIsOnlyMemberOf(String userLogin, String... expectedGroups) {
    Set<String> groups = getGroupsOfUser(userLogin).stream()
      .map(Group::getName)
      .collect(Collectors.toSet());
    Assertions.assertThat(groups).containsExactlyInAnyOrder(expectedGroups);
    return this;
  }

  public GroupTester deleteAllGenerated() {
    List<String> allGroups = session.wsClient().userGroups().search(new SearchRequest()).getGroupsList().stream().map(UserGroups.Group::getName)
      .toList();
    allGroups.stream()
      .filter(g -> g.matches("Group\\d+$"))
      .forEach(g -> session.wsClient().userGroups().delete(new DeleteRequest().setName(g)));
    return this;
  }

  public GroupTester delete(UserGroups.Group... groups) {
    List<String> allGroups = session.wsClient().userGroups().search(new SearchRequest()).getGroupsList().stream().map(UserGroups.Group::getName)
      .toList();
    stream(groups)
      .filter(g -> allGroups.contains(g.getName()))
      .forEach(g -> session.wsClient().userGroups().delete(new DeleteRequest().setName(g.getName())));
    return this;
  }
}
