/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.api.BatchExtension;
import org.sonar.api.CoreProperties;
import org.sonar.api.ServerExtension;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.resources.Project;
import org.sonar.api.task.TaskExtension;
import org.sonar.batch.ProjectTree;
import org.sonar.batch.bootstrap.BatchSettings;
import org.sonar.batch.bootstrap.ExtensionInstaller;
import org.sonar.batch.profiling.PhasesSumUpTimeProfiler;
import org.sonar.batch.scan.maven.MavenPluginExecutor;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProjectScanContainerTest {

  @Test
  public void should_add_fake_maven_executor_on_non_maven_env() {
    ProjectScanContainer container = new ProjectScanContainer(new ComponentContainer());
    container.add(mock(ExtensionInstaller.class));
    container.doBeforeStart();

    assertThat(container.getComponentByType(MavenPluginExecutor.class)).isNotNull();
  }

  @Test
  public void should_use_maven_executor_provided_by_maven() {
    ProjectScanContainer container = new ProjectScanContainer(new ComponentContainer());
    container.add(mock(ExtensionInstaller.class));
    MavenPluginExecutor mavenPluginExecutor = mock(MavenPluginExecutor.class);
    container.add(mavenPluginExecutor);
    container.doBeforeStart();

    assertThat(container.getComponentsByType(MavenPluginExecutor.class)).hasSize(1);
    assertThat(container.getComponentByType(MavenPluginExecutor.class)).isSameAs(mavenPluginExecutor);
  }

  @Test
  public void should_activate_profiling() {
    ComponentContainer parentContainer = new ComponentContainer();
    Settings settings = new Settings();
    parentContainer.add(settings);
    ProjectScanContainer container = new ProjectScanContainer(parentContainer);
    container.add(mock(ExtensionInstaller.class));
    container.doBeforeStart();

    assertThat(container.getComponentsByType(PhasesSumUpTimeProfiler.class)).hasSize(0);

    settings.setProperty(CoreProperties.PROFILING_LOG_PROPERTY, "true");

    container = new ProjectScanContainer(parentContainer);
    container.add(mock(ExtensionInstaller.class));
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

  @Test
  public void should_isolate_project_settings() {
    ComponentContainer parentContainer = new ComponentContainer();
    BatchSettings settings = mock(BatchSettings.class);
    parentContainer.add(settings);
    ProjectTree projectTree = mock(ProjectTree.class);
    Project project = mock(Project.class);
    when(project.getModules()).thenReturn(Lists.<Project> newArrayList());
    when(projectTree.getRootProject()).thenReturn(project);
    ProjectDefinition definition = mock(ProjectDefinition.class);
    when(projectTree.getProjectDefinition(project)).thenReturn(definition);
    parentContainer.add(projectTree);
    ProjectScanContainer container = new ProjectScanContainer(parentContainer);
    container.add(mock(ExtensionInstaller.class));
    container.doBeforeStart();

    container = spy(container);
    doNothing().when(container).scan(any(Project.class));
    container.doAfterStart();
    verify(settings).init(definition);

    container.stop();
    verify(settings).restore();
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
