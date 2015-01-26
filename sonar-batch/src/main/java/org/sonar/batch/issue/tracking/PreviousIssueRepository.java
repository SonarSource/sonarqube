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
package org.sonar.batch.issue.tracking;

import com.google.common.base.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.batch.index.BatchResource;
import org.sonar.batch.index.Cache;
import org.sonar.batch.index.Caches;
import org.sonar.batch.index.ResourceCache;
import org.sonar.batch.protocol.input.issues.PreviousIssue;
import org.sonar.batch.repository.PreviousIssuesLoader;

import javax.annotation.Nullable;

@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
public class PreviousIssueRepository implements BatchComponent {

  private static final Logger LOG = LoggerFactory.getLogger(PreviousIssueRepository.class);

  private final Caches caches;
  private Cache<PreviousIssue> issuesCache;
  private final PreviousIssuesLoader previousIssuesLoader;
  private final ProjectReactor reactor;
  private final ResourceCache resourceCache;

  public PreviousIssueRepository(Caches caches, PreviousIssuesLoader previousIssuesLoader, ProjectReactor reactor, ResourceCache resourceCache) {
    this.caches = caches;
    this.previousIssuesLoader = previousIssuesLoader;
    this.reactor = reactor;
    this.resourceCache = resourceCache;
  }

  public void load() {
    TimeProfiler profiler = new TimeProfiler(LOG).start("Load previous issues");
    try {
      this.issuesCache = caches.createCache("previousIssues");
      previousIssuesLoader.load(reactor, new Function<PreviousIssue, Void>() {

        @Override
        public Void apply(@Nullable PreviousIssue issue) {
          if (issue == null) {
            return null;
          }
          String componentKey = issue.componentKey();
          BatchResource r = resourceCache.get(componentKey);
          if (r == null) {
            // Deleted resource
            issuesCache.put(0, issue.key(), issue);
          }
          issuesCache.put(r.batchId(), issue.key(), issue);
          return null;
        }
      });
    } finally {
      profiler.stop();
    }
  }

  public Iterable<PreviousIssue> byComponent(BatchResource component) {
    return issuesCache.values(component.batchId());
  }

  public Iterable<PreviousIssue> issuesOnMissingComponents() {
    return issuesCache.values(0);
  }
}
