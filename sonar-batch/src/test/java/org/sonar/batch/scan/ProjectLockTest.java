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
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.Semaphores;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.ProjectTree;
import org.sonar.batch.bootstrap.DefaultAnalysisMode;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class ProjectLockTest {

  ProjectLock projectLock;
  Semaphores semaphores = mock(Semaphores.class);
  ProjectTree projectTree = mock(ProjectTree.class);
  I18n i18n = mock(I18n.class);
  Project project;
  private DefaultAnalysisMode mode;

  @Before
  public void setUp() {
    mode = mock(DefaultAnalysisMode.class);

    project = new Project("my-project-key");
    when(projectTree.getRootProject()).thenReturn(project);

    projectLock = new ProjectLock(semaphores, projectTree, mode, i18n);
  }

  @Test
  public void shouldAcquireSemaphore() {
    when(semaphores.acquire(anyString(), anyInt(), anyInt())).thenReturn(new Semaphores.Semaphore().setLocked(true));
    projectLock.start();

    verify(semaphores).acquire("batch-my-project-key", 15, 10);
  }

  @Test
  public void shouldNotAcquireSemaphoreIfTheProjectIsAlreadyBeenAnalysing() {
    when(semaphores.acquire(anyString(), anyInt(), anyInt())).thenReturn(new Semaphores.Semaphore().setLocked(false).setDurationSinceLocked(1234L));
    try {
    projectLock.start();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(SonarException.class);
    }
    verify(i18n).age(eq(Locale.ENGLISH), eq(1234L));
  }

  @Test
  public void shouldNotAcquireSemaphoreInDryRunMode() {
    when(mode.isPreview()).thenReturn(true);
    projectLock.start();
    verifyZeroInteractions(semaphores);
  }

  @Test
  public void shouldReleaseSemaphore() {
    projectLock.stop();
    verify(semaphores).release("batch-my-project-key");
  }

  @Test
  public void shouldNotReleaseSemaphoreInPreviewMode() {
    when(mode.isPreview()).thenReturn(true);
    projectLock.stop();
    verifyZeroInteractions(semaphores);
  }

}
