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

package org.sonar.server.computation.step;

import org.sonar.core.computation.dbcleaner.ProjectCleaner;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.purge.IdUuidPair;
import org.sonar.server.computation.AnalysisReportService;
import org.sonar.server.computation.ComputeEngineContext;

public class DataCleanerStep implements ComputationStep {
  private final ProjectCleaner projectCleaner;
  private final AnalysisReportService reportService;

  public DataCleanerStep(ProjectCleaner projectCleaner, AnalysisReportService reportService) {
    this.projectCleaner = projectCleaner;
    this.reportService = reportService;
  }

  @Override
  public void execute(DbSession session, ComputeEngineContext context) {
    projectCleaner.purge(session, new IdUuidPair(context.getProject().getId(), context.getProject().uuid()));
    // reportService.deleteDirectory(context.getReportDirectory());
  }

  @Override
  public String getDescription() {
    return "Purge database";
  }
}
