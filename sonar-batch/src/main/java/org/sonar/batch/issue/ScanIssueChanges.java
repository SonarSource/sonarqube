/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.issue;

import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueChange;
import org.sonar.api.issue.IssueChanges;
import org.sonar.core.issue.ApplyIssueChange;
import org.sonar.core.issue.DefaultIssue;

import java.util.Date;

public class ScanIssueChanges implements IssueChanges {

  private final IssueCache cache;

  public ScanIssueChanges(IssueCache cache) {
    this.cache = cache;
  }

  @Override
  public Issue apply(Issue issue, IssueChange change) {
    if (!change.hasChanges()) {
      return issue;
    }
    DefaultIssue reloaded = reload(issue);
    ApplyIssueChange.apply(reloaded, change);
    // TODO set the date of loading of issues
    reloaded.setUpdatedAt(new Date());
    cache.addOrUpdate(reloaded);
    // TODO keep history of changes
    return reloaded;
  }


  private DefaultIssue reload(Issue issue) {
    DefaultIssue reloaded = (DefaultIssue) cache.componentIssue(issue.componentKey(), issue.key());
    if (reloaded == null) {
      throw new IllegalStateException("Bad API usage. Unregistered issues can't be changed.");
    }
    return reloaded;
  }
}
