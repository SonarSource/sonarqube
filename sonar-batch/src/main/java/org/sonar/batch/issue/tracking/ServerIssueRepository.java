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
import org.sonar.api.BatchSide;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Status;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.batch.index.BatchResource;
import org.sonar.batch.index.Cache;
import org.sonar.batch.index.Caches;
import org.sonar.batch.index.ResourceCache;
import org.sonar.batch.protocol.input.BatchInput.ServerIssue;
import org.sonar.batch.repository.ServerIssuesLoader;
import org.sonar.batch.scan.filesystem.InputPathCache;
import org.sonar.core.component.ComponentKeys;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
@BatchSide
public class ServerIssueRepository {

  private static final Logger LOG = Loggers.get(ServerIssueRepository.class);

  private final Caches caches;
  private Cache<ServerIssue> issuesCache;
  private final ServerIssuesLoader previousIssuesLoader;
  private final ProjectReactor reactor;
  private final ResourceCache resourceCache;
  private final AnalysisMode analysisMode;
  private final InputPathCache inputPathCache;

  public ServerIssueRepository(Caches caches, ServerIssuesLoader previousIssuesLoader, ProjectReactor reactor, ResourceCache resourceCache,
    AnalysisMode analysisMode, InputPathCache inputPathCache) {
    this.caches = caches;
    this.previousIssuesLoader = previousIssuesLoader;
    this.reactor = reactor;
    this.resourceCache = resourceCache;
    this.analysisMode = analysisMode;
    this.inputPathCache = inputPathCache;
  }

  public void load() {
    if (analysisMode.isIncremental()) {
      return;
    }
    Profiler profiler = Profiler.create(LOG).startInfo("Load server issues");
    this.issuesCache = caches.createCache("previousIssues");
    caches.registerValueCoder(ServerIssue.class, new ServerIssueValueCoder());
    previousIssuesLoader.load(reactor.getRoot().getKeyWithBranch(), new Function<ServerIssue, Void>() {

      @Override
      public Void apply(@Nullable ServerIssue issue) {
        if (issue == null) {
          return null;
        }
        String componentKey = ComponentKeys.createEffectiveKey(issue.getModuleKey(), issue.hasPath() ? issue.getPath() : null);
        BatchResource r = resourceCache.get(componentKey);
        if (r == null) {
          // Deleted resource
          issuesCache.put(0, issue.getKey(), issue);
        } else {
          issuesCache.put(r.batchId(), issue.getKey(), issue);
        }
        return null;
      }
    }, false);
    profiler.stopDebug();
  }

  public Iterable<ServerIssue> byComponent(BatchResource component) {
    if (analysisMode.isIncremental()) {
      if (!component.isFile()) {
        throw new UnsupportedOperationException("Incremental mode should only get issues on files");
      }
      InputFile inputFile = (InputFile) inputPathCache.getInputPath(component);
      if (inputFile.status() == Status.ADDED) {
        return Collections.emptyList();
      }
      Profiler profiler = Profiler.create(LOG).startInfo("Load server issues for " + component.resource().getPath());
      final List<ServerIssue> result = new ArrayList<>();
      previousIssuesLoader.load(component.key(), new Function<ServerIssue, Void>() {

        @Override
        public Void apply(@Nullable ServerIssue issue) {
          if (issue == null) {
            return null;
          }
          result.add(issue);
          return null;
        }
      }, true);
      profiler.stopDebug();
      return result;
    } else {
      return issuesCache.values(component.batchId());
    }
  }

  public Iterable<ServerIssue> issuesOnMissingComponents() {
    if (analysisMode.isIncremental()) {
      throw new UnsupportedOperationException("Only issues of analyzed components are loaded in incremental mode");
    }
    return issuesCache.values(0);
  }
}
