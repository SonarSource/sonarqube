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

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerSide;
import org.sonar.api.utils.TempFolder;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.db.DbClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

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

  public QualityProfileDto copyToName(String fromProfileKey, String toName) {
    QualityProfileDto to = prepareTarget(fromProfileKey, toName);
    File backupFile = temp.newFile();
    try {
      backup(fromProfileKey, backupFile);
      restore(backupFile, QProfileName.createFor(to.getLanguage(), to.getName()));
      return to;
    } finally {
      FileUtils.deleteQuietly(backupFile);
    }
  }

  private QualityProfileDto prepareTarget(String fromProfileKey, String toName) {
    DbSession dbSession = db.openSession(false);
    try {
      QualityProfileDto fromProfile = db.qualityProfileDao().getNonNullByKey(dbSession, fromProfileKey);
      QProfileName toProfileName = new QProfileName(fromProfile.getLanguage(), toName);
      verify(fromProfile, toProfileName);
      QualityProfileDto toProfile = db.qualityProfileDao().getByNameAndLanguage(toProfileName.getName(), toProfileName.getLanguage(), dbSession);
      if (toProfile == null) {
        // Do not delegate creation to QProfileBackuper because we need to keep
        // the parent-child association, if exists.
        toProfile = factory.create(dbSession, toProfileName);
        toProfile.setParentKee(fromProfile.getParentKee());
        db.qualityProfileDao().update(dbSession, toProfile);
        dbSession.commit();
      }
      return toProfile;

    } finally {
      dbSession.close();
    }
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

  private void backup(String profileKey, File backupFile) {
    Writer writer = null;
    try {
      writer = new OutputStreamWriter(FileUtils.openOutputStream(backupFile), Charsets.UTF_8);
      backuper.backup(profileKey, writer);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to open temporary backup file: " + backupFile, e);
    } finally {
      IOUtils.closeQuietly(writer);
    }
  }

  private void restore(File backupFile, QProfileName profileName) {
    Reader reader = null;
    try {
      reader = new InputStreamReader(FileUtils.openInputStream(backupFile), Charsets.UTF_8);
      backuper.restore(reader, profileName);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create temporary backup file: " + backupFile, e);
    } finally {
      IOUtils.closeQuietly(reader);
    }
  }
}
