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

import org.sonar.api.ServerComponent;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.computation.db.AnalysisReportDao;
import org.sonar.server.db.DbClient;

import javax.annotation.CheckForNull;
import java.util.List;

import static org.sonar.core.computation.db.AnalysisReportDto.Status.PENDING;

/**
 * since 5.0
 */
public class ComputationService implements ServerComponent {
  private final DbClient dbClient;
  private final AnalysisReportDao dao;

  public ComputationService(DbClient dbClient) {
    this.dbClient = dbClient;
    dao = this.dbClient.analysisReportDao();
  }

  public void create(String projectKey) {
    AnalysisReportDto report = new AnalysisReportDto()
      .setProjectKey(projectKey)
      .setStatus(PENDING);

    DbSession session = dbClient.openSession(false);
    try {
      dao.insert(session, report);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<AnalysisReportDto> findByProjectKey(String projectKey) {
    DbSession session = dbClient.openSession(false);
    try {
      return dao.findByProjectKey(session, projectKey);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * @return a booked analysis report if one is available, null otherwise
   */
  @CheckForNull
  public synchronized AnalysisReportDto findAndBookNextAvailableAnalysisReport() {
    DbSession session = dbClient.openSession(false);
    try {
      AnalysisReportDto nextAvailableReport = dao.getNextAvailableReport(session);
      if (nextAvailableReport == null) {
        return null;
      }

      AnalysisReportDto report = dao.tryToBookReportAnalysis(session, nextAvailableReport);
      session.commit();

      return report;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void analyzeReport(AnalysisReportDto report) {
    // TODO TBE – implementation needed
  }
}
