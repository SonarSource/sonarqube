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

import com.google.common.collect.ImmutableMap;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueNormalizer;
import org.sonar.server.search.IndexClient;

public class IndexProjectIssuesStep implements ComputationStep {

  private final DbClient dbClient;
  private final IndexClient index;

  public IndexProjectIssuesStep(DbClient dbClient, IndexClient index) {
    this.dbClient = dbClient;
    this.index = index;
  }

  @Override
  public void execute(DbSession session, AnalysisReportDto report) {
    indexProjectIssues(session, report);
  }

  @Override
  public String description() {
    return "Update issues index";
  }

  private void indexProjectIssues(DbSession session, AnalysisReportDto report) {
    dbClient.issueDao().synchronizeAfter(session,
      index.get(IssueIndex.class).getLastSynchronization(ImmutableMap.of(IssueNormalizer.IssueField.PROJECT.field(), report.getProject().uuid())),
      ImmutableMap.of(IssueNormalizer.IssueField.PROJECT.field(), report.getProject().uuid()));
    session.commit();
  }
}
