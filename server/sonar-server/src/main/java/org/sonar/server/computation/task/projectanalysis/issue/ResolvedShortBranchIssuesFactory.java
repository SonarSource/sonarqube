package org.sonar.server.computation.task.projectanalysis.issue;

import java.util.Collection;
import java.util.stream.Collectors;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.ShortBranchComponentsWithIssues;

public class ResolvedShortBranchIssuesFactory {

  private final ShortBranchComponentsWithIssues shortBranchComponentsWithIssues;
  private final DbClient dbClient;

  public ResolvedShortBranchIssuesFactory(ShortBranchComponentsWithIssues shortBranchComponentsWithIssues, DbClient dbClient) {
    this.shortBranchComponentsWithIssues = shortBranchComponentsWithIssues;
    this.dbClient = dbClient;
  }

  public Collection<DefaultIssue> create(Component component) {
    try (DbSession session = dbClient.openSession(false)) {
      return shortBranchComponentsWithIssues.getUuids(component.getKey())
        .stream()
        .flatMap(uuid -> dbClient.issueDao().selectResolvedOrConfirmedByComponentUuid(session, uuid).stream())
        .map(IssueDto::toDefaultIssue)
        .collect(Collectors.toList());
    }
  }
}
