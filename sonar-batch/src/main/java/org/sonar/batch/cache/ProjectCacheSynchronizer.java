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
package org.sonar.batch.cache;

import org.sonar.api.batch.bootstrap.ProjectReactor;

import org.sonar.batch.bootstrap.AnalysisProperties;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.batch.protocol.input.BatchInput.ServerIssue;
import com.google.common.base.Function;
import org.sonar.batch.protocol.input.FileData;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sonar.batch.protocol.input.ProjectRepositories;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.batch.repository.user.UserRepositoryLoader;
import org.sonar.batch.issue.tracking.ServerLineHashesLoader;
import org.sonar.batch.repository.ServerIssuesLoader;
import org.sonar.batch.repository.ProjectRepositoriesLoader;

public class ProjectCacheSynchronizer {
  private static final Logger LOG = Loggers.get(ProjectCacheSynchronizer.class);
  private ProjectDefinition project;
  private AnalysisProperties properties;
  private ProjectRepositoriesLoader projectRepositoryLoader;
  private ServerIssuesLoader issuesLoader;
  private ServerLineHashesLoader lineHashesLoader;
  private UserRepositoryLoader userRepository;
  private ProjectCacheStatus cacheStatus;

  public ProjectCacheSynchronizer(ProjectReactor project, ProjectRepositoriesLoader projectRepositoryLoader, AnalysisProperties properties,
    ServerIssuesLoader issuesLoader, ServerLineHashesLoader lineHashesLoader, UserRepositoryLoader userRepository, ProjectCacheStatus cacheStatus) {
    this.project = project.getRoot();
    this.projectRepositoryLoader = projectRepositoryLoader;
    this.properties = properties;
    this.issuesLoader = issuesLoader;
    this.lineHashesLoader = lineHashesLoader;
    this.userRepository = userRepository;
    this.cacheStatus = cacheStatus;
  }

  public void load(boolean force) {
    Date lastSync = cacheStatus.getSyncStatus(project.getKeyWithBranch());

    if (lastSync != null) {
      LOG.debug("Found project [" + project.getKeyWithBranch() + " ] cache [" + lastSync + "]");

      if (!force) {
        return;
      }
    }

    cacheStatus.delete(project.getKeyWithBranch());
    ProjectRepositories projectRepo = projectRepositoryLoader.load(project, properties);

    if (projectRepo.lastAnalysisDate() == null) {
      return;
    }

    IssueAccumulator consumer = new IssueAccumulator();
    issuesLoader.load(project.getKeyWithBranch(), consumer, false);

    for (String login : consumer.loginSet) {
      userRepository.load(login);
    }

    loadLineHashes(projectRepo.fileDataByModuleAndPath());
    cacheStatus.save(project.getKeyWithBranch());
  }

  private String getComponentKey(String moduleKey, String filePath) {
    return moduleKey + ":" + filePath;
  }

  private void loadLineHashes(Map<String, Map<String, FileData>> fileDataByModuleAndPath) {
    for (Entry<String, Map<String, FileData>> e1 : fileDataByModuleAndPath.entrySet()) {
      String moduleKey = e1.getKey();

      for (Entry<String, FileData> e2 : e1.getValue().entrySet()) {
        String filePath = e2.getKey();
        lineHashesLoader.getLineHashes(getComponentKey(moduleKey, filePath));
      }
    }
  }

  private static class IssueAccumulator implements Function<ServerIssue, Void> {
    Set<String> loginSet = new HashSet<>();

    @Override
    public Void apply(ServerIssue input) {
      loginSet.add(input.getAssigneeLogin());
      return null;
    }
  }
}
