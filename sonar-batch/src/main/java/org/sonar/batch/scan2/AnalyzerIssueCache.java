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
package org.sonar.batch.scan2;

import org.sonar.api.BatchComponent;
import org.sonar.api.batch.analyzer.issue.internal.DefaultAnalyzerIssue;
import org.sonar.batch.index.Cache;
import org.sonar.batch.index.Caches;

/**
 * Shared issues among all project modules
 */
public class AnalyzerIssueCache implements BatchComponent {

  // component key -> issue key -> issue
  private final Cache<DefaultAnalyzerIssue> cache;

  public AnalyzerIssueCache(Caches caches) {
    cache = caches.createCache("issues");
  }

  public Iterable<DefaultAnalyzerIssue> byComponent(String resourceKey) {
    return cache.values(resourceKey);
  }

  public Iterable<DefaultAnalyzerIssue> all() {
    return cache.values();
  }

  public AnalyzerIssueCache put(String resourceKey, DefaultAnalyzerIssue issue) {
    cache.put(resourceKey, issue.key(), issue);
    return this;
  }

}
