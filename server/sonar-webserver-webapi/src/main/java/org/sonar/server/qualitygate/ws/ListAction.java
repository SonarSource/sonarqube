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

import com.google.common.io.Resources;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
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
import org.sonar.server.qualitygate.QualityGateFinder;
import org.sonar.server.qualitygate.QualityGateModeChecker;
import org.sonarqube.ws.Qualitygates.ListWsResponse;
import org.sonarqube.ws.Qualitygates.ListWsResponse.QualityGate;

import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ListAction implements QualityGatesWsAction {

  private final DbClient dbClient;
  private final QualityGatesWsSupport wsSupport;
  private final QualityGateFinder finder;
  private final QualityGateCaycChecker qualityGateCaycChecker;
  private final QualityGateModeChecker qualityGateModeChecker;

  public ListAction(DbClient dbClient, QualityGatesWsSupport wsSupport, QualityGateFinder finder, QualityGateCaycChecker qualityGateCaycChecker,
    QualityGateModeChecker qualityGateModeChecker) {
    this.dbClient = dbClient;
    this.wsSupport = wsSupport;
    this.finder = finder;
    this.qualityGateCaycChecker = qualityGateCaycChecker;
    this.qualityGateModeChecker = qualityGateModeChecker;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("list")
      .setDescription("Get a list of quality gates")
      .setSince("4.3")
      .setResponseExample(Resources.getResource(this.getClass(), "list-example.json"))
      .setChangelog(
        new Change("10.8", "'isAiCodeSupported' field is added on quality gate"),
        new Change("10.8", "'hasMQRConditions' and 'hasStandardConditions' fields are added on quality gate"),
        new Change("10.0", "Field 'default' in the response has been removed"),
        new Change("10.0", "Field 'id' in the response has been removed"),
        new Change("9.9", "'caycStatus' field is added on quality gate"),
        new Change("8.4", "Field 'id' in the response is deprecated. Format changes from integer to string."),
        new Change("7.0", "'isDefault' field is added on quality gate"),
        new Change("7.0", "'default' field on root level is deprecated"),
        new Change("7.0", "'isBuiltIn' field is added in the response"),
        new Change("7.0", "'actions' fields are added in the response"))
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      QualityGateDto defaultQualityGate = finder.getDefault(dbSession);
      Collection<QualityGateDto> qualityGates = dbClient.qualityGateDao().selectAll(dbSession);

      writeProtobuf(buildResponse(dbSession, qualityGates, defaultQualityGate), request, response);
    }
  }

  private ListWsResponse buildResponse(DbSession dbSession, Collection<QualityGateDto> qualityGates, @Nullable QualityGateDto defaultQualityGate) {
    String defaultUuid = defaultQualityGate == null ? null : defaultQualityGate.getUuid();
    ListWsResponse.Builder builder = ListWsResponse.newBuilder()
      .setActions(ListWsResponse.RootActions.newBuilder().setCreate(wsSupport.isQualityGateAdmin()))
      .addAllQualitygates(qualityGates.stream()
        .map(qualityGate -> {
          Collection<QualityGateConditionDto> conditions = dbClient.gateConditionDao().selectForQualityGate(dbSession, qualityGate.getUuid());
          List<MetricDto> metrics = getMetricsFromConditions(dbSession, conditions);
          QualityGateModeChecker.QualityModeResult qualityModeResult = qualityGateModeChecker.getUsageOfModeMetrics(metrics);
          return QualityGate.newBuilder()
            .setName(qualityGate.getName())
            .setIsDefault(qualityGate.getUuid().equals(defaultUuid))
            .setIsBuiltIn(qualityGate.isBuiltIn())
            .setIsAiCodeSupported(qualityGate.isAiCodeSupported())
            .setCaycStatus(qualityGateCaycChecker.checkCaycCompliant(conditions, metrics).toString())
            .setHasMQRConditions(qualityModeResult.hasMQRConditions())
            .setHasStandardConditions(qualityModeResult.hasStandardConditions())
            .setActions(wsSupport.getActions(dbSession, qualityGate, defaultQualityGate))
            .build();
        })
        .toList());
    return builder.build();
  }

  @NotNull
  private List<MetricDto> getMetricsFromConditions(DbSession dbSession, Collection<QualityGateConditionDto> conditions) {
    return dbClient.metricDao().selectByUuids(dbSession, conditions.stream().map(QualityGateConditionDto::getMetricUuid).collect(Collectors.toSet()))
      .stream()
      .filter(MetricDto::isEnabled)
      .toList();
  }

}
