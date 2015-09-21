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

import org.sonar.batch.repository.ProjectRepositoriesLoader;

import org.sonarqube.ws.QualityProfiles.WsSearchResponse.QualityProfile;
import com.google.common.base.Function;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.batch.protocol.input.BatchInput.ServerIssue;
import org.sonar.batch.repository.ProjectRepositories;
import org.sonar.batch.repository.QualityProfileLoader;
import org.sonar.batch.repository.ServerIssuesLoader;
import org.sonar.batch.repository.user.UserRepositoryLoader;
import org.sonar.batch.rule.ActiveRulesLoader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProjectCacheSynchronizer {
  private static final Logger LOG = LoggerFactory.getLogger(ProjectCacheSynchronizer.class);

  private final ServerIssuesLoader issuesLoader;
  private final UserRepositoryLoader userRepository;
  private final ProjectCacheStatus cacheStatus;
  private final QualityProfileLoader qualityProfileLoader;
  private final ProjectRepositoriesLoader projectRepositoriesLoader;
  private final ActiveRulesLoader activeRulesLoader;

  public ProjectCacheSynchronizer(QualityProfileLoader qualityProfileLoader, ProjectRepositoriesLoader projectSettingsLoader,
    ActiveRulesLoader activeRulesLoader, ServerIssuesLoader issuesLoader,
    UserRepositoryLoader userRepository, ProjectCacheStatus cacheStatus) {
    this.qualityProfileLoader = qualityProfileLoader;
    this.projectRepositoriesLoader = projectSettingsLoader;
    this.activeRulesLoader = activeRulesLoader;
    this.issuesLoader = issuesLoader;
    this.userRepository = userRepository;
    this.cacheStatus = cacheStatus;
  }

  public void load(String projectKey, boolean force) {
    Date lastSync = cacheStatus.getSyncStatus();

    if (lastSync != null) {
      if (!force) {
        LOG.info("Found project [{}] cache [{}]", projectKey, lastSync);
        return;
      } else {
        LOG.info("-- Found project [{}] cache [{}], synchronizing data..", projectKey, lastSync);
      }
      cacheStatus.delete();
    } else {
      LOG.info("-- Cache for project [{}] not found, synchronizing data..", projectKey);
    }

    loadData(projectKey);
    saveStatus();
  }

  private void saveStatus() {
    cacheStatus.save();
    LOG.info("-- Succesfully synchronized project cache");
  }

  private void loadData(String projectKey) {
    Profiler profiler = Profiler.create(Loggers.get(ProjectCacheSynchronizer.class));
    ProjectRepositories projectRepo = null;

    profiler.startInfo("Load project settings");
    projectRepo = projectRepositoriesLoader.load(projectKey, true, null);

    if (!projectRepo.exists()) {
      LOG.debug("Project doesn't exist in the server");
    } else if (projectRepo.lastAnalysisDate() == null) {
      LOG.debug("No previous analysis found");
    }
    profiler.stopInfo();

    profiler.startInfo("Load project quality profiles");
    Collection<QualityProfile> qProfiles;
    if (projectRepo.exists()) {
      qProfiles = qualityProfileLoader.load(projectKey, null, null);
    } else {
      qProfiles = qualityProfileLoader.loadDefault(null);
    }
    profiler.stopInfo();

    profiler.startInfo("Load project active rules");
    Collection<String> keys = getKeys(qProfiles);
    for (String k : keys) {
      activeRulesLoader.load(k, null);
    }

    if (projectRepo.lastAnalysisDate() != null) {
      profiler.startInfo("Load server issues");
      UserLoginAccumulator consumer = new UserLoginAccumulator();
      issuesLoader.load(projectKey, consumer);
      profiler.stopInfo();

      profiler.startInfo("Load user information (" + consumer.loginSet.size() + " users)");
      for (String login : consumer.loginSet) {
        userRepository.load(login, null);
      }
      profiler.stopInfo("Load user information");
    }
  }

  private static Collection<String> getKeys(Collection<QualityProfile> qProfiles) {
    List<String> list = new ArrayList<>(qProfiles.size());
    for (QualityProfile qp : qProfiles) {
      list.add(qp.getKey());
    }

    return list;
  }

  private static class UserLoginAccumulator implements Function<ServerIssue, Void> {
    Set<String> loginSet = new HashSet<>();

    @Override
    public Void apply(ServerIssue input) {
      if (!StringUtils.isEmpty(input.getAssigneeLogin())) {
        loginSet.add(input.getAssigneeLogin());
      }
      return null;
    }
  }
}
