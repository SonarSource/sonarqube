/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.home.cache.DirectoryLock;

public class WorkDirectoryCleanerTest {
  private WorkDirectoryCleaner cleaner;
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void setUp() throws IOException {
    // create files to clean
    temp.newFile();
    File newFolder = temp.newFolder();
    File fileInFolder = new File(newFolder, "test");
    fileInFolder.createNewFile();

    File lock = new File(temp.getRoot(), DirectoryLock.LOCK_FILE_NAME);
    lock.createNewFile();

    // mock project
    InputModuleHierarchy hierarchy = mock(InputModuleHierarchy.class);
    DefaultInputModule root = mock(DefaultInputModule.class);
    when(hierarchy.root()).thenReturn(root);
    when(root.getWorkDir()).thenReturn(temp.getRoot());

    assertThat(temp.getRoot().list().length).isGreaterThan(1);
    cleaner = new WorkDirectoryCleaner(hierarchy);
  }

  @Test
  public void testNonExisting() {
    temp.delete();
    cleaner.execute();
  }

  @Test
  public void testClean() {
    File lock = new File(temp.getRoot(), DirectoryLock.LOCK_FILE_NAME);
    cleaner.execute();

    assertThat(temp.getRoot()).exists();
    assertThat(lock).exists();
    assertThat(temp.getRoot().list()).containsOnly(DirectoryLock.LOCK_FILE_NAME);
  }

}
