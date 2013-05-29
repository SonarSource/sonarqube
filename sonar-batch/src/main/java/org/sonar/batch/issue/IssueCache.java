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
import org.sonar.core.issue.DefaultIssue;

/**
 * Shared issues among all project modules
 */
public class IssueCache implements BatchComponent {

  // component key -> issue key -> issue
  private final Cache<String, DefaultIssue> cache;

  public IssueCache(Caches caches) {
    cache = caches.createCache("issues");
  }

  public Iterable<DefaultIssue> byComponent(String componentKey) {
    return cache.values(componentKey);
  }

  public Iterable<DefaultIssue> all() {
    return cache.allValues();
  }

  public IssueCache put(DefaultIssue issue) {
    cache.put(issue.componentKey(), issue.key(), issue);
    return this;
  }

  public boolean remove(Issue issue) {
    return cache.remove(issue.componentKey(), issue.key());
  }
}
