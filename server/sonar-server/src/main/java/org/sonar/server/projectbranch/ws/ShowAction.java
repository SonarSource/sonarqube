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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.WsUtils;
import org.sonarqube.ws.WsBranches;
import org.sonarqube.ws.WsBranches.Branch.Status;
import org.sonarqube.ws.WsBranches.ShowWsResponse;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.BUGS_KEY;
import static org.sonar.api.measures.CoreMetrics.CODE_SMELLS_KEY;
import static org.sonar.api.measures.CoreMetrics.VULNERABILITIES_KEY;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.core.util.stream.MoreCollectors.index;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.db.component.BranchType.LONG;
import static org.sonar.db.component.BranchType.SHORT;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_FILE_EXAMPLE_001;
import static org.sonarqube.ws.Common.BranchType;
import static org.sonarqube.ws.client.projectbranches.ProjectBranchesParameters.ACTION_SHOW;
import static org.sonarqube.ws.client.projectbranches.ProjectBranchesParameters.PARAM_BRANCH;
import static org.sonarqube.ws.client.projectbranches.ProjectBranchesParameters.PARAM_COMPONENT;

public class ShowAction implements BranchWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public ShowAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_SHOW)
      .setSince("6.6")
      .setDescription("Show branch information of a project")
      .setResponseExample(Resources.getResource(getClass(), "show-example.json"))
      .setInternal(true)
      .setHandler(this);

    action
      .createParam(PARAM_COMPONENT)
      .setDescription("Component key")
      .setExampleValue(KEY_FILE_EXAMPLE_001)
      .setRequired(true);

    action
      .createParam(PARAM_BRANCH)
      .setDescription("Branch key")
      .setExampleValue(KEY_BRANCH_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String projectKey = request.mandatoryParam(PARAM_COMPONENT);
    String branchName = request.param(PARAM_BRANCH);

    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto component = loadComponent(dbSession, projectKey, branchName);
      userSession.checkComponentPermission(UserRole.USER, component);

      List<MetricDto> metrics = dbClient.metricDao().selectByKeys(dbSession, asList(ALERT_STATUS_KEY, BUGS_KEY, VULNERABILITIES_KEY, CODE_SMELLS_KEY));
      Map<Integer, MetricDto> metricsById = metrics.stream().collect(uniqueIndex(MetricDto::getId));
      Map<String, Integer> metricIdsByKey = metrics.stream().collect(uniqueIndex(MetricDto::getKey, MetricDto::getId));

      BranchDto branch = getBranch(dbSession, component.projectUuid());
      String mergeBranchUuid = branch.getMergeBranchUuid();
      BranchDto mergeBranch = mergeBranchUuid == null ? null : getBranch(dbSession, mergeBranchUuid);

      Multimap<String, MeasureDto> measuresByComponentUuids = dbClient.measureDao()
        .selectByComponentsAndMetrics(dbSession, Collections.singletonList(branch.getUuid()), metricsById.keySet())
        .stream().collect(index(MeasureDto::getComponentUuid));

      WsUtils.writeProtobuf(buildResponse(branch, mergeBranch, metricIdsByKey, measuresByComponentUuids), request, response);
    }
  }

  private ComponentDto loadComponent(DbSession dbSession, String projectKey, @Nullable String branchName){
    if (branchName == null) {
      return componentFinder.getByKey(dbSession, projectKey);
    }
    return componentFinder.getByKeyAndBranch(dbSession, projectKey, branchName);
  }

  private BranchDto getBranch(DbSession dbSession, String uuid) {
    Optional<BranchDto> branch = dbClient.branchDao().selectByUuid(dbSession, uuid);
    checkState(branch.isPresent(), "Branch uuid '%s' not found", uuid);
    return branch.get();
  }

  private static ShowWsResponse buildResponse(BranchDto branch, @Nullable BranchDto mergeBranch,
    Map<String, Integer> metricIdsByKey, Multimap<String, MeasureDto> measuresByComponentUuids) {
    WsBranches.Branch.Builder builder = WsBranches.Branch.newBuilder();
    setNullable(branch.getKey(), builder::setName);
    builder.setIsMain(branch.isMain());
    builder.setType(BranchType.valueOf(branch.getBranchType().name()));
    if (mergeBranch != null) {
      setNullable(mergeBranch.getKey(), builder::setMergeBranch);
    }

    Status.Builder statusBuilder = Status.newBuilder();
    Collection<MeasureDto> componentMeasures = measuresByComponentUuids.get(branch.getUuid());
    if (branch.getBranchType().equals(LONG)) {
      int qualityGateStatusMetricId = metricIdsByKey.get(ALERT_STATUS_KEY);
      componentMeasures.stream().filter(m -> m.getMetricId() == qualityGateStatusMetricId).findAny()
        .ifPresent(measure -> statusBuilder.setQualityGateStatus(measure.getData()));
    }

    if (branch.getBranchType().equals(SHORT)) {
      int bugsMetricId = metricIdsByKey.get(BUGS_KEY);
      componentMeasures.stream().filter(m -> m.getMetricId() == bugsMetricId).findAny()
        .ifPresent(measure -> statusBuilder.setBugs(measure.getValue().intValue()));

      int vulnerabilitiesMetricId = metricIdsByKey.get(VULNERABILITIES_KEY);
      componentMeasures.stream().filter(m -> m.getMetricId() == vulnerabilitiesMetricId).findAny()
        .ifPresent(measure -> statusBuilder.setVulnerabilities(measure.getValue().intValue()));

      int codeSmellMetricId = metricIdsByKey.get(CODE_SMELLS_KEY);
      componentMeasures.stream().filter(m -> m.getMetricId() == codeSmellMetricId).findAny()
        .ifPresent(measure -> statusBuilder.setCodeSmells(measure.getValue().intValue()));
    }

    builder.setStatus(statusBuilder);
    return ShowWsResponse.newBuilder().setBranch(builder).build();
  }

}
