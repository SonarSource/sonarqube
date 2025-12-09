/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
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

import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.qualityprofile.builtin.QProfileName;

@ServerSide
public class QProfileCopier {
  private final DbClient db;
  private final QProfileFactory factory;
  private final QProfileBackuper backuper;

  public QProfileCopier(DbClient db, QProfileFactory factory, QProfileBackuper backuper) {
    this.db = db;
    this.factory = factory;
    this.backuper = backuper;
  }

  public QProfileDto copyToName(DbSession dbSession, QProfileDto sourceProfile, String toName) {
    QProfileDto to = prepareTarget(dbSession, sourceProfile, toName);
    backuper.copy(dbSession, sourceProfile, to);
    return to;
  }

  private QProfileDto prepareTarget(DbSession dbSession, QProfileDto sourceProfile, String toName) {
    verify(sourceProfile.getName(), toName);
    QProfileName toProfileName = new QProfileName(sourceProfile.getLanguage(), toName);
    QProfileDto toProfile = db.qualityProfileDao().selectByNameAndLanguage(dbSession, toProfileName.getName(), toProfileName.getLanguage());
    if (toProfile == null) {
      toProfile = factory.createCustom(dbSession, toProfileName, sourceProfile.getParentKee());
      dbSession.commit();
    }
    return toProfile;
  }

  private static void verify(String fromProfileName, String toProfileName) {
    if (fromProfileName.equals(toProfileName)) {
      throw new IllegalArgumentException(String.format("Source and target profiles are equal: %s", toProfileName));
    }
  }
}
