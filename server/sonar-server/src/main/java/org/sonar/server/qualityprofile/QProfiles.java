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

import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.qualityprofile.QualityProfileDto;

/**
 * Use {@link org.sonar.server.qualityprofile.QProfileService} instead
 */
@Deprecated
@ServerSide
public class QProfiles {

  private final QProfileLookup profileLookup;
  private final DbClient dbClient;

  public QProfiles(QProfileLookup profileLookup, DbClient dbClient) {
    this.profileLookup = profileLookup;
    this.dbClient = dbClient;
  }

  public List<QProfile> allProfiles() {
    return profileLookup.allProfiles();
  }

  public List<QProfile> profilesByLanguage(String language) {
    return profileLookup.profiles(language);
  }

  /**
   * Used in /project/profile and in /api/profiles
   */
  @CheckForNull
  public QProfile findProfileByProjectAndLanguage(long projectId, String language) {
    QualityProfileDto dto = dbClient.qualityProfileDao().selectByProjectAndLanguage(projectId, language);
    if (dto != null) {
      return QProfile.from(dto);
    }
    return null;
  }

}
