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
package org.sonar.batch.scan;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.Semaphores;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.ProjectTree;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ProjectLockTest {

  ProjectLock projectLock;
  Semaphores semaphores = mock(Semaphores.class);
  ProjectTree projectTree = mock(ProjectTree.class);
  Settings settings;
  Project project;

  @Before
  public void setUp() {
    settings = new Settings();
    setDryRunMode(false);
    project = new Project("my-project-key");
    when(projectTree.getRootProject()).thenReturn(project);

    projectLock = new ProjectLock(semaphores, projectTree, settings);
  }

  @Test
  public void shouldAcquireSemaphore() {
    when(semaphores.acquire(anyString(), anyInt(), anyInt())).thenReturn(new Semaphores.Semaphore().setLocked(true));
    projectLock.start();

    verify(semaphores).acquire("batch-my-project-key", 15, 10);
  }

  @Test(expected = SonarException.class)
  public void shouldNotAcquireSemaphoreIfTheProjectIsAlreadyBeenAnalysing() {
    when(semaphores.acquire(anyString(), anyInt(), anyInt())).thenReturn(new Semaphores.Semaphore().setLocked(false).setDurationSinceLocked(1234L));
    projectLock.start();
  }

  @Test
  public void shouldNotAcquireSemaphoreInDryRunMode() {
    setDryRunMode(true);
    settings = new Settings().setProperty(CoreProperties.DRY_RUN, true);
    projectLock.start();
    verifyZeroInteractions(semaphores);
  }

  @Test
  public void shouldReleaseSemaphore() {
    projectLock.stop();
    verify(semaphores).release("batch-my-project-key");
  }

  @Test
  public void shouldNotReleaseSemaphoreInDryRunMode() {
    setDryRunMode(true);
    projectLock.stop();
    verifyZeroInteractions(semaphores);
  }

  private void setDryRunMode(boolean isInDryRunMode) {
    settings.setProperty(CoreProperties.DRY_RUN, isInDryRunMode);
  }

}
