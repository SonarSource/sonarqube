/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.sticker.ws;

import com.google.common.io.Resources;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.io.IOUtils.copy;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;

public class QualityGateAction implements StickersWsAction {

  private static final String PARAM_COMPONENT = "component";
  private static final String PARAM_BRANCH = "branch";
  private static final String PARAM_TYPE = "type";

  private static final String TYPE_BADGE = "BADGE";
  private static final String TYPE_CARD = "CARD";

  private static final String SVG_QUALITY_GATE_PASSING = "quality_gate-passing.svg";
  private static final String SVG_QUALITY_GATE_WARNING = "quality_gate-warning.svg";
  private static final String SVG_QUALITY_GATE_FAILING = "quality_gate-failing.svg";
  private static final String SVG_NOT_FOUND = "not_found.svg";
  private static final String SVG_UNAUTHORIZED = "unauthorized.svg";

  private static final String STATUS_OK = "OK";
  private static final String STATUS_WARNING = "WARN";
  private static final String STATUS_ERROR = "ERROR";

  private final UserSession userSession;
  private final DbClient dbClient;

  public QualityGateAction(UserSession userSession, DbClient dbClient) {
    this.userSession = userSession;
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction("quality_gate")
      .setHandler(this)
      .setDescription("Generate badge for quality gate as an SVG.<br/>" +
        "Requires the 'Browse' permission on the project.")
      .setResponseExample(Resources.getResource(getClass(), "quality_gate-example.svg"));
    action.createParam(PARAM_COMPONENT)
      .setDescription("Project key. When project does not exist, the badge will contain 'not found'.")
      .setRequired(true)
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
    action
      .createParam(PARAM_BRANCH)
      .setDescription("Branch key")
      .setExampleValue(KEY_BRANCH_EXAMPLE_001);
    action.createParam(PARAM_TYPE)
      .setDescription("Type of badge.")
      .setPossibleValues(asList(TYPE_BADGE, TYPE_CARD));
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    response.stream().setMediaType("image/svg+xml");
    try (DbSession dbSession = dbClient.openSession(false)) {
      String projectKey = request.mandatoryParam(PARAM_COMPONENT);
      String branch = request.param(PARAM_BRANCH);
      Optional<ComponentDto> project = getProject(dbSession, projectKey, branch);
      copy(Resources.getResource(getClass(), "templates/" + generateSvg(dbSession, project)).openStream(), response.stream().output());
    }
  }

  private String generateSvg(DbSession dbSession, Optional<ComponentDto> projectOpt) {
    if (!projectOpt.isPresent()) {
      return SVG_NOT_FOUND;
    }
    ComponentDto project = projectOpt.get();
    if (!userSession.hasComponentPermission(USER, project)) {
      return SVG_UNAUTHORIZED;
    }
    Optional<String> qualityGateOpt = getQualityGate(dbSession, project);
    if (!qualityGateOpt.isPresent()) {
      return SVG_NOT_FOUND;
    }
    String qualityGate = qualityGateOpt.get();
    switch (qualityGate) {
      case STATUS_OK:
        return SVG_QUALITY_GATE_PASSING;
      case STATUS_WARNING:
        return SVG_QUALITY_GATE_WARNING;
      case STATUS_ERROR:
        return SVG_QUALITY_GATE_FAILING;
      default:
        throw new IllegalStateException(format("Invalid quality gate '%s'", qualityGate));
    }
  }

  private Optional<ComponentDto> getProject(DbSession dbSession, String projectKey, @Nullable String branch) {
    return branch == null ? Optional.ofNullable(dbClient.componentDao().selectByKey(dbSession, projectKey).orNull())
      : dbClient.componentDao().selectByKeyAndBranch(dbSession, projectKey, branch);
  }

  private Optional<String> getQualityGate(DbSession dbSession, ComponentDto project) {
    Optional<LiveMeasureDto> measure = dbClient.liveMeasureDao().selectMeasure(dbSession, project.uuid(), ALERT_STATUS_KEY);
    return measure.map(LiveMeasureDto::getTextValue);
  }

}
