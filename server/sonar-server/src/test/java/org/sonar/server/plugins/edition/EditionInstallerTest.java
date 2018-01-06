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

import com.google.common.base.Optional;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.core.platform.PluginInfo;
import org.sonar.server.edition.License;
import org.sonar.server.edition.MutableEditionManagementState;
import org.sonar.server.plugins.ServerPluginRepository;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.updatecenter.common.UpdateCenter;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;

public class EditionInstallerTest {
  @Rule
  public LogTester logTester = new LogTester();

  private static final String PLUGIN_KEY = "key";

  private EditionPluginDownloader downloader = mock(EditionPluginDownloader.class);
  private EditionPluginUninstaller uninstaller = mock(EditionPluginUninstaller.class);
  private UpdateCenterMatrixFactory updateCenterMatrixFactory = mock(UpdateCenterMatrixFactory.class);
  private ServerPluginRepository pluginRepository = mock(ServerPluginRepository.class);
  private UpdateCenter updateCenter = mock(UpdateCenter.class);
  private MutableEditionManagementState editionManagementState = mock(MutableEditionManagementState.class);

  private EditionInstallerExecutor synchronousExecutor = new EditionInstallerExecutor() {
    public void execute(Runnable r) {
      r.run();
    }
  };
  private EditionInstallerExecutor mockedExecutor = mock(EditionInstallerExecutor.class);

  private EditionInstaller underTestSynchronousExecutor = new EditionInstaller(downloader, uninstaller, pluginRepository, synchronousExecutor, updateCenterMatrixFactory,
    editionManagementState);
  private EditionInstaller underTestMockedExecutor = new EditionInstaller(downloader, uninstaller, pluginRepository, mockedExecutor, updateCenterMatrixFactory,
    editionManagementState);

  @Before
  public void setUp() {
    when(updateCenterMatrixFactory.getUpdateCenter(anyBoolean())).thenReturn(Optional.of(updateCenter));
  }

  @Test
  public void launch_task_download_plugins() {
    underTestSynchronousExecutor.install(licenseWithPluginKeys(PLUGIN_KEY));

    verify(downloader).downloadEditionPlugins(singleton(PLUGIN_KEY), updateCenter);
  }

  @Test
  public void editionManagementState_is_changed_before_running_background_thread() {
    PluginInfo commercial1 = createPluginInfo("p1", true);
    PluginInfo commercial2 = createPluginInfo("p2", true);
    PluginInfo open1 = createPluginInfo("p3", false);
    mockPluginRepository(commercial1, commercial2, open1);
    License newLicense = licenseWithPluginKeys("p1", "p4");

    underTestMockedExecutor.install(newLicense);

    verify(editionManagementState).startAutomaticInstall(newLicense);
    verify(editionManagementState, times(0)).automaticInstallReady();
    verifyNoMoreInteractions(editionManagementState);
    ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
    verify(mockedExecutor).execute(runnableCaptor.capture());

    reset(editionManagementState);
    runnableCaptor.getValue().run();

    verify(editionManagementState).automaticInstallReady();
    verifyNoMoreInteractions(editionManagementState);
  }

  @Test
  public void editionManagementState_is_changed_to_automatic_failure_when_read_existing_plugins_fails() {
    RuntimeException fakeException = new RuntimeException("Faking getPluginInfosByKeys throwing an exception");
    when(pluginRepository.getPluginInfosByKeys())
      .thenThrow(fakeException);
    License newLicense = licenseWithPluginKeys("p1", "p4");

    underTestSynchronousExecutor.install(newLicense);

    verifyMoveToAutomaticFailureAndLogsError(newLicense, fakeException.getMessage());
  }

  @Test
  public void editionManagementState_is_changed_to_automatic_failure_when_downloader_fails() {
    PluginInfo commercial1 = createPluginInfo("p1", true);
    PluginInfo commercial2 = createPluginInfo("p2", true);
    PluginInfo open1 = createPluginInfo("p3", false);
    mockPluginRepository(commercial1, commercial2, open1);
    RuntimeException fakeException = new RuntimeException("Faking downloadEditionPlugins throwing an exception");
    doThrow(fakeException)
      .when(downloader)
      .downloadEditionPlugins(anySetOf(String.class), any(UpdateCenter.class));
    License newLicense = licenseWithPluginKeys("p1", "p4");

    underTestSynchronousExecutor.install(newLicense);

    verifyMoveToAutomaticFailureAndLogsError(newLicense, fakeException.getMessage());
  }

  @Test
  public void editionManagementState_is_changed_to_automatic_failure_when_uninstaller_fails() {
    PluginInfo commercial1 = createPluginInfo("p1", true);
    PluginInfo commercial2 = createPluginInfo("p2", true);
    PluginInfo open1 = createPluginInfo("p3", false);
    mockPluginRepository(commercial1, commercial2, open1);
    RuntimeException fakeException = new RuntimeException("Faking uninstall throwing an exception");
    doThrow(fakeException)
      .when(uninstaller)
      .uninstall(anyString());
    License newLicense = licenseWithPluginKeys("p1", "p4");

    underTestSynchronousExecutor.install(newLicense);

    verifyMoveToAutomaticFailureAndLogsError(newLicense, fakeException.getMessage());
  }

  @Test
  public void check_plugins_to_install_and_remove() {
    PluginInfo commercial1 = createPluginInfo("p1", true);
    PluginInfo commercial2 = createPluginInfo("p2", true);
    PluginInfo open1 = createPluginInfo("p3", false);
    mockPluginRepository(commercial1, commercial2, open1);
    License newLicense = licenseWithPluginKeys("p1", "p4");

    underTestSynchronousExecutor.install(newLicense);

    verify(editionManagementState).startAutomaticInstall(newLicense);
    verify(editionManagementState).automaticInstallReady();
    verifyNoMoreInteractions(editionManagementState);
    verify(downloader).downloadEditionPlugins(singleton("p4"), updateCenter);
    verify(uninstaller).uninstall("p2");
    verifyNoMoreInteractions(uninstaller);
    verifyNoMoreInteractions(downloader);
    assertThat(logTester.logs()).containsOnly("Installing edition 'edition-key', download: [p4], remove: [p2]");

  }

  @Test
  public void uninstall_commercial_plugins() {
    PluginInfo commercial1 = createPluginInfo("p1", true);
    PluginInfo commercial2 = createPluginInfo("p2", true);
    PluginInfo open1 = createPluginInfo("p3", false);
    mockPluginRepository(commercial1, commercial2, open1);

    underTestSynchronousExecutor.uninstall();

    verify(uninstaller).uninstall("p2");
    verify(uninstaller).uninstall("p1");

    verifyNoMoreInteractions(uninstaller);
    verifyZeroInteractions(downloader);
  }

  @Test
  public void move_to_manualInstall_state_when_offline() {
    mockPluginRepository(createPluginInfo("p1", true));
    synchronousExecutor = mock(EditionInstallerExecutor.class);
    when(updateCenterMatrixFactory.getUpdateCenter(true)).thenReturn(Optional.absent());
    underTestSynchronousExecutor = new EditionInstaller(downloader, uninstaller, pluginRepository, synchronousExecutor, updateCenterMatrixFactory, editionManagementState);
    License newLicense = licenseWithPluginKeys("p1");

    underTestSynchronousExecutor.install(newLicense);

    verifyZeroInteractions(synchronousExecutor);
    verifyZeroInteractions(uninstaller);
    verifyZeroInteractions(downloader);
    verify(editionManagementState).startManualInstall(newLicense);
    verifyNoMoreInteractions(editionManagementState);
    assertThat(logTester.logs()).containsOnly("Installation of edition 'edition-key' needs to be done manually");
  }

  @Test
  public void is_offline() {
    when(updateCenterMatrixFactory.getUpdateCenter(false)).thenReturn(Optional.absent());
    assertThat(underTestSynchronousExecutor.isOffline()).isTrue();
  }

  @Test
  public void is_not_offline() {
    assertThat(underTestSynchronousExecutor.isOffline()).isFalse();
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

    boolean flag = underTestSynchronousExecutor.requiresInstallationChange(editionPlugins);

    assertThat(flag).isTrue();
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

    boolean flag = underTestSynchronousExecutor.requiresInstallationChange(editionPlugins);

    assertThat(flag).isFalse();
    verifyZeroInteractions(downloader);
    verifyZeroInteractions(uninstaller);
    verifyZeroInteractions(editionManagementState);
  }

  private void mockPluginRepository(PluginInfo... installedPlugins) {
    Map<String, PluginInfo> pluginsByKey = Arrays.stream(installedPlugins).collect(uniqueIndex(PluginInfo::getKey));
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

  private static License licenseWithPluginKeys(String... pluginKeys) {
    return new License("edition-key", Arrays.asList(pluginKeys), "foo");
  }

  private void verifyMoveToAutomaticFailureAndLogsError(License newLicense, String expectedErrorMessage) {
    verify(editionManagementState).startAutomaticInstall(newLicense);
    verify(editionManagementState).installFailed(expectedErrorMessage);
    verifyNoMoreInteractions(editionManagementState);
    assertThat(logTester.logs(LoggerLevel.ERROR))
      .containsOnly("Failed to install edition " + newLicense.getEditionKey() + " with plugins " + newLicense.getPluginKeys());
  }

}
