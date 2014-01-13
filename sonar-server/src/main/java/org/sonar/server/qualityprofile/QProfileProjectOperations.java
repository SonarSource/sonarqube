/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
import org.sonar.core.component.ComponentDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.user.UserSession;

public class QProfileProjectOperations implements ServerComponent {

  private final PropertiesDao propertiesDao;

  public QProfileProjectOperations(PropertiesDao propertiesDao) {
    this.propertiesDao = propertiesDao;
  }

  public void addProject(QualityProfileDto qualityProfile, ComponentDto component, UserSession userSession) {
    checkPermission(userSession);
    propertiesDao.setProperty(new PropertyDto().setKey(
      QProfileOperations.PROFILE_PROPERTY_PREFIX + qualityProfile.getLanguage()).setValue(qualityProfile.getName()).setResourceId(component.getId()));
  }

  public void removeProject(QualityProfileDto qualityProfile, ComponentDto project, UserSession userSession) {
    removeProject(qualityProfile.getLanguage(), project, userSession);
  }

  public void removeProject(String language, ComponentDto project, UserSession userSession) {
    checkPermission(userSession);
    propertiesDao.deleteProjectProperty(QProfileOperations.PROFILE_PROPERTY_PREFIX + language, project.getId());
  }

  public void removeAllProjects(QualityProfileDto qualityProfile, UserSession userSession) {
    checkPermission(userSession);
    propertiesDao.deleteProjectProperties(QProfileOperations.PROFILE_PROPERTY_PREFIX + qualityProfile.getLanguage(), qualityProfile.getName());
  }

  private void checkPermission(UserSession userSession) {
    userSession.checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

}
