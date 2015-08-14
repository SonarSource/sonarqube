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
import javax.annotation.Nullable;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.batch.index.BatchComponent;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.index.Cache;
import org.sonar.batch.index.Caches;
import org.sonar.batch.protocol.input.BatchInput.ServerIssue;
import org.sonar.batch.repository.ServerIssuesLoader;
import org.sonar.batch.scan.ImmutableProjectReactor;
import org.sonar.core.component.ComponentKeys;

@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
@BatchSide
public class ServerIssueRepository {

  private static final Logger LOG = Loggers.get(ServerIssueRepository.class);
  private static final String LOG_MSG = "Load server issues";

  private final Caches caches;
  private Cache<ServerIssue> issuesCache;
  private final ServerIssuesLoader previousIssuesLoader;
  private final ImmutableProjectReactor reactor;
  private final BatchComponentCache resourceCache;

  public ServerIssueRepository(Caches caches, ServerIssuesLoader previousIssuesLoader, ImmutableProjectReactor reactor, BatchComponentCache resourceCache) {
    this.caches = caches;
    this.previousIssuesLoader = previousIssuesLoader;
    this.reactor = reactor;
    this.resourceCache = resourceCache;
  }

  public void load() {
    Profiler profiler = Profiler.create(LOG).startInfo(LOG_MSG);
    this.issuesCache = caches.createCache("previousIssues");
    caches.registerValueCoder(ServerIssue.class, new ServerIssueValueCoder());
    boolean fromCache = previousIssuesLoader.load(reactor.getRoot().getKeyWithBranch(), new SaveIssueConsumer());
    profiler.stopInfo(fromCache);
  }

  public Iterable<ServerIssue> byComponent(BatchComponent component) {
    return issuesCache.values(component.batchId());
  }

  private class SaveIssueConsumer implements Function<ServerIssue, Void> {

    @Override
    public Void apply(@Nullable ServerIssue issue) {
      if (issue == null) {
        return null;
      }
      String componentKey = ComponentKeys.createEffectiveKey(issue.getModuleKey(), issue.hasPath() ? issue.getPath() : null);
      BatchComponent r = resourceCache.get(componentKey);
      if (r == null) {
        // Deleted resource
        issuesCache.put(0, issue.getKey(), issue);
      } else {
        issuesCache.put(r.batchId(), issue.getKey(), issue);
      }
      return null;
    }
  }
  
  public Iterable<ServerIssue> issuesOnMissingComponents() {
    return issuesCache.values(0);
  }
}
