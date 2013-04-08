/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.core.issue;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import org.sonar.api.component.Component;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueChangelog;

import java.util.List;

/**
 * @since 3.6
 */
public class DefaultIssuable implements Issuable {

  private List<Issue> issues;
  private ListMultimap<Issue, IssueChangelog> issuesWithChangelogs;

  public DefaultIssuable(List<Issue> issues) {
    this.issues = issues;
    this.issuesWithChangelogs = ArrayListMultimap.create();
  }

  public Issue apply(Issue issue, IssueChangelog issueChangelog) {
    Issue existingIssue = findIssue(issue);
    Issue.Builder builder = new Issue.Builder(existingIssue);

    if (!Objects.equal(existingIssue.severity(), issueChangelog.severity())) {
      builder.severity(issueChangelog.severity());
    }
    if (!Objects.equal(existingIssue.status(), issueChangelog.status())) {
      builder.status(issueChangelog.status());
    }
    if (!Objects.equal(existingIssue.resolution(), issueChangelog.resolution())) {
      builder.resolution(issueChangelog.resolution());
    }
    if (!Objects.equal(existingIssue.line(), issueChangelog.line())) {
      builder.line(issueChangelog.line());
      // Do not add changelog if only line has changed
    }

    issuesWithChangelogs.get(issue).add(issueChangelog);
    return builder.build();
  }

  private Issue findIssue(final Issue issue) {
    return Iterables.find(issues, new Predicate<Issue>() {
      public boolean apply(Issue currentIssue) {
        return currentIssue.uuid().equals(issue.uuid());
      }
    });
  }

  public List<Issue> issues() {
    return issues;
  }

  @Override
  public Component component() {
    return null;
  }
}
