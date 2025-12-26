/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.v2.api.dop.jfrog.service;

import java.util.Optional;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.measure.ProjectMeasureDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.dop.jfrog.JFrogEvidenceSonarQubeFeature;
import org.sonar.server.v2.api.dop.jfrog.response.SonarQubePredicate;
import org.sonar.server.v2.api.dop.jfrog.response.SonarQubeStatement;

public class JFrogEvidenceHandlerImpl implements JFrogEvidenceHandler {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final JFrogEvidenceMarkdownService markdownService;
  private final JFrogEvidenceSonarQubeFeature feature;

  public JFrogEvidenceHandlerImpl(DbClient dbClient, UserSession userSession, JFrogEvidenceMarkdownService markdownService,
    JFrogEvidenceSonarQubeFeature feature) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.markdownService = markdownService;
    this.feature = feature;
  }

  @Override
  public SonarQubeStatement getEvidence(String taskId) {
    checkFeatureAvailable();
    try (DbSession dbSession = dbClient.openSession(false)) {
      CeActivityDto ceActivity = getCeActivity(dbSession, taskId);
      ProjectDto project = getProject(dbSession, ceActivity);
      checkPermission(project);

      String analysisUuid = getAnalysisUuid(ceActivity, taskId);
      String componentUuid = getComponentUuid(ceActivity, taskId);

      Optional<String> qualityGateDetails = loadQualityGateDetails(dbSession, analysisUuid, componentUuid);

      SonarQubePredicate predicate = QualityGateDetailsParser.parse(qualityGateDetails.orElse(null));
      String markdown = markdownService.generateMarkdown(predicate);

      return SonarQubeStatement.create(predicate, markdown);
    }
  }

  private void checkFeatureAvailable() {
    if (!feature.isAvailable()) {
      throw new ForbiddenException("JFrog evidence is only available in Enterprise Edition and above");
    }
  }

  private CeActivityDto getCeActivity(DbSession dbSession, String taskId) {
    return dbClient.ceActivityDao().selectByUuid(dbSession, taskId)
      .orElseThrow(() -> new NotFoundException(String.format("Task '%s' not found", taskId)));
  }

  private ProjectDto getProject(DbSession dbSession, CeActivityDto ceActivity) {
    String entityUuid = Optional.ofNullable(ceActivity.getEntityUuid())
      .orElseThrow(() -> new NotFoundException(String.format("Project not found for task '%s'", ceActivity.getUuid())));

    return dbClient.projectDao().selectByUuid(dbSession, entityUuid)
      .orElseThrow(() -> new NotFoundException(String.format("Project '%s' not found", entityUuid)));
  }

  private void checkPermission(ProjectDto project) {
    if (!userSession.hasEntityPermission(ProjectPermission.ADMIN, project) &&
      !userSession.hasEntityPermission(ProjectPermission.USER, project) &&
      !userSession.hasEntityPermission(ProjectPermission.SCAN, project) &&
      !userSession.hasPermission(GlobalPermission.SCAN)) {
      throw new ForbiddenException("Insufficient privileges");
    }
  }

  private static String getAnalysisUuid(CeActivityDto ceActivity, String taskId) {
    return Optional.ofNullable(ceActivity.getAnalysisUuid())
      .orElseThrow(() -> new NotFoundException(String.format("Analysis not found for task '%s'", taskId)));
  }

  private static String getComponentUuid(CeActivityDto ceActivity, String taskId) {
    return Optional.ofNullable(ceActivity.getComponentUuid())
      .orElseThrow(() -> new NotFoundException(String.format("Component not found for task '%s'", taskId)));
  }

  private Optional<String> loadQualityGateDetails(DbSession dbSession, String analysisUuid, String componentUuid) {
    return dbClient.projectMeasureDao().selectMeasure(dbSession, analysisUuid, componentUuid, CoreMetrics.QUALITY_GATE_DETAILS_KEY)
      .map(ProjectMeasureDto::getData);
  }
}
