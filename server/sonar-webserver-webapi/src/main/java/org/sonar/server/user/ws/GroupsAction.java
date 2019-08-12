/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.user.ws;

import java.util.List;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.server.ws.WebService.SelectionMode;
import org.sonar.api.utils.Paging;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupMembershipDto;
import org.sonar.db.user.GroupMembershipQuery;
import org.sonar.db.user.UserDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.user.UserSession;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonarqube.ws.Users.GroupsWsResponse;
import org.sonarqube.ws.Users.GroupsWsResponse.Group;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Optional.ofNullable;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.SELECTED;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.api.utils.Paging.forPageIndex;
import static org.sonar.server.exceptions.NotFoundException.checkFound;
import static org.sonar.server.exceptions.NotFoundException.checkFoundWithOptional;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_LOGIN;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_ORGANIZATION;

public class GroupsAction implements UsersWsAction {

  private static final int MAX_PAGE_SIZE = 500;

  private final DbClient dbClient;
  private final UserSession userSession;
  private final DefaultOrganizationProvider defaultOrganizationProvider;
  private final DefaultGroupFinder defaultGroupFinder;

  public GroupsAction(DbClient dbClient, UserSession userSession, DefaultOrganizationProvider defaultOrganizationProvider, DefaultGroupFinder defaultGroupFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
    this.defaultGroupFinder = defaultGroupFinder;
  }

  @Override
  public void define(NewController context) {
    NewAction action = context.createAction("groups")
      .setDescription("Lists the groups a user belongs to. <br/>" +
        "Requires Administer System permission.")
      .setHandler(this)
      .setResponseExample(getClass().getResource("groups-example.json"))
      .addSelectionModeParam()
      .addSearchQuery("users", "group names")
      .addPagingParams(25)
      .setChangelog(new Change("6.4", "Paging response fields moved to a Paging object"),
        new Change("6.4", "'default' response field has been added"))
      .setSince("5.2");

    action.createParam(PARAM_LOGIN)
      .setDescription("A user login")
      .setExampleValue("admin")
      .setRequired(true);

    action.createParam(PARAM_ORGANIZATION)
      .setDescription("Organization key")
      .setExampleValue("my-org")
      .setInternal(true)
      .setSince("6.4");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    GroupsWsResponse groupsWsResponse = doHandle(toGroupsRequest(request));
    writeProtobuf(groupsWsResponse, request, response);
  }

  private GroupsWsResponse doHandle(GroupsRequest request) {

    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = findOrganizationByKey(dbSession, request.getOrganization());
      userSession.checkPermission(OrganizationPermission.ADMINISTER, organization);

      String login = request.getLogin();
      GroupMembershipQuery query = GroupMembershipQuery.builder()
        .organizationUuid(organization.getUuid())
        .groupSearch(request.getQuery())
        .membership(getMembership(request.getSelected()))
        .pageIndex(request.getPage())
        .pageSize(request.getPageSize())
        .build();
      UserDto user = checkFound(dbClient.userDao().selectActiveUserByLogin(dbSession, login), "Unknown user: %s", login);
      int total = dbClient.groupMembershipDao().countGroups(dbSession, query, user.getId());
      Paging paging = forPageIndex(query.pageIndex()).withPageSize(query.pageSize()).andTotal(total);
      List<GroupMembershipDto> groups = dbClient.groupMembershipDao().selectGroups(dbSession, query, user.getId(), paging.offset(), query.pageSize());
      return buildResponse(groups, defaultGroupFinder.findDefaultGroup(dbSession, organization.getUuid()), paging);
    }
  }

  private OrganizationDto findOrganizationByKey(DbSession dbSession, @Nullable String key) {
    String effectiveKey = key == null ? defaultOrganizationProvider.get().getKey() : key;
    Optional<OrganizationDto> org = dbClient.organizationDao().selectByKey(dbSession, effectiveKey);
    checkFoundWithOptional(org, "No organization with key '%s'", key);
    return org.get();
  }

  private static GroupsRequest toGroupsRequest(Request request) {
    int pageSize = request.mandatoryParamAsInt(PAGE_SIZE);
    checkArgument(pageSize <= MAX_PAGE_SIZE, "The '%s' parameter must be less than %s", PAGE_SIZE, MAX_PAGE_SIZE);
    return GroupsRequest.builder()
      .setLogin(request.mandatoryParam(PARAM_LOGIN))
      .setOrganization(request.param(PARAM_ORGANIZATION))
      .setSelected(request.mandatoryParam(SELECTED))
      .setQuery(request.param(TEXT_QUERY))
      .setPage(request.mandatoryParamAsInt(PAGE))
      .setPageSize(pageSize)
      .build();
  }

  private static String getMembership(String selected) {
    SelectionMode selectionMode = SelectionMode.fromParam(selected);
    String membership = GroupMembershipQuery.ANY;
    if (SelectionMode.SELECTED == selectionMode) {
      membership = GroupMembershipQuery.IN;
    } else if (SelectionMode.DESELECTED == selectionMode) {
      membership = GroupMembershipQuery.OUT;
    }
    return membership;
  }

  private static GroupsWsResponse buildResponse(List<GroupMembershipDto> groups, GroupDto defaultGroup, Paging paging) {
    GroupsWsResponse.Builder responseBuilder = GroupsWsResponse.newBuilder();
    groups.forEach(group -> responseBuilder.addGroups(toWsGroup(group, defaultGroup)));
    responseBuilder.getPagingBuilder()
      .setPageIndex(paging.pageIndex())
      .setPageSize(paging.pageSize())
      .setTotal(paging.total())
      .build();
    return responseBuilder.build();
  }

  private static Group toWsGroup(GroupMembershipDto group, GroupDto defaultGroup) {
    Group.Builder groupBuilder = Group.newBuilder()
      .setId(group.getId())
      .setName(group.getName())
      .setSelected(group.getUserId() != null)
      .setDefault(defaultGroup.getId().longValue() == group.getId());
    ofNullable(group.getDescription()).ifPresent(groupBuilder::setDescription);
    return groupBuilder.build();
  }

  private static class GroupsRequest {

    private final String login;
    private final String organization;
    private final String query;
    private final String selected;
    private final Integer page;
    private final Integer pageSize;

    private GroupsRequest(Builder builder) {
      this.login = builder.login;
      this.organization = builder.organization;
      this.query = builder.query;
      this.selected = builder.selected;
      this.page = builder.page;
      this.pageSize = builder.pageSize;
    }

    public String getLogin() {
      return login;
    }

    @CheckForNull
    public String getOrganization() {
      return organization;
    }

    @CheckForNull
    public String getQuery() {
      return query;
    }

    @CheckForNull
    public String getSelected() {
      return selected;
    }

    @CheckForNull
    public Integer getPage() {
      return page;
    }

    @CheckForNull
    public Integer getPageSize() {
      return pageSize;
    }

    public static Builder builder() {
      return new Builder();
    }
  }

  private static class Builder {
    private String login;
    private String organization;
    private String query;
    private String selected;
    private Integer page;
    private Integer pageSize;

    private Builder() {
      // enforce factory method use
    }

    public Builder setLogin(String login) {
      this.login = login;
      return this;
    }

    public Builder setOrganization(@Nullable String organization) {
      this.organization = organization;
      return this;
    }

    public Builder setQuery(@Nullable String query) {
      this.query = query;
      return this;
    }

    public Builder setSelected(@Nullable String selected) {
      this.selected = selected;
      return this;
    }

    public Builder setPage(@Nullable Integer page) {
      this.page = page;
      return this;
    }

    public Builder setPageSize(@Nullable Integer pageSize) {
      this.pageSize = pageSize;
      return this;
    }

    public GroupsRequest build() {
      checkArgument(!isNullOrEmpty(login), "Login is mandatory and must not be empty");
      return new GroupsRequest(this);
    }
  }
}
