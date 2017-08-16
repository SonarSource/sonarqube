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
package org.sonar.server.projectbranch.ws;

import com.google.common.collect.Multimap;
import com.google.common.io.Resources;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchKeyType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.WsUtils;
import org.sonarqube.ws.WsBranches;
import org.sonarqube.ws.WsBranches.Branch.Status;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.BUGS_KEY;
import static org.sonar.api.measures.CoreMetrics.CODE_SMELLS_KEY;
import static org.sonar.api.measures.CoreMetrics.VULNERABILITIES_KEY;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.core.util.stream.MoreCollectors.index;
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.db.component.BranchType.LONG;
import static org.sonar.db.component.BranchType.SHORT;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.checkFoundWithOptional;
import static org.sonarqube.ws.Common.BranchType;
import static org.sonarqube.ws.client.projectbranches.ProjectBranchesParameters.ACTION_LIST;
import static org.sonarqube.ws.client.projectbranches.ProjectBranchesParameters.PARAM_PROJECT;

public class ListAction implements BranchWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;

  public ListAction(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_LIST)
      .setSince("6.6")
      .setDescription("List the branches of a project")
      .setResponseExample(Resources.getResource(getClass(), "list-example.json"))
      .setInternal(true)
      .setHandler(this);

    action
      .createParam(PARAM_PROJECT)
      .setDescription("Project key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001)
      .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String projectKey = request.mandatoryParam(PARAM_PROJECT);

    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto project = checkFoundWithOptional(
        dbClient.componentDao().selectByKey(dbSession, projectKey),
        "Project key '%s' not found", projectKey);

      userSession.checkComponentPermission(UserRole.USER, project);
      checkArgument(project.isEnabled() && PROJECT.equals(project.qualifier()), "Invalid project key");

      List<MetricDto> metrics = dbClient.metricDao().selectByKeys(dbSession, asList(ALERT_STATUS_KEY, BUGS_KEY, VULNERABILITIES_KEY, CODE_SMELLS_KEY));
      Map<Integer, MetricDto> metricsById = metrics.stream().collect(uniqueIndex(MetricDto::getId));
      Map<String, Integer> metricIdsByKey = metrics.stream().collect(uniqueIndex(MetricDto::getKey, MetricDto::getId));

      Collection<BranchDto> branches = dbClient.branchDao().selectByComponent(dbSession, project);
      Map<String, BranchDto> mergeBranchesByUuid = dbClient.branchDao()
        .selectByUuids(dbSession, branches.stream().map(BranchDto::getMergeBranchUuid).filter(Objects::nonNull).collect(toList()))
        .stream().collect(uniqueIndex(BranchDto::getUuid));
      Multimap<String, MeasureDto> measuresByComponentUuids = dbClient.measureDao()
        .selectByComponentsAndMetrics(dbSession, branches.stream().map(BranchDto::getUuid).collect(toList()), metricsById.keySet())
        .stream().collect(index(MeasureDto::getComponentUuid));

      WsBranches.ListWsResponse.Builder protobufResponse = WsBranches.ListWsResponse.newBuilder();
      branches.stream()
        .filter(b -> b.getKeeType().equals(BranchKeyType.BRANCH))
        .forEach(b -> addToProtobuf(protobufResponse, b, mergeBranchesByUuid, metricIdsByKey, measuresByComponentUuids));
      WsUtils.writeProtobuf(protobufResponse.build(), request, response);
    }
  }

  private static void addToProtobuf(WsBranches.ListWsResponse.Builder response, BranchDto branch, Map<String, BranchDto> mergeBranchesByUuid,
    Map<String, Integer> metricIdsByKey, Multimap<String, MeasureDto> measuresByComponentUuids) {
    WsBranches.Branch.Builder builder = response.addBranchesBuilder();
    setNullable(branch.getKey(), builder::setName);
    builder.setIsMain(branch.isMain());
    builder.setType(BranchType.valueOf(branch.getBranchType().name()));
    String mergeBranchUuid = branch.getMergeBranchUuid();
    if (mergeBranchUuid != null) {
      BranchDto mergeBranch = mergeBranchesByUuid.get(mergeBranchUuid);
      checkState(mergeBranch != null, "Component uuid '%s' cannot be found", mergeBranch);
      setNullable(mergeBranch.getKey(), builder::setMergeBranch);
    }

    Collection<MeasureDto> componentMeasures = measuresByComponentUuids.get(branch.getUuid());
    if (branch.getBranchType().equals(LONG)) {
      Status.Builder statusBuilder = Status.newBuilder();
      int qualityGateStatusMetricId = metricIdsByKey.get(ALERT_STATUS_KEY);
      componentMeasures.stream().filter(m -> m.getMetricId() == qualityGateStatusMetricId).findAny()
        .ifPresent(measure -> builder.setStatus(statusBuilder.setQualityGateStatus(measure.getData())));
    }

    if (branch.getBranchType().equals(SHORT)) {
      Status.Builder statusBuilder = Status.newBuilder();
      int bugsMetricId = metricIdsByKey.get(BUGS_KEY);
      componentMeasures.stream().filter(m -> m.getMetricId() == bugsMetricId).findAny()
        .ifPresent(measure -> statusBuilder.setBugs(measure.getValue().intValue()));

      int vulnerabilitiesMetricId = metricIdsByKey.get(VULNERABILITIES_KEY);
      componentMeasures.stream().filter(m -> m.getMetricId() == vulnerabilitiesMetricId).findAny()
        .ifPresent(measure -> statusBuilder.setVulnerabilities(measure.getValue().intValue()));

      int codeSmellMetricId = metricIdsByKey.get(CODE_SMELLS_KEY);
      componentMeasures.stream().filter(m -> m.getMetricId() == codeSmellMetricId).findAny()
        .ifPresent(measure -> statusBuilder.setCodeSmells(measure.getValue().intValue()));
      builder.setStatus(statusBuilder);
    }
    builder.build();
  }

}
