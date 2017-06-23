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
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.WsUserGroups;
import org.sonarqube.ws.WsUsers;
import org.sonarqube.ws.WsUsers.GroupsWsResponse.Group;
import org.sonarqube.ws.client.user.GroupsRequest;
import org.sonarqube.ws.client.usergroup.AddUserWsRequest;
import org.sonarqube.ws.client.usergroup.CreateWsRequest;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;

public class GroupTester {

  private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

  private final Session session;

  GroupTester(Session session) {
    this.session = session;
  }

  @SafeVarargs
  public final WsUserGroups.Group generate(@Nullable Organizations.Organization organization, Consumer<CreateWsRequest.Builder>... populators) {
    int id = ID_GENERATOR.getAndIncrement();
    CreateWsRequest.Builder request = CreateWsRequest.builder()
      .setName("Group" + id)
      .setDescription("Description " + id)
      .setOrganization(organization != null ? organization.getKey() : null);
    stream(populators).forEach(p -> p.accept(request));
    return session.wsClient().userGroups().create(request.build()).getGroup();
  }

  public List<Group> getGroupsOfUser(@Nullable Organizations.Organization organization, String userLogin) {
    GroupsRequest request = GroupsRequest.builder()
      .setOrganization(organization != null ? organization.getKey() : null)
      .setLogin(userLogin)
      .build();
    WsUsers.GroupsWsResponse response = session.users().service().groups(request);
    return response.getGroupsList();
  }

  public GroupTester addMemberToGroups(Organizations.Organization organization, String userLogin, String... groups) {
    for (String group : groups) {
      AddUserWsRequest request = AddUserWsRequest.builder()
        .setLogin(userLogin)
        .setOrganization(organization.getKey())
        .setName(group)
        .build();
      session.wsClient().userGroups().addUser(request);
    }
    return this;
  }

  public GroupTester assertThatUserIsMemberOf(@Nullable Organizations.Organization organization, String userLogin, String expectedGroup, String... otherExpectedGroups) {
    List<String> groups = getGroupsOfUser(organization, userLogin)
      .stream()
      .map(Group::getName)
      .collect(Collectors.toList());

    assertThat(groups).contains(expectedGroup);
    if (otherExpectedGroups.length > 0) {
      assertThat(groups).contains(otherExpectedGroups);
    }
    return this;
  }

  public GroupTester assertThatUserIsOnlyMemberOf(@Nullable Organizations.Organization organization, String userLogin, String... expectedGroups) {
    Set<String> groups = getGroupsOfUser(organization, userLogin)
      .stream()
      .map(Group::getName)
      .collect(Collectors.toSet());
    assertThat(groups).containsExactlyInAnyOrder(expectedGroups);
    return this;
  }
}
