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

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.sonar.api.ServerComponent;
import org.sonar.api.component.Component;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.user.UserSession;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class QProfileProjectService implements ServerComponent {

  public static final String PROPERTY_PREFIX = "sonar.profile.";

  private final QualityProfileDao qualityProfileDao;
  private final PropertiesDao propertiesDao;

  public QProfileProjectService(QualityProfileDao qualityProfileDao, PropertiesDao propertiesDao) {
    this.qualityProfileDao = qualityProfileDao;
    this.propertiesDao = propertiesDao;
  }

  public QProfileProjects projects(QualityProfileDto qualityProfile) {
    List<ComponentDto> componentDtos = qualityProfileDao.selectProjects(qualityProfile.getName(), PROPERTY_PREFIX + qualityProfile.getLanguage());
    List<Component> projects = newArrayList(Iterables.transform(componentDtos, new Function<ComponentDto, Component>() {
      @Override
      public Component apply(ComponentDto dto) {
        return (Component) dto;
      }
    }));
    return new QProfileProjects(QProfile.from(qualityProfile), projects);
  }

  public List<QProfile> profiles(long projectId) {
    List<QualityProfileDto> dtos = qualityProfileDao.selectByProject(projectId, PROPERTY_PREFIX  + "%");
    return newArrayList(Iterables.transform(dtos, new Function<QualityProfileDto, QProfile>() {
      @Override
      public QProfile apply(QualityProfileDto dto) {
        return QProfile.from(dto);
      }
    }));
  }

  public void addProject(QualityProfileDto qualityProfile, ComponentDto component, UserSession userSession) {
    checkPermission(userSession);
    propertiesDao.setProperty(new PropertyDto().setKey(PROPERTY_PREFIX + qualityProfile.getLanguage()).setValue(qualityProfile.getName()).setResourceId(component.getId()));
  }

  public void removeProject(QualityProfileDto qualityProfile, ComponentDto project, UserSession userSession) {
    removeProject(qualityProfile.getLanguage(), project, userSession);
  }

  public void removeProject(String language, ComponentDto project, UserSession userSession) {
    checkPermission(userSession);
    propertiesDao.deleteProjectProperty(PROPERTY_PREFIX + language, project.getId());
  }

  public void removeAllProjects(QualityProfileDto qualityProfile, UserSession userSession) {
    checkPermission(userSession);
    propertiesDao.deleteProjectProperties(PROPERTY_PREFIX + qualityProfile.getLanguage(), qualityProfile.getName());
  }

  private void checkPermission(UserSession userSession) {
    userSession.checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

}
