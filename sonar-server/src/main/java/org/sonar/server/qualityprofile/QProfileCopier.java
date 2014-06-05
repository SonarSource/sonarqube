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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.TempFolder;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.server.db.DbClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

public class QProfileCopier implements ServerComponent {

  private final DbClient db;
  private final QProfileBackuper backuper;
  private final TempFolder temp;

  public QProfileCopier(DbClient db, QProfileBackuper backuper, TempFolder temp) {
    this.db = db;
    this.backuper = backuper;
    this.temp = temp;
  }

  void copy(QualityProfileKey from, QualityProfileKey to) {
    verifyKeys(from, to);
    prepareProfiles(from, to);
    File backupFile = temp.newFile();
    backup(from, backupFile);
    restore(backupFile, to);
    FileUtils.deleteQuietly(backupFile);
  }

  private void verifyKeys(QualityProfileKey from, QualityProfileKey to) {
    if (from.equals(to)) {
      throw new IllegalArgumentException(String.format(
        "Source and target profiles are equal: %s", from));
    }
    if (!StringUtils.equals(from.lang(), to.lang())) {
      throw new IllegalArgumentException(String.format(
        "Source and target profiles do not have the same language: %s and %s", from, to));
    }
  }

  private void prepareProfiles(QualityProfileKey from, QualityProfileKey to) {
    DbSession dbSession = db.openSession(false);
    try {
      QualityProfileDto fromProfile = db.qualityProfileDao().getByKey(dbSession, from);
      if (fromProfile == null) {
        throw new IllegalArgumentException("Quality profile does not exist: " + from);
      }
      QualityProfileDto toProfile = db.qualityProfileDao().getByKey(dbSession, to);
      if (toProfile == null) {
        // Do not delegate creation to QualityProfileBackuper because we need to keep
        // the parent-child association, if exists.
        toProfile = QualityProfileDto.createFor(to).setParent(fromProfile.getParent());
        db.qualityProfileDao().insert(dbSession, toProfile);
      }
      dbSession.commit();

    } finally {
      dbSession.close();
    }
  }

  private void backup(QualityProfileKey from, File backupFile) {
    Writer writer = null;
    try {
      writer = new OutputStreamWriter(FileUtils.openOutputStream(backupFile));
      backuper.backup(from, writer);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to open temporary backup file: " + backupFile, e);
    } finally {
      IOUtils.closeQuietly(writer);
    }
  }

  private void restore(File backupFile, QualityProfileKey to) {
    Reader reader = null;
    try {
      reader = new InputStreamReader(FileUtils.openInputStream(backupFile));
      backuper.restore(reader, to);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create temporary backup file: " + backupFile, e);
    } finally {
      IOUtils.closeQuietly(reader);
    }
  }
}
