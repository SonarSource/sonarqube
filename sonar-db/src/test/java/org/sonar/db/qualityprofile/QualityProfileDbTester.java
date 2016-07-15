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
package org.sonar.db.qualityprofile;

import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;

public class QualityProfileDbTester {
  private final DbClient dbClient;
  private final DbSession dbSession;

  public QualityProfileDbTester(DbTester db) {
    this.dbClient = db.getDbClient();
    this.dbSession = db.getSession();
  }

  public void insertQualityProfiles(QualityProfileDto qualityProfile, QualityProfileDto... qualityProfiles) {
    dbClient.qualityProfileDao().insert(dbSession, qualityProfile, qualityProfiles);
    dbSession.commit();
  }

  public void insertProjectWithQualityProfileAssociations(ComponentDto project, QualityProfileDto... qualityProfiles) {
    dbClient.componentDao().insert(dbSession, project);
    for (QualityProfileDto qualityProfile : qualityProfiles) {
      dbClient.qualityProfileDao().insertProjectProfileAssociation(project.uuid(), qualityProfile.getKey(), dbSession);
    }

    dbSession.commit();
  }

  public void associateProjectWithQualityProfile(ComponentDto project, QualityProfileDto... qualityProfiles) {
    for (QualityProfileDto qualityProfile : qualityProfiles) {
      dbClient.qualityProfileDao().insertProjectProfileAssociation(project.uuid(), qualityProfile.getKey(), dbSession);
    }

    dbSession.commit();
  }

}
