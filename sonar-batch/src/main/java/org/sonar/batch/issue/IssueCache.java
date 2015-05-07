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

import org.sonar.api.BatchSide;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.batch.index.Cache;
import org.sonar.batch.index.Caches;

import java.util.Collection;

/**
 * Shared issues among all project modules
 */
@BatchSide
public class IssueCache {

  // component key -> issue key -> issue
  private final Cache<DefaultIssue> cache;

  public IssueCache(Caches caches) {
    cache = caches.createCache("issues");
  }

  public Iterable<DefaultIssue> byComponent(String componentKey) {
    return cache.values(componentKey);
  }

  public Iterable<DefaultIssue> all() {
    return cache.values();
  }

  public Collection<Object> componentKeys() {
    return cache.keySet();
  }

  public IssueCache put(DefaultIssue issue) {
    cache.put(issue.componentKey(), issue.key(), issue);
    return this;
  }

  public void clear(String componentKey) {
    cache.clear(componentKey);
  }
}
