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
package org.sonar.server.qualitygate.ws;

import com.google.common.io.Resources;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualitygate.ProjectQgateAssociation;
import org.sonar.db.qualitygate.ProjectQgateAssociationQuery;
import org.sonar.server.qualitygate.QgateProjectFinder;
import org.sonarqube.ws.Qualitygates;

import static org.sonar.api.server.ws.WebService.Param.SELECTED;
import static org.sonar.db.qualitygate.ProjectQgateAssociationQuery.ANY;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_GATE_ID;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_PAGE;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_PAGE_SIZE;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_QUERY;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SearchAction implements QualityGatesWsAction {

  private final DbClient dbClient;
  private final QgateProjectFinder projectFinder;
  private final QualityGatesWsSupport wsSupport;

  public SearchAction(DbClient dbClient, QgateProjectFinder projectFinder, QualityGatesWsSupport wsSupport) {
    this.dbClient = dbClient;
    this.projectFinder = projectFinder;
    this.wsSupport = wsSupport;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("search")
      .setDescription("Search for projects associated (or not) to a quality gate.<br/>" +
        "Only authorized projects for current user will be returned.")
      .setSince("4.3")
      .setResponseExample(Resources.getResource(this.getClass(), "search-example.json"))
      .setHandler(this);

    action.createParam(PARAM_GATE_ID)
      .setDescription("Quality Gate ID")
      .setRequired(true)
      .setExampleValue("1");

    action.createParam(PARAM_QUERY)
      .setDescription("To search for projects containing this string. If this parameter is set, \"selected\" is set to \"all\".")
      .setExampleValue("abc");

    action.addSelectionModeParam();

    action.createParam(PARAM_PAGE)
      .setDescription("Page number")
      .setDefaultValue("1")
      .setExampleValue("2");

    action.createParam(PARAM_PAGE_SIZE)
      .setDescription("Page size")
      .setExampleValue("10");

    wsSupport.createOrganizationParam(action);
  }

  @Override
  public void handle(Request request, Response response) {

    try (DbSession dbSession = dbClient.openSession(false)) {

      OrganizationDto organization = wsSupport.getOrganization(dbSession, request);

      QgateProjectFinder.Association associations = projectFinder.find(dbSession, organization,
        ProjectQgateAssociationQuery.builder()
          .gateId(request.mandatoryParam(PARAM_GATE_ID))
          .membership(request.param(PARAM_QUERY) == null ? request.param(SELECTED) : ANY)
          .projectSearch(request.param(PARAM_QUERY))
          .pageIndex(request.paramAsInt(PARAM_PAGE))
          .pageSize(request.paramAsInt(PARAM_PAGE_SIZE))
          .build());

      Qualitygates.SearchResponse.Builder createResponse = Qualitygates.SearchResponse.newBuilder()
        .setMore(associations.hasMoreResults());

      for (ProjectQgateAssociation project : associations.projects()) {
        createResponse.addResultsBuilder()
          .setId(project.id())
          .setName(project.name())
          .setSelected(project.isMember());
      }

      writeProtobuf(createResponse.build(), request, response);

    }
  }
}
