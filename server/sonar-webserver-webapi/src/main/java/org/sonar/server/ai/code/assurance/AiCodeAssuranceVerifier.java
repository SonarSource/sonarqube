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

import org.sonar.core.platform.EditionProvider;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.qualitygate.QualityGateDto;

import static org.sonar.core.platform.EditionProvider.Edition.COMMUNITY;

/**
 * Make sure that for {@link EditionProvider.Edition#COMMUNITY} we'll always get false or {@link AiCodeAssurance#NONE}, no matter of the
 * value in database.
 * This is to support correctly downgraded instances.
 */
public class AiCodeAssuranceVerifier {
  private final boolean isSupported;
  private final DbClient dbClient;

  public AiCodeAssuranceVerifier(PlatformEditionProvider editionProvider, DbClient dbClient) {
    this.dbClient = dbClient;
    this.isSupported = editionProvider.get().map(edition -> !edition.equals(COMMUNITY)).orElse(false);
  }

  public AiCodeAssurance getAiCodeAssurance(ProjectDto projectDto) {
    if (!isSupported || !projectDto.getContainsAiCode()) {
      return AiCodeAssurance.NONE;
    }
    try (DbSession dbSession = dbClient.openSession(false)) {
      QualityGateDto qualityGate = dbClient.qualityGateDao().selectByProjectUuid(dbSession, projectDto.getUuid());
      if (qualityGate == null) {
        qualityGate = dbClient.qualityGateDao().selectDefault(dbSession);
      }
      if (qualityGate != null && qualityGate.isAiCodeSupported()) {
        return AiCodeAssurance.AI_CODE_ASSURED;
      }
      return AiCodeAssurance.CONTAINS_AI_CODE;
    }
  }

  public boolean isAiCodeAssured(ProjectDto projectDto) {
    return AiCodeAssurance.AI_CODE_ASSURED.equals(getAiCodeAssurance(projectDto));
  }

  public AiCodeAssurance getAiCodeAssurance(boolean containsAiCode, boolean aiCodeSupportedQg) {
    if (!isSupported || !containsAiCode) {
      return AiCodeAssurance.NONE;
    }
    if (aiCodeSupportedQg) {
      return AiCodeAssurance.AI_CODE_ASSURED;
    }
    return AiCodeAssurance.CONTAINS_AI_CODE;
  }
}
