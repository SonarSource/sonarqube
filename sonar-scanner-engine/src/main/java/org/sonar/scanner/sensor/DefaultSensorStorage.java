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
package org.sonar.scanner.sensor;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputComponent;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.batch.sensor.code.NewSignificantCode;
import org.sonar.api.batch.sensor.code.internal.DefaultSignificantCode;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.batch.sensor.coverage.internal.DefaultCoverage;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonar.api.batch.sensor.cpd.internal.DefaultCpdTokens;
import org.sonar.api.batch.sensor.error.AnalysisError;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.internal.DefaultHighlighting;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.batch.sensor.rule.AdHocRule;
import org.sonar.api.batch.sensor.symbol.NewSymbolTable;
import org.sonar.api.batch.sensor.symbol.internal.DefaultSymbolTable;
import org.sonar.api.config.Configuration;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.metric.ScannerMetrics;
import org.sonar.core.util.CloseableIterator;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.internal.pmd.PmdBlockChunker;
import org.sonar.scanner.cpd.index.SonarCpdBlockIndex;
import org.sonar.scanner.issue.IssuePublisher;
import org.sonar.scanner.protocol.Constants;
import org.sonar.scanner.protocol.output.FileStructure;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.report.ReportPublisher;
import org.sonar.scanner.report.ScannerReportUtils;
import org.sonar.scanner.repository.ContextPropertiesCache;
import org.sonar.scanner.scan.branch.BranchConfiguration;

import static java.lang.Math.max;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toList;
import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES_DATA_KEY;
import static org.sonar.api.measures.CoreMetrics.LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.PUBLIC_DOCUMENTED_API_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.TEST_SUCCESS_DENSITY_KEY;

public class DefaultSensorStorage implements SensorStorage {

  private static final Logger LOG = Loggers.get(DefaultSensorStorage.class);
  private static final int DEFAULT_CPD_MIN_LINES = 10;

  /**
   * The metrics that can be computed by analyzers but that are
   * filtered from analysis reports. That allows analyzers to continue
   * providing measures that are supported only by older versions.
   * <p>
   * The metrics in this list should not be declared in {@link ScannerMetrics#ALLOWED_CORE_METRICS}.
   */
  private static final Set<String> DEPRECATED_METRICS_KEYS = unmodifiableSet(new HashSet<>(asList(
    COMMENT_LINES_DATA_KEY)));

  /**
   * Metrics that were computed by analyzers and that are now computed
   * by core
   */
  private static final Set<String> NEWLY_CORE_METRICS_KEYS = unmodifiableSet(new HashSet<>(asList(
    // Computed on Scanner side
    LINES_KEY,
    // Computed on CE side
    TEST_SUCCESS_DENSITY_KEY,
    PUBLIC_DOCUMENTED_API_DENSITY_KEY)));

  private final MetricFinder metricFinder;
  private final IssuePublisher moduleIssues;
  private final ReportPublisher reportPublisher;
  private final SonarCpdBlockIndex index;
  private final ContextPropertiesCache contextPropertiesCache;
  private final Configuration settings;
  private final ScannerMetrics scannerMetrics;
  private final BranchConfiguration branchConfiguration;
  private final Set<String> alreadyLogged = new HashSet<>();

  public DefaultSensorStorage(MetricFinder metricFinder, IssuePublisher moduleIssues, Configuration settings,
    ReportPublisher reportPublisher, SonarCpdBlockIndex index,
    ContextPropertiesCache contextPropertiesCache, ScannerMetrics scannerMetrics, BranchConfiguration branchConfiguration) {
    this.metricFinder = metricFinder;
    this.moduleIssues = moduleIssues;
    this.settings = settings;
    this.reportPublisher = reportPublisher;
    this.index = index;
    this.contextPropertiesCache = contextPropertiesCache;
    this.scannerMetrics = scannerMetrics;
    this.branchConfiguration = branchConfiguration;
  }

  @Override
  public void store(Measure newMeasure) {
    saveMeasure(newMeasure.inputComponent(), (DefaultMeasure<?>) newMeasure);
  }

  private void logOnce(String metricKey, String msg, Object... params) {
    if (alreadyLogged.add(metricKey)) {
      LOG.warn(msg, params);
    }
  }

  private void saveMeasure(InputComponent component, DefaultMeasure<?> measure) {
    if (component.isFile()) {
      DefaultInputFile defaultInputFile = (DefaultInputFile) component;
      defaultInputFile.setPublished(true);
    }

    if (component instanceof InputDir || (component instanceof DefaultInputModule && ((DefaultInputModule) component).definition().getParent() != null)) {
      logOnce(measure.metric().key(), "Storing measures on folders or modules is deprecated. Provided value of metric '{}' is ignored.", measure.metric().key());
      return;
    }

    if (DEPRECATED_METRICS_KEYS.contains(measure.metric().key())) {
      logOnce(measure.metric().key(), "Metric '{}' is deprecated. Provided value is ignored.", measure.metric().key());
      return;
    }

    Metric metric = metricFinder.findByKey(measure.metric().key());
    if (metric == null) {
      throw new UnsupportedOperationException("Unknown metric: " + measure.metric().key());
    }

    if (!measure.isFromCore() && NEWLY_CORE_METRICS_KEYS.contains(measure.metric().key())) {
      logOnce(measure.metric().key(), "Metric '{}' is an internal metric computed by SonarQube/SonarCloud. Provided value is ignored.", measure.metric().key());
      return;
    }

    if (!scannerMetrics.getMetrics().contains(metric)) {
      throw new UnsupportedOperationException("Metric '" + metric.key() + "' should not be computed by a Sensor");
    }

    if (((DefaultInputComponent) component).hasMeasureFor(metric)) {
      throw new UnsupportedOperationException("Can not add the same measure twice on " + component + ": " + measure);
    }
    ((DefaultInputComponent) component).setHasMeasureFor(metric);
    if (metric.key().equals(CoreMetrics.EXECUTABLE_LINES_DATA_KEY)) {
      if (component.isFile()) {
        ((DefaultInputFile) component).setExecutableLines(
          KeyValueFormat.parseIntInt((String) measure.value()).entrySet().stream().filter(e -> e.getValue() > 0).map(Map.Entry::getKey).collect(Collectors.toSet()));
      } else {
        throw new IllegalArgumentException("Executable lines can only be saved on files");
      }
    }
    reportPublisher.getWriter().appendComponentMeasure(((DefaultInputComponent) component).scannerId(), toReportMeasure(measure));
  }

  public static ScannerReport.Measure toReportMeasure(DefaultMeasure measureToSave) {
    ScannerReport.Measure.Builder builder = ScannerReport.Measure.newBuilder();
    builder.setMetricKey(measureToSave.metric().key());
    setValueAccordingToType(builder, measureToSave);
    return builder.build();
  }

  private static void setValueAccordingToType(ScannerReport.Measure.Builder builder, DefaultMeasure<?> measure) {
    Serializable value = measure.value();
    Metric<?> metric = measure.metric();
    if (Boolean.class.equals(metric.valueType())) {
      builder.setBooleanValue(ScannerReport.Measure.BoolValue.newBuilder().setValue((Boolean) value));
    } else if (Integer.class.equals(metric.valueType())) {
      builder.setIntValue(ScannerReport.Measure.IntValue.newBuilder().setValue(((Number) value).intValue()));
    } else if (Double.class.equals(metric.valueType())) {
      builder.setDoubleValue(ScannerReport.Measure.DoubleValue.newBuilder().setValue(((Number) value).doubleValue()));
    } else if (String.class.equals(metric.valueType())) {
      builder.setStringValue(ScannerReport.Measure.StringValue.newBuilder().setValue((String) value));
    } else if (Long.class.equals(metric.valueType())) {
      builder.setLongValue(ScannerReport.Measure.LongValue.newBuilder().setValue(((Number) value).longValue()));
    } else {
      throw new UnsupportedOperationException("Unsupported type :" + metric.valueType());
    }
  }

  private boolean shouldSkipStorage(DefaultInputFile defaultInputFile) {
    return branchConfiguration.isShortOrPullRequest() && defaultInputFile.status() == InputFile.Status.SAME;
  }

  /**
   * Thread safe assuming that each issues for each file are only written once.
   */
  @Override
  public void store(Issue issue) {
    if (issue.primaryLocation().inputComponent() instanceof DefaultInputFile) {
      DefaultInputFile defaultInputFile = (DefaultInputFile) issue.primaryLocation().inputComponent();
      if (shouldSkipStorage(defaultInputFile)) {
        return;
      }
      defaultInputFile.setPublished(true);
    }
    moduleIssues.initAndAddIssue(issue);
  }

  /**
   * Thread safe assuming that each issues for each file are only written once.
   */
  @Override
  public void store(ExternalIssue externalIssue) {
    if (externalIssue.primaryLocation().inputComponent() instanceof DefaultInputFile) {
      DefaultInputFile defaultInputFile = (DefaultInputFile) externalIssue.primaryLocation().inputComponent();
      defaultInputFile.setPublished(true);
    }
    moduleIssues.initAndAddExternalIssue(externalIssue);
  }

  @Override
  public void store(AdHocRule adHocRule) {
    ScannerReportWriter writer = reportPublisher.getWriter();
    final ScannerReport.AdHocRule.Builder builder = ScannerReport.AdHocRule.newBuilder();
    builder.setEngineId(adHocRule.engineId());
    builder.setRuleId(adHocRule.ruleId());
    builder.setName(adHocRule.name());
    String description = adHocRule.description();
    if (description != null) {
      builder.setDescription(description);
    }
    builder.setSeverity(Constants.Severity.valueOf(adHocRule.severity().name()));
    builder.setType(ScannerReport.IssueType.valueOf(adHocRule.type().name()));
    writer.appendAdHocRule(builder.build());
  }

  @Override
  public void store(NewHighlighting newHighlighting) {
    DefaultHighlighting highlighting = (DefaultHighlighting) newHighlighting;
    ScannerReportWriter writer = reportPublisher.getWriter();
    DefaultInputFile inputFile = (DefaultInputFile) highlighting.inputFile();
    if (shouldSkipStorage(inputFile)) {
      return;
    }
    inputFile.setPublished(true);
    int componentRef = inputFile.scannerId();
    if (writer.hasComponentData(FileStructure.Domain.SYNTAX_HIGHLIGHTINGS, componentRef)) {
      throw new UnsupportedOperationException("Trying to save highlighting twice for the same file is not supported: " + inputFile);
    }
    final ScannerReport.SyntaxHighlightingRule.Builder builder = ScannerReport.SyntaxHighlightingRule.newBuilder();
    final ScannerReport.TextRange.Builder rangeBuilder = ScannerReport.TextRange.newBuilder();

    writer.writeComponentSyntaxHighlighting(componentRef,
      highlighting.getSyntaxHighlightingRuleSet().stream()
        .map(input -> {
          builder.setRange(rangeBuilder.setStartLine(input.range().start().line())
            .setStartOffset(input.range().start().lineOffset())
            .setEndLine(input.range().end().line())
            .setEndOffset(input.range().end().lineOffset())
            .build());
          builder.setType(ScannerReportUtils.toProtocolType(input.getTextType()));
          return builder.build();
        }).collect(toList()));
  }

  @Override
  public void store(NewSymbolTable newSymbolTable) {
    DefaultSymbolTable symbolTable = (DefaultSymbolTable) newSymbolTable;
    ScannerReportWriter writer = reportPublisher.getWriter();
    DefaultInputFile inputFile = (DefaultInputFile) symbolTable.inputFile();
    if (shouldSkipStorage(inputFile)) {
      return;
    }
    inputFile.setPublished(true);
    int componentRef = inputFile.scannerId();
    if (writer.hasComponentData(FileStructure.Domain.SYMBOLS, componentRef)) {
      throw new UnsupportedOperationException("Trying to save symbol table twice for the same file is not supported: " + symbolTable.inputFile());
    }
    final ScannerReport.Symbol.Builder builder = ScannerReport.Symbol.newBuilder();
    final ScannerReport.TextRange.Builder rangeBuilder = ScannerReport.TextRange.newBuilder();
    writer.writeComponentSymbols(componentRef,
      symbolTable.getReferencesBySymbol().entrySet().stream()
        .map(input -> {
          builder.clear();
          rangeBuilder.clear();
          TextRange declaration = input.getKey();
          builder.setDeclaration(rangeBuilder.setStartLine(declaration.start().line())
            .setStartOffset(declaration.start().lineOffset())
            .setEndLine(declaration.end().line())
            .setEndOffset(declaration.end().lineOffset())
            .build());
          for (TextRange reference : input.getValue()) {
            builder.addReference(rangeBuilder.setStartLine(reference.start().line())
              .setStartOffset(reference.start().lineOffset())
              .setEndLine(reference.end().line())
              .setEndOffset(reference.end().lineOffset())
              .build());
          }
          return builder.build();
        }).collect(Collectors.toList()));
  }

  @Override
  public void store(NewCoverage coverage) {
    DefaultCoverage defaultCoverage = (DefaultCoverage) coverage;
    DefaultInputFile inputFile = (DefaultInputFile) defaultCoverage.inputFile();
    inputFile.setPublished(true);

    Map<Integer, ScannerReport.LineCoverage.Builder> coveragePerLine = reloadExistingCoverage(inputFile);

    int lineCount = inputFile.lines();
    mergeLineCoverageValues(lineCount, defaultCoverage.hitsByLine(), coveragePerLine, (value, builder) -> builder.setHits(builder.getHits() || value > 0));
    mergeLineCoverageValues(lineCount, defaultCoverage.conditionsByLine(), coveragePerLine, (value, builder) -> builder.setConditions(max(value, builder.getConditions())));
    mergeLineCoverageValues(lineCount, defaultCoverage.coveredConditionsByLine(), coveragePerLine,
      (value, builder) -> builder.setCoveredConditions(max(value, builder.getCoveredConditions())));

    reportPublisher.getWriter().writeComponentCoverage(inputFile.scannerId(),
      coveragePerLine.values().stream().map(ScannerReport.LineCoverage.Builder::build).collect(Collectors.toList()));

  }

  private Map<Integer, ScannerReport.LineCoverage.Builder> reloadExistingCoverage(DefaultInputFile inputFile) {
    Map<Integer, ScannerReport.LineCoverage.Builder> coveragePerLine = new LinkedHashMap<>();
    try (CloseableIterator<ScannerReport.LineCoverage> lineCoverageCloseableIterator = reportPublisher.getReader().readComponentCoverage(inputFile.scannerId())) {
      while (lineCoverageCloseableIterator.hasNext()) {
        final ScannerReport.LineCoverage lineCoverage = lineCoverageCloseableIterator.next();
        coveragePerLine.put(lineCoverage.getLine(), ScannerReport.LineCoverage.newBuilder(lineCoverage));
      }
    }
    return coveragePerLine;
  }

  interface LineCoverageOperation {
    void apply(Integer value, ScannerReport.LineCoverage.Builder builder);
  }

  private void mergeLineCoverageValues(int lineCount, SortedMap<Integer, Integer> valueByLine, Map<Integer, ScannerReport.LineCoverage.Builder> coveragePerLine,
    LineCoverageOperation op) {
    for (Map.Entry<Integer, Integer> lineMeasure : valueByLine.entrySet()) {
      int lineIdx = lineMeasure.getKey();
      if (lineIdx <= lineCount) {
        Integer value = lineMeasure.getValue();
        op.apply(value, coveragePerLine.computeIfAbsent(lineIdx, line -> ScannerReport.LineCoverage.newBuilder().setLine(line)));
      }
    }
  }

  @Override
  public void store(NewCpdTokens cpdTokens) {
    DefaultCpdTokens defaultCpdTokens = (DefaultCpdTokens) cpdTokens;
    DefaultInputFile inputFile = (DefaultInputFile) defaultCpdTokens.inputFile();
    inputFile.setPublished(true);
    PmdBlockChunker blockChunker = new PmdBlockChunker(getCpdBlockSize(inputFile.language()));
    List<Block> blocks = blockChunker.chunk(inputFile.key(), defaultCpdTokens.getTokenLines());
    index.insert(inputFile, blocks);
  }

  private int getCpdBlockSize(@Nullable String languageKey) {
    if (languageKey == null) {
      return DEFAULT_CPD_MIN_LINES;
    }
    return settings.getInt("sonar.cpd." + languageKey + ".minimumLines")
      .orElseGet(() -> {
        if ("cobol".equals(languageKey)) {
          return 30;
        }
        if ("abap".equals(languageKey)) {
          return 20;
        }
        return DEFAULT_CPD_MIN_LINES;
      });
  }

  @Override
  public void store(AnalysisError analysisError) {
    DefaultInputFile defaultInputFile = (DefaultInputFile) analysisError.inputFile();
    if (shouldSkipStorage(defaultInputFile)) {
      return;
    }
    defaultInputFile.setPublished(true);
  }

  @Override
  public void storeProperty(String key, String value) {
    contextPropertiesCache.put(key, value);
  }

  @Override
  public void store(NewSignificantCode newSignificantCode) {
    DefaultSignificantCode significantCode = (DefaultSignificantCode) newSignificantCode;
    ScannerReportWriter writer = reportPublisher.getWriter();
    DefaultInputFile inputFile = (DefaultInputFile) significantCode.inputFile();
    if (shouldSkipStorage(inputFile)) {
      return;
    }
    inputFile.setPublished(true);
    int componentRef = inputFile.scannerId();
    if (writer.hasComponentData(FileStructure.Domain.SGNIFICANT_CODE, componentRef)) {
      throw new UnsupportedOperationException(
        "Trying to save significant code information twice for the same file is not supported: " + significantCode.inputFile());
    }

    List<ScannerReport.LineSgnificantCode> protobuf = significantCode.significantCodePerLine().values().stream()
      .map(range -> ScannerReport.LineSgnificantCode.newBuilder()
        .setLine(range.start().line())
        .setStartOffset(range.start().lineOffset())
        .setEndOffset(range.end().lineOffset())
        .build())
      .collect(Collectors.toList());

    writer.writeComponentSignificantCode(componentRef, protobuf);
  }
}
