/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.qualitygate.db.ProjectQgateAssociation;
import org.sonar.core.qualitygate.db.ProjectQgateAssociationQuery;
import org.sonar.server.qualitygate.QgateProjectFinder;

public class QGatesSearchAction implements BaseQGateWsAction {

  private final QgateProjectFinder projectFinder;

  public QGatesSearchAction(QgateProjectFinder projectFinder) {
    this.projectFinder = projectFinder;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("search")
      .setDescription("Search for projects associated (or not) to a quality gate")
      .setSince("4.3")
      .setResponseExample(Resources.getResource(this.getClass(), "example-search.json"))
      .setHandler(this);

    action.createParam(QGatesWs.PARAM_GATE_ID)
      .setDescription("Quality Gate ID")
      .setRequired(true)
      .setExampleValue("1");

    action.createParam(QGatesWs.PARAM_QUERY)
      .setDescription("To search for projects containing this string. If this parameter is set, \"selected\" is set to \"all\".")
      .setExampleValue("abc");

    action.createParam(QGatesWs.PARAM_SELECTED)
      .setDescription("If \"selected\", search for projects associated to the quality gate")
      .setDefaultValue(ProjectQgateAssociationQuery.IN)
      .setPossibleValues(ProjectQgateAssociationQuery.AVAILABLE_MEMBERSHIP)
      .setExampleValue(ProjectQgateAssociationQuery.OUT);

    action.createParam(QGatesWs.PARAM_PAGE)
      .setDescription("Page number")
      .setDefaultValue("1")
      .setExampleValue("2");

    action.createParam(QGatesWs.PARAM_PAGE_SIZE)
      .setDescription("Page size")
      .setExampleValue("10");
  }

  @Override
  public void handle(Request request, Response response) {
    QgateProjectFinder.Association associations = projectFinder.find(ProjectQgateAssociationQuery.builder()
      .gateId(request.mandatoryParam(QGatesWs.PARAM_GATE_ID))
      .membership(request.param(QGatesWs.PARAM_QUERY) == null ? request.param(QGatesWs.PARAM_SELECTED) : ProjectQgateAssociationQuery.ANY)
      .projectSearch(request.param(QGatesWs.PARAM_QUERY))
      .pageIndex(request.paramAsInt(QGatesWs.PARAM_PAGE))
      .pageSize(request.paramAsInt(QGatesWs.PARAM_PAGE_SIZE))
      .build());
    JsonWriter writer = response.newJsonWriter();
    writer.beginObject().prop("more", associations.hasMoreResults());
    writer.name("results").beginArray();
    for (ProjectQgateAssociation project : associations.projects()) {
      writer.beginObject().prop("id", project.id()).prop("name", project.name()).prop(QGatesWs.PARAM_SELECTED, project.isMember()).endObject();
    }
    writer.endArray().endObject().close();
  }

}
