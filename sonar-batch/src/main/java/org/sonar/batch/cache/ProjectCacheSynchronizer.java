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

import com.google.common.base.Function;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.batch.protocol.input.BatchInput.ServerIssue;
import org.sonar.batch.protocol.input.QProfile;
import org.sonar.batch.repository.ProjectSettingsLoader;
import org.sonar.batch.repository.ProjectSettingsRepo;
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
  private final ProjectSettingsLoader projectSettingsLoader;
  private final ActiveRulesLoader activeRulesLoader;

  public ProjectCacheSynchronizer(QualityProfileLoader qualityProfileLoader, ProjectSettingsLoader projectSettingsLoader,
    ActiveRulesLoader activeRulesLoader, ServerIssuesLoader issuesLoader,
    UserRepositoryLoader userRepository, ProjectCacheStatus cacheStatus) {
    this.qualityProfileLoader = qualityProfileLoader;
    this.projectSettingsLoader = projectSettingsLoader;
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

    profiler.startInfo("Load project settings");
    ProjectSettingsRepo settings = projectSettingsLoader.load(projectKey, null);
    profiler.stopInfo();

    if (settings.lastAnalysisDate() == null) {
      LOG.debug("No previous analysis found");
      return;
    }

    profiler.startInfo("Load project quality profiles");
    Collection<QProfile> qProfiles = qualityProfileLoader.load(projectKey, null);
    profiler.stopInfo();

    Collection<String> profileKeys = getKeys(qProfiles);

    profiler.startInfo("Load project active rules");
    activeRulesLoader.load(profileKeys, projectKey);
    profiler.stopInfo();

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

  private static Collection<String> getKeys(Collection<QProfile> qProfiles) {
    List<String> list = new ArrayList<>(qProfiles.size());
    for (QProfile qp : qProfiles) {
      list.add(qp.key());
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
