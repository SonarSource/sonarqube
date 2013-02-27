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

import org.junit.Test;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.task.TaskDefinition;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TaskContainerTest {
  @Test
  public void should_register_task_extensions_when_project_present() {
    final ExtensionInstaller extensionInstaller = mock(ExtensionInstaller.class);
    Container bootstrapModule = new Container() {
      @Override
      protected void configure() {
        // used to install project extensions
        container.addSingleton(extensionInstaller);
      }
    };
    bootstrapModule.init();
    ProjectTaskContainer module = new ProjectTaskContainer(TaskDefinition.create());
    bootstrapModule.installChild(module);

    verify(extensionInstaller).installTaskExtensions(any(ComponentContainer.class), eq(true));
  }

  @Test
  public void should_register_task_extensions_when_no_project() {
    final ExtensionInstaller extensionInstaller = mock(ExtensionInstaller.class);
    Container bootstrapModule = new Container() {
      @Override
      protected void configure() {
        // used to install project extensions
        container.addSingleton(extensionInstaller);
      }
    };
    bootstrapModule.init();
    ProjectLessTaskContainer module = new ProjectLessTaskContainer(TaskDefinition.create(), false);
    bootstrapModule.installChild(module);

    verify(extensionInstaller).installTaskExtensions(any(ComponentContainer.class), eq(false));
  }
}
