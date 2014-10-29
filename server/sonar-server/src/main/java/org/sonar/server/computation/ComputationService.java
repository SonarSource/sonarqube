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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.db.DbClient;

/**
 * since 5.0
 */
public class ComputationService implements ServerComponent {
  private static final Logger LOG = LoggerFactory.getLogger(ComputationService.class);

  private final DbClient dbClient;
  private final ComputationStepRegistry stepRegistry;

  public ComputationService(DbClient dbClient, ComputationStepRegistry stepRegistry) {
    this.dbClient = dbClient;
    this.stepRegistry = stepRegistry;
  }

  public void analyzeReport(AnalysisReportDto report) {
    LOG.info(String.format("#%s - %s - Analysis report processing started", report.getId(), report.getProjectKey()));

    // Synchronization of lot of data can only be done with a batch session for the moment
    DbSession session = dbClient.openSession(true);

    try {
      for (ComputationStep step : stepRegistry.steps()) {
        LOG.info(String.format("%s step started", step.description()));
        step.execute(session, report);
        session.commit();
        LOG.info(String.format("%s step finished", step.description()));
      }
    } finally {
      MyBatis.closeQuietly(session);
      LOG.info(String.format("#%s - %s - Analysis report processing finished", report.getId(), report.getProjectKey()));
    }
  }
}
