/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.qualitygate.QualityGateCaycChecker;
import org.sonar.server.qualitygate.QualityGateCaycStatus;
import org.sonar.server.qualitygate.QualityGateFinder;
import org.sonarqube.ws.Qualitygates.ShowWsResponse;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Optional.ofNullable;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_NAME;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ShowAction implements QualityGatesWsAction {

  private final DbClient dbClient;
  private final QualityGateFinder qualityGateFinder;
  private final QualityGatesWsSupport wsSupport;
  private final QualityGateCaycChecker qualityGateCaycChecker;

  public ShowAction(DbClient dbClient, QualityGateFinder qualityGateFinder, QualityGatesWsSupport wsSupport, QualityGateCaycChecker qualityGateCaycChecker) {
    this.dbClient = dbClient;
    this.qualityGateFinder = qualityGateFinder;
    this.wsSupport = wsSupport;
    this.qualityGateCaycChecker = qualityGateCaycChecker;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("show")
      .setDescription("Display the details of a quality gate")
      .setSince("4.3")
      .setResponseExample(Resources.getResource(this.getClass(), "show-example.json"))
      .setChangelog(
        new Change("10.0", "Field 'id' in the response has been removed"),
        new Change("10.0", "Parameter 'id' is removed. Use 'name' instead."),
        new Change("9.9", "'caycStatus' field is added to the response"),
        new Change("8.4", "Parameter 'id' is deprecated. Format changes from integer to string. Use 'name' instead."),
        new Change("8.4", "Field 'id' in the response is deprecated."),
        new Change("7.6", "'period' and 'warning' fields of conditions are removed from the response"),
        new Change("7.0", "'isBuiltIn' field is added to the response"),
        new Change("7.0", "'actions' field is added in the response"))
      .setHandler(this);

    action.createParam(PARAM_NAME)
      .setRequired(true)
      .setDescription("Name of the quality gate. Either id or name must be set")
      .setExampleValue("My Quality Gate");
  }

  @Override
  public void handle(Request request, Response response) {
    String name = request.mandatoryParam(PARAM_NAME);

    try (DbSession dbSession = dbClient.openSession(false)) {
      QualityGateDto qualityGate = wsSupport.getByName(dbSession, name);
      Collection<QualityGateConditionDto> conditions = getConditions(dbSession, qualityGate);
      Map<String, MetricDto> metricsByUuid = getMetricsByUuid(dbSession, conditions);
      QualityGateDto defaultQualityGate = qualityGateFinder.getDefault(dbSession);
      QualityGateCaycStatus caycStatus = qualityGateCaycChecker.checkCaycCompliant(dbSession, qualityGate.getUuid());
      writeProtobuf(buildResponse(dbSession, qualityGate, defaultQualityGate, conditions, metricsByUuid, caycStatus), request, response);
    }
  }

  public Collection<QualityGateConditionDto> getConditions(DbSession dbSession, QualityGateDto qualityGate) {
    return dbClient.gateConditionDao().selectForQualityGate(dbSession, qualityGate.getUuid());
  }

  private Map<String, MetricDto> getMetricsByUuid(DbSession dbSession, Collection<QualityGateConditionDto> conditions) {
    Set<String> metricUuids = conditions.stream().map(QualityGateConditionDto::getMetricUuid).collect(Collectors.toSet());
    return dbClient.metricDao().selectByUuids(dbSession, metricUuids).stream().filter(MetricDto::isEnabled).collect(Collectors.toMap(MetricDto::getUuid, Function.identity()));
  }

  private ShowWsResponse buildResponse(DbSession dbSession, QualityGateDto qualityGate, QualityGateDto defaultQualityGate,
    Collection<QualityGateConditionDto> conditions, Map<String, MetricDto> metricsByUuid, QualityGateCaycStatus caycStatus) {
    return ShowWsResponse.newBuilder()
      .setName(qualityGate.getName())
      .setIsBuiltIn(qualityGate.isBuiltIn())
      .setCaycStatus(caycStatus.toString())
      .addAllConditions(conditions.stream()
        .map(toWsCondition(metricsByUuid))
        .toList())
      .setActions(wsSupport.getActions(dbSession, qualityGate, defaultQualityGate))
      .build();
  }

  private Function<QualityGateConditionDto, ShowWsResponse.Condition> toWsCondition(Map<String, MetricDto> metricsByUuid) {
    return condition -> {
      String metricUuid = condition.getMetricUuid();
      MetricDto metric = metricsByUuid.get(metricUuid);
      checkState(metric != null, "Could not find metric with id %s", metricUuid);
      ShowWsResponse.Condition.Builder builder = ShowWsResponse.Condition.newBuilder()
        .setId(condition.getUuid())
        .setMetric(metric.getKey())
        .setIsCaycCondition(qualityGateCaycChecker.isCaycCondition(metric))
        .setOp(condition.getOperator());
      ofNullable(condition.getErrorThreshold()).ifPresent(builder::setError);
      return builder.build();
    };
  }

}
