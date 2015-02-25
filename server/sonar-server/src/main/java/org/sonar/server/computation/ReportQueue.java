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

package org.sonar.server.computation;

import org.apache.commons.io.FileUtils;
import org.sonar.api.ServerComponent;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.Uuids;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.process.ProcessConstants;
import org.sonar.server.db.DbClient;

import javax.annotation.CheckForNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.sonar.core.computation.db.AnalysisReportDto.Status.PENDING;

public class ReportQueue implements ServerComponent {

  public static class Item {
    public final AnalysisReportDto dto;
    public final File zipFile;

    public Item(AnalysisReportDto dto, File zipFile) {
      this.dto = dto;
      this.zipFile = zipFile;
    }
  }

  private final DbClient dbClient;
  private final System2 system2;
  private final Settings settings;

  public ReportQueue(DbClient dbClient, System2 system2, Settings settings) {
    this.dbClient = dbClient;
    this.system2 = system2;
    this.settings = settings;
  }

  public Item add(String projectKey, InputStream reportData) {
    String uuid = Uuids.create();
    File file = reportFileForUuid(uuid);

    DbSession session = dbClient.openSession(false);
    try {
      checkThatProjectExistsInDatabase(projectKey, session);

      // save report data on file. Directory is created if it does not exist yet.
      FileUtils.copyInputStreamToFile(reportData, file);

      // add report metadata to the queue
      AnalysisReportDto dto = new AnalysisReportDto()
        .setProjectKey(projectKey)
        .setStatus(PENDING)
        .setUuid(uuid);
      dbClient.analysisReportDao().insert(session, dto);
      session.commit();
      return new Item(dto, file);

    } catch (Exception e) {
      FileUtils.deleteQuietly(file);
      throw new IllegalStateException("Fail to store analysis report of project " + projectKey, e);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void checkThatProjectExistsInDatabase(String projectKey, DbSession session) {
    dbClient.componentDao().getByKey(session, projectKey);
  }

  public void remove(Item item) {
    DbSession session = dbClient.openSession(false);
    try {
      FileUtils.deleteQuietly(item.zipFile);

      // FIXME why functional date is updated here ?
      item.dto.setFinishedAt(system2.now());
      dbClient.analysisReportDao().delete(session, item.dto.getId());
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public Item pop() {
    DbSession session = dbClient.openSession(false);
    try {
      AnalysisReportDto dto = dbClient.analysisReportDao().pop(session);
      if (dto != null) {
        File file = reportFileForUuid(dto.getUuid());
        if (file.exists()) {
          return new Item(dto, file);
        }
        Loggers.get(getClass()).error("Analysis report not found: " + file.getAbsolutePath());
        dbClient.analysisReportDao().delete(session, dto.getId());
        session.commit();
      }
      return null;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<AnalysisReportDto> findByProjectKey(String projectKey) {
    DbSession session = dbClient.openSession(false);
    try {
      return dbClient.analysisReportDao().selectByProjectKey(session, projectKey);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * Truncates table ANALYSIS_REPORTS and delete all files from directory {data}/analysis
   */
  public void clear() {
    File dir = reportsDir();
    try {
      FileUtils.deleteDirectory(dir);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to delete directory: " + dir.getAbsolutePath(), e);
    }

    DbSession session = dbClient.openSession(false);
    try {
      dbClient.analysisReportDao().truncate(session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void resetToPendingStatus() {
    DbSession session = dbClient.openSession(false);
    try {
      dbClient.analysisReportDao().resetAllToPendingStatus(session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * All the reports of the queue, whatever the status
   */
  public List<AnalysisReportDto> all() {
    DbSession session = dbClient.openSession(false);
    try {
      return dbClient.analysisReportDao().selectAll(session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * This directory is a flat list of the reports referenced in table ANALYSIS_REPORTS.
   * Never return null but the directory may not exist.
   */
  private File reportsDir() {
    return new File(settings.getString(ProcessConstants.PATH_DATA), "analysis");
  }

  private File reportFileForUuid(String uuid) {
    return new File(reportsDir(), String.format("%s.zip", uuid));
  }
}
