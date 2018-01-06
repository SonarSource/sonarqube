/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.issue;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.util.Uuids;
import org.sonar.scanner.issue.tracking.SourceHashHolder;
import org.sonar.scanner.issue.tracking.TrackedIssue;
import org.sonar.scanner.protocol.input.ScannerInput.ServerIssue;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.TextRange;

public class IssueTransformer {
  private IssueTransformer() {
    // static only
  }

  public static TrackedIssue toTrackedIssue(ServerIssue serverIssue, String componentKey) {
    TrackedIssue issue = new TrackedIssue();
    issue.setKey(serverIssue.getKey());
    issue.setStatus(serverIssue.getStatus());
    issue.setResolution(serverIssue.hasResolution() ? serverIssue.getResolution() : null);
    issue.setMessage(serverIssue.hasMsg() ? serverIssue.getMsg() : null);
    issue.setStartLine(serverIssue.hasLine() ? serverIssue.getLine() : null);
    issue.setEndLine(serverIssue.hasLine() ? serverIssue.getLine() : null);
    issue.setSeverity(serverIssue.getSeverity().name());
    issue.setAssignee(serverIssue.hasAssigneeLogin() ? serverIssue.getAssigneeLogin() : null);
    // key in serverIssue might have branch, so don't use it
    issue.setComponentKey(componentKey);
    issue.setCreationDate(new Date(serverIssue.getCreationDate()));
    issue.setRuleKey(RuleKey.of(serverIssue.getRuleRepository(), serverIssue.getRuleKey()));
    issue.setNew(false);
    return issue;
  }

  public static void close(TrackedIssue issue) {
    issue.setStatus(Issue.STATUS_CLOSED);
    issue.setStartLine(null);
    issue.setEndLine(null);
    issue.setResolution(Issue.RESOLUTION_FIXED);
  }

  public static void resolveRemove(TrackedIssue issue) {
    issue.setStatus(Issue.STATUS_CLOSED);
    issue.setStartLine(null);
    issue.setEndLine(null);
    issue.setResolution(Issue.RESOLUTION_REMOVED);
  }

  public static Collection<TrackedIssue> toTrackedIssue(InputComponent component, Collection<ScannerReport.Issue> rawIssues, @Nullable SourceHashHolder hashes) {
    List<TrackedIssue> issues = new ArrayList<>(rawIssues.size());

    for (ScannerReport.Issue issue : rawIssues) {
      issues.add(toTrackedIssue(component, issue, hashes));
    }

    return issues;
  }

  public static TrackedIssue toTrackedIssue(InputComponent component, ScannerReport.Issue rawIssue, @Nullable SourceHashHolder hashes) {
    RuleKey ruleKey = RuleKey.of(rawIssue.getRuleRepository(), rawIssue.getRuleKey());

    Preconditions.checkNotNull(component.key(), "Component key must be set");
    Preconditions.checkNotNull(ruleKey, "Rule key must be set");

    TrackedIssue issue = new TrackedIssue(hashes != null ? hashes.getHashedSource() : null);

    issue.setKey(Uuids.createFast());
    issue.setComponentKey(component.key());
    issue.setRuleKey(ruleKey);
    issue.setGap(rawIssue.getGap() != 0 ? rawIssue.getGap() : null);
    issue.setSeverity(rawIssue.getSeverity().name());
    issue.setMessage(StringUtils.trimToNull(rawIssue.getMsg()));
    issue.setResolution(null);
    issue.setStatus(Issue.STATUS_OPEN);
    issue.setNew(true);

    if (rawIssue.hasTextRange()) {
      TextRange r = rawIssue.getTextRange();

      issue.setStartLine(r.getStartLine());
      issue.setStartLineOffset(r.getStartOffset());
      issue.setEndLine(r.getEndLine());
      issue.setEndLineOffset(r.getEndOffset());
    }

    return issue;
  }

}
