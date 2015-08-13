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

import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.batch.analysis.DefaultAnalysisMode;
import org.sonar.batch.cache.WSLoader.LoadStrategy;
import org.sonar.batch.analysis.AnalysisProperties;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.batch.repository.user.UserRepositoryLoader;
import org.sonar.batch.issue.tracking.ServerLineHashesLoader;
import org.sonar.batch.repository.DefaultProjectRepositoriesLoader;
import org.sonar.batch.repository.DefaultServerIssuesLoader;
import org.sonar.batch.repository.ProjectRepositoriesLoader;
import org.sonar.batch.repository.ServerIssuesLoader;
import org.sonar.batch.issue.tracking.DefaultServerLineHashesLoader;
import org.sonar.core.platform.ComponentContainer;

public class ProjectSyncContainer extends ComponentContainer {
  private final boolean force;
  private final AnalysisProperties properties;

  public ProjectSyncContainer(ComponentContainer globalContainer, AnalysisProperties analysisProperties, boolean force) {
    super(globalContainer);
    this.properties = analysisProperties;
    this.force = force;
  }

  @Override
  public void doBeforeStart() {
    ProjectReactor projectReactor = createProjectReactor();
    add(projectReactor);
    addComponents();
  }

  private ProjectReactor createProjectReactor() {
    ProjectDefinition rootProjectDefinition = ProjectDefinition.create();
    rootProjectDefinition.setProperties(properties.properties());
    return new ProjectReactor(rootProjectDefinition);
  }

  @Override
  public void doAfterStart() {
    getComponentByType(ProjectCacheSynchronizer.class).load(force);
  }

  private void addComponents() {
    add(new StrategyWSLoaderProvider(LoadStrategy.SERVER_FIRST),
      properties,
      DefaultAnalysisMode.class,
      ProjectCacheSynchronizer.class,
      UserRepositoryLoader.class);

    addIfMissing(DefaultProjectCacheStatus.class, ProjectCacheStatus.class);
    addIfMissing(DefaultProjectRepositoriesLoader.class, ProjectRepositoriesLoader.class);
    addIfMissing(DefaultServerIssuesLoader.class, ServerIssuesLoader.class);
    addIfMissing(DefaultServerLineHashesLoader.class, ServerLineHashesLoader.class);
  }

}
