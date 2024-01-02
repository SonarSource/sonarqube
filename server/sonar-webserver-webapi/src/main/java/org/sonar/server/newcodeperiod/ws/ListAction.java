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
package org.sonar.server.newcodeperiod.ws;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.web.UserRole;
import org.sonar.core.documentation.DocumentationLinkGenerator;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.newcodeperiod.NewCodePeriodDao;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.NewCodePeriods;
import org.sonarqube.ws.NewCodePeriods.ListWSResponse;

import static org.sonar.server.ws.WsUtils.createHtmlExternalLink;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.NewCodePeriods.ShowWSResponse.newBuilder;

public class ListAction implements NewCodePeriodsWsAction {
  private static final String PARAM_PROJECT = "project";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;
  private final NewCodePeriodDao newCodePeriodDao;
  private final String newCodeDefinitionDocumentationUrl;

  public ListAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder, NewCodePeriodDao newCodePeriodDao,
    DocumentationLinkGenerator documentationLinkGenerator) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
    this.newCodePeriodDao = newCodePeriodDao;
    this.newCodeDefinitionDocumentationUrl = documentationLinkGenerator.getDocumentationLink("/project-administration/defining-new-code/");
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("list")
      .setDescription("Lists the " + createHtmlExternalLink(newCodeDefinitionDocumentationUrl, "new code definition") +
        " for all branches in a project.<br>" +
        "Requires the permission to browse the project")
      .setSince("8.0")
      .setResponseExample(getClass().getResource("list-example.json"))
      .setHandler(this);

    action.createParam(PARAM_PROJECT)
      .setRequired(true)
      .setDescription("Project key");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String projectKey = request.mandatoryParam(PARAM_PROJECT);

    try (DbSession dbSession = dbClient.openSession(false)) {
      ProjectDto project = componentFinder.getProjectByKey(dbSession, projectKey);
      userSession.checkEntityPermission(UserRole.USER, project);
      Collection<BranchDto> branches = dbClient.branchDao().selectByProject(dbSession, project).stream()
        .filter(b -> b.getBranchType() == BranchType.BRANCH)
        .sorted(Comparator.comparing(BranchDto::getKey))
        .toList();

      List<NewCodePeriodDto> newCodePeriods = newCodePeriodDao.selectAllByProject(dbSession, project.getUuid());

      Map<String, NewCodePeriodDto> newCodePeriodByBranchUuid = newCodePeriods
        .stream()
        .collect(Collectors.toMap(NewCodePeriodDto::getBranchUuid, Function.identity()));

      NewCodePeriodDto projectDefault = newCodePeriodByBranchUuid.getOrDefault(null, getGlobalOrDefault(dbSession));

      Map<String, String> analysis = newCodePeriods.stream()
        .filter(newCodePeriodDto -> newCodePeriodDto.getType().equals(NewCodePeriodType.SPECIFIC_ANALYSIS))
        .collect(Collectors.toMap(NewCodePeriodDto::getUuid, NewCodePeriodDto::getValue));

      Map<String, Long> analysisUuidDateMap = dbClient.snapshotDao().selectByUuids(dbSession, new HashSet<>(analysis.values()))
        .stream()
        .collect(Collectors.toMap(SnapshotDto::getUuid, SnapshotDto::getCreatedAt));

      ListWSResponse.Builder builder = ListWSResponse.newBuilder();
      for (BranchDto branch : branches) {
        NewCodePeriodDto newCodePeriod = newCodePeriodByBranchUuid.getOrDefault(branch.getUuid(), projectDefault);

        String effectiveValue = null;

        //handles specific analysis only
        Long analysisDate = analysisUuidDateMap.get(analysis.get(newCodePeriod.getUuid()));
        if (analysisDate != null) {
          effectiveValue = DateUtils.formatDateTime(analysisDate);
        }

        builder.addNewCodePeriods(
          build(projectKey, branch.getKey(), newCodePeriod, effectiveValue));
      }

      writeProtobuf(builder.build(), request, response);
    }
  }

  private NewCodePeriodDto getGlobalOrDefault(DbSession dbSession) {
    return newCodePeriodDao.selectGlobal(dbSession).orElse(NewCodePeriodDto.defaultInstance());
  }

  private static NewCodePeriods.ShowWSResponse build(String projectKey, String branchKey, NewCodePeriodDto ncd, @Nullable String effectiveValue) {
    boolean inherited = ncd.getBranchUuid() == null;
    NewCodePeriods.ShowWSResponse.Builder builder = newBuilder()
      .setType(convertType(ncd.getType()))
      .setInherited(inherited)
      .setBranchKey(branchKey)
      .setProjectKey(projectKey)
      .setUpdatedAt(ncd.getUpdatedAt());

    if (effectiveValue != null) {
      builder.setEffectiveValue(effectiveValue);
    }

    if (ncd.getValue() != null) {
      builder.setValue(ncd.getValue());
    }

    if (!inherited && ncd.getPreviousNonCompliantValue() != null) {
      builder.setPreviousNonCompliantValue(ncd.getPreviousNonCompliantValue());
    }

    return builder.build();
  }

  private static NewCodePeriods.NewCodePeriodType convertType(NewCodePeriodType type) {
    switch (type) {
      case NUMBER_OF_DAYS:
        return NewCodePeriods.NewCodePeriodType.NUMBER_OF_DAYS;
      case PREVIOUS_VERSION:
        return NewCodePeriods.NewCodePeriodType.PREVIOUS_VERSION;
      case SPECIFIC_ANALYSIS:
        return NewCodePeriods.NewCodePeriodType.SPECIFIC_ANALYSIS;
      case REFERENCE_BRANCH:
        return NewCodePeriods.NewCodePeriodType.REFERENCE_BRANCH;
      default:
        throw new IllegalStateException("Unexpected type: " + type);
    }
  }

}
