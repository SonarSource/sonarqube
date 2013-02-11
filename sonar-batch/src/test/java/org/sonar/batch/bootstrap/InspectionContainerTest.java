/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.bootstrap;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Test;
import org.mockito.Matchers;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.batch.ProjectTree;
import org.sonar.batch.index.ResourcePersister;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InspectionContainerTest {
  @Test
  public void should_register_project_extensions() {
    // components injected in the parent container
    final Project project = new Project("foo");
    project.setConfiguration(new PropertiesConfiguration());
    final ProjectTree projectTree = mock(ProjectTree.class);
    when(projectTree.getProjectDefinition(project)).thenReturn(ProjectDefinition.create());
    final ResourcePersister resourcePersister = mock(ResourcePersister.class);
    when(resourcePersister.getSnapshot(Matchers.<Resource> any())).thenReturn(new Snapshot());

    final ExtensionInstaller extensionInstaller = mock(ExtensionInstaller.class);
    Container batchModule = new Container() {
      @Override
      protected void configure() {
        container.addSingleton(extensionInstaller);
        container.addSingleton(projectTree);
        container.addSingleton(resourcePersister);
        container.addSingleton(new BatchSettings());
      }
    };

    batchModule.init();
    InspectionContainer projectModule = new InspectionContainer(project);
    batchModule.installChild(projectModule);

    verify(extensionInstaller).installInspectionExtensions(any(ComponentContainer.class));
    assertThat(projectModule.container.getComponentByType(ProjectSettings.class)).isNotNull();
  }
}
