/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.platform;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.platform.ServerFileSystem;
import org.sonar.api.utils.TempFolder;
import org.sonar.api.utils.internal.DefaultTempFolder;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TempFolderProviderTest {

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void createTempFolder() throws Exception {
    ServerFileSystem fs = mock(ServerFileSystem.class);
    File serverTempFolder = temp.newFolder();
    when(fs.getTempDir()).thenReturn(serverTempFolder);
    TempFolder tempUtils = new TempFolderProvider().provide(fs);
    tempUtils.newDir();
    tempUtils.newFile();
    assertThat(new File(serverTempFolder, "tmp")).exists();
    assertThat(new File(serverTempFolder, "tmp").list()).hasSize(2);

    ((DefaultTempFolder) tempUtils).stop();
    assertThat(new File(serverTempFolder, "tmp")).doesNotExist();
  }
}
