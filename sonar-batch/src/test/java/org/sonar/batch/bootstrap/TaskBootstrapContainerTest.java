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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.SonarException;

import static org.mockito.Mockito.mock;

public class TaskBootstrapContainerTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void should_throw_when_no_project_and_task_require_project() {
    final ExtensionInstaller extensionInstaller = mock(ExtensionInstaller.class);
    Container bootstrapModule = new Container() {
      @Override
      protected void configure() {
        // used to install project extensions
        container.addSingleton(extensionInstaller);
        container.addSingleton(Settings.class);
      }
    };
    bootstrapModule.init();
    TaskBootstrapContainer module = new TaskBootstrapContainer("inspect", null);
    bootstrapModule.installChild(module);

    thrown.expect(SonarException.class);
    thrown.expectMessage("Task 'Project Scan' requires to be run on a project");

    module.start();
  }

}
