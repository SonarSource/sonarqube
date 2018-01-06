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
package org.sonar.server.plugins.edition;

import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.server.platform.ServerFileSystem;
import org.sonar.server.plugins.ServerPluginRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class EditionPluginUninstallerTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void start_creates_uninstall_directory() {
    File dir = new File(temp.getRoot(), "uninstall");
    ServerPluginRepository repo = mock(ServerPluginRepository.class);
    ServerFileSystem fs = mock(ServerFileSystem.class);
    when(fs.getEditionUninstalledPluginsDir()).thenReturn(dir);
    EditionPluginUninstaller uninstaller = new EditionPluginUninstaller(repo, fs);

    uninstaller.start();
    verify(fs).getEditionUninstalledPluginsDir();
    verifyNoMoreInteractions(fs);
    assertThat(dir).isDirectory();
  }
}
