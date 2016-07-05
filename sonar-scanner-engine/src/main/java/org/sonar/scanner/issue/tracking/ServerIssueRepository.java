/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.scanner.issue.tracking;

import com.google.common.base.Function;
import javax.annotation.Nullable;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.component.ComponentKeys;
import org.sonar.scanner.index.BatchComponent;
import org.sonar.scanner.index.BatchComponentCache;
import org.sonar.scanner.index.Cache;
import org.sonar.scanner.index.Caches;
import org.sonar.scanner.protocol.input.ScannerInput.ServerIssue;
import org.sonar.scanner.repository.ServerIssuesLoader;
import org.sonar.scanner.scan.ImmutableProjectReactor;

@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
@ScannerSide
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
    previousIssuesLoader.load(reactor.getRoot().getKeyWithBranch(), new SaveIssueConsumer());
    profiler.stopInfo();
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
