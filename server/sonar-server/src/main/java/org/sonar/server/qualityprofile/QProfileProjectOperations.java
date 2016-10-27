/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import javax.annotation.Nullable;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

/**
 * Should be refactored in order to use project key. Maybe should it be move to {@link QProfileFactory}
 * Permission checks should also be done in the upper service.
 */
@ServerSide
@ComputeEngineSide
public class QProfileProjectOperations {

  private final DbClient db;

  public QProfileProjectOperations(DbClient db) {
    this.db = db;
  }

  public void addProject(String profileKey, String projectUuid, UserSession userSession) {
    DbSession session = db.openSession(false);
    try {
      addProject(profileKey, projectUuid, userSession, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void addProject(String profileKey, String projectUuid, UserSession userSession, DbSession session) {
    ComponentDto project = db.componentDao().selectOrFailByUuid(session, projectUuid);
    checkPermission(userSession, project.key());
    QualityProfileDto qualityProfile = findNotNull(profileKey, session);

    QualityProfileDto currentProfile = db.qualityProfileDao().selectByProjectAndLanguage(session, project.key(), qualityProfile.getLanguage());

    boolean updated = false;
    if (currentProfile == null) {
      db.qualityProfileDao().insertProjectProfileAssociation(project.uuid(), qualityProfile.getKey(), session);
      updated = true;
    } else if (!profileKey.equals(currentProfile.getKey())) {
      db.qualityProfileDao().updateProjectProfileAssociation(projectUuid, profileKey, currentProfile.getKey(), session);
      updated = true;
    }
    if (updated) {
      session.commit();
    }
  }

  public void removeProject(String profileKey, String projectUuid, UserSession userSession) {
    DbSession session = db.openSession(false);
    try {
      ComponentDto project = db.componentDao().selectOrFailByUuid(session, projectUuid);
      checkPermission(userSession, project.key());
      QualityProfileDto qualityProfile = findNotNull(profileKey, session);

      db.qualityProfileDao().deleteProjectProfileAssociation(project.uuid(), qualityProfile.getKey(), session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  void removeProject(String language, long projectId, UserSession userSession) {
    DbSession session = db.openSession(false);
    try {
      ComponentDto project = db.componentDao().selectOrFailById(session, projectId);
      checkPermission(userSession, project.key());

      QualityProfileDto associatedProfile = db.qualityProfileDao().selectByProjectAndLanguage(session, project.getKey(), language);
      if (associatedProfile != null) {
        db.qualityProfileDao().deleteProjectProfileAssociation(project.uuid(), associatedProfile.getKey(), session);
        session.commit();
      }
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  void removeAllProjects(String profileKey, UserSession userSession) {
    checkPermission(userSession);
    DbSession session = db.openSession(false);
    try {
      QualityProfileDto qualityProfile = findNotNull(profileKey, session);
      db.qualityProfileDao().deleteAllProjectProfileAssociation(qualityProfile.getKey(), session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private QualityProfileDto findNotNull(String key, DbSession session) {
    QualityProfileDto qualityProfile = db.qualityProfileDao().selectByKey(session, key);
    checkProfileIsNotNull(qualityProfile);
    return qualityProfile;
  }

  private static void checkPermission(UserSession userSession) {
    userSession.checkPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

  private static void checkPermission(UserSession userSession, String projectKey) {
    if (!userSession.hasPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN) && !userSession.hasComponentPermission(UserRole.ADMIN, projectKey)) {
      throw new ForbiddenException("Insufficient privileges");
    }
  }

  private static QualityProfileDto checkProfileIsNotNull(@Nullable QualityProfileDto profile) {
    if (profile == null) {
      throw new NotFoundException("This quality profile does not exists.");
    }
    return profile;
  }

}
