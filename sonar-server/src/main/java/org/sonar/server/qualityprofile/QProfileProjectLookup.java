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

import com.google.common.collect.Lists;
import org.apache.ibatis.session.SqlSession;
import org.elasticsearch.common.collect.Maps;
import org.sonar.api.ServerComponent;
import org.sonar.api.component.Component;
import org.sonar.api.web.UserRole;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.user.AuthorizationDao;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class QProfileProjectLookup implements ServerComponent {

  private final MyBatis myBatis;
  private final QualityProfileDao qualityProfileDao;
  private final AuthorizationDao authorizationDao;

  public QProfileProjectLookup(MyBatis myBatis, QualityProfileDao qualityProfileDao, AuthorizationDao authorizationDao) {
    this.myBatis = myBatis;
    this.qualityProfileDao = qualityProfileDao;
    this.authorizationDao = authorizationDao;
  }

  public List<Component> projects(int profileId) {
    SqlSession session = myBatis.openSession(false);
    try {
      QualityProfileDto qualityProfile = qualityProfileDao.selectById(profileId, session);
      QProfileValidations.checkProfileIsNotNull(qualityProfile);
      Map<String, Component> componentsByKeys = Maps.newHashMap();
      for (Component component: qualityProfileDao.selectProjects(
          qualityProfile.getName(), QProfileOperations.PROFILE_PROPERTY_PREFIX + qualityProfile.getLanguage(), session
        )) {
        componentsByKeys.put(component.key(), component);
      }

      UserSession userSession = UserSession.get();
      List<Component> result = Lists.newArrayList();
      Collection<String> authorizedProjectKeys = authorizationDao.selectAuthorizedRootProjectsKeys(userSession.userId(), UserRole.USER);
      for (String key: componentsByKeys.keySet()) {
        if (authorizedProjectKeys.contains(key)) {
          result.add(componentsByKeys.get(key));
        }
      }

      return result;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public int countProjects(QProfile profile) {
    return qualityProfileDao.countProjects(profile.name(), QProfileOperations.PROFILE_PROPERTY_PREFIX + profile.language());
  }

  @CheckForNull
  public QProfile findProfileByProjectAndLanguage(long projectId, String language) {
    QualityProfileDto dto = qualityProfileDao.selectByProjectAndLanguage(projectId, language, QProfileOperations.PROFILE_PROPERTY_PREFIX + language);
    if (dto != null) {
      return QProfile.from(dto);
    }
    return null;
  }

}
