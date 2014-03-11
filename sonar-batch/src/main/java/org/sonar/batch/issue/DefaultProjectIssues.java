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
package org.sonar.batch.issue;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.ProjectIssues;
import org.sonar.api.issue.internal.DefaultIssue;

import javax.annotation.Nullable;

/**
 * Expose list of issues for the current project
 * @since 4.0
 */
public class DefaultProjectIssues implements ProjectIssues {

  private final IssueCache cache;

  public DefaultProjectIssues(IssueCache cache) {
    this.cache = cache;
  }

  @Override
  public Iterable<Issue> issues() {
    return (Iterable) Iterables.filter(cache.all(), new ResolvedPredicate(false));
  }

  @Override
  public Iterable<Issue> resolvedIssues() {
    return (Iterable) Iterables.filter(cache.all(), new ResolvedPredicate(true));
  }

  private static class ResolvedPredicate implements Predicate<DefaultIssue> {
    private final boolean resolved;

    private ResolvedPredicate(boolean resolved) {
      this.resolved = resolved;
    }

    @Override
    public boolean apply(@Nullable DefaultIssue issue) {
      if (issue != null) {
        return resolved ? issue.resolution() != null : issue.resolution() == null;
      }
      return false;
    }
  }
}
