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
import com.google.common.collect.Iterables;
import org.sonar.api.component.Component;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueChangelog;

import java.util.List;

/**
 * @since 3.6
 */
public class DefaultIssuable implements Issuable {

  public List<Issue> issues;

  public DefaultIssuable(List<Issue> issues) {
    this.issues = issues;
  }

  public Issue apply(Issue issue, IssueChangelog issueChangelog) {
    DefaultIssue defaultIssue = findIssue(issue);

    if (!Objects.equal(defaultIssue.severity(), issueChangelog.severity())) {
      defaultIssue.severity(issueChangelog.severity());
    }
    if (!Objects.equal(defaultIssue.status(), issueChangelog.status())) {
      defaultIssue.status(issueChangelog.status());
    }
    if (!Objects.equal(defaultIssue.resolution(), issueChangelog.resolution())) {
      defaultIssue.resolution(issueChangelog.resolution());
    }
    if (!Objects.equal(defaultIssue.line(), issueChangelog.line())) {
      defaultIssue.line(issueChangelog.line());
      // Do not add changelog if only line has changed
    }

    defaultIssue.addIssueChangelog(issueChangelog);
    return defaultIssue;
  }

  private DefaultIssue findIssue(final Issue issue) {
    return (DefaultIssue) Iterables.find(issues, new Predicate<Issue>() {
      public boolean apply(Issue currentIssue) {
        return currentIssue.uuid().equals(issue.uuid());
      }
    });
  }

  public List<Issue> issues() {
    return issues;
  }

  public Component component() {
    return null;
  }
}
