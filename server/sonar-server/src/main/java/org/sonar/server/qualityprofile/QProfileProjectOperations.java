/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.qualityprofile;

import org.sonar.api.ServerComponent;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.user.UserSession;

/**
 * Should be refactored in order to use project key. Mabye should it be move to {@link QProfileFactory}
 * Permission checks should also be done in the upper service.
 */
public class QProfileProjectOperations implements ServerComponent {

  private final DbClient db;

  public QProfileProjectOperations(DbClient db) {
    this.db = db;
  }

  public void addProject(int profileId, long projectId, UserSession userSession) {
    DbSession session = db.openSession(false);
    try {
      addProject(profileId, projectId, userSession, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  void addProject(int profileId, long projectId, UserSession userSession, DbSession session) {
    ComponentDto project = db.componentDao().getById(projectId, session);
    checkPermission(userSession, project.key());
    QualityProfileDto qualityProfile = findNotNull(profileId, session);

    db.propertiesDao().setProperty(new PropertyDto().setKey(
      QProfileProjectLookup.PROFILE_PROPERTY_PREFIX + qualityProfile.getLanguage()).setValue(qualityProfile.getName()).setResourceId(project.getId()), session);
    session.commit();
  }

  public void removeProject(int profileId, long projectId, UserSession userSession) {
    DbSession session = db.openSession(false);
    try {
      ComponentDto project = db.componentDao().getById(projectId, session);
      checkPermission(userSession, project.key());
      QualityProfileDto qualityProfile = findNotNull(profileId, session);

      db.propertiesDao().deleteProjectProperty(QProfileProjectLookup.PROFILE_PROPERTY_PREFIX + qualityProfile.getLanguage(), project.getId(), session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void removeProject(String language, long projectId, UserSession userSession) {
    DbSession session = db.openSession(false);
    try {
      ComponentDto project = db.componentDao().getById(projectId, session);
      checkPermission(userSession, project.key());

      db.propertiesDao().deleteProjectProperty(QProfileProjectLookup.PROFILE_PROPERTY_PREFIX + language, project.getId(), session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void removeAllProjects(int profileId, UserSession userSession) {
    checkPermission(userSession);
    DbSession session = db.openSession(false);
    try {
      QualityProfileDto qualityProfile = findNotNull(profileId, session);
      db.propertiesDao().deleteProjectProperties(QProfileProjectLookup.PROFILE_PROPERTY_PREFIX + qualityProfile.getLanguage(), qualityProfile.getName(), session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private QualityProfileDto findNotNull(int id, DbSession session) {
    QualityProfileDto qualityProfile = db.qualityProfileDao().getById(id, session);
    QProfileValidations.checkProfileIsNotNull(qualityProfile);
    return qualityProfile;
  }

  private void checkPermission(UserSession userSession) {
    userSession.checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

  private void checkPermission(UserSession userSession, String projectKey) {
    if (!userSession.hasGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN) && !userSession.hasProjectPermission(UserRole.ADMIN, projectKey)) {
      throw new ForbiddenException("Insufficient privileges");
    }
  }

}
