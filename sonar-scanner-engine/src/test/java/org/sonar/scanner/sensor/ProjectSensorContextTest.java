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
package org.sonar.scanner.sensor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.Version;
import org.sonar.core.platform.PluginInfo;
import org.sonar.scanner.bootstrap.ScannerPluginRepository;
import org.sonar.scanner.cache.AnalysisCacheEnabled;
import org.sonar.scanner.cache.ReadCacheImpl;
import org.sonar.scanner.cache.WriteCacheImpl;
import org.sonar.scanner.scan.branch.BranchConfiguration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProjectSensorContextTest {

  private final ActiveRules activeRules = new ActiveRulesBuilder().build();
  private final MapSettings settings = new MapSettings();
  private final DefaultSensorStorage sensorStorage = mock(DefaultSensorStorage.class);
  private final BranchConfiguration branchConfiguration = mock(BranchConfiguration.class);
  private final WriteCacheImpl writeCache = mock(WriteCacheImpl.class);
  private final ReadCacheImpl readCache = mock(ReadCacheImpl.class);
  private final AnalysisCacheEnabled analysisCacheEnabled = mock(AnalysisCacheEnabled.class);
  private final UnchangedFilesHandler unchangedFilesHandler = mock(UnchangedFilesHandler.class);
  private final SonarRuntime runtime = SonarRuntimeImpl.forSonarQube(Version.parse("5.5"), SonarQubeSide.SCANNER, SonarEdition.COMMUNITY);
  private DefaultFileSystem fs = mock(DefaultFileSystem.class);
  private ExecutingSensorContext executingSensorContext = mock(ExecutingSensorContext.class);
  private ScannerPluginRepository pluginRepository = mock(ScannerPluginRepository.class);

  private ProjectSensorContext underTest = new ProjectSensorContext(mock(DefaultInputProject.class), settings.asConfig(), settings, fs, activeRules, sensorStorage, runtime,
  branchConfiguration, writeCache, readCache, analysisCacheEnabled, unchangedFilesHandler, executingSensorContext, pluginRepository);

  private static final String PLUGIN_KEY = "org.sonarsource.pluginKey";

  @BeforeEach
  void prepare() {
    when(executingSensorContext.getSensorExecuting()).thenReturn(new SensorId(PLUGIN_KEY, "sensorName"));
  }


  @Test
  void addTelemetryProperty_whenTheOrganizationIsSonarSource_mustStoreTheTelemetry() {

    when(pluginRepository.getPluginInfo(PLUGIN_KEY)).thenReturn(new PluginInfo(PLUGIN_KEY).setOrganizationName("sonarsource"));

    underTest.addTelemetryProperty("key", "value");

    //then verify that the defaultStorage is called with the telemetry property once
    verify(sensorStorage).storeTelemetry("key", "value");
  }

  @Test
  void addTelemetryProperty_whenTheOrganizationIsNotSonarSource_mustThrowExcaption() {
    when(pluginRepository.getPluginInfo(PLUGIN_KEY)).thenReturn(new PluginInfo(PLUGIN_KEY).setOrganizationName("notSonarsource"));

    assertThrows(IllegalStateException.class, () -> underTest.addTelemetryProperty("key", "value"));

    verifyNoInteractions(sensorStorage);
  }
}
