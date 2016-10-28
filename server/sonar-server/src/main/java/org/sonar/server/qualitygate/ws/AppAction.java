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

import java.util.Collection;
import java.util.stream.Collectors;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.WsQualityGates.AppWsResponse.Metric;

import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.measures.Metric.ValueType.RATING;
import static org.sonar.core.permission.GlobalPermissions.QUALITY_GATE_ADMIN;
import static org.sonar.server.qualitygate.ValidRatingMetrics.isCoreRatingMetric;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.WsQualityGates.AppWsResponse;

public class AppAction implements QualityGatesWsAction {

  private final UserSession userSession;
  private final DbClient dbClient;

  public AppAction(UserSession userSession, DbClient dbClient) {
    this.userSession = userSession;
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("app")
      .setInternal(true)
      .setDescription("Get initialization items for the admin UI. For internal use")
      .setResponseExample(getClass().getResource("app-example.json"))
      .setSince("4.3")
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) {
    writeProtobuf(AppWsResponse.newBuilder()
      .setEdit(userSession.hasPermission(QUALITY_GATE_ADMIN))
      .addAllMetrics(loadMetrics()
        .stream()
        .map(AppAction::toMetric)
        .collect(Collectors.toList()))
      .build(),
      request, response);
  }

  private static Metric toMetric(MetricDto metricDto) {
    String domain = metricDto.getDomain();
    Metric.Builder metricBuilder = Metric.newBuilder()
      .setKey(metricDto.getKey())
      .setName(metricDto.getShortName())
      .setType(metricDto.getValueType())
      .setHidden(metricDto.isHidden());
    if (domain != null) {
      metricBuilder.setDomain(domain);
    }
    return metricBuilder.build();
  }

  private Collection<MetricDto> loadMetrics() {
    DbSession dbSession = dbClient.openSession(false);
    try {
      return dbClient.metricDao().selectEnabled(dbSession).stream()
        .filter(metric -> !metric.isDataType() && !ALERT_STATUS_KEY.equals(metric.getKey()) &&
          (!RATING.name().equals(metric.getValueType()) || isCoreRatingMetric(metric.getKey())))
        .collect(Collectors.toList());
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

}
