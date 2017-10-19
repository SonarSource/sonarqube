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

import com.google.common.base.Optional;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.sonar.core.platform.PluginInfo;
import org.sonar.server.edition.MutableEditionManagementState;
import org.sonar.server.plugins.ServerPluginRepository;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.updatecenter.common.UpdateCenter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class EditionInstallerTest {
  private static final String PLUGIN_KEY = "key";

  private EditionPluginDownloader downloader = mock(EditionPluginDownloader.class);
  private EditionPluginUninstaller uninstaller = mock(EditionPluginUninstaller.class);
  private UpdateCenterMatrixFactory updateCenterMatrixFactory = mock(UpdateCenterMatrixFactory.class);
  private ServerPluginRepository pluginRepository = mock(ServerPluginRepository.class);
  private UpdateCenter updateCenter = mock(UpdateCenter.class);
  private MutableEditionManagementState editionManagementState = mock(MutableEditionManagementState.class);

  private EditionInstallerExecutor executor = new EditionInstallerExecutor() {
    public void execute(Runnable r) {
      r.run();
    }
  };

  private EditionInstaller installer = new EditionInstaller(downloader, uninstaller, pluginRepository, executor, updateCenterMatrixFactory, editionManagementState);

  @Before
  public void setUp() {
    when(updateCenterMatrixFactory.getUpdateCenter(anyBoolean())).thenReturn(Optional.of(updateCenter));
  }

  @Test
  public void launch_task_download_plugins() {
    assertThat(installer.install(Collections.singleton(PLUGIN_KEY))).isTrue();
    verify(downloader).downloadEditionPlugins(Collections.singleton(PLUGIN_KEY), updateCenter);
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

    verify(editionManagementState).automaticInstallReady();
    verify(downloader).downloadEditionPlugins(Collections.singleton("p4"), updateCenter);
    verify(uninstaller).uninstall("p2");
    verifyNoMoreInteractions(uninstaller);
    verifyNoMoreInteractions(downloader);
  }

  @Test
  public void uninstall_commercial_plugins() {
    PluginInfo commercial1 = createPluginInfo("p1", true);
    PluginInfo commercial2 = createPluginInfo("p2", true);
    PluginInfo open1 = createPluginInfo("p3", false);
    mockPluginRepository(commercial1, commercial2, open1);

    installer.uninstall();

    verify(uninstaller).uninstall("p2");
    verify(uninstaller).uninstall("p1");

    verifyNoMoreInteractions(uninstaller);
    verifyZeroInteractions(downloader);
  }

  @Test
  public void do_nothing_if_offline() {
    mockPluginRepository(createPluginInfo("p1", true));
    executor = mock(EditionInstallerExecutor.class);
    when(updateCenterMatrixFactory.getUpdateCenter(true)).thenReturn(Optional.absent());
    installer = new EditionInstaller(downloader, uninstaller, pluginRepository, executor, updateCenterMatrixFactory, editionManagementState);
    assertThat(installer.install(Collections.singleton("p1"))).isFalse();

    verifyZeroInteractions(executor);
    verifyZeroInteractions(uninstaller);
    verifyZeroInteractions(downloader);
    verifyZeroInteractions(editionManagementState);
  }

  @Test
  public void is_offline() {
    when(updateCenterMatrixFactory.getUpdateCenter(false)).thenReturn(Optional.absent());
    assertThat(installer.isOffline()).isTrue();
  }

  @Test
  public void is_not_offline() {
    assertThat(installer.isOffline()).isFalse();
  }

  @Test
  public void requires_installation_change() {
    PluginInfo commercial1 = createPluginInfo("p1", true);
    PluginInfo commercial2 = createPluginInfo("p2", true);
    PluginInfo open1 = createPluginInfo("p3", false);
    mockPluginRepository(commercial1, commercial2, open1);

    Set<String> editionPlugins = new HashSet<>();
    editionPlugins.add("p1");
    editionPlugins.add("p4");

    assertThat(installer.requiresInstallationChange(editionPlugins)).isTrue();
    verifyZeroInteractions(downloader);
    verifyZeroInteractions(uninstaller);
    verifyZeroInteractions(editionManagementState);
  }

  @Test
  public void does_not_require_installation_change() {
    PluginInfo commercial1 = createPluginInfo("p1", true);
    PluginInfo commercial2 = createPluginInfo("p2", true);
    PluginInfo open1 = createPluginInfo("p3", false);
    mockPluginRepository(commercial1, commercial2, open1);

    Set<String> editionPlugins = new HashSet<>();
    editionPlugins.add("p1");
    editionPlugins.add("p2");

    assertThat(installer.requiresInstallationChange(editionPlugins)).isFalse();
    verifyZeroInteractions(downloader);
    verifyZeroInteractions(uninstaller);
    verifyZeroInteractions(editionManagementState);
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
