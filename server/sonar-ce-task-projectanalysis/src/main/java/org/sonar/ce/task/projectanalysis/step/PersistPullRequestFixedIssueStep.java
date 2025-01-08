/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.step;

import java.util.ArrayList;
import java.util.List;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.issue.fixedissues.PullRequestFixedIssueRepository;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IssueFixedDto;

public class PersistPullRequestFixedIssueStep implements ComputationStep {

  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final DbClient dbClient;
  private final TreeRootHolder treeRootHolder;
  private final PullRequestFixedIssueRepository pullRequestFixedIssueRepository;

  public PersistPullRequestFixedIssueStep(AnalysisMetadataHolder analysisMetadataHolder, DbClient dbClient, TreeRootHolder treeRootHolder,
    PullRequestFixedIssueRepository pullRequestFixedIssueRepository) {
    this.analysisMetadataHolder = analysisMetadataHolder;

    this.dbClient = dbClient;
    this.treeRootHolder = treeRootHolder;
    this.pullRequestFixedIssueRepository = pullRequestFixedIssueRepository;
  }

  @Override
  public void execute(Context context) {
    if (!analysisMetadataHolder.isPullRequest()) {
      return;
    }
    try (DbSession dbSession = dbClient.openSession(true)) {
      List<IssueFixedDto> currentIssuesFixed = dbClient.issueFixedDao().selectByPullRequest(dbSession, treeRootHolder.getRoot().getUuid());
      List<IssueFixedDto> newIssuesFixed = pullRequestFixedIssueRepository.getFixedIssues().stream()
        .map(i -> new IssueFixedDto(treeRootHolder.getRoot().getUuid(), i.key()))
        .toList();
      List<IssueFixedDto> issuesFixedToInsert = difference(newIssuesFixed, currentIssuesFixed);
      List<IssueFixedDto> issuesFixedToDelete = difference(currentIssuesFixed, newIssuesFixed);
      issuesFixedToInsert.forEach(i -> dbClient.issueFixedDao().insert(dbSession, i));
      issuesFixedToDelete.forEach(i -> dbClient.issueFixedDao().delete(dbSession, i));
      dbSession.commit();
    }
  }

  private static List<IssueFixedDto> difference(List<IssueFixedDto> sourceList, List<IssueFixedDto> otherList) {
    ArrayList<IssueFixedDto> differences = new ArrayList<>(sourceList);
    differences.removeAll(otherList);
    return differences;
  }


  @Override
  public String getDescription() {
    return "Persist Fixed issues in Pull Request";
  }
}
