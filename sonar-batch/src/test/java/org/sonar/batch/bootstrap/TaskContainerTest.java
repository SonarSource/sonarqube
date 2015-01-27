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
package org.sonar.batch.bootstrap;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonar.batch.scan.DeprecatedProjectReactorBuilder;
import org.sonar.batch.scan.ProjectReactorBuilder;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class TaskContainerTest {

  @Test
  public void should_add_project_reactor_builder_by_default() {
    GlobalContainer container = GlobalContainer.create(Collections.<String, String>emptyMap(),
      Lists.newArrayList(new BootstrapProperties(Collections.<String, String>emptyMap())));
    TaskContainer taskContainer = new TaskContainer(container, Collections.<String, String>emptyMap());
    taskContainer.installCoreTasks();

    assertThat(taskContainer.getComponentByType(ProjectReactorBuilder.class)).isNotNull().isInstanceOf(ProjectReactorBuilder.class);

    container = GlobalContainer.create(Collections.<String, String>emptyMap(),
      Lists.newArrayList(new BootstrapProperties(Collections.<String, String>emptyMap()), new EnvironmentInformation("SonarQubeRunner", "2.4")));
    taskContainer = new TaskContainer(container, Collections.<String, String>emptyMap());
    taskContainer.installCoreTasks();

    assertThat(taskContainer.getComponentByType(ProjectReactorBuilder.class)).isNotNull().isInstanceOf(ProjectReactorBuilder.class);
  }

  @Test
  public void should_add_deprecated_project_reactor_builder_if_old_runner() {
    GlobalContainer container = GlobalContainer.create(Collections.<String, String>emptyMap(),
      Lists.newArrayList(new BootstrapProperties(Collections.<String, String>emptyMap()), new EnvironmentInformation("SonarRunner", "2.3")));
    TaskContainer taskContainer = new TaskContainer(container, Collections.<String, String>emptyMap());
    taskContainer.installCoreTasks();

    assertThat(taskContainer.getComponentByType(DeprecatedProjectReactorBuilder.class)).isNotNull().isInstanceOf(DeprecatedProjectReactorBuilder.class);
  }

}
