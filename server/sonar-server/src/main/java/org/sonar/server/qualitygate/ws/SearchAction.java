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
package org.sonar.server.qualitygate.ws;

import com.google.common.io.Resources;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.qualitygate.ProjectQgateAssociation;
import org.sonar.db.qualitygate.ProjectQgateAssociationQuery;
import org.sonar.server.qualitygate.QgateProjectFinder;
import org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters;

public class SearchAction implements QualityGatesWsAction {

  private final QgateProjectFinder projectFinder;

  public SearchAction(QgateProjectFinder projectFinder) {
    this.projectFinder = projectFinder;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("search")
      .setDescription("Search for projects associated (or not) to a quality gate.<br/>" +
        "Only authorized projects for current user will be returned.")
      .setSince("4.3")
      .setResponseExample(Resources.getResource(this.getClass(), "example-search.json"))
      .setHandler(this);

    action.createParam(QualityGatesWsParameters.PARAM_GATE_ID)
      .setDescription("Quality Gate ID")
      .setRequired(true)
      .setExampleValue("1");

    action.createParam(QualityGatesWsParameters.PARAM_QUERY)
      .setDescription("To search for projects containing this string. If this parameter is set, \"selected\" is set to \"all\".")
      .setExampleValue("abc");

    action.addSelectionModeParam();

    action.createParam(QualityGatesWsParameters.PARAM_PAGE)
      .setDescription("Page number")
      .setDefaultValue("1")
      .setExampleValue("2");

    action.createParam(QualityGatesWsParameters.PARAM_PAGE_SIZE)
      .setDescription("Page size")
      .setExampleValue("10");
  }

  @Override
  public void handle(Request request, Response response) {
    QgateProjectFinder.Association associations = projectFinder.find(ProjectQgateAssociationQuery.builder()
      .gateId(request.mandatoryParam(QualityGatesWsParameters.PARAM_GATE_ID))
      .membership(request.param(QualityGatesWsParameters.PARAM_QUERY) == null ? request.param(Param.SELECTED) : ProjectQgateAssociationQuery.ANY)
      .projectSearch(request.param(QualityGatesWsParameters.PARAM_QUERY))
      .pageIndex(request.paramAsInt(QualityGatesWsParameters.PARAM_PAGE))
      .pageSize(request.paramAsInt(QualityGatesWsParameters.PARAM_PAGE_SIZE))
      .build());
    JsonWriter writer = response.newJsonWriter();
    writer.beginObject().prop("more", associations.hasMoreResults());
    writer.name("results").beginArray();

    for (ProjectQgateAssociation project : associations.projects()) {
      writer.beginObject().prop("id", project.id()).prop("name", project.name()).prop(Param.SELECTED, project.isMember()).endObject();
    }
    writer.endArray().endObject().close();
  }

}
