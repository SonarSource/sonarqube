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

import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;

import org.sonar.api.BatchComponent;
import org.sonar.batch.index.Cache;
import org.sonar.batch.index.Caches;

/**
 * Shared issues among all project modules
 */
public class AnalyzerIssueCache implements BatchComponent {

  // project key -> resource key -> issue key -> issue
  private final Cache<DefaultIssue> cache;

  public AnalyzerIssueCache(Caches caches) {
    cache = caches.createCache("issues");
  }

  public Iterable<DefaultIssue> byComponent(String projectKey, String resourceKey) {
    return cache.values(projectKey, resourceKey);
  }

  public Iterable<DefaultIssue> all() {
    return cache.values();
  }

  public AnalyzerIssueCache put(String projectKey, String resourceKey, DefaultIssue issue) {
    cache.put(projectKey, resourceKey, issue.key(), issue);
    return this;
  }

  public Iterable<DefaultIssue> byModule(String projectKey) {
    return cache.values(projectKey);
  }

}
