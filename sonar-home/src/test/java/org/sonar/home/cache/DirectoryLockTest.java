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
package org.sonar.home.cache;

import static org.mockito.Mockito.mock;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.rules.ExpectedException;

import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Paths;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class DirectoryLockTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public ExpectedException exception = ExpectedException.none();
  private DirectoryLock lock;

  @Before
  public void setUp() {
    lock = new DirectoryLock(temp.getRoot().toPath(), mock(Logger.class));
  }

  @Test
  public void lock() {
    assertThat(temp.getRoot().list()).isEmpty();
    lock.lock();
    assertThat(temp.getRoot().toPath().resolve(".sonar_lock")).exists();
    lock.unlock();
  }

  @Test
  public void tryLock() {
    assertThat(temp.getRoot().list()).isEmpty();
    lock.tryLock();
    assertThat(temp.getRoot().toPath().resolve(".sonar_lock")).exists();
    lock.unlock();
  }

  @Test(expected = OverlappingFileLockException.class)
  public void error_2locks() {
    assertThat(temp.getRoot().list()).isEmpty();
    lock.lock();
    lock.lock();
  }

  @Test
  public void unlockWithoutLock() {
    lock.unlock();
  }

  @Test
  public void errorCreatingLock() {
    lock = new DirectoryLock(Paths.get("non", "existing", "path"), mock(Logger.class));

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Failed to create lock");
    lock.lock();
  }

  @Test
  public void errorTryLock() {
    lock = new DirectoryLock(Paths.get("non", "existing", "path"), mock(Logger.class));

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Failed to create lock");
    lock.tryLock();
  }
}
