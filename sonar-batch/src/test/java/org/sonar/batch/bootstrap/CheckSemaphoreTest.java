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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.DatabaseSemaphore;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.ProjectTree;

import java.util.Date;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.DatabaseSemaphore.Lock;

public class CheckSemaphoreTest {

  private CheckSemaphore checkSemaphore;

  private DatabaseSemaphore databaseSemaphore;
  private ProjectTree projectTree;
  private Settings settings;

  private Project project;
  private Lock lock;

  @Before
  public void setUp() {
    lock = mock(Lock.class);

    databaseSemaphore = mock(DatabaseSemaphore.class);
    when(databaseSemaphore.acquire(anyString())).thenReturn(lock);
    when(databaseSemaphore.acquire(anyString(), anyInt())).thenReturn(lock);

    projectTree = mock(ProjectTree.class);
    settings = new Settings();
    setDryRunMode(false);
    setForceMode(false);

    project = new Project("key", "branch", "name");
    when(projectTree.getRootProject()).thenReturn(project);

    checkSemaphore = new CheckSemaphore(databaseSemaphore, projectTree, settings);
  }

  @Test
  public void shouldAcquireSemaphore() {
    when(lock.isAcquired()).thenReturn(true);
    checkSemaphore.start();

    verify(databaseSemaphore).acquire(anyString());
  }

  @Test
  public void shouldUseProjectKeyInTheKeyOfTheSemaphore() {
    project = new Project("key");
    when(projectTree.getRootProject()).thenReturn(project);

    when(lock.isAcquired()).thenReturn(true);
    checkSemaphore.start();

    verify(databaseSemaphore).acquire("batch-key");
  }

  @Test
  public void shouldUseProjectKeyAndBranchIfExistingInTheKeyOfTheSemaphore() {
    when(lock.isAcquired()).thenReturn(true);
    checkSemaphore.start();

    verify(databaseSemaphore).acquire("batch-key:branch");
  }

  @Test
  public void shouldAcquireSemaphoreIfForceAnalyseActivated() {
    setForceMode(true);
    when(lock.isAcquired()).thenReturn(true);
    checkSemaphore.start();
    verify(databaseSemaphore).acquire(anyString(), anyInt());
  }

  @Test(expected = SonarException.class)
  public void shouldNotAcquireSemaphoreIfTheProjectIsAlreadyBeenAnalysing() {
    when(lock.getLocketAt()).thenReturn(new Date());
    when(lock.isAcquired()).thenReturn(false);
    checkSemaphore.start();
    verify(databaseSemaphore, never()).acquire(anyString());
  }

  @Test
  public void shouldNotAcquireSemaphoreInDryRunMode() {
    setDryRunMode(true);
    settings = new Settings().setProperty(CoreProperties.DRY_RUN, true);
    checkSemaphore.start();
    verify(databaseSemaphore, never()).acquire(anyString());
    verify(databaseSemaphore, never()).acquire(anyString(), anyInt());
  }

  @Test
  public void shouldReleaseSemaphore() {
    checkSemaphore.stop();
    verify(databaseSemaphore).release(anyString());
  }

  @Test
  public void shouldNotReleaseSemaphoreInDryRunMode() {
    setDryRunMode(true);
    checkSemaphore.stop();
    verify(databaseSemaphore, never()).release(anyString());
  }

  private void setDryRunMode(boolean isInDryRunMode) {
    settings.setProperty(CoreProperties.DRY_RUN, isInDryRunMode);
  }

  private void setForceMode(boolean isInForcedMode) {
    settings.setProperty(CoreProperties.FORCE_ANALYSIS, isInForcedMode);
  }

}
