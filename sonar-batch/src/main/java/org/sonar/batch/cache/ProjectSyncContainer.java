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

import javax.annotation.Nullable;

import org.sonar.batch.repository.ProjectRepositoriesFactoryProvider;
import org.sonar.batch.analysis.DefaultAnalysisMode;
import org.sonar.api.CoreProperties;

import java.util.HashMap;
import java.util.Map;

import org.sonar.batch.analysis.AnalysisProperties;
import org.sonar.batch.bootstrap.GlobalProperties;
import org.sonar.batch.repository.ProjectSettingsLoader;
import org.sonar.batch.repository.DefaultProjectSettingsLoader;
import org.sonar.batch.rule.ActiveRulesLoader;
import org.sonar.batch.rule.DefaultActiveRulesLoader;
import org.sonar.batch.repository.QualityProfileLoader;
import org.sonar.batch.repository.DefaultQualityProfileLoader;
import org.sonar.batch.cache.WSLoader.LoadStrategy;
import org.sonar.batch.repository.user.UserRepositoryLoader;
import org.sonar.batch.repository.DefaultProjectRepositoriesLoader;
import org.sonar.batch.repository.DefaultServerIssuesLoader;
import org.sonar.batch.repository.ProjectRepositoriesLoader;
import org.sonar.batch.repository.ServerIssuesLoader;
import org.sonar.core.platform.ComponentContainer;

public class ProjectSyncContainer extends ComponentContainer {
  private final boolean force;
  private final String projectKey;

  public ProjectSyncContainer(ComponentContainer globalContainer, @Nullable String projectKey, boolean force) {
    super(globalContainer);
    this.projectKey = projectKey;
    this.force = force;
  }

  @Override
  public void doBeforeStart() {
    addComponents();
  }

  @Override
  public void doAfterStart() {
    if (projectKey != null) {
      getComponentByType(ProjectCacheSynchronizer.class).load(projectKey, force);
    } else {
      getComponentByType(NonAssociatedCacheSynchronizer.class).execute(force);
    }
  }

  private static DefaultAnalysisMode createIssuesAnalysisMode(@Nullable String projectKey) {
    Map<String, String> props = new HashMap<>();
    props.put(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_ISSUES);
    if (projectKey != null) {
      props.put(CoreProperties.PROJECT_KEY_PROPERTY, projectKey);
    }
    GlobalProperties globalProps = new GlobalProperties(props);
    AnalysisProperties analysisProps = new AnalysisProperties(props);
    return new DefaultAnalysisMode(globalProps, analysisProps);
  }

  private void addComponents() {
    add(new StrategyWSLoaderProvider(LoadStrategy.SERVER_ONLY),
      new ProjectKeySupplier(projectKey),
      projectKey != null ? ProjectCacheSynchronizer.class : NonAssociatedCacheSynchronizer.class,
      UserRepositoryLoader.class,
      new ProjectRepositoriesFactoryProvider(projectKey),
      new ProjectPersistentCacheProvider(),
      createIssuesAnalysisMode(projectKey));

    addIfMissing(DefaultProjectCacheStatus.class, ProjectCacheStatus.class);
    addIfMissing(DefaultProjectRepositoriesLoader.class, ProjectRepositoriesLoader.class);
    addIfMissing(DefaultServerIssuesLoader.class, ServerIssuesLoader.class);
    addIfMissing(DefaultQualityProfileLoader.class, QualityProfileLoader.class);
    addIfMissing(DefaultActiveRulesLoader.class, ActiveRulesLoader.class);
    addIfMissing(DefaultProjectSettingsLoader.class, ProjectSettingsLoader.class);
  }
}
