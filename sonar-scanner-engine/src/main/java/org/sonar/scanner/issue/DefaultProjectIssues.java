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

import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.ProjectIssues;
import org.sonar.scanner.issue.tracking.TrackedIssue;

/**
 * Expose list of issues for the current project
 * @since 4.0
 */
public class DefaultProjectIssues implements ProjectIssues {
  private static final Predicate<TrackedIssue> RESOLVED = new ResolvedPredicate(true);
  private static final Predicate<TrackedIssue> NOT_RESOLVED = new ResolvedPredicate(false);
  private final IssueCache cache;

  public DefaultProjectIssues(IssueCache cache) {
    this.cache = cache;
  }

  @Override
  public Iterable<Issue> issues() {
    return StreamSupport.stream(cache.all().spliterator(), false)
      .filter(NOT_RESOLVED)
      .map(TrackedIssueAdapter::new)
      .collect(Collectors.toList());
  }

  @Override
  public Iterable<Issue> resolvedIssues() {
    return StreamSupport.stream(cache.all().spliterator(), false)
      .filter(RESOLVED)
      .map(TrackedIssueAdapter::new)
      .collect(Collectors.toList());
  }

  private static class ResolvedPredicate implements Predicate<TrackedIssue> {
    private final boolean resolved;

    private ResolvedPredicate(boolean resolved) {
      this.resolved = resolved;
    }

    @Override
    public boolean test(@Nullable TrackedIssue issue) {
      if (issue != null) {
        return resolved ? (issue.resolution() != null) : (issue.resolution() == null);
      }
      return false;
    }
  }
}
