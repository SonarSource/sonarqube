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

import java.io.Serializable;
import javax.annotation.concurrent.ThreadSafe;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.batch.sensor.cache.WriteCache;
import org.sonar.api.batch.sensor.code.NewSignificantCode;
import org.sonar.api.batch.sensor.code.internal.DefaultSignificantCode;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.batch.sensor.coverage.internal.DefaultCoverage;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonar.api.batch.sensor.cpd.internal.DefaultCpdTokens;
import org.sonar.api.batch.sensor.error.NewAnalysisError;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.internal.DefaultHighlighting;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.internal.DefaultExternalIssue;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonar.api.batch.sensor.measure.NewMeasure;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.batch.sensor.rule.NewAdHocRule;
import org.sonar.api.batch.sensor.rule.internal.DefaultAdHocRule;
import org.sonar.api.batch.sensor.symbol.NewSymbolTable;
import org.sonar.api.batch.sensor.symbol.internal.DefaultSymbolTable;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.Settings;
import org.sonar.api.scanner.fs.InputProject;
import org.sonar.api.utils.Version;
import org.sonar.core.platform.PluginInfo;
import org.sonar.scanner.bootstrap.ScannerPluginRepository;
import org.sonar.scanner.cache.AnalysisCacheEnabled;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.sensor.noop.NoOpNewAnalysisError;

@ThreadSafe
public class ProjectSensorContext implements SensorContext {

  static final NoOpNewAnalysisError NO_OP_NEW_ANALYSIS_ERROR = new NoOpNewAnalysisError();

  private final Settings mutableSettings;
  private final FileSystem fs;
  private final ActiveRules activeRules;
  private final DefaultSensorStorage sensorStorage;
  private final DefaultInputProject project;
  private final SonarRuntime sonarRuntime;
  private final Configuration config;
  private final boolean skipUnchangedFiles;
  private final UnchangedFilesHandler unchangedFilesHandler;
  private final WriteCache writeCache;
  private final ReadCache readCache;
  private final AnalysisCacheEnabled analysisCacheEnabled;
  private final ExecutingSensorContext executingSensorContext;
  private final ScannerPluginRepository pluginRepo;

  public ProjectSensorContext(DefaultInputProject project, Configuration config, Settings mutableSettings, FileSystem fs,
                              ActiveRules activeRules,
                              DefaultSensorStorage sensorStorage, SonarRuntime sonarRuntime, BranchConfiguration branchConfiguration,
                              WriteCache writeCache, ReadCache readCache,
                              AnalysisCacheEnabled analysisCacheEnabled, UnchangedFilesHandler unchangedFilesHandler,
                              ExecutingSensorContext executingSensorContext, ScannerPluginRepository pluginRepo) {
    this.project = project;
    this.config = config;
    this.mutableSettings = mutableSettings;
    this.fs = fs;
    this.activeRules = activeRules;
    this.sensorStorage = sensorStorage;
    this.sonarRuntime = sonarRuntime;
    this.writeCache = writeCache;
    this.readCache = readCache;
    this.analysisCacheEnabled = analysisCacheEnabled;
    this.skipUnchangedFiles = branchConfiguration.isPullRequest();
    this.unchangedFilesHandler = unchangedFilesHandler;
    this.executingSensorContext = executingSensorContext;
    this.pluginRepo = pluginRepo;
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
    throw new UnsupportedOperationException("No modules for global Sensors");
  }

  @Override
  public InputProject project() {
    return project;
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
    return new DefaultIssue(project, sensorStorage);
  }

  @Override
  public NewExternalIssue newExternalIssue() {
    return new DefaultExternalIssue(project, sensorStorage);
  }

  @Override
  public NewAdHocRule newAdHocRule() {
    return new DefaultAdHocRule(sensorStorage);
  }

  @Override
  public NewHighlighting newHighlighting() {
    return new DefaultHighlighting(sensorStorage);
  }

  @Override
  public NewSymbolTable newSymbolTable() {
    return new DefaultSymbolTable(sensorStorage);
  }

  @Override
  public NewCoverage newCoverage() {
    return new DefaultCoverage(sensorStorage);
  }

  @Override
  public NewCpdTokens newCpdTokens() {
    return new DefaultCpdTokens(sensorStorage);
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

  @Override
  public void markAsUnchanged(InputFile inputFile) {
    unchangedFilesHandler.markAsUnchanged((DefaultInputFile) inputFile);
  }

  @Override
  public WriteCache nextCache() {
    return writeCache;
  }

  @Override
  public ReadCache previousCache() {
    return readCache;
  }

  @Override
  public boolean isCacheEnabled() {
    return analysisCacheEnabled.isEnabled();
  }

  @Override
  public void addTelemetryProperty(String key, String value) {
    if (isSonarSourcePlugin()) {
      this.sensorStorage.storeTelemetry(key, value);
    } else {
      throw new IllegalStateException("Telemetry properties can only be added by SonarSource plugins");
    }
  }

  @Override
  public NewSignificantCode newSignificantCode() {
    return new DefaultSignificantCode(sensorStorage);
  }

  @Override
  public boolean canSkipUnchangedFiles() {
    return this.skipUnchangedFiles;
  }

  private boolean isSonarSourcePlugin() {
    SensorId sensorExecuting = executingSensorContext.getSensorExecuting();
    if (sensorExecuting != null) {
      PluginInfo pluginInfo = pluginRepo.getPluginInfo(sensorExecuting.getPluginKey());
      return "sonarsource".equalsIgnoreCase(pluginInfo.getOrganizationName());
    }
    return false;
  }
}
