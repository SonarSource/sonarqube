/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import org.apache.commons.io.FileUtils;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.TempFolder;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QProfileDto;

import static java.nio.charset.StandardCharsets.UTF_8;

@ServerSide
public class QProfileCopier {

  private final DbClient db;
  private final QProfileFactory factory;
  private final QProfileBackuper backuper;
  private final TempFolder temp;

  public QProfileCopier(DbClient db, QProfileFactory factory, QProfileBackuper backuper, TempFolder temp) {
    this.db = db;
    this.factory = factory;
    this.backuper = backuper;
    this.temp = temp;
  }

  public QProfileDto copyToName(DbSession dbSession, QProfileDto sourceProfile, String toName) {
    OrganizationDto organization = db.organizationDao().selectByUuid(dbSession, sourceProfile.getOrganizationUuid())
      .orElseThrow(() -> new IllegalStateException("Organization with UUID [" + sourceProfile.getOrganizationUuid() + "] does not exist"));
    QProfileDto to = prepareTarget(dbSession, organization, sourceProfile, toName);
    File backupFile = temp.newFile();
    try {
      backup(dbSession, sourceProfile, backupFile);
      restore(dbSession, backupFile, to);
      return to;
    } finally {
      org.sonar.core.util.FileUtils.deleteQuietly(backupFile);
    }
  }

  private QProfileDto prepareTarget(DbSession dbSession, OrganizationDto organization, QProfileDto sourceProfile, String toName) {
    QProfileName toProfileName = new QProfileName(sourceProfile.getLanguage(), toName);
    verify(sourceProfile, toProfileName);
    QProfileDto toProfile = db.qualityProfileDao().selectByNameAndLanguage(dbSession, organization, toProfileName.getName(), toProfileName.getLanguage());
    if (toProfile == null) {
      toProfile = factory.checkAndCreateCustom(dbSession, organization, toProfileName);
      toProfile.setParentKee(sourceProfile.getParentKee());
      db.qualityProfileDao().update(dbSession, toProfile);
      dbSession.commit();
    }
    return toProfile;
  }

  private void verify(QProfileDto fromProfile, QProfileName toProfileName) {
    if (fromProfile.getName().equals(toProfileName.getName())) {
      throw new IllegalArgumentException(String.format("Source and target profiles are equal: %s",
        fromProfile.getName()));
    }
  }

  private void backup(DbSession dbSession, QProfileDto profile, File backupFile) {
    try (Writer writer = new OutputStreamWriter(FileUtils.openOutputStream(backupFile), UTF_8)) {
      backuper.backup(dbSession, profile, writer);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to open temporary backup file: " + backupFile, e);
    }
  }

  private void restore(DbSession dbSession, File backupFile, QProfileDto profile) {
    try (Reader reader = new InputStreamReader(FileUtils.openInputStream(backupFile), UTF_8)) {
      backuper.restore(dbSession, reader, profile);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create temporary backup file: " + backupFile, e);
    }
  }
}
