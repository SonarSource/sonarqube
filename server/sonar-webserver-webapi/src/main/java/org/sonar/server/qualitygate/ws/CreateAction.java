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
package org.sonar.server.qualitygate.ws;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import org.sonar.api.measures.Metric;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.qualitygate.Condition;
import org.sonar.server.qualitygate.QualityGateConditionsUpdater;
import org.sonar.server.qualitygate.QualityGateUpdater;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Qualitygates.CreateResponse;

import static org.sonar.api.measures.CoreMetrics.NEW_COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.Metric.DIRECTION_BETTER;
import static org.sonar.api.measures.Metric.DIRECTION_WORST;
import static org.sonar.server.qualitygate.Condition.Operator.GREATER_THAN;
import static org.sonar.server.qualitygate.Condition.Operator.LESS_THAN;
import static org.sonar.server.qualitygate.QualityGateCaycChecker.CAYC_METRICS;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.ACTION_CREATE;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_NAME;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class CreateAction implements QualityGatesWsAction {

  static final Map<String, Integer> DEFAULT_METRIC_VALUES = Map.of(
    NEW_COVERAGE_KEY, 80,
    NEW_DUPLICATED_LINES_DENSITY_KEY, 3
  );

  private static final Map<Integer, Condition.Operator> OPERATORS_BY_DIRECTION = Map.of(
    DIRECTION_BETTER, LESS_THAN,
    DIRECTION_WORST, GREATER_THAN);

  public static final int NAME_MAXIMUM_LENGTH = 100;

  private final DbClient dbClient;
  private final UserSession userSession;
  private final QualityGateUpdater qualityGateUpdater;
  private final QualityGateConditionsUpdater qualityGateConditionsUpdater;

  public CreateAction(DbClient dbClient, UserSession userSession, QualityGateUpdater qualityGateUpdater, QualityGateConditionsUpdater qualityGateConditionsUpdater) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.qualityGateUpdater = qualityGateUpdater;
    this.qualityGateConditionsUpdater = qualityGateConditionsUpdater;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION_CREATE)
      .setPost(true)
      .setDescription("Create a Quality Gate.<br>" +
        "Requires the 'Administer Quality Gates' permission.")
      .setSince("4.3")
      .setChangelog(
        new Change("8.4", "Field 'id' in the response is deprecated. Format changes from integer to string."))
      .setResponseExample(getClass().getResource("create-example.json"))
      .setHandler(this);

    action.createParam(PARAM_NAME)
      .setRequired(true)
      .setMaximumLength(NAME_MAXIMUM_LENGTH)
      .setDescription("The name of the quality gate to create")
      .setExampleValue("My Quality Gate");
  }

  @Override
  public void handle(Request request, Response response) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      userSession.checkPermission(GlobalPermission.ADMINISTER_QUALITY_GATES);

      String name = request.mandatoryParam(PARAM_NAME);

      QualityGateDto newQualityGate = qualityGateUpdater.create(dbSession, name);
      addCaycConditions(dbSession, newQualityGate);

      CreateResponse.Builder createResponse = CreateResponse.newBuilder()
        .setId(newQualityGate.getUuid())
        .setName(newQualityGate.getName());
      dbSession.commit();
      writeProtobuf(createResponse.build(), request, response);
    }
  }

  private void addCaycConditions(DbSession dbSession, QualityGateDto newQualityGate) {
    CAYC_METRICS.forEach(m ->
      qualityGateConditionsUpdater.createCondition(dbSession, newQualityGate, m.getKey(), OPERATORS_BY_DIRECTION.get(m.getDirection()).getDbValue(),
        String.valueOf(getDefaultCaycValue(m)))
    );
  }

  private static int getDefaultCaycValue(Metric<? extends Serializable> metric) {
    return DEFAULT_METRIC_VALUES.containsKey(metric.getKey()) ?
      DEFAULT_METRIC_VALUES.get(metric.getKey()) :
      Objects.requireNonNull(metric.getBestValue()).intValue();
  }
}
