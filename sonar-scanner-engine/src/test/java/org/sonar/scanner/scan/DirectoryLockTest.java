/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.nio.file.Paths;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DirectoryLockTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  private DirectoryLock lock;

  @Before
  public void setUp() {
    lock = new DirectoryLock(temp.getRoot().toPath());
  }

  @Test
  public void tryLock() {
    assertThat(temp.getRoot()).isEmptyDirectory();
    lock.tryLock();
    assertThat(temp.getRoot().toPath().resolve(".sonar_lock")).exists();
    lock.unlock();
  }

  @Test
  public void unlockWithoutLock() {
    lock.unlock();
  }

  @Test
  public void errorTryLock() {
    lock = new DirectoryLock(Paths.get("non", "existing", "path"));

    assertThatThrownBy(() -> lock.tryLock())
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Failed to create lock");
  }
}
