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
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.preview.PreviewCache;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.user.UserSession;

import java.util.Map;

public class QProfileOperations implements ServerComponent {

  public static final String PROFILE_PROPERTY_PREFIX = "sonar.profile.";

  private final DbClient db;
  private final QProfileRepositoryExporter exporter;
  private final PreviewCache dryRunCache;

  public QProfileOperations(DbClient db,
    QProfileRepositoryExporter exporter, PreviewCache dryRunCache) {
    this.db = db;
    this.exporter = exporter;
    this.dryRunCache = dryRunCache;
  }

  public QProfileResult newProfile(String name, String language, Map<String, String> xmlProfilesByPlugin, UserSession userSession) {
    DbSession session = db.openSession(false);
    try {
      QProfile profile = newProfile(name, language, true, userSession, session);

      QProfileResult result = new QProfileResult();
      result.setProfile(profile);

      for (Map.Entry<String, String> entry : xmlProfilesByPlugin.entrySet()) {
        result.add(exporter.importXml(profile, entry.getKey(), entry.getValue(), session));
      }
      dryRunCache.reportGlobalModification(session);
      session.commit();
      return result;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private QProfile newProfile(String name, String language, boolean failIfAlreadyExists, UserSession userSession, DbSession session) {
    checkPermission(userSession);
    if (failIfAlreadyExists) {
      checkNotAlreadyExists(name, language, session);
    }
    QualityProfileDto dto = new QualityProfileDto().setName(name).setLanguage(language);
    db.qualityProfileDao().insert(session, dto);
    return QProfile.from(dto);
  }

  private void checkPermission(UserSession userSession) {
    userSession.checkLoggedIn();
    userSession.checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

  private void checkNotAlreadyExists(String name, String language, DbSession session) {
    if (db.qualityProfileDao().getByNameAndLanguage(name, language, session) != null) {
      throw new BadRequestException("quality_profiles.profile_x_already_exists", name);
    }
  }

}
