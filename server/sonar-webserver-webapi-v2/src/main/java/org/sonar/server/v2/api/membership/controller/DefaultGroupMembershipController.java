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
package org.sonar.server.v2.api.membership.controller;

import java.util.List;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserGroupDto;
import org.sonar.server.common.SearchResults;
import org.sonar.server.common.group.service.GroupMembershipSearchRequest;
import org.sonar.server.common.group.service.GroupMembershipService;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.common.organization.OrganizationService;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.membership.request.GroupMembershipCreateRestRequest;
import org.sonar.server.v2.api.membership.request.GroupsMembershipSearchRestRequest;
import org.sonar.server.v2.api.membership.response.GroupMembershipRestResponse;
import org.sonar.server.v2.api.membership.response.GroupsMembershipSearchRestResponse;
import org.sonar.server.v2.api.model.RestPage;
import org.sonar.server.v2.api.response.PageRestResponse;

public class DefaultGroupMembershipController implements GroupMembershipController {

  private final GroupMembershipService groupMembershipService;
  private final UserSession userSession;
  private final ManagedInstanceChecker managedInstanceChecker;
  private final OrganizationService organizationService;

  public DefaultGroupMembershipController(UserSession userSession, GroupMembershipService groupMembershipService, ManagedInstanceChecker managedInstanceChecker, OrganizationService organizationService) {
    this.groupMembershipService = groupMembershipService;
    this.userSession = userSession;
    this.managedInstanceChecker = managedInstanceChecker;
    this.organizationService = organizationService;
  }

  @Override
  public GroupsMembershipSearchRestResponse search(GroupsMembershipSearchRestRequest groupsSearchRestRequest, RestPage restPage) {
    OrganizationDto organization = organizationService.getOrganizationByKey(groupsSearchRestRequest.organization());
    userSession.checkPermission(OrganizationPermission.ADMINISTER, organization);

    SearchResults<UserGroupDto> groupMembershipSearchResults = searchMembership(groupsSearchRestRequest, restPage);

    List<GroupMembershipRestResponse> groupMembershipRestRespons = toRestGroupMembershipResponse(groupMembershipSearchResults);
    return new GroupsMembershipSearchRestResponse(groupMembershipRestRespons,
      new PageRestResponse(restPage.pageIndex(), restPage.pageSize(), groupMembershipSearchResults.total())
    );
  }

  private SearchResults<UserGroupDto> searchMembership(GroupsMembershipSearchRestRequest groupsSearchRestRequest, RestPage restPage) {
    GroupMembershipSearchRequest groupMembershipSearchRequest = new GroupMembershipSearchRequest(groupsSearchRestRequest.groupId(), groupsSearchRestRequest.userId());
    return groupMembershipService.searchMembers(groupMembershipSearchRequest, restPage.pageIndex(), restPage.pageSize());
  }

  @Override
  public void delete(String id) {
    //throwIfNotAllowedToModifyGroups();
    GroupDto groupDto = groupMembershipService.findGroupMembership(id);
    OrganizationDto organization = organizationService.getOrganizationByUuid(groupDto.getOrganizationUuid());
    userSession.checkPermission(OrganizationPermission.ADMINISTER, organization);
    groupMembershipService.removeMembership(id);
  }

  @Override
  public GroupMembershipRestResponse create(GroupMembershipCreateRestRequest request) {
    //throwIfNotAllowedToModifyGroups();
    OrganizationDto organization = organizationService.getOrganizationByKey(request.organization());
    userSession.checkPermission(OrganizationPermission.ADMINISTER, organization);

    UserGroupDto userGroupDto = groupMembershipService.addMembership(request.groupId(), request.userId());
    return toRestGroupMembershipResponse(userGroupDto);
  }

  private static List<GroupMembershipRestResponse> toRestGroupMembershipResponse(SearchResults<UserGroupDto> groupMembershipSearchResults) {
    return groupMembershipSearchResults.searchResults().stream()
      .map(DefaultGroupMembershipController::toRestGroupMembershipResponse)
      .toList();
  }

  private void throwIfNotAllowedToModifyGroups() {
    userSession.checkIsSystemAdministrator();
    managedInstanceChecker.throwIfInstanceIsManaged();
  }

  private static GroupMembershipRestResponse toRestGroupMembershipResponse(UserGroupDto group) {
    return new GroupMembershipRestResponse(group.getUuid(), group.getGroupUuid(), group.getUserUuid());
  }

}
