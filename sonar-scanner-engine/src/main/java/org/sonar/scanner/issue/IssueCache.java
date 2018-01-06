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

import java.util.Collection;
import org.sonar.api.batch.ScannerSide;
import org.sonar.scanner.issue.tracking.TrackedIssue;
import org.sonar.scanner.storage.Storage;
import org.sonar.scanner.storage.Storages;

/**
 * Shared issues among all project modules
 */
@ScannerSide
public class IssueCache {

  // component key -> issue key -> issue
  private final Storage<TrackedIssue> cache;

  public IssueCache(Storages caches) {
    cache = caches.createCache("issues");
  }

  public Iterable<TrackedIssue> byComponent(String componentKey) {
    return cache.values(componentKey);
  }

  public Iterable<TrackedIssue> all() {
    return cache.values();
  }

  public Collection<Object> componentKeys() {
    return cache.keySet();
  }

  public IssueCache put(TrackedIssue issue) {
    cache.put(issue.componentKey(), issue.key(), issue);
    return this;
  }

  public void clear(String componentKey) {
    cache.clear(componentKey);
  }
}
