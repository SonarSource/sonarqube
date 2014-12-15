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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;

public class AnalysisReportService implements ServerComponent {
  private static final Logger LOG = LoggerFactory.getLogger(AnalysisReportService.class);
  private final DbClient dbClient;

  public AnalysisReportService(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public void decompress(DbSession session, ComputeEngineContext context) {
    ComponentDto project = context.getProject();

    context.setReportDirectory(dbClient.analysisReportDao().getDecompressedReport(session, project.getId()));
  }

  public void clean(@Nullable File directory) {
    if (directory == null) {
      return;
    }

    try {
      FileUtils.deleteDirectory(directory);
    } catch (IOException e) {
      LOG.warn(String.format("Failed to delete directory '%s'", directory.getPath()), e);
    }
  }
}
