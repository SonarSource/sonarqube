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
import com.google.common.collect.Maps;
import org.sonar.api.ServerSide;
import org.sonar.api.component.Component;
import org.sonar.api.web.UserRole;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@ServerSide
public class QProfileProjectLookup {

  private final DbClient db;

  public QProfileProjectLookup(DbClient db) {
    this.db = db;
  }

  public List<Component> projects(int profileId) {
    DbSession session = db.openSession(false);
    try {
      QualityProfileDto qualityProfile = db.qualityProfileDao().getById(profileId, session);
      QProfileValidations.checkProfileIsNotNull(qualityProfile);
      Map<String, Component> componentsByKeys = Maps.newHashMap();
      for (Component component : db.qualityProfileDao().selectProjects(qualityProfile.getName(), qualityProfile.getLanguage(), session)) {
        componentsByKeys.put(component.key(), component);
      }

      UserSession userSession = UserSession.get();
      List<Component> result = Lists.newArrayList();
      Collection<String> authorizedProjectKeys = db.authorizationDao().selectAuthorizedRootProjectsKeys(userSession.userId(), UserRole.USER);
      for (Map.Entry<String, Component> entry : componentsByKeys.entrySet()) {
        if (authorizedProjectKeys.contains(entry.getKey())) {
          result.add(entry.getValue());
        }
      }

      return result;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public int countProjects(QProfile profile) {
    return db.qualityProfileDao().countProjects(profile.name(), profile.language());
  }

  @CheckForNull
  public QProfile findProfileByProjectAndLanguage(long projectId, String language) {
    QualityProfileDto dto = db.qualityProfileDao().getByProjectAndLanguage(projectId, language);
    if (dto != null) {
      return QProfile.from(dto);
    }
    return null;
  }

}
