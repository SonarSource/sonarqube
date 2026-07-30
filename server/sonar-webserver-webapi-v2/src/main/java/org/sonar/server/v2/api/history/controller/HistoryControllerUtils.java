/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.v2.api.history.controller;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.user.UserSession;
import org.sonarsource.history.HistoryUtils;
import org.sonarsource.history.model.EntityType;
import org.springframework.lang.Nullable;

import static org.sonar.server.exceptions.NotFoundException.checkFoundWithOptional;

public final class HistoryControllerUtils {

  private HistoryControllerUtils() {
    // static methods only
  }

  static EntityType toEntityType(String entityType) {
    try {
      return EntityType.valueOf(entityType);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Unknown history entity type: " + entityType);
    }
  }

  public static void checkPermission(UserSession userSession, DbClient dbClient, String entityId, EntityType entityType) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      if (EntityType.PROJECT_BRANCH.equals(entityType)) {
        BranchDto branch = checkFoundWithOptional(
          dbClient.branchDao().selectByUuid(dbSession, entityId),
          "Project branch with uuid '%s' not found", entityId);
        ProjectDto project = checkFoundWithOptional(
          dbClient.projectDao().selectByUuid(dbSession, branch.getProjectUuid()),
          "Project with uuid '%s' not found", branch.getProjectUuid());
        userSession.checkEntityPermission(ProjectPermission.USER, project);
        if (ComponentQualifiers.APP.equals(project.getQualifier())) {
          userSession.checkChildProjectsPermission(ProjectPermission.USER, project);
        }
      } else if (EntityType.PORTFOLIO.equals(entityType)) {
        ComponentDto portfolio = checkFoundWithOptional(
          dbClient.componentDao().selectByUuid(dbSession, entityId),
          "Portfolio with uuid '%s' not found", entityId);
        userSession.checkComponentPermission(ProjectPermission.USER, portfolio);
      }
    }
  }

  public static HistoryDateRange normalize(Clock clock, OffsetDateTime startDate, @Nullable OffsetDateTime endDate) {
    Instant startInstant = startDate.toInstant();
    Instant endInstant = endDate != null ? endDate.toInstant() : null;
    Instant today = clock.instant();
    if (endInstant == null || endInstant.isAfter(today)) {
      endInstant = today;
    }
    HistoryUtils.validateDateRange(startInstant, endInstant, clock);
    return new HistoryDateRange(startInstant, endInstant);
  }

  public record HistoryDateRange(Instant start, Instant end) {
  }
}
