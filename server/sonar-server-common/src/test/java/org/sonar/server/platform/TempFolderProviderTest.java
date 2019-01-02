/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.TempFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TempFolderProviderTest {

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  TempFolderProvider underTest = new TempFolderProvider();

  @Test
  public void existing_temp_dir() throws Exception {
    ServerFileSystem fs = mock(ServerFileSystem.class);
    File tmpDir = temp.newFolder();
    when(fs.getTempDir()).thenReturn(tmpDir);

    TempFolder folder = underTest.provide(fs);
    assertThat(folder).isNotNull();
    File newDir = folder.newDir();
    assertThat(newDir).exists().isDirectory();
    assertThat(newDir.getParentFile().getCanonicalPath()).startsWith(tmpDir.getCanonicalPath());
  }

  @Test
  public void create_temp_dir_if_missing() throws Exception {
    ServerFileSystem fs = mock(ServerFileSystem.class);
    File tmpDir = temp.newFolder();
    when(fs.getTempDir()).thenReturn(tmpDir);
    FileUtils.forceDelete(tmpDir);

    TempFolder folder = underTest.provide(fs);
    assertThat(folder).isNotNull();
    File newDir = folder.newDir();
    assertThat(newDir).exists().isDirectory();
    assertThat(newDir.getParentFile().getCanonicalPath()).startsWith(tmpDir.getCanonicalPath());
  }
}
