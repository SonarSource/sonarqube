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
package org.sonar.server.badge.ws;

import com.google.common.io.Resources;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import org.sonar.api.measures.Metric.Level;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.write;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.server.badge.ws.ETagUtils.RFC1123_DATE;
import static org.sonar.server.badge.ws.ETagUtils.getETag;
import static org.sonarqube.ws.MediaTypes.SVG;

public class QualityGateAction implements ProjectBadgesWsAction  {

  private final DbClient dbClient;
  private final ProjectBadgesSupport support;
  private final SvgGenerator svgGenerator;

  public QualityGateAction(DbClient dbClient, ProjectBadgesSupport support, SvgGenerator svgGenerator) {
    this.dbClient = dbClient;
    this.support = support;
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
    support.addProjectAndBranchParams(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    response.setHeader("Cache-Control", "no-cache");
    response.stream().setMediaType(SVG);
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto project = support.getComponent(dbSession, request);
      Level qualityGateStatus = getQualityGate(dbSession, project);
      String result = svgGenerator.generateQualityGate(qualityGateStatus);
      String eTag = getETag(result);
      Optional<String> requestedETag = request.header("If-None-Match");
      if (requestedETag.filter(eTag::equals).isPresent()) {
        response.stream().setStatus(304);
        return;
      }
      response.setHeader("ETag", eTag);
      write(result, response.stream().output(), UTF_8);
    } catch (ProjectBadgesException | ForbiddenException | NotFoundException e) {
      // There is an issue, so do not return any ETag but make this response expire now
      SimpleDateFormat sdf = new SimpleDateFormat(RFC1123_DATE, Locale.US);
      response.setHeader("Expires", sdf.format(new Date()));
      write(svgGenerator.generateError(e.getMessage()), response.stream().output(), UTF_8);
    }
  }

  private Level getQualityGate(DbSession dbSession, ComponentDto project) {
    return Level.valueOf(dbClient.liveMeasureDao().selectMeasure(dbSession, project.uuid(), ALERT_STATUS_KEY)
      .map(LiveMeasureDto::getTextValue)
      .orElseThrow(() -> new ProjectBadgesException("Quality gate has not been found")));
  }

}
