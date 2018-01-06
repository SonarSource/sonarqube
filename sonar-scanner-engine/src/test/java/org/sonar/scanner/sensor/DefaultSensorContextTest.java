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
package org.sonar.scanner.sensor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.Version;
import org.sonar.scanner.scan.branch.BranchConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultSensorContextTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private ActiveRules activeRules;
  private DefaultFileSystem fs;
  private DefaultSensorContext adaptor;
  private MapSettings settings;
  private SensorStorage sensorStorage;
  private AnalysisMode analysisMode;
  private SonarRuntime runtime;
  private BranchConfiguration branchConfig;

  @Before
  public void prepare() throws Exception {
    activeRules = new ActiveRulesBuilder().build();
    fs = new DefaultFileSystem(temp.newFolder().toPath());
    MetricFinder metricFinder = mock(MetricFinder.class);
    when(metricFinder.<Integer>findByKey(CoreMetrics.NCLOC_KEY)).thenReturn(CoreMetrics.NCLOC);
    when(metricFinder.<String>findByKey(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION_KEY)).thenReturn(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION);
    settings = new MapSettings();
    branchConfig = mock(BranchConfiguration.class);
    sensorStorage = mock(SensorStorage.class);
    analysisMode = mock(AnalysisMode.class);
    runtime = SonarRuntimeImpl.forSonarQube(Version.parse("5.5"), SonarQubeSide.SCANNER);
    adaptor = new DefaultSensorContext(mock(InputModule.class), settings.asConfig(), settings, fs, activeRules, analysisMode, sensorStorage, runtime, branchConfig);
  }

  @Test
  public void shouldProvideComponents() {
    assertThat(adaptor.activeRules()).isEqualTo(activeRules);
    assertThat(adaptor.fileSystem()).isEqualTo(fs);
    assertThat(adaptor.settings()).isEqualTo(settings);
    assertThat(adaptor.getSonarQubeVersion()).isEqualTo(Version.parse("5.5"));
    assertThat(adaptor.runtime()).isEqualTo(runtime);

    assertThat(adaptor.newIssue()).isNotNull();
    assertThat(adaptor.newMeasure()).isNotNull();

    assertThat(adaptor.isCancelled()).isFalse();
  }

  @Test
  public void shouldSkipDupsAndCoverageOnShortBranches() {
    when(branchConfig.isShortLivingBranch()).thenReturn(true);
    assertThat(adaptor.newCpdTokens()).isEqualTo(DefaultSensorContext.NO_OP_NEW_CPD_TOKENS);
    assertThat(adaptor.newCoverage()).isEqualTo(DefaultSensorContext.NO_OP_NEW_COVERAGE);
  }
}
