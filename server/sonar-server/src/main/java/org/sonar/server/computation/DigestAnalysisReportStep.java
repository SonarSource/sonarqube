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
import org.sonar.api.config.Settings;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.server.db.DbClient;

public class DigestAnalysisReportStep {
  private static final String EXPERIMENTAL_MODE_PROPERTY = "sonar.computation.experimental";
  private static final Logger LOG = LoggerFactory.getLogger(DigestAnalysisReportStep.class);

  private final DbClient dbClient;
  private final Settings settings;

  public DigestAnalysisReportStep(DbClient dbClient, Settings settings) {
    this.dbClient = dbClient;
    this.settings = settings;
  }

  public void execute(String path) {
    dbClient.openSession(false);
    boolean isExperimentalModeOn = settings.getBoolean(EXPERIMENTAL_MODE_PROPERTY);

    LOG.info("Digest analysis report");
    if (isExperimentalModeOn) {
      TimeProfiler stepProfiler = new TimeProfiler(LOG).start(String.format("Digest analysis report at '%s'", path));
      LOG.info("experimental mode digestion");
      stepProfiler.stop();
    }
  }
}
