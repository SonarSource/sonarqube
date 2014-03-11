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

import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerComponent;
import org.sonar.api.component.Component;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.resource.ResourceDao;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

public class QProfileProjectOperations implements ServerComponent {

  private final MyBatis myBatis;
  private final QualityProfileDao qualityProfileDao;
  private final ResourceDao resourceDao;
  private final PropertiesDao propertiesDao;

  public QProfileProjectOperations(MyBatis myBatis, QualityProfileDao qualityProfileDao, ResourceDao resourceDao, PropertiesDao propertiesDao) {
    this.myBatis = myBatis;
    this.qualityProfileDao = qualityProfileDao;
    this.resourceDao = resourceDao;
    this.propertiesDao = propertiesDao;
  }

  public void addProject(int profileId, long projectId, UserSession userSession) {
    checkPermission(userSession);
    SqlSession session = myBatis.openSession();
    try {
      ComponentDto project = (ComponentDto) findProjectNotNull(projectId, session);
      QualityProfileDto qualityProfile = findNotNull(profileId, session);

      propertiesDao.setProperty(new PropertyDto().setKey(
        QProfileOperations.PROFILE_PROPERTY_PREFIX + qualityProfile.getLanguage()).setValue(qualityProfile.getName()).setResourceId(project.getId()), session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void removeProject(int profileId, long projectId, UserSession userSession) {
    checkPermission(userSession);
    SqlSession session = myBatis.openSession();
    try {
      ComponentDto project = (ComponentDto) findProjectNotNull(projectId, session);
      QualityProfileDto qualityProfile = findNotNull(profileId, session);

      propertiesDao.deleteProjectProperty(QProfileOperations.PROFILE_PROPERTY_PREFIX + qualityProfile.getLanguage(), project.getId(), session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void removeProject(String language, long projectId, UserSession userSession) {
    checkPermission(userSession);
    SqlSession session = myBatis.openSession();
    try {
      ComponentDto project = (ComponentDto) findProjectNotNull(projectId, session);

      propertiesDao.deleteProjectProperty(QProfileOperations.PROFILE_PROPERTY_PREFIX + language, project.getId(), session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void removeAllProjects(int profileId, UserSession userSession) {
    checkPermission(userSession);
    SqlSession session = myBatis.openSession();
    try {
      QualityProfileDto qualityProfile = findNotNull(profileId, session);

      propertiesDao.deleteProjectProperties(QProfileOperations.PROFILE_PROPERTY_PREFIX + qualityProfile.getLanguage(), qualityProfile.getName(), session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private QualityProfileDto findNotNull(int id, SqlSession session) {
    QualityProfileDto qualityProfile = qualityProfileDao.selectById(id, session);
    QProfileValidations.checkProfileIsNotNull(qualityProfile);
    return qualityProfile;
  }

  private Component findProjectNotNull(long projectId, SqlSession session) {
    Component component = resourceDao.findById(projectId, session);
    if (component == null) {
      throw new NotFoundException("This project does not exists.");
    }
    return component;
  }

  private void checkPermission(UserSession userSession) {
    userSession.checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

}
