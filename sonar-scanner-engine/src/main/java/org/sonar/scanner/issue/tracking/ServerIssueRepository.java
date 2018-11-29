/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.internal.AbstractProjectOrModule;
import org.sonar.api.batch.fs.internal.DefaultInputComponent;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.component.ComponentKeys;
import org.sonar.scanner.protocol.input.ScannerInput.ServerIssue;
import org.sonar.scanner.repository.ServerIssuesLoader;
import org.sonar.scanner.scan.filesystem.InputComponentStore;
import org.sonar.scanner.storage.Storage;
import org.sonar.scanner.storage.Storages;

public class ServerIssueRepository {

  private static final Logger LOG = Loggers.get(ServerIssueRepository.class);
  private static final String LOG_MSG = "Load server issues";

  private final Storages caches;
  private Storage<ServerIssue> issuesCache;
  private final ServerIssuesLoader previousIssuesLoader;
  private final InputComponentStore componentStore;
  private final AbstractProjectOrModule project;

  public ServerIssueRepository(Storages caches, ServerIssuesLoader previousIssuesLoader, InputComponentStore componentStore, AbstractProjectOrModule project) {
    this.caches = caches;
    this.previousIssuesLoader = previousIssuesLoader;
    this.componentStore = componentStore;
    this.project = project;
  }

  public void load() {
    Profiler profiler = Profiler.create(LOG).startInfo(LOG_MSG);
    this.issuesCache = caches.createCache("previousIssues");
    caches.registerValueCoder(ServerIssue.class, new ServerIssueValueCoder());
    previousIssuesLoader.load(project.getKeyWithBranch(), this::store);
    profiler.stopInfo();
  }

  public Iterable<ServerIssue> byComponent(InputComponent component) {
    return issuesCache.values(((DefaultInputComponent) component).scannerId());
  }

  private void store(ServerIssue issue) {
    String moduleKeyWithBranch = issue.getModuleKey();
    AbstractProjectOrModule moduleOrProject = componentStore.getModule(moduleKeyWithBranch);
    if (moduleOrProject != null) {
      String componentKeyWithoutBranch = ComponentKeys.createEffectiveKey(moduleOrProject.key(), issue.hasPath() ? issue.getPath() : null);
      DefaultInputComponent r = (DefaultInputComponent) componentStore.getByKey(componentKeyWithoutBranch);
      if (r != null) {
        issuesCache.put(r.scannerId(), issue.getKey(), issue);
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
