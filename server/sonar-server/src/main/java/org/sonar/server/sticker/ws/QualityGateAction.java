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
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.commons.io.IOUtils.write;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.server.sticker.ws.QualityGateAction.SvgError.checkError;
import static org.sonar.server.sticker.ws.SvgGenerator.Color.GREEN;
import static org.sonar.server.sticker.ws.SvgGenerator.Color.ORANGE;
import static org.sonar.server.sticker.ws.SvgGenerator.Color.RED;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;

public class QualityGateAction implements StickersWsAction {

  private static final String PARAM_COMPONENT = "component";
  private static final String PARAM_BRANCH = "branch";
  private static final String PARAM_TYPE = "type";

  private static final String TYPE_BADGE = "BADGE";
  private static final String TYPE_CARD = "CARD";

  private static final String LABEL_QUALITY_GATE = "Quality Gate";

  private static final String MESSAGE_SUCCESS = "Success";
  private static final String MESSAGE_WARNING = "Warning";
  private static final String MESSAGE_FAILED = "Failed";

  private static final String STATUS_OK = "OK";
  private static final String STATUS_WARNING = "WARN";
  private static final String STATUS_ERROR = "ERROR";

  private final UserSession userSession;
  private final DbClient dbClient;
  private final SvgGenerator svgGenerator;

  public QualityGateAction(UserSession userSession, DbClient dbClient, SvgGenerator svgGenerator) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.svgGenerator = svgGenerator;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction("quality_gate")
      .setHandler(this)
      .setSince("7.1")
      .setDescription("Generate badge for quality gate as an SVG.<br/>" +
        "When user is has not enough permission to access the project, the badge will contain 'Insufficient privileges'.")
      .setResponseExample(Resources.getResource(getClass(), "quality_gate-example.svg"));
    action.createParam(PARAM_COMPONENT)
      .setDescription("Project key. When project does not exist, the badge will contain 'Project not found'.")
      .setRequired(true)
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
    action
      .createParam(PARAM_BRANCH)
      .setDescription("Branch key. When branch does not exist, the badge will contain 'Branch not found'. Short living branches have not quality gate.")
      .setExampleValue(KEY_BRANCH_EXAMPLE_001);
    action.createParam(PARAM_TYPE)
      .setDescription("Type of badge.")
      .setPossibleValues(asList(TYPE_BADGE, TYPE_CARD));
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    response.stream().setMediaType("image/svg+xml");
    String projectKey = request.mandatoryParam(PARAM_COMPONENT);
    String branch = request.param(PARAM_BRANCH);
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto project = getProject(dbSession, projectKey, branch);
      userSession.checkComponentPermission(USER, project);
      write(generateSvg(dbSession, project), response.stream().output(), UTF_8);
    } catch (SvgError | ForbiddenException e) {
      write(svgGenerator.generateError(e.getMessage()), response.stream().output(), UTF_8);
    }
  }

  private String generateSvg(DbSession dbSession, ComponentDto project) {
    String qualityGate = getQualityGate(dbSession, project);
    switch (qualityGate) {
      case STATUS_OK:
        return svgGenerator.generateBadge(LABEL_QUALITY_GATE, MESSAGE_SUCCESS, GREEN);
      case STATUS_WARNING:
        return svgGenerator.generateBadge(LABEL_QUALITY_GATE, MESSAGE_WARNING, ORANGE);
      case STATUS_ERROR:
        return svgGenerator.generateBadge(LABEL_QUALITY_GATE, MESSAGE_FAILED, RED);
      default:
        throw new IllegalStateException(format("Invalid quality gate '%s'", qualityGate));
    }
  }

  private ComponentDto getProject(DbSession dbSession, String projectKey, @Nullable String branchKey) {
    if (branchKey == null) {
      return dbClient.componentDao().selectByKey(dbSession, projectKey).or(() -> {
        throw new SvgError(format("Project '%s' does not exist", projectKey));
      });
    } else {
      ComponentDto project = dbClient.componentDao().selectByKeyAndBranch(dbSession, projectKey, branchKey)
        .orElseThrow(() -> new SvgError(format("Branch '%s' does not exist", branchKey)));
      String projectUuid = project.uuid();
      BranchDto branch = dbClient.branchDao().selectByUuid(dbSession, projectUuid)
        .orElseThrow(() -> new IllegalStateException(format("Branch with uuid '%s' does not exist", projectUuid)));
      checkError(branch.getBranchType().equals(BranchType.LONG), "Short branch has no quality gate");
      return project;
    }
  }

  private String getQualityGate(DbSession dbSession, ComponentDto project) {
    Optional<LiveMeasureDto> measure = dbClient.liveMeasureDao().selectMeasure(dbSession, project.uuid(), ALERT_STATUS_KEY);
    Optional<String> value = measure.map(LiveMeasureDto::getTextValue);
    return value.orElseThrow(() -> new IllegalStateException(format("No quality gate found for project '%s' and branch '%s'", project.getKey(), project.getBranch())));
  }

  static class SvgError extends RuntimeException {
    SvgError(String message) {
      super(message);
    }

    static void checkError(boolean expression, @Nullable Object errorMessage) {
      if (!expression) {
        throw new SvgError(String.valueOf(errorMessage));
      }
    }
  }
}
