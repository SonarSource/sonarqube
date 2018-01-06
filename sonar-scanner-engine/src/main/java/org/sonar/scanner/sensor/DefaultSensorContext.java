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

import java.io.Serializable;
import javax.annotation.concurrent.ThreadSafe;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.batch.sensor.coverage.internal.DefaultCoverage;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonar.api.batch.sensor.cpd.internal.DefaultCpdTokens;
import org.sonar.api.batch.sensor.error.NewAnalysisError;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.internal.DefaultHighlighting;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonar.api.batch.sensor.measure.NewMeasure;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.batch.sensor.symbol.NewSymbolTable;
import org.sonar.api.batch.sensor.symbol.internal.DefaultSymbolTable;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.Version;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.sensor.noop.NoOpNewAnalysisError;
import org.sonar.scanner.sensor.noop.NoOpNewCoverage;
import org.sonar.scanner.sensor.noop.NoOpNewCpdTokens;
import org.sonar.scanner.sensor.noop.NoOpNewHighlighting;
import org.sonar.scanner.sensor.noop.NoOpNewSymbolTable;

@ThreadSafe
public class DefaultSensorContext implements SensorContext {

  private static final NoOpNewHighlighting NO_OP_NEW_HIGHLIGHTING = new NoOpNewHighlighting();
  private static final NoOpNewSymbolTable NO_OP_NEW_SYMBOL_TABLE = new NoOpNewSymbolTable();
  static final NoOpNewCpdTokens NO_OP_NEW_CPD_TOKENS = new NoOpNewCpdTokens();
  private static final NoOpNewAnalysisError NO_OP_NEW_ANALYSIS_ERROR = new NoOpNewAnalysisError();
  static final NoOpNewCoverage NO_OP_NEW_COVERAGE = new NoOpNewCoverage();

  private final Settings mutableSettings;
  private final FileSystem fs;
  private final ActiveRules activeRules;
  private final SensorStorage sensorStorage;
  private final AnalysisMode analysisMode;
  private final InputModule module;
  private final SonarRuntime sonarRuntime;
  private final Configuration config;
  private final BranchConfiguration branchConfiguration;

  public DefaultSensorContext(InputModule module, Configuration config, Settings mutableSettings, FileSystem fs, ActiveRules activeRules,
    AnalysisMode analysisMode, SensorStorage sensorStorage, SonarRuntime sonarRuntime, BranchConfiguration branchConfiguration) {
    this.module = module;
    this.config = config;
    this.mutableSettings = mutableSettings;
    this.fs = fs;
    this.activeRules = activeRules;
    this.analysisMode = analysisMode;
    this.sensorStorage = sensorStorage;
    this.sonarRuntime = sonarRuntime;
    this.branchConfiguration = branchConfiguration;
  }

  @Override
  public Settings settings() {
    return mutableSettings;
  }

  @Override
  public Configuration config() {
    return config;
  }

  @Override
  public FileSystem fileSystem() {
    return fs;
  }

  @Override
  public ActiveRules activeRules() {
    return activeRules;
  }

  @Override
  public InputModule module() {
    return module;
  }

  @Override
  public Version getSonarQubeVersion() {
    return sonarRuntime.getApiVersion();
  }

  @Override
  public SonarRuntime runtime() {
    return sonarRuntime;
  }

  @Override
  public <G extends Serializable> NewMeasure<G> newMeasure() {
    return new DefaultMeasure<>(sensorStorage);
  }

  @Override
  public NewIssue newIssue() {
    return new DefaultIssue(sensorStorage);
  }

  @Override
  public NewHighlighting newHighlighting() {
    if (analysisMode.isIssues()) {
      return NO_OP_NEW_HIGHLIGHTING;
    }
    return new DefaultHighlighting(sensorStorage);
  }

  @Override
  public NewSymbolTable newSymbolTable() {
    if (analysisMode.isIssues()) {
      return NO_OP_NEW_SYMBOL_TABLE;
    }
    return new DefaultSymbolTable(sensorStorage);
  }

  @Override
  public NewCoverage newCoverage() {
    if (branchConfiguration.isShortLivingBranch()) {
      return NO_OP_NEW_COVERAGE;
    }
    return new DefaultCoverage(sensorStorage);
  }

  @Override
  public NewCpdTokens newCpdTokens() {
    if (analysisMode.isIssues() || branchConfiguration.isShortLivingBranch()) {
      return NO_OP_NEW_CPD_TOKENS;
    }
    return new DefaultCpdTokens(config, sensorStorage);
  }

  @Override
  public NewAnalysisError newAnalysisError() {
    return NO_OP_NEW_ANALYSIS_ERROR;
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public void addContextProperty(String key, String value) {
    sensorStorage.storeProperty(key, value);
  }

  @Override
  public void markForPublishing(InputFile inputFile) {
    DefaultInputFile file = (DefaultInputFile) inputFile;
    file.setPublished(true);
  }
}
