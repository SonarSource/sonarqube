/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import org.apache.commons.lang.StringUtils;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.TempFolder;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.qualityprofile.ws.QProfileWsSupport;

import static java.nio.charset.StandardCharsets.UTF_8;

@ServerSide
public class QProfileCopier {

  private final DbClient db;
  private final QProfileFactory factory;
  private final QProfileBackuper backuper;
  private final TempFolder temp;
  private final QProfileWsSupport qProfileWsSupport;

  public QProfileCopier(DbClient db, QProfileFactory factory, QProfileBackuper backuper, TempFolder temp, QProfileWsSupport qProfileWsSupport) {
    this.db = db;
    this.factory = factory;
    this.backuper = backuper;
    this.temp = temp;
    this.qProfileWsSupport = qProfileWsSupport;
  }

  public QualityProfileDto copyToName(DbSession dbSession, String fromProfileKey, String toName) {
    QualityProfileDto from = db.qualityProfileDao().selectOrFailByKey(dbSession, fromProfileKey);
    QualityProfileDto to = prepareTarget(dbSession, from, toName);
    File backupFile = temp.newFile();
    try {
      backup(dbSession, from, backupFile);
      restore(dbSession, backupFile, QProfileName.createFor(to.getLanguage(), to.getName()));
      return to;
    } finally {
      org.sonar.core.util.FileUtils.deleteQuietly(backupFile);
    }
  }

  private QualityProfileDto prepareTarget(DbSession dbSession, QualityProfileDto from, String toName) {
    QProfileName toProfileName = new QProfileName(from.getLanguage(), toName);
    verify(from, toProfileName);
    QualityProfileDto toProfile = db.qualityProfileDao().selectByNameAndLanguage(toProfileName.getName(), toProfileName.getLanguage(), dbSession);
    if (toProfile == null) {
      // Do not delegate creation to QProfileBackuper because we need to keep
      // the parent-child association, if exists.
      toProfile = factory.create(dbSession, qProfileWsSupport.getDefaultOrganization(dbSession), toProfileName);
      toProfile.setParentKee(from.getParentKee());
      db.qualityProfileDao().update(dbSession, toProfile);
      dbSession.commit();
    }
    return toProfile;
  }

  private void verify(QualityProfileDto fromProfile, QProfileName toProfileName) {
    if (!StringUtils.equals(fromProfile.getLanguage(), toProfileName.getLanguage())) {
      throw new IllegalArgumentException(String.format(
        "Source and target profiles do not have the same language: %s and %s",
        fromProfile.getLanguage(), toProfileName.getLanguage()));
    }
    if (fromProfile.getName().equals(toProfileName.getName())) {
      throw new IllegalArgumentException(String.format("Source and target profiles are equal: %s",
        fromProfile.getName()));
    }
  }

  private void backup(DbSession dbSession, QualityProfileDto profile, File backupFile) {
    try (Writer writer = new OutputStreamWriter(FileUtils.openOutputStream(backupFile), UTF_8)) {
      backuper.backup(dbSession, profile, writer);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to open temporary backup file: " + backupFile, e);
    }
  }

  private void restore(DbSession dbSession, File backupFile, QProfileName profileName) {
    try (Reader reader = new InputStreamReader(FileUtils.openInputStream(backupFile), UTF_8)) {
      backuper.restore(dbSession, reader, profileName);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create temporary backup file: " + backupFile, e);
    }
  }
}
