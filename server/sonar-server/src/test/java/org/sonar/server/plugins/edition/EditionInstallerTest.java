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
package org.sonar.server.plugins.edition;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.core.platform.PluginInfo;
import org.sonar.server.plugins.ServerPluginRepository;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class EditionInstallerTest {
  private static final String pluginKey = "key";
  @Mock
  private EditionPluginDownloader downloader;
  @Mock
  private EditionPluginUninstaller uninstaller;
  @Mock
  private ServerPluginRepository pluginRepository;

  private EditionInstallerExecutor executor = new EditionInstallerExecutor() {
    public void execute(Runnable r) {
      r.run();
    }
  };

  private EditionInstaller installer;

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);
    installer = new EditionInstaller(downloader, uninstaller, pluginRepository, executor);
  }

  @Test
  public void install() {
    installer.install(Collections.singleton(pluginKey));
    verify(downloader).installEdition(Collections.singleton(pluginKey));
  }

  @Test
  public void check_plugins_to_install_and_remove() {
    PluginInfo commercial1 = createPluginInfo("p1", true);
    PluginInfo commercial2 = createPluginInfo("p2", true);
    PluginInfo open1 = createPluginInfo("p3", false);
    mockPluginRepository(commercial1, commercial2, open1);

    Set<String> editionPlugins = new HashSet<>();
    editionPlugins.add("p1");
    editionPlugins.add("p4");
    installer.install(editionPlugins);

    verify(downloader).installEdition(Collections.singleton("p4"));
    verify(uninstaller).uninstall("p2");
    verifyNoMoreInteractions(uninstaller);
    verifyNoMoreInteractions(downloader);

  }

  private void mockPluginRepository(PluginInfo... installedPlugins) {
    Map<String, PluginInfo> pluginsByKey = Arrays.asList(installedPlugins).stream().collect(Collectors.toMap(p -> p.getKey(), p -> p));
    when(pluginRepository.getPluginInfosByKeys()).thenReturn(pluginsByKey);
    when(pluginRepository.getPluginInfos()).thenReturn(Arrays.asList(installedPlugins));
  }

  private static PluginInfo createPluginInfo(String pluginKey, boolean commercial) {
    PluginInfo info = new PluginInfo(pluginKey);
    if (commercial) {
      info.setOrganizationName("SonarSource");
      info.setLicense("Commercial");
    }
    return info;
  }

}
