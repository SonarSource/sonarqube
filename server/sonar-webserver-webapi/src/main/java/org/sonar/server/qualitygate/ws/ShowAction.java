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
package org.sonar.server.qualitygate.ws;

import com.google.common.io.Resources;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.qualitygate.QualityGateFinder;
import org.sonarqube.ws.Qualitygates.ShowWsResponse;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Optional.ofNullable;
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ID;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_NAME;
import static org.sonar.server.ws.WsUtils.checkFound;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ShowAction implements QualityGatesWsAction {

  private final DbClient dbClient;
  private final QualityGateFinder qualityGateFinder;
  private final QualityGatesWsSupport wsSupport;

  public ShowAction(DbClient dbClient, QualityGateFinder qualityGateFinder, QualityGatesWsSupport wsSupport) {
    this.dbClient = dbClient;
    this.qualityGateFinder = qualityGateFinder;
    this.wsSupport = wsSupport;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("show")
      .setDescription("Display the details of a quality gate")
      .setSince("4.3")
      .setResponseExample(Resources.getResource(this.getClass(), "show-example.json"))
      .setChangelog(
        new Change("7.6", "'period' and 'warning' fields of conditions are removed from the response"),
        new Change("7.0", "'isBuiltIn' field is added to the response"),
        new Change("7.0", "'actions' field is added in the response"))
      .setHandler(this);

    action.createParam(PARAM_ID)
      .setDescription("ID of the quality gate. Either id or name must be set")
      .setExampleValue("1");

    action.createParam(PARAM_NAME)
      .setDescription("Name of the quality gate. Either id or name must be set")
      .setExampleValue("My Quality Gate");

    wsSupport.createOrganizationParam(action);
  }

  @Override
  public void handle(Request request, Response response) {
    Long id = request.paramAsLong(PARAM_ID);
    String name = request.param(PARAM_NAME);
    checkOneOfIdOrNamePresent(id, name);

    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = wsSupport.getOrganization(dbSession, request);
      QualityGateDto qualityGate = getByNameOrId(dbSession, organization, name, id);
      Collection<QualityGateConditionDto> conditions = getConditions(dbSession, qualityGate);
      Map<Integer, MetricDto> metricsById = getMetricsById(dbSession, conditions);
      QualityGateDto defaultQualityGate = qualityGateFinder.getDefault(dbSession, organization);
      writeProtobuf(buildResponse(organization, qualityGate, defaultQualityGate, conditions, metricsById), request, response);
    }
  }

  private QualityGateDto getByNameOrId(DbSession dbSession, OrganizationDto organization, @Nullable String name, @Nullable Long id) {
    if (name != null) {
      return checkFound(dbClient.qualityGateDao().selectByOrganizationAndName(dbSession, organization, name), "No quality gate has been found for name %s", name);
    }
    if (id != null) {
      return wsSupport.getByOrganizationAndId(dbSession, organization, id);
    }
    throw new IllegalArgumentException("No parameter has been set to identify a quality gate");
  }

  public Collection<QualityGateConditionDto> getConditions(DbSession dbSession, QualityGateDto qualityGate) {
    return dbClient.gateConditionDao().selectForQualityGate(dbSession, qualityGate.getId());
  }

  private Map<Integer, MetricDto> getMetricsById(DbSession dbSession, Collection<QualityGateConditionDto> conditions) {
    Set<Integer> metricIds = conditions.stream().map(c -> (int) c.getMetricId()).collect(toSet());
    return dbClient.metricDao().selectByIds(dbSession, metricIds).stream()
      .filter(MetricDto::isEnabled)
      .collect(uniqueIndex(MetricDto::getId));
  }

  private ShowWsResponse buildResponse(OrganizationDto organization, QualityGateDto qualityGate, QualityGateDto defaultQualityGate,
    Collection<QualityGateConditionDto> conditions, Map<Integer, MetricDto> metricsById) {
    return ShowWsResponse.newBuilder()
      .setId(qualityGate.getId())
      .setName(qualityGate.getName())
      .setIsBuiltIn(qualityGate.isBuiltIn())
      .addAllConditions(conditions.stream()
        .map(toWsCondition(metricsById))
        .collect(toList()))
      .setActions(wsSupport.getActions(organization, qualityGate, defaultQualityGate))
      .build();
  }

  private static Function<QualityGateConditionDto, ShowWsResponse.Condition> toWsCondition(Map<Integer, MetricDto> metricsById) {
    return condition -> {
      int metricId = (int) condition.getMetricId();
      MetricDto metric = metricsById.get(metricId);
      checkState(metric != null, "Could not find metric with id %s", metricId);
      ShowWsResponse.Condition.Builder builder = ShowWsResponse.Condition.newBuilder()
        .setId(condition.getId())
        .setMetric(metric.getKey())
        .setOp(condition.getOperator());
      ofNullable(condition.getErrorThreshold()).ifPresent(builder::setError);
      return builder.build();
    };
  }

  private static void checkOneOfIdOrNamePresent(@Nullable Long qGateId, @Nullable String qGateName) {
    checkArgument(qGateId == null ^ qGateName == null, "Either '%s' or '%s' must be provided", PARAM_ID, PARAM_NAME);
  }
}
