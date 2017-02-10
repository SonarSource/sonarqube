/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.qualityprofile;

import org.sonar.api.server.ServerSide;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.WsUtils;

/**
 * Should be refactored in order to use project key. Maybe should it be move to {@link QProfileFactory}
 * Permission checks should also be done in the upper service.
 */
@ServerSide
public class QProfileProjectOperations {

  private final DbClient db;
  private final UserSession userSession;

  public QProfileProjectOperations(DbClient db, UserSession userSession) {
    this.db = db;
    this.userSession = userSession;
  }

  public void addProject(DbSession dbSession, String profileKey, ComponentDto project) {
    checkAdminOnProject(project);
    QualityProfileDto qualityProfile = selectProfileByKey(dbSession, profileKey);

    QualityProfileDto currentProfile = db.qualityProfileDao().selectByProjectAndLanguage(dbSession, project.key(), qualityProfile.getLanguage());

    boolean updated = false;
    if (currentProfile == null) {
      db.qualityProfileDao().insertProjectProfileAssociation(project.uuid(), qualityProfile.getKey(), dbSession);
      updated = true;
    } else if (!profileKey.equals(currentProfile.getKey())) {
      db.qualityProfileDao().updateProjectProfileAssociation(project.uuid(), profileKey, currentProfile.getKey(), dbSession);
      updated = true;
    }
    if (updated) {
      dbSession.commit();
    }
  }

  public void removeProject(DbSession dbSession, String profileKey, ComponentDto project) {
    checkAdminOnProject(project);
    QualityProfileDto qualityProfile = selectProfileByKey(dbSession, profileKey);

    db.qualityProfileDao().deleteProjectProfileAssociation(project.uuid(), qualityProfile.getKey(), dbSession);
    dbSession.commit();
  }

  private QualityProfileDto selectProfileByKey(DbSession session, String profileKey) {
    QualityProfileDto qualityProfile = db.qualityProfileDao().selectByKey(session, profileKey);
    return WsUtils.checkFound(qualityProfile, "Quality profile does not exist");
  }

  private void checkAdminOnProject(ComponentDto project) {
    if (!userSession.hasOrganizationPermission(project.getOrganizationUuid(), GlobalPermissions.QUALITY_PROFILE_ADMIN) &&
      !userSession.hasComponentPermission(UserRole.ADMIN, project)) {
      throw new ForbiddenException("Insufficient privileges");
    }
  }

}
