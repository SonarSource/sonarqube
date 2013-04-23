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

import org.sonar.api.BatchComponent;
import org.sonar.api.issue.Issue;
import org.sonar.batch.index.Cache;
import org.sonar.batch.index.Caches;

import java.util.Collection;

/**
 * Shared issues among all project modules
 */
public class IssueCache implements BatchComponent {

  // component key -> issue key -> issue
  private final Cache<String, Issue> cache;

  public IssueCache(Caches caches) {
    cache = caches.createCache("issues");
  }

  public Collection<Issue> componentIssues(String componentKey) {
    return cache.values(componentKey);
  }

  public Issue componentIssue(String componentKey, String issueKey) {
    return cache.get(componentKey, issueKey);
  }

  public IssueCache addOrUpdate(Issue issue) {
    cache.put(issue.componentKey(), issue.key(), issue);
    return this;
  }
}
