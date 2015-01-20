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
package org.sonar.server.computation.issue;

import com.google.common.collect.Sets;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.util.cache.DiskCache;

public class IssueComputation {

  private final RuleCache ruleCache;
  private final ScmAccountCache scmAccountCache;
  private final SourceLinesCache linesCache;
  private final DiskCache<DefaultIssue>.DiskAppender finalIssuesAppender;

  public IssueComputation(RuleCache ruleCache, SourceLinesCache linesCache, ScmAccountCache scmAccountCache,
                          FinalIssues finalIssues) {
    this.ruleCache = ruleCache;
    this.linesCache = linesCache;
    this.scmAccountCache = scmAccountCache;
    this.finalIssuesAppender = finalIssues.newAppender();
  }

  public void processComponentIssues(String componentUuid, Iterable<DefaultIssue> issues) {
    linesCache.init(componentUuid);
    for (DefaultIssue issue : issues) {
      if (issue.isNew()) {
        guessAuthor(issue);
        autoAssign(issue);
        copyRuleTags(issue);
        // TODO execute extension points
      }
      finalIssuesAppender.append(issue);
    }
    linesCache.clear();
  }

  public void afterReportProcessing() {
    finalIssuesAppender.close();
  }

  private void guessAuthor(DefaultIssue issue) {
    if (issue.line() != null) {
      issue.setAuthorLogin(linesCache.lineAuthor(issue.line()));
    }
  }

  private void autoAssign(DefaultIssue issue) {
    String scmAccount = issue.authorLogin();
    if (scmAccount != null) {
      issue.setAssignee(scmAccountCache.getNullable(scmAccount));
    }
  }

  private void copyRuleTags(DefaultIssue issue) {
    RuleDto rule = ruleCache.getNullable(issue.ruleKey());
    if (rule != null) {
      issue.setTags(Sets.union(rule.getTags(), rule.getSystemTags()));
    }
  }

}
