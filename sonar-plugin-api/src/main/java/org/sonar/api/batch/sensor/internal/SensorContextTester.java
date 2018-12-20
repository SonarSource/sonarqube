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
package org.sonar.api.batch.sensor.internal;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.fs.internal.DefaultTextPointer;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.code.NewSignificantCode;
import org.sonar.api.batch.sensor.code.internal.DefaultSignificantCode;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.batch.sensor.coverage.internal.DefaultCoverage;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonar.api.batch.sensor.cpd.internal.DefaultCpdTokens;
import org.sonar.api.batch.sensor.cpd.internal.TokensLine;
import org.sonar.api.batch.sensor.error.AnalysisError;
import org.sonar.api.batch.sensor.error.NewAnalysisError;
import org.sonar.api.batch.sensor.error.internal.DefaultAnalysisError;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.highlighting.internal.DefaultHighlighting;
import org.sonar.api.batch.sensor.highlighting.internal.SyntaxHighlightingRule;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.internal.DefaultExternalIssue;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.batch.sensor.measure.NewMeasure;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.batch.sensor.rule.AdHocRule;
import org.sonar.api.batch.sensor.rule.NewAdHocRule;
import org.sonar.api.batch.sensor.rule.internal.DefaultAdHocRule;
import org.sonar.api.batch.sensor.symbol.NewSymbolTable;
import org.sonar.api.batch.sensor.symbol.internal.DefaultSymbolTable;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.Settings;
import org.sonar.api.config.internal.ConfigurationBridge;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.internal.ApiVersion;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.measures.Metric;
import org.sonar.api.scanner.fs.InputProject;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;

import static java.util.Collections.unmodifiableMap;

/**
 * Utility class to help testing {@link Sensor}. This is not an API and method signature may evolve.
 * <p>
 * Usage: call {@link #create(File)} to create an "in memory" implementation of {@link SensorContext} with a filesystem initialized with provided baseDir.
 * <p>
 * You have to manually register inputFiles using:
 * <pre>
 *   sensorContextTester.fileSystem().add(new DefaultInputFile("myProjectKey", "src/Foo.java")
 * .setLanguage("java")
 * .initMetadata("public class Foo {\n}"));
 * </pre>
 * <p>
 * Then pass it to your {@link Sensor}. You can then query elements provided by your sensor using methods {@link #allIssues()}, ...
 */
public class SensorContextTester implements SensorContext {

  private Settings settings;
  private DefaultFileSystem fs;
  private ActiveRules activeRules;
  private InMemorySensorStorage sensorStorage;
  private DefaultInputProject project;
  private DefaultInputModule module;
  private SonarRuntime runtime;
  private boolean cancelled;

  private SensorContextTester(Path moduleBaseDir) {
    this.settings = new MapSettings();
    this.fs = new DefaultFileSystem(moduleBaseDir).setEncoding(Charset.defaultCharset());
    this.activeRules = new ActiveRulesBuilder().build();
    this.sensorStorage = new InMemorySensorStorage();
    this.project = new DefaultInputProject(ProjectDefinition.create().setKey("projectKey").setBaseDir(moduleBaseDir.toFile()).setWorkDir(moduleBaseDir.resolve(".sonar").toFile()));
    this.module = new DefaultInputModule(ProjectDefinition.create().setKey("projectKey").setBaseDir(moduleBaseDir.toFile()).setWorkDir(moduleBaseDir.resolve(".sonar").toFile()));
    this.runtime = SonarRuntimeImpl.forSonarQube(ApiVersion.load(System2.INSTANCE), SonarQubeSide.SCANNER);
  }

  public static SensorContextTester create(File moduleBaseDir) {
    return new SensorContextTester(moduleBaseDir.toPath());
  }

  public static SensorContextTester create(Path moduleBaseDir) {
    return new SensorContextTester(moduleBaseDir);
  }

  @Override
  public Settings settings() {
    return settings;
  }

  @Override
  public Configuration config() {
    return new ConfigurationBridge(settings);
  }

  public SensorContextTester setSettings(Settings settings) {
    this.settings = settings;
    return this;
  }

  @Override
  public DefaultFileSystem fileSystem() {
    return fs;
  }

  public SensorContextTester setFileSystem(DefaultFileSystem fs) {
    this.fs = fs;
    return this;
  }

  @Override
  public ActiveRules activeRules() {
    return activeRules;
  }

  public SensorContextTester setActiveRules(ActiveRules activeRules) {
    this.activeRules = activeRules;
    return this;
  }

  /**
   * Default value is the version of this API at compilation time. You can override it
   * using {@link #setRuntime(SonarRuntime)} to test your Sensor behaviour.
   */
  @Override
  public Version getSonarQubeVersion() {
    return runtime().getApiVersion();
  }

  /**
   * @see #setRuntime(SonarRuntime) to override defaults (SonarQube scanner with version
   * of this API as used at compilation time).
   */
  @Override
  public SonarRuntime runtime() {
    return runtime;
  }

  public SensorContextTester setRuntime(SonarRuntime runtime) {
    this.runtime = runtime;
    return this;
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }

  @Override
  public InputModule module() {
    return module;
  }

  @Override
  public InputProject project() {
    return project;
  }

  @Override
  public <G extends Serializable> NewMeasure<G> newMeasure() {
    return new DefaultMeasure<>(sensorStorage);
  }

  public Collection<Measure> measures(String componentKey) {
    return sensorStorage.measuresByComponentAndMetric.row(componentKey).values();
  }

  public <G extends Serializable> Measure<G> measure(String componentKey, Metric<G> metric) {
    return measure(componentKey, metric.key());
  }

  public <G extends Serializable> Measure<G> measure(String componentKey, String metricKey) {
    return sensorStorage.measuresByComponentAndMetric.row(componentKey).get(metricKey);
  }

  @Override
  public NewIssue newIssue() {
    return new DefaultIssue(project, sensorStorage);
  }

  public Collection<Issue> allIssues() {
    return sensorStorage.allIssues;
  }

  @Override
  public NewExternalIssue newExternalIssue() {
    return new DefaultExternalIssue(project, sensorStorage);
  }

  @Override
  public NewAdHocRule newAdHocRule() {
    return new DefaultAdHocRule(sensorStorage);
  }

  public Collection<ExternalIssue> allExternalIssues() {
    return sensorStorage.allExternalIssues;
  }

  public Collection<AdHocRule> allAdHocRules() {
    return sensorStorage.allAdHocRules;
  }

  public Collection<AnalysisError> allAnalysisErrors() {
    return sensorStorage.allAnalysisErrors;
  }

  @CheckForNull
  public Integer lineHits(String fileKey, int line) {
    return sensorStorage.coverageByComponent.get(fileKey).stream()
      .map(c -> c.hitsByLine().get(line))
      .flatMap(Stream::of)
      .filter(Objects::nonNull)
      .reduce(null, SensorContextTester::sumOrNull);
  }

  @CheckForNull
  public static Integer sumOrNull(@Nullable Integer o1, @Nullable Integer o2) {
    return o1 == null ? o2 : (o1 + o2);
  }

  @CheckForNull
  public Integer conditions(String fileKey, int line) {
    return sensorStorage.coverageByComponent.get(fileKey).stream()
      .map(c -> c.conditionsByLine().get(line))
      .flatMap(Stream::of)
      .filter(Objects::nonNull)
      .reduce(null, SensorContextTester::maxOrNull);
  }

  @CheckForNull
  public Integer coveredConditions(String fileKey, int line) {
    return sensorStorage.coverageByComponent.get(fileKey).stream()
      .map(c -> c.coveredConditionsByLine().get(line))
      .flatMap(Stream::of)
      .filter(Objects::nonNull)
      .reduce(null, SensorContextTester::maxOrNull);
  }

  @CheckForNull
  public TextRange significantCodeTextRange(String fileKey, int line) {
    if (sensorStorage.significantCodePerComponent.containsKey(fileKey)) {
      return sensorStorage.significantCodePerComponent.get(fileKey)
        .significantCodePerLine()
        .get(line);
    }
    return null;

  }

  @CheckForNull
  public static Integer maxOrNull(@Nullable Integer o1, @Nullable Integer o2) {
    return o1 == null ? o2 : Math.max(o1, o2);
  }

  @CheckForNull
  public List<TokensLine> cpdTokens(String componentKey) {
    DefaultCpdTokens defaultCpdTokens = sensorStorage.cpdTokensByComponent.get(componentKey);
    return defaultCpdTokens != null ? defaultCpdTokens.getTokenLines() : null;
  }

  @Override
  public NewHighlighting newHighlighting() {
    return new DefaultHighlighting(sensorStorage);
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
  public NewSymbolTable newSymbolTable() {
    return new DefaultSymbolTable(sensorStorage);
  }

  @Override
  public NewAnalysisError newAnalysisError() {
    return new DefaultAnalysisError(sensorStorage);
  }

  /**
   * Return list of syntax highlighting applied for a given position in a file. The result is a list because in theory you
   * can apply several styles to the same range.
   *
   * @param componentKey Key of the file like 'myProjectKey:src/foo.php'
   * @param line         Line you want to query
   * @param lineOffset   Offset you want to query.
   * @return List of styles applied to this position or empty list if there is no highlighting at this position.
   */
  public List<TypeOfText> highlightingTypeAt(String componentKey, int line, int lineOffset) {
    DefaultHighlighting syntaxHighlightingData = sensorStorage.highlightingByComponent.get(componentKey);
    if (syntaxHighlightingData == null) {
      return Collections.emptyList();
    }
    List<TypeOfText> result = new ArrayList<>();
    DefaultTextPointer location = new DefaultTextPointer(line, lineOffset);
    for (SyntaxHighlightingRule sortedRule : syntaxHighlightingData.getSyntaxHighlightingRuleSet()) {
      if (sortedRule.range().start().compareTo(location) <= 0 && sortedRule.range().end().compareTo(location) > 0) {
        result.add(sortedRule.getTextType());
      }
    }
    return result;
  }

  /**
   * Return list of symbol references ranges for the symbol at a given position in a file.
   *
   * @param componentKey Key of the file like 'myProjectKey:src/foo.php'
   * @param line         Line you want to query
   * @param lineOffset   Offset you want to query.
   * @return List of references for the symbol (potentially empty) or null if there is no symbol at this position.
   */
  @CheckForNull
  public Collection<TextRange> referencesForSymbolAt(String componentKey, int line, int lineOffset) {
    DefaultSymbolTable symbolTable = sensorStorage.symbolsPerComponent.get(componentKey);
    if (symbolTable == null) {
      return null;
    }
    DefaultTextPointer location = new DefaultTextPointer(line, lineOffset);
    for (Map.Entry<TextRange, Set<TextRange>> symbol : symbolTable.getReferencesBySymbol().entrySet()) {
      if (symbol.getKey().start().compareTo(location) <= 0 && symbol.getKey().end().compareTo(location) > 0) {
        return symbol.getValue();
      }
    }
    return null;
  }

  @Override
  public void addContextProperty(String key, String value) {
    sensorStorage.storeProperty(key, value);
  }

  /**
   * @return an immutable map of the context properties defined with {@link SensorContext#addContextProperty(String, String)}.
   * @since 6.1
   */
  public Map<String, String> getContextProperties() {
    return unmodifiableMap(sensorStorage.contextProperties);
  }

  @Override
  public void markForPublishing(InputFile inputFile) {
    DefaultInputFile file = (DefaultInputFile) inputFile;
    file.setPublished(true);
  }

  @Override
  public NewSignificantCode newSignificantCode() {
    return new DefaultSignificantCode(sensorStorage);
  }
}
