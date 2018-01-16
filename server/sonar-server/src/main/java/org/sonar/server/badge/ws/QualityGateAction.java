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
package org.sonar.server.badge.ws;

import com.google.common.io.Resources;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric.Level;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.write;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonarqube.ws.MediaTypes.SVG;

public class QualityGateAction implements ProjectBadgesWsAction {

  private static final String PARAM_PROJECT = "project";
  private static final String PARAM_BRANCH = "branch";

  private final UserSession userSession;
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final SvgGenerator svgGenerator;

  public QualityGateAction(UserSession userSession, DbClient dbClient, ComponentFinder componentFinder, SvgGenerator svgGenerator) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.svgGenerator = svgGenerator;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction("quality_gate")
      .setHandler(this)
      .setSince("7.1")
      .setDescription("Generate badge for project's quality gate as an SVG.<br/>" +
        "Requires 'Browse' permission on the specified project.")
      .setResponseExample(Resources.getResource(getClass(), "quality_gate-example.svg"));
    action.createParam(PARAM_PROJECT)
      .setDescription("Project key")
      .setRequired(true)
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
    action
      .createParam(PARAM_BRANCH)
      .setDescription("Branch key")
      .setExampleValue(KEY_BRANCH_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    response.stream().setMediaType(SVG);
    String projectKey = request.mandatoryParam(PARAM_PROJECT);
    String branch = request.param(PARAM_BRANCH);
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto project = componentFinder.getByKeyAndOptionalBranch(dbSession, projectKey, branch);
      userSession.checkComponentPermission(USER, project);
      Level qualityGateStatus = getQualityGate(dbSession, project);
      write(svgGenerator.generateQualityGate(qualityGateStatus), response.stream().output(), UTF_8);
    } catch (ProjectBadgesException | ForbiddenException | NotFoundException e) {
      write(svgGenerator.generateError(e.getMessage()), response.stream().output(), UTF_8);
    }
  }

  private Level getQualityGate(DbSession dbSession, ComponentDto project) {
    return Level.valueOf(dbClient.liveMeasureDao().selectMeasure(dbSession, project.uuid(), CoreMetrics.ALERT_STATUS_KEY)
      .map(LiveMeasureDto::getTextValue)
      .orElseThrow(() -> new ProjectBadgesException(format("Quality gate has not been found for project '%s' and branch '%s'", project.getKey(), project.getBranch()))));
  }

}
