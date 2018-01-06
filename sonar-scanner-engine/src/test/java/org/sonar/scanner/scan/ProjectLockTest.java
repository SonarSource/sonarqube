/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scanner.scan;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.home.cache.DirectoryLock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProjectLockTest {
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Rule
  public ExpectedException exception = ExpectedException.none();
  private ProjectLock lock;

  @Before
  public void setUp() {
    lock = setUpTest(tempFolder.getRoot());
  }

  private ProjectLock setUpTest(File file) {
    InputModuleHierarchy hierarchy = mock(InputModuleHierarchy.class);
    DefaultInputModule root = mock(DefaultInputModule.class);
    when(hierarchy.root()).thenReturn(root);
    when(root.getWorkDir()).thenReturn(file.toPath());

    return new ProjectLock(hierarchy);
  }

  @Test
  public void tryLock() {
    Path lockFilePath = tempFolder.getRoot().toPath().resolve(DirectoryLock.LOCK_FILE_NAME);
    lock.tryLock();
    assertThat(Files.exists(lockFilePath)).isTrue();
    assertThat(Files.isRegularFile(lockFilePath)).isTrue();

    lock.stop();
    assertThat(Files.exists(lockFilePath)).isTrue();
  }

  @Test
  public void tryLockConcurrently() {
    exception.expect(IllegalStateException.class);
    exception.expectMessage("Another SonarQube analysis is already in progress for this project");
    lock.tryLock();
    lock.tryLock();
  }

  @Test
  /**
   * If there is an error starting up the scan, we'll still try to unlock even if the lock
   * was never done
   */
  public void stopWithoutStarting() {
    lock.stop();
    lock.stop();
  }

  @Test
  public void tryLockTwice() {
    lock.tryLock();
    lock.stop();
    lock.tryLock();
    lock.stop();
  }

  @Test
  public void unLockWithNoLock() {
    lock.stop();
  }

}
