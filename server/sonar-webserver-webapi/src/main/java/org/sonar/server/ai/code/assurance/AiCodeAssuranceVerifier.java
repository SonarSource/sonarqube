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
package org.sonar.server.ai.code.assurance;

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.core.platform.EditionProvider;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.exceptions.NotFoundException;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;

/**
 * Make sure that for {@link EditionProvider.Edition#COMMUNITY} we'll always get false or {@link AiCodeAssurance#NONE}, no matter of the
 * value in database.
 * This is to support correctly downgraded instances.
 */
public class AiCodeAssuranceVerifier {
  private final DbClient dbClient;
  private final AiCodeAssuranceEntitlement entitlement;

  enum QualityGateStatus {
    OK, ERROR
  }

  public AiCodeAssuranceVerifier(AiCodeAssuranceEntitlement entitlement, DbClient dbClient) {
    this.dbClient = dbClient;
    this.entitlement = entitlement;
  }

  public AiCodeAssurance getAiCodeAssurance(ProjectDto projectDto, @Nullable String branchKey) {
    if (branchKey == null) {
      return getAiCodeAssurance(projectDto);
    } else {
      return getAiCodeAssuranceForBranch(projectDto, branchKey);
    }
  }

  public AiCodeAssurance getAiCodeAssurance(ProjectDto projectDto) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return getAiCodeAssurance(dbSession, projectDto, getMainBranch(dbSession, projectDto).getUuid());
    }
  }

  public boolean isAiCodeAssured(ProjectDto projectDto) {
    return getAiCodeAssurance(projectDto).isAiCodeAssured();
  }

  private AiCodeAssurance getAiCodeAssuranceForBranch(ProjectDto projectDto, String branchKey) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      BranchDto branch =
        dbClient.branchDao().selectByBranchKey(dbSession, projectDto.getUuid(), branchKey)
          .orElseThrow(() -> new NotFoundException("Branch " + branchKey + " does not exist for project " + projectDto.getUuid()));
      return getAiCodeAssurance(dbSession, projectDto, branch.getUuid());
    }
  }

  private AiCodeAssurance getAiCodeAssurance(DbSession dbSession, ProjectDto projectDto, String branchUuid) {
    if (!entitlement.isEnabled() || !projectDto.getContainsAiCode()) {
      return AiCodeAssurance.NONE;
    }
    QualityGateDto qualityGate = dbClient.qualityGateDao().selectByProjectUuid(dbSession, projectDto.getUuid());
    if (qualityGate == null) {
      qualityGate = dbClient.qualityGateDao().selectDefault(dbSession);
    }
    if (qualityGate == null || !qualityGate.isAiCodeSupported()) {
      return AiCodeAssurance.AI_CODE_ASSURANCE_OFF;
    }
    Optional<MeasureDto> qualityGateMeasure = dbClient.measureDao().selectByComponentUuidAndMetricKeys(dbSession, branchUuid,
      singletonList(ALERT_STATUS_KEY));
    if (qualityGateMeasure.isEmpty()) {
      return AiCodeAssurance.AI_CODE_ASSURANCE_ON;
    }
    String qualityGateStatus = qualityGateMeasure.get().getString(ALERT_STATUS_KEY);
    if (qualityGateStatus == null) {
      return AiCodeAssurance.AI_CODE_ASSURANCE_ON;
    }
    if (QualityGateStatus.OK.name().equals(qualityGateStatus)) {
      return AiCodeAssurance.AI_CODE_ASSURANCE_PASS;
    }
    return AiCodeAssurance.AI_CODE_ASSURANCE_FAIL;
  }

  private BranchDto getMainBranch(DbSession dbSession, ProjectDto project) {
    return dbClient.branchDao().selectMainBranchByProjectUuid(dbSession, project.getUuid())
      .orElseThrow(() -> new NotFoundException(format("Main branch in project '%s' is not found", project.getKey())));
  }
}
