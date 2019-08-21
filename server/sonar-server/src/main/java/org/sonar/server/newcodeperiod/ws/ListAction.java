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
package org.sonar.server.newcodeperiod.ws;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.newcodeperiod.NewCodePeriodDao;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.NewCodePeriods;
import org.sonarqube.ws.NewCodePeriods.ListWSResponse;

import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.db.component.BranchType.LONG;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.NewCodePeriods.ShowWSResponse.newBuilder;

public class ListAction implements NewCodePeriodsWsAction {
  private static final String PARAM_PROJECT = "project";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;
  private final NewCodePeriodDao newCodePeriodDao;

  public ListAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder, NewCodePeriodDao newCodePeriodDao) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
    this.newCodePeriodDao = newCodePeriodDao;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("list")
      .setDescription("List the New Code Periods for all long lived branches in a project.<br>" +
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
      ComponentDto project = componentFinder.getByKey(dbSession, projectKey);
      userSession.checkComponentPermission(UserRole.ADMIN, project);
      Collection<BranchDto> branches = dbClient.branchDao().selectByComponent(dbSession, project).stream()
        .filter(b -> b.getBranchType() == LONG)
        .collect(toList());
      Map<String, InheritedNewCodePeriod> newCodePeriodByBranchUuid = newCodePeriodDao
        .selectAllByProject(dbSession, project.uuid())
        .stream()
        .collect(Collectors.toMap(NewCodePeriodDto::getBranchUuid, dto -> new InheritedNewCodePeriod(dto, dto.getBranchUuid() == null)));

      InheritedNewCodePeriod projectDefault = newCodePeriodByBranchUuid.getOrDefault(null,
        newCodePeriodDao.selectGlobal(dbSession)
          .map(dto -> new InheritedNewCodePeriod(dto, true))
          .orElse(new InheritedNewCodePeriod(NewCodePeriodDto.defaultInstance(), true))
      );

      ListWSResponse.Builder builder = ListWSResponse.newBuilder();
      for (BranchDto branch : branches) {
        InheritedNewCodePeriod inherited = newCodePeriodByBranchUuid.getOrDefault(branch.getUuid(), projectDefault);
        builder.addNewCodePeriods(
          build(projectKey, branch.getKey(), inherited.getType(), inherited.getValue(), inherited.inherited));
      }

      writeProtobuf(builder.build(), request, response);
    }
  }

  private NewCodePeriods.ShowWSResponse build(String projectKey, String branchKey, NewCodePeriodType newCodePeriodType, @Nullable String value, boolean inherited) {
    NewCodePeriods.ShowWSResponse.Builder builder = newBuilder()
      .setType(convertType(newCodePeriodType))
      .setInherited(inherited)
      .setBranchKey(branchKey)
      .setProjectKey(projectKey);

    if (value != null) {
      builder.setValue(value);
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
      default:
        throw new IllegalStateException("Unexpected type: " + type);
    }
  }

  private static class InheritedNewCodePeriod {
    NewCodePeriodDto newCodePeriod;
    boolean inherited;

    InheritedNewCodePeriod(NewCodePeriodDto newCodePeriod, boolean inherited) {
      this.newCodePeriod = newCodePeriod;
      this.inherited = inherited;
    }

    NewCodePeriodType getType() {
      return newCodePeriod.getType();
    }

    String getValue() {
      return newCodePeriod.getValue();
    }
  }
}
