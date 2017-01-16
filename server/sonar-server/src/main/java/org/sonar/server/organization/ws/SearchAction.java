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
package org.sonar.server.organization.ws;

import java.util.List;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.Paging;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationQuery;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.Organizations.Organization;

import static org.sonar.db.organization.OrganizationQuery.newOrganizationQueryBuilder;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SearchAction implements OrganizationsAction {
  private static final String PARAM_ORGANIZATIONS = "organizations";
  private static final String ACTION = "search";

  private final DbClient dbClient;
  private final OrganizationsWsSupport wsSupport;

  public SearchAction(DbClient dbClient, OrganizationsWsSupport wsSupport) {
    this.dbClient = dbClient;
    this.wsSupport = wsSupport;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setPost(false)
      .setDescription("Search for organizations")
      .setResponseExample(getClass().getResource("example-search.json"))
      .setInternal(true)
      .setSince("6.2")
      .setHandler(this);

    action.createParam(PARAM_ORGANIZATIONS)
      .setDescription("Comma-separated list of organization keys")
      .setExampleValue(String.join(",", "my-org-1", "foocorp"))
      .setRequired(false)
      .setSince("6.3");

    action.addPagingParams(25);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Paging paging = Paging.forPageIndex(request.mandatoryParamAsInt(Param.PAGE))
        .withPageSize(request.mandatoryParamAsInt(Param.PAGE_SIZE))
        .andTotal(0);
      OrganizationQuery organizationQuery = newOrganizationQueryBuilder()
        .setKeys(request.paramAsStrings(PARAM_ORGANIZATIONS))
        .build();

      List<OrganizationDto> dtos = dbClient.organizationDao().selectByQuery(
        dbSession,
        organizationQuery,
        paging.offset(),
        paging.pageSize());

      writeResponse(request, response, dtos);
    }
  }

  private void writeResponse(Request request, Response response, List<OrganizationDto> dtos) {
    Organizations.SearchWsResponse.Builder responseBuilder = Organizations.SearchWsResponse.newBuilder();
    Organization.Builder organizationBuilder = Organization.newBuilder();
    dtos.forEach(dto -> responseBuilder.addOrganizations(wsSupport.toOrganization(organizationBuilder, dto)));
    writeProtobuf(responseBuilder.build(), request, response);
  }

}
