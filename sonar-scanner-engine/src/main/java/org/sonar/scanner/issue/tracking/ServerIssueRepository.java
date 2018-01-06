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
package org.sonar.scanner.issue.tracking;

import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.fs.internal.DefaultInputComponent;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.component.ComponentKeys;
import org.sonar.scanner.protocol.input.ScannerInput.ServerIssue;
import org.sonar.scanner.repository.ServerIssuesLoader;
import org.sonar.scanner.scan.filesystem.InputComponentStore;
import org.sonar.scanner.storage.Storage;
import org.sonar.scanner.storage.Storages;

@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
@ScannerSide
public class ServerIssueRepository {

  private static final Logger LOG = Loggers.get(ServerIssueRepository.class);
  private static final String LOG_MSG = "Load server issues";

  private final Storages caches;
  private Storage<ServerIssue> issuesCache;
  private final ServerIssuesLoader previousIssuesLoader;
  private final InputComponentStore componentStore;

  public ServerIssueRepository(Storages caches, ServerIssuesLoader previousIssuesLoader, InputComponentStore componentStore) {
    this.caches = caches;
    this.previousIssuesLoader = previousIssuesLoader;
    this.componentStore = componentStore;
  }

  public void load() {
    Profiler profiler = Profiler.create(LOG).startInfo(LOG_MSG);
    this.issuesCache = caches.createCache("previousIssues");
    caches.registerValueCoder(ServerIssue.class, new ServerIssueValueCoder());
    DefaultInputModule root = (DefaultInputModule) componentStore.root();
    previousIssuesLoader.load(root.getKeyWithBranch(), this::store);
    profiler.stopInfo();
  }

  public Iterable<ServerIssue> byComponent(InputComponent component) {
    return issuesCache.values(((DefaultInputComponent) component).batchId());
  }

  private void store(ServerIssue issue) {
    String moduleKeyWithBranch = issue.getModuleKey();
    InputModule module = componentStore.getModule(moduleKeyWithBranch);
    if (module != null) {
      String componentKeyWithoutBranch = ComponentKeys.createEffectiveKey(module.key(), issue.hasPath() ? issue.getPath() : null);
      DefaultInputComponent r = (DefaultInputComponent) componentStore.getByKey(componentKeyWithoutBranch);
      if (r != null) {
        issuesCache.put(r.batchId(), issue.getKey(), issue);
        return;
      }
    }
    // Deleted resource
    issuesCache.put(0, issue.getKey(), issue);
  }

  public Iterable<ServerIssue> issuesOnMissingComponents() {
    return issuesCache.values(0);
  }
}
