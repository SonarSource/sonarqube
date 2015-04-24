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
package org.sonar.batch.scan;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.BatchExtension;
import org.sonar.api.CoreProperties;
import org.sonar.api.ServerExtension;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.bootstrap.ProjectBootstrapper;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.api.task.TaskExtension;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.TempFolder;
import org.sonar.batch.bootstrap.BootstrapProperties;
import org.sonar.batch.bootstrap.DefaultAnalysisMode;
import org.sonar.batch.bootstrap.ExtensionInstaller;
import org.sonar.batch.bootstrap.GlobalSettings;
import org.sonar.batch.bootstrap.TaskProperties;
import org.sonar.batch.profiling.PhasesSumUpTimeProfiler;
import org.sonar.batch.protocol.input.GlobalRepositories;
import org.sonar.batch.protocol.input.ProjectRepositories;
import org.sonar.batch.repository.ProjectRepositoriesLoader;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProjectScanContainerTest {

  private ProjectBootstrapper projectBootstrapper;
  private ProjectScanContainer container;
  private Settings settings;
  private ComponentContainer parentContainer;
  private BootstrapProperties bootstrapProperties;

  @Before
  public void prepare() {
    projectBootstrapper = mock(ProjectBootstrapper.class);
    bootstrapProperties = new BootstrapProperties(Collections.<String, String>emptyMap());
    DefaultAnalysisMode analysisMode = new DefaultAnalysisMode(Collections.<String, String>emptyMap());
    when(projectBootstrapper.bootstrap()).thenReturn(new ProjectReactor(ProjectDefinition.create()));
    parentContainer = new ComponentContainer();
    parentContainer.add(System2.INSTANCE);
    parentContainer.add(bootstrapProperties);
    parentContainer.add(analysisMode);
    GlobalRepositories globalRef = new GlobalRepositories();
    settings = new GlobalSettings(bootstrapProperties, new PropertyDefinitions(), globalRef, analysisMode);
    parentContainer.add(settings);
    ProjectRepositoriesLoader projectReferentialsLoader = new ProjectRepositoriesLoader() {
      @Override
      public ProjectRepositories load(ProjectReactor reactor, TaskProperties taskProperties) {
        return new ProjectRepositories();
      }
    };
    parentContainer.add(projectReferentialsLoader);
    parentContainer.add(mock(TaskProperties.class));
    container = new ProjectScanContainer(parentContainer);
  }

  @Test
  public void should_activate_profiling() {
    container.add(mock(ExtensionInstaller.class), projectBootstrapper, mock(TempFolder.class));
    container.doBeforeStart();

    assertThat(container.getComponentsByType(PhasesSumUpTimeProfiler.class)).hasSize(0);

    settings.setProperty(CoreProperties.PROFILING_LOG_PROPERTY, "true");

    container = new ProjectScanContainer(parentContainer);
    container.add(mock(ExtensionInstaller.class), projectBootstrapper, mock(TempFolder.class));
    container.doBeforeStart();

    assertThat(container.getComponentsByType(PhasesSumUpTimeProfiler.class)).hasSize(1);
  }

  @Test
  public void should_add_only_batch_extensions() {
    ProjectScanContainer.BatchExtensionFilter filter = new ProjectScanContainer.BatchExtensionFilter();

    assertThat(filter.accept(new MyBatchExtension())).isTrue();
    assertThat(filter.accept(MyBatchExtension.class)).isTrue();

    assertThat(filter.accept(new MyProjectExtension())).isFalse();
    assertThat(filter.accept(MyProjectExtension.class)).isFalse();
    assertThat(filter.accept(new MyServerExtension())).isFalse();
    assertThat(filter.accept(MyServerExtension.class)).isFalse();
    assertThat(filter.accept(new MyTaskExtension())).isFalse();
    assertThat(filter.accept(MyTaskExtension.class)).isFalse();
  }

  @InstantiationStrategy(InstantiationStrategy.PER_BATCH)
  static class MyBatchExtension implements BatchExtension {

  }

  static class MyProjectExtension implements BatchExtension {

  }

  static class MyServerExtension implements ServerExtension {

  }

  static class MyTaskExtension implements TaskExtension {

  }
}
