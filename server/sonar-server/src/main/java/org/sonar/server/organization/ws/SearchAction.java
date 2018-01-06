/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.organization.ws;

import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationQuery;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.Organizations.Organization;

import static java.util.Collections.emptySet;
import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.db.Pagination.forPage;
import static org.sonar.db.organization.OrganizationQuery.newOrganizationQueryBuilder;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.Common.Paging;

public class SearchAction implements OrganizationsWsAction {
  static final String PARAM_ORGANIZATIONS = "organizations";
  static final String PARAM_MEMBER = "member";
  private static final String ACTION = "search";
  private static final int MAX_SIZE = 500;

  private final DbClient dbClient;
  private final UserSession userSession;
  private final OrganizationsWsSupport wsSupport;

  public SearchAction(DbClient dbClient, UserSession userSession, OrganizationsWsSupport wsSupport) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.wsSupport = wsSupport;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setPost(false)
      .setDescription("Search for organizations")
      .setResponseExample(getClass().getResource("search-example.json"))
      .setInternal(true)
      .setSince("6.2")
      .setChangelog(new Change("6.4", "Paging fields have been added to the response"))
      .setHandler(this);

    action.createParam(PARAM_ORGANIZATIONS)
      .setDescription("Comma-separated list of organization keys")
      .setExampleValue(String.join(",", "my-org-1", "foocorp"))
      .setMaxValuesAllowed(MAX_SIZE)
      .setRequired(false)
      .setSince("6.3");

    action.createParam(PARAM_MEMBER)
      .setDescription("Filter organizations based on whether the authenticated user is a member. If false, no filter applies.")
      .setSince("7.0")
      .setDefaultValue(String.valueOf(false))
      .setBooleanPossibleValues();

    action.addPagingParams(100, MAX_SIZE);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationQuery dbQuery = buildDbQuery(request);
      int total = dbClient.organizationDao().countByQuery(dbSession, dbQuery);
      Paging paging = buildWsPaging(request, total);
      List<OrganizationDto> organizations = dbClient.organizationDao().selectByQuery(dbSession, dbQuery, forPage(paging.getPageIndex()).andSize(paging.getPageSize()));
      Set<String> adminOrganizationUuids = searchOrganizationWithAdminPermission(dbSession);
      writeResponse(request, response, organizations, adminOrganizationUuids, paging);
    }
  }

  private OrganizationQuery buildDbQuery(Request request) {
    return newOrganizationQueryBuilder()
      .setKeys(request.paramAsStrings(PARAM_ORGANIZATIONS))
      .setMember(getUserIdIfFilterOnMembership(request))
      .build();
  }

  private Set<String> searchOrganizationWithAdminPermission(DbSession dbSession) {
    Integer userId = userSession.getUserId();
    return userId == null ? emptySet()
      : dbClient.organizationDao().selectByPermission(dbSession, userId, ADMINISTER.getKey()).stream().map(OrganizationDto::getUuid).collect(toSet());
  }

  private void writeResponse(Request httpRequest, Response httpResponse, List<OrganizationDto> organizations, Set<String> adminOrganizationUuids, Paging paging) {
    Organizations.SearchWsResponse.Builder response = Organizations.SearchWsResponse.newBuilder();
    response.setPaging(paging);
    Organization.Builder wsOrganization = Organization.newBuilder();
    organizations
      .forEach(o -> {
        boolean isAdmin = adminOrganizationUuids.contains(o.getUuid());
        wsOrganization.clear();
        wsOrganization.setIsAdmin(isAdmin);
        response.addOrganizations(wsSupport.toOrganization(wsOrganization, o));
      });
    writeProtobuf(response.build(), httpRequest, httpResponse);
  }

  private static Paging buildWsPaging(Request request, int total) {
    return Paging.newBuilder()
      .setPageIndex(request.mandatoryParamAsInt(Param.PAGE))
      .setPageSize(request.mandatoryParamAsInt(Param.PAGE_SIZE))
      .setTotal(total)
      .build();
  }

  @CheckForNull
  private Integer getUserIdIfFilterOnMembership(Request request) {
    boolean filterOnAuthenticatedUser = request.mandatoryParamAsBoolean(PARAM_MEMBER);
    return (userSession.isLoggedIn() && filterOnAuthenticatedUser) ? userSession.getUserId() : null;
  }
}
