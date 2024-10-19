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
package org.sonar.server.user.ws;

import java.util.Optional;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.server.common.PaginationInformation;
import org.sonar.server.common.SearchResults;
import org.sonar.server.common.user.service.UserInformation;
import org.sonar.server.common.user.service.UserService;
import org.sonar.server.common.user.service.UsersSearchRequest;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Users;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.server.common.PaginationInformation.forPageIndex;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SearchAction implements UsersWsAction {
  private static final int MAX_PAGE_SIZE = 500;

  private static final String DEACTIVATED_PARAM = "deactivated";
  private static final String MANAGED_PARAM = "managed";
  static final String LAST_CONNECTION_DATE_FROM = "lastConnectedAfter";
  static final String LAST_CONNECTION_DATE_TO = "lastConnectedBefore";
  static final String SONAR_LINT_LAST_CONNECTION_DATE_FROM = "slLastConnectedAfter";
  static final String SONAR_LINT_LAST_CONNECTION_DATE_TO = "slLastConnectedBefore";
  static final String EXTERNAL_IDENTITY = "externalIdentity";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final UserService userService;
  private final SearchWsReponseGenerator searchWsReponseGenerator;

  public SearchAction(DbClient dbClient, UserSession userSession,
    UserService userService, SearchWsReponseGenerator searchWsReponseGenerator) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.userService = userService;
    this.searchWsReponseGenerator = searchWsReponseGenerator;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("search")
      .setDescription("Get a list of users. By default, only active users are returned.<br/>" +
        "Requires 'Administer System' permission at an Organization Level or at Global Level." +
        " For Organization Admins, list of users part of the organization(s) are returned")
      .setSince("3.6")
      .setChangelog(
        new Change("10.4", "Deprecated. Use GET api/v2/users-management/users instead"),
        new Change("10.3", "New optional parameters " + EXTERNAL_IDENTITY + " to find a user by its IdP login"),
        new Change("10.1", "New optional parameters " + SONAR_LINT_LAST_CONNECTION_DATE_FROM +
          " and " + SONAR_LINT_LAST_CONNECTION_DATE_TO + " to filter users by SonarLint last connection date. Only available with Administer System permission."),
        new Change("10.1", "New optional parameters " + LAST_CONNECTION_DATE_FROM +
          " and " + LAST_CONNECTION_DATE_TO + " to filter users by SonarQube last connection date. Only available with Administer System permission."),
        new Change("10.1", "New field 'sonarLintLastConnectionDate' is added to response"),
        new Change("10.0", "'q' parameter values is now always performing a case insensitive match"),
        new Change("10.0", "New parameter 'managed' to optionally search by managed status"),
        new Change("10.0", "Response includes 'managed' field."),
        new Change("9.9", "Organization Admin can access Email and Last Connection Info of all members of the "
            + "organization. API is accessible only for System Administrators or Organization Administrators"),
        new Change("9.7", "New parameter 'deactivated' to optionally search for deactivated users"),
        new Change("7.7", "New field 'lastConnectionDate' is added to response"),
        new Change("7.4", "External identity is only returned to system administrators"),
        new Change("6.4", "Paging response fields moved to a Paging object"),
        new Change("6.4", "Avatar has been added to the response"),
        new Change("6.4", "Email is only returned when user has Administer System permission"))
      .setHandler(this)
      .setResponseExample(getClass().getResource("search-example.json"))
      .setDeprecatedSince("10.4");

    action.addPagingParams(50, SearchOptions.MAX_PAGE_SIZE);

    final String dateExample = "2020-01-01T00:00:00+0100";
    action.createParam(TEXT_QUERY)
      .setMinimumLength(2)
      .setDescription("Filter on login, name and email.<br />" +
        "This parameter performs a partial match (contains), it is case insensitive.");
    action.createParam(DEACTIVATED_PARAM)
      .setSince("9.7")
      .setDescription("Return deactivated users instead of active users")
      .setRequired(false)
      .setDefaultValue(false)
      .setBooleanPossibleValues();
    action.createParam(MANAGED_PARAM)
      .setSince("10.0")
      .setDescription("Return managed or non-managed users. Only available for managed instances, throws for non-managed instances.")
      .setRequired(false)
      .setDefaultValue(null)
      .setBooleanPossibleValues();
    action.createParam(LAST_CONNECTION_DATE_FROM)
      .setSince("10.1")
      .setDescription("""
        Filter the users based on the last connection date field.
        Only users who interacted with this instance at or after the date will be returned.
        The format must be ISO 8601 datetime format (YYYY-MM-DDThh:mm:ss±hhmm)""")
      .setRequired(false)
      .setDefaultValue(null)
      .setExampleValue(dateExample);
    action.createParam(LAST_CONNECTION_DATE_TO)
      .setSince("10.1")
      .setDescription("""
        Filter the users based on the last connection date field.
        Only users that never connected or who interacted with this instance at or before the date will be returned.
        The format must be ISO 8601 datetime format (YYYY-MM-DDThh:mm:ss±hhmm)""")
      .setRequired(false)
      .setDefaultValue(null)
      .setExampleValue(dateExample);
    action.createParam(SONAR_LINT_LAST_CONNECTION_DATE_FROM)
      .setSince("10.1")
      .setDescription("""
        Filter the users based on the sonar lint last connection date field
        Only users who interacted with this instance using SonarLint at or after the date will be returned.
        The format must be ISO 8601 datetime format (YYYY-MM-DDThh:mm:ss±hhmm)""")
      .setRequired(false)
      .setDefaultValue(null)
      .setExampleValue(dateExample);
    action.createParam(SONAR_LINT_LAST_CONNECTION_DATE_TO)
      .setSince("10.1")
      .setDescription("""
        Filter the users based on the sonar lint last connection date field.
        Only users that never connected or who interacted with this instance using SonarLint at or before the date will be returned.
        The format must be ISO 8601 datetime format (YYYY-MM-DDThh:mm:ss±hhmm)""")
      .setRequired(false)
      .setDefaultValue(null)
      .setExampleValue(dateExample);
    action.createParam(EXTERNAL_IDENTITY)
      .setSince("10.3")
      .setDescription("""
        Find a user by its external identity (ie. its login in the Identity Provider).
        This is case sensitive and only available with Administer System permission.
        """);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    throwIfAdminOnlyParametersAreUsed(request);

    var searchRequest = toSearchRequest(request);

    boolean showEmailAndLastConnectionInfo = false;
    if (!userSession.isSystemAdministrator()) {
      try (DbSession dbSession = dbClient.openSession(false)) {
        var userOrganizations = dbClient.organizationMemberDao().selectOrganizationUuidsByUser(dbSession, userSession.getUuid());
        var orgsWithUserAsAdmin = userOrganizations.stream()
            .filter(o -> userSession.hasPermission(OrganizationPermission.ADMINISTER, o))
            .toList();
        if (!orgsWithUserAsAdmin.isEmpty()) {
          searchRequest.setOrganizationUuids(orgsWithUserAsAdmin);
          showEmailAndLastConnectionInfo = true;
        } else {
          throw insufficientPrivilegesException();
        }
      }
    }

    Users.SearchWsResponse wsResponse = doHandle(searchRequest.build(), showEmailAndLastConnectionInfo);
    writeProtobuf(wsResponse, request, response);
  }

  private void throwIfAdminOnlyParametersAreUsed(Request request) {
    if (!userSession.isSystemAdministrator()) {
      throwIfParameterValuePresent(request, LAST_CONNECTION_DATE_FROM);
      throwIfParameterValuePresent(request, LAST_CONNECTION_DATE_TO);
      throwIfParameterValuePresent(request, SONAR_LINT_LAST_CONNECTION_DATE_FROM);
      throwIfParameterValuePresent(request, SONAR_LINT_LAST_CONNECTION_DATE_TO);
      throwIfParameterValuePresent(request, EXTERNAL_IDENTITY);
    }
  }

  private Users.SearchWsResponse doHandle(UsersSearchRequest request, boolean showEmailAndLastConnectionInfo) {
    SearchResults<UserInformation> userSearchResults = userService.findUsers(request);
    PaginationInformation paging = forPageIndex(request.getPage()).withPageSize(request.getPageSize()).andTotal(userSearchResults.total());

    return searchWsReponseGenerator.toUsersForResponse(userSearchResults.searchResults(), paging, showEmailAndLastConnectionInfo);
  }

  private UsersSearchRequest.Builder toSearchRequest(Request request) {
    int pageSize = request.mandatoryParamAsInt(PAGE_SIZE);
    checkArgument(pageSize <= MAX_PAGE_SIZE, "The '%s' parameter must be less than %s", PAGE_SIZE, MAX_PAGE_SIZE);
    return UsersSearchRequest.builder()
      .setQuery(request.param(TEXT_QUERY))
      .setDeactivated(request.mandatoryParamAsBoolean(DEACTIVATED_PARAM))
      .setManaged(request.paramAsBoolean(MANAGED_PARAM))
      .setLastConnectionDateFrom(request.param(LAST_CONNECTION_DATE_FROM))
      .setLastConnectionDateTo(request.param(LAST_CONNECTION_DATE_TO))
      .setSonarLintLastConnectionDateFrom(request.param(SONAR_LINT_LAST_CONNECTION_DATE_FROM))
      .setSonarLintLastConnectionDateTo(request.param(SONAR_LINT_LAST_CONNECTION_DATE_TO))
      .setExternalLogin(request.param(EXTERNAL_IDENTITY))
      .setPage(request.mandatoryParamAsInt(PAGE))
      .setPageSize(pageSize);
  }

  private static void throwIfParameterValuePresent(Request request, String parameter) {
    Optional.ofNullable(request.param(parameter)).ifPresent(v -> throwForbiddenFor(parameter));
  }

  private static void throwForbiddenFor(String parameterName) {
    throw new ServerException(403, "parameter " + parameterName + " requires Administer System permission.");
  }

}
