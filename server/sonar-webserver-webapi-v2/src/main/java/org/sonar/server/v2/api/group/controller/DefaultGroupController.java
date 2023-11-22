/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.v2.api.group.controller;

import java.util.List;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.GroupDto;
import org.sonar.server.common.SearchResults;
import org.sonar.server.common.group.service.GroupInformation;
import org.sonar.server.common.group.service.GroupSearchRequest;
import org.sonar.server.common.group.service.GroupService;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.group.request.GroupUpdateRestRequest;
import org.sonar.server.v2.api.group.request.GroupsSearchRestRequest;
import org.sonar.server.v2.api.group.response.GroupsSearchRestResponse;
import org.sonar.server.v2.api.group.response.RestGroupResponse;
import org.sonar.server.v2.api.model.RestPage;
import org.sonar.server.v2.api.response.PageRestResponse;

public class DefaultGroupController implements GroupController {

  private static final String GROUP_NOT_FOUND_MESSAGE = "Group '%s' not found";
  private final GroupService groupService;
  private final DbClient dbClient;
  private final UserSession userSession;
  private final ManagedInstanceChecker managedInstanceChecker;

  public DefaultGroupController(GroupService groupService, DbClient dbClient, ManagedInstanceChecker managedInstanceChecker, UserSession userSession) {
    this.groupService = groupService;
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.managedInstanceChecker = managedInstanceChecker;
  }

  @Override
  public GroupsSearchRestResponse search(GroupsSearchRestRequest groupsSearchRestRequest, RestPage restPage) {
    userSession.checkLoggedIn().checkIsSystemAdministrator();
    try (DbSession dbSession = dbClient.openSession(false)) {
      GroupSearchRequest groupSearchRequest = new GroupSearchRequest(groupsSearchRestRequest.q(), groupsSearchRestRequest.managed(), restPage.pageIndex(), restPage.pageSize());
      SearchResults<GroupInformation> searchResults = groupService.search(dbSession, groupSearchRequest);
      List<RestGroupResponse> restGroupResponses = toRestGroupResponses(searchResults);
      return new GroupsSearchRestResponse(restGroupResponses, new PageRestResponse(restPage.pageIndex(), restPage.pageSize(), searchResults.total()));
    }
  }

  private static List<RestGroupResponse> toRestGroupResponses(SearchResults<GroupInformation> searchResults) {
    return searchResults.searchResults().stream()
      .map(groupInformation -> toRestGroup(groupInformation.groupDto()))
      .toList();
  }

  @Override
  public RestGroupResponse fetchGroup(String id) {
    userSession.checkLoggedIn().checkIsSystemAdministrator();
    try (DbSession session = dbClient.openSession(false)) {
      GroupDto groupDto = findGroupDtoOrThrow(id, session);
      return toRestGroup(groupDto);
    }
  }

  @Override
  public void deleteGroup(String id) {
    throwIfNotAllowedToChangeGroupName();
    try (DbSession session = dbClient.openSession(false)) {
      GroupDto group = findGroupDtoOrThrow(id, session);
      groupService.delete(session, group);
      session.commit();
    }
  }

  @Override
  public RestGroupResponse updateGroup(String id, GroupUpdateRestRequest updateRequest) {
    throwIfNotAllowedToChangeGroupName();
    try (DbSession session = dbClient.openSession(false)) {
      GroupDto group = findGroupDtoOrThrow(id, session);
      GroupDto updatedGroup = groupService.updateGroup(
        session,
        group,
        updateRequest.getName().orElse(group.getName()),
        updateRequest.getDescription().orElse(group.getDescription())
      );
      session.commit();
      return toRestGroup(updatedGroup);
    }
  }

  private void throwIfNotAllowedToChangeGroupName() {
    userSession.checkIsSystemAdministrator();
    managedInstanceChecker.throwIfInstanceIsManaged();
  }

  private GroupDto findGroupDtoOrThrow(String id, DbSession session) {
    return groupService.findGroupByUuid(session, id)
      .orElseThrow(() -> new NotFoundException(String.format(GROUP_NOT_FOUND_MESSAGE, id)));
  }

  private static RestGroupResponse toRestGroup(GroupDto groupDto) {
    return new RestGroupResponse(groupDto.getUuid(), groupDto.getName(), groupDto.getDescription());
  }
}
