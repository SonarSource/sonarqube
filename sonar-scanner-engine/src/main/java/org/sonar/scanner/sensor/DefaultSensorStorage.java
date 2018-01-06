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

import com.google.common.annotations.VisibleForTesting;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.batch.sensor.coverage.internal.DefaultCoverage;
import org.sonar.api.batch.sensor.cpd.internal.DefaultCpdTokens;
import org.sonar.api.batch.sensor.error.AnalysisError;
import org.sonar.api.batch.sensor.highlighting.internal.DefaultHighlighting;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.batch.sensor.symbol.internal.DefaultSymbolTable;
import org.sonar.api.config.Configuration;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.metric.ScannerMetrics;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.internal.pmd.PmdBlockChunker;
import org.sonar.scanner.cpd.deprecated.DefaultCpdBlockIndexer;
import org.sonar.scanner.cpd.index.SonarCpdBlockIndex;
import org.sonar.scanner.issue.ModuleIssues;
import org.sonar.scanner.protocol.output.FileStructure;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.report.ReportPublisher;
import org.sonar.scanner.report.ScannerReportUtils;
import org.sonar.scanner.repository.ContextPropertiesCache;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.measure.MeasureCache;

import static java.util.stream.Collectors.toList;
import static org.sonar.api.measures.CoreMetrics.BRANCH_COVERAGE;
import static org.sonar.api.measures.CoreMetrics.COMMENTED_OUT_CODE_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.CONDITIONS_BY_LINE;
import static org.sonar.api.measures.CoreMetrics.CONDITIONS_TO_COVER;
import static org.sonar.api.measures.CoreMetrics.COVERAGE;
import static org.sonar.api.measures.CoreMetrics.COVERAGE_LINE_HITS_DATA;
import static org.sonar.api.measures.CoreMetrics.COVERED_CONDITIONS_BY_LINE;
import static org.sonar.api.measures.CoreMetrics.DEPENDENCY_MATRIX_KEY;
import static org.sonar.api.measures.CoreMetrics.DIRECTORY_CYCLES_KEY;
import static org.sonar.api.measures.CoreMetrics.DIRECTORY_EDGES_WEIGHT_KEY;
import static org.sonar.api.measures.CoreMetrics.DIRECTORY_FEEDBACK_EDGES_KEY;
import static org.sonar.api.measures.CoreMetrics.DIRECTORY_TANGLES_KEY;
import static org.sonar.api.measures.CoreMetrics.DIRECTORY_TANGLE_INDEX_KEY;
import static org.sonar.api.measures.CoreMetrics.FILE_CYCLES_KEY;
import static org.sonar.api.measures.CoreMetrics.FILE_EDGES_WEIGHT_KEY;
import static org.sonar.api.measures.CoreMetrics.FILE_FEEDBACK_EDGES_KEY;
import static org.sonar.api.measures.CoreMetrics.FILE_TANGLES_KEY;
import static org.sonar.api.measures.CoreMetrics.FILE_TANGLE_INDEX_KEY;
import static org.sonar.api.measures.CoreMetrics.IT_BRANCH_COVERAGE;
import static org.sonar.api.measures.CoreMetrics.IT_CONDITIONS_BY_LINE;
import static org.sonar.api.measures.CoreMetrics.IT_CONDITIONS_TO_COVER;
import static org.sonar.api.measures.CoreMetrics.IT_COVERAGE;
import static org.sonar.api.measures.CoreMetrics.IT_COVERAGE_LINE_HITS_DATA;
import static org.sonar.api.measures.CoreMetrics.IT_COVERED_CONDITIONS_BY_LINE;
import static org.sonar.api.measures.CoreMetrics.IT_LINES_TO_COVER;
import static org.sonar.api.measures.CoreMetrics.IT_LINE_COVERAGE;
import static org.sonar.api.measures.CoreMetrics.IT_UNCOVERED_CONDITIONS;
import static org.sonar.api.measures.CoreMetrics.IT_UNCOVERED_LINES;
import static org.sonar.api.measures.CoreMetrics.LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.LINES_TO_COVER;
import static org.sonar.api.measures.CoreMetrics.LINE_COVERAGE;
import static org.sonar.api.measures.CoreMetrics.OVERALL_BRANCH_COVERAGE;
import static org.sonar.api.measures.CoreMetrics.OVERALL_CONDITIONS_BY_LINE;
import static org.sonar.api.measures.CoreMetrics.OVERALL_CONDITIONS_TO_COVER;
import static org.sonar.api.measures.CoreMetrics.OVERALL_COVERAGE;
import static org.sonar.api.measures.CoreMetrics.OVERALL_COVERAGE_LINE_HITS_DATA;
import static org.sonar.api.measures.CoreMetrics.OVERALL_COVERED_CONDITIONS_BY_LINE;
import static org.sonar.api.measures.CoreMetrics.OVERALL_LINES_TO_COVER;
import static org.sonar.api.measures.CoreMetrics.OVERALL_LINE_COVERAGE;
import static org.sonar.api.measures.CoreMetrics.OVERALL_UNCOVERED_CONDITIONS;
import static org.sonar.api.measures.CoreMetrics.OVERALL_UNCOVERED_LINES;
import static org.sonar.api.measures.CoreMetrics.PUBLIC_DOCUMENTED_API_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.TEST_SUCCESS_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.UNCOVERED_CONDITIONS;
import static org.sonar.api.measures.CoreMetrics.UNCOVERED_LINES;

public class DefaultSensorStorage implements SensorStorage {

  private static final Logger LOG = Loggers.get(DefaultSensorStorage.class);

  private static final List<String> DEPRECATED_METRICS_KEYS = Arrays.asList(
    DEPENDENCY_MATRIX_KEY,
    DIRECTORY_CYCLES_KEY,
    DIRECTORY_EDGES_WEIGHT_KEY,
    DIRECTORY_FEEDBACK_EDGES_KEY,
    DIRECTORY_TANGLE_INDEX_KEY,
    DIRECTORY_TANGLES_KEY,
    FILE_CYCLES_KEY,
    FILE_EDGES_WEIGHT_KEY,
    FILE_FEEDBACK_EDGES_KEY,
    FILE_TANGLE_INDEX_KEY,
    FILE_TANGLES_KEY,
    // SONARPHP-621
    COMMENTED_OUT_CODE_LINES_KEY);

  // Some Sensors still save those metrics
  private static final List<String> PLATFORM_METRICS_KEYS = Arrays.asList(
    // Computed on Scanner side
    LINES_KEY,
    // Computed on CE side
    TEST_SUCCESS_DENSITY_KEY,
    PUBLIC_DOCUMENTED_API_DENSITY_KEY);

  private final MetricFinder metricFinder;
  private final ModuleIssues moduleIssues;
  private final ReportPublisher reportPublisher;
  private final MeasureCache measureCache;
  private final SonarCpdBlockIndex index;
  private final ContextPropertiesCache contextPropertiesCache;
  private final Configuration settings;
  private final ScannerMetrics scannerMetrics;
  private final BranchConfiguration branchConfiguration;
  private final Map<Metric<?>, Metric<?>> deprecatedCoverageMetricMapping = new HashMap<>();
  private final Set<Metric<?>> coverageMetrics = new HashSet<>();
  private final Set<Metric<?>> byLineMetrics = new HashSet<>();
  private final Set<String> alreadyLogged = new HashSet<>();

  public DefaultSensorStorage(MetricFinder metricFinder, ModuleIssues moduleIssues, Configuration settings,
    ReportPublisher reportPublisher, MeasureCache measureCache, SonarCpdBlockIndex index,
    ContextPropertiesCache contextPropertiesCache, ScannerMetrics scannerMetrics, BranchConfiguration branchConfiguration) {
    this.metricFinder = metricFinder;
    this.moduleIssues = moduleIssues;
    this.settings = settings;
    this.reportPublisher = reportPublisher;
    this.measureCache = measureCache;
    this.index = index;
    this.contextPropertiesCache = contextPropertiesCache;
    this.scannerMetrics = scannerMetrics;
    this.branchConfiguration = branchConfiguration;

    coverageMetrics.add(UNCOVERED_LINES);
    coverageMetrics.add(LINES_TO_COVER);
    coverageMetrics.add(UNCOVERED_CONDITIONS);
    coverageMetrics.add(CONDITIONS_TO_COVER);
    coverageMetrics.add(CONDITIONS_BY_LINE);
    coverageMetrics.add(COVERED_CONDITIONS_BY_LINE);
    coverageMetrics.add(COVERAGE_LINE_HITS_DATA);

    byLineMetrics.add(COVERAGE_LINE_HITS_DATA);
    byLineMetrics.add(COVERED_CONDITIONS_BY_LINE);
    byLineMetrics.add(CONDITIONS_BY_LINE);

    deprecatedCoverageMetricMapping.put(IT_COVERAGE, COVERAGE);
    deprecatedCoverageMetricMapping.put(IT_LINE_COVERAGE, LINE_COVERAGE);
    deprecatedCoverageMetricMapping.put(IT_BRANCH_COVERAGE, BRANCH_COVERAGE);
    deprecatedCoverageMetricMapping.put(IT_UNCOVERED_LINES, UNCOVERED_LINES);
    deprecatedCoverageMetricMapping.put(IT_LINES_TO_COVER, LINES_TO_COVER);
    deprecatedCoverageMetricMapping.put(IT_UNCOVERED_CONDITIONS, UNCOVERED_CONDITIONS);
    deprecatedCoverageMetricMapping.put(IT_CONDITIONS_TO_COVER, CONDITIONS_TO_COVER);
    deprecatedCoverageMetricMapping.put(IT_CONDITIONS_BY_LINE, CONDITIONS_BY_LINE);
    deprecatedCoverageMetricMapping.put(IT_COVERED_CONDITIONS_BY_LINE, COVERED_CONDITIONS_BY_LINE);
    deprecatedCoverageMetricMapping.put(IT_COVERAGE_LINE_HITS_DATA, COVERAGE_LINE_HITS_DATA);
    deprecatedCoverageMetricMapping.put(OVERALL_COVERAGE, COVERAGE);
    deprecatedCoverageMetricMapping.put(OVERALL_LINE_COVERAGE, LINE_COVERAGE);
    deprecatedCoverageMetricMapping.put(OVERALL_BRANCH_COVERAGE, BRANCH_COVERAGE);
    deprecatedCoverageMetricMapping.put(OVERALL_UNCOVERED_LINES, UNCOVERED_LINES);
    deprecatedCoverageMetricMapping.put(OVERALL_LINES_TO_COVER, LINES_TO_COVER);
    deprecatedCoverageMetricMapping.put(OVERALL_UNCOVERED_CONDITIONS, UNCOVERED_CONDITIONS);
    deprecatedCoverageMetricMapping.put(OVERALL_CONDITIONS_TO_COVER, CONDITIONS_TO_COVER);
    deprecatedCoverageMetricMapping.put(OVERALL_CONDITIONS_BY_LINE, CONDITIONS_BY_LINE);
    deprecatedCoverageMetricMapping.put(OVERALL_COVERED_CONDITIONS_BY_LINE, COVERED_CONDITIONS_BY_LINE);
    deprecatedCoverageMetricMapping.put(OVERALL_COVERAGE_LINE_HITS_DATA, COVERAGE_LINE_HITS_DATA);
  }

  @Override
  public void store(Measure newMeasure) {
    if (newMeasure.inputComponent() instanceof DefaultInputFile) {
      DefaultInputFile defaultInputFile = (DefaultInputFile) newMeasure.inputComponent();
      if (shouldSkipStorage(defaultInputFile)) {
        return;
      }
      defaultInputFile.setPublished(true);
    }
    saveMeasure(newMeasure.inputComponent(), (DefaultMeasure<?>) newMeasure);
  }

  private void logOnce(String metricKey, String msg, Object... params) {
    if (alreadyLogged.add(metricKey)) {
      LOG.warn(msg, params);
    }
  }

  public void saveMeasure(InputComponent component, DefaultMeasure<?> measure) {
    if (component.isFile()) {
      DefaultInputFile defaultInputFile = (DefaultInputFile) component;
      if (shouldSkipStorage(defaultInputFile)) {
        return;
      }
      defaultInputFile.setPublished(true);
    }

    if (isDeprecatedMetric(measure.metric().key())) {
      logOnce(measure.metric().key(), "Metric '{}' is deprecated. Provided value is ignored.", measure.metric().key());
      return;
    }
    Metric<?> metric = metricFinder.findByKey(measure.metric().key());
    if (metric == null) {
      throw new UnsupportedOperationException("Unknown metric: " + measure.metric().key());
    }
    if (!measure.isFromCore() && isPlatformMetric(measure.metric().key())) {
      logOnce(measure.metric().key(), "Metric '{}' is an internal metric computed by SonarQube. Provided value is ignored.", measure.metric().key());
      return;
    }
    DefaultMeasure measureToSave;
    if (deprecatedCoverageMetricMapping.containsKey(metric)) {
      metric = deprecatedCoverageMetricMapping.get(metric);
      measureToSave = new DefaultMeasure<>()
        .forMetric((Metric) metric)
        .on(measure.inputComponent())
        .withValue(measure.value());
    } else {
      measureToSave = measure;
    }
    if (!scannerMetrics.getMetrics().contains(metric)) {
      throw new UnsupportedOperationException("Metric '" + metric.key() + "' should not be computed by a Sensor");
    }

    if (coverageMetrics.contains(metric)) {
      logOnce(metric.key(), "Coverage measure for metric '{}' should not be saved directly by a Sensor. Plugin should be updated to use SensorContext::newCoverage instead.",
        metric.key());
      if (!component.isFile()) {
        throw new UnsupportedOperationException("Saving coverage measure is only allowed on files. Attempt to save '" + metric.key() + "' on '" + component.key() + "'");
      }
      if (((DefaultInputFile) component).isExcludedForCoverage()) {
        return;
      }
      saveCoverageMetricInternal((InputFile) component, metric, measureToSave);
    } else {
      if (measureCache.contains(component.key(), metric.key())) {
        throw new UnsupportedOperationException("Can not add the same measure twice on " + component + ": " + measure);
      }
      measureCache.put(component.key(), metric.key(), measureToSave);
    }
  }

  private void saveCoverageMetricInternal(InputFile file, Metric<?> metric, DefaultMeasure<?> measure) {
    if (isLineMetrics(metric)) {
      validateCoverageMeasure((String) measure.value(), file);
      DefaultMeasure<?> previousMeasure = measureCache.byMetric(file.key(), metric.key());
      if (previousMeasure != null) {
        measureCache.put(file.key(), metric.key(), new DefaultMeasure<String>()
          .forMetric((Metric<String>) metric)
          .withValue(mergeCoverageLineMetric(metric, (String) previousMeasure.value(), (String) measure.value())));
      } else {
        measureCache.put(file.key(), metric.key(), measure);
      }
    } else {
      // Other coverage metrics are all integer values. Just erase value, it will be recomputed at the end anyway
      measureCache.put(file.key(), metric.key(), measure);
    }
  }

  /**
   * Merge the two line coverage data measures. For lines hits use the sum, and for conditions 
   * keep max value in case they both contains a value for the same line.
   */
  static String mergeCoverageLineMetric(Metric<?> metric, String value1, String value2) {
    Map<Integer, Integer> data1 = KeyValueFormat.parseIntInt(value1);
    Map<Integer, Integer> data2 = KeyValueFormat.parseIntInt(value2);
    if (metric.key().equals(CoreMetrics.COVERAGE_LINE_HITS_DATA_KEY)) {
      return KeyValueFormat.format(Stream.of(data1, data2)
        .map(Map::entrySet)
        .flatMap(Collection::stream)
        .collect(
          Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            Integer::sum,
            TreeMap::new)));
    } else {
      return KeyValueFormat.format(Stream.of(data1, data2)
        .map(Map::entrySet)
        .flatMap(Collection::stream)
        .collect(
          Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            Integer::max,
            TreeMap::new)));
    }
  }

  public static boolean isDeprecatedMetric(String metricKey) {
    return DEPRECATED_METRICS_KEYS.contains(metricKey);
  }

  public static boolean isPlatformMetric(String metricKey) {
    return PLATFORM_METRICS_KEYS.contains(metricKey);
  }

  private boolean isLineMetrics(Metric<?> metric) {
    return this.byLineMetrics.contains(metric);
  }

  public static void validateCoverageMeasure(String value, InputFile inputFile) {
    Map<Integer, Integer> m = KeyValueFormat.parseIntInt(value);
    validatePositiveLine(m, inputFile.toString());
    validateMaxLine(m, inputFile);
  }

  private static void validateMaxLine(Map<Integer, Integer> m, InputFile inputFile) {
    int maxLine = inputFile.lines();

    for (int line : m.keySet()) {
      if (line > maxLine) {
        throw new IllegalStateException(String.format("Can't create measure for line %d for file '%s' with %d lines", line, inputFile, maxLine));
      }
    }
  }

  private static void validatePositiveLine(Map<Integer, Integer> m, String filePath) {
    for (int l : m.keySet()) {
      if (l <= 0) {
        throw new IllegalStateException(String.format("Measure with line %d for file '%s' must be > 0", l, filePath));
      }
    }
  }

  private boolean shouldSkipStorage(DefaultInputFile defaultInputFile) {
    return branchConfiguration.isShortLivingBranch() && defaultInputFile.status() == InputFile.Status.SAME;
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

  @Override
  public void store(DefaultHighlighting highlighting) {
    ScannerReportWriter writer = reportPublisher.getWriter();
    DefaultInputFile inputFile = (DefaultInputFile) highlighting.inputFile();
    if (shouldSkipStorage(inputFile)) {
      return;
    }
    inputFile.setPublished(true);
    int componentRef = inputFile.batchId();
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
  public void store(DefaultSymbolTable symbolTable) {
    ScannerReportWriter writer = reportPublisher.getWriter();
    DefaultInputFile inputFile = (DefaultInputFile) symbolTable.inputFile();
    if (shouldSkipStorage(inputFile)) {
      return;
    }
    inputFile.setPublished(true);
    int componentRef = inputFile.batchId();
    if (writer.hasComponentData(FileStructure.Domain.SYMBOLS, componentRef)) {
      throw new UnsupportedOperationException("Trying to save symbol table twice for the same file is not supported: " + symbolTable.inputFile().absolutePath());
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
  public void store(DefaultCoverage defaultCoverage) {
    DefaultInputFile inputFile = (DefaultInputFile) defaultCoverage.inputFile();
    if (shouldSkipStorage(inputFile)) {
      return;
    }
    inputFile.setPublished(true);
    if (defaultCoverage.linesToCover() > 0) {
      saveCoverageMetricInternal(inputFile, LINES_TO_COVER, new DefaultMeasure<Integer>().forMetric(LINES_TO_COVER).withValue(defaultCoverage.linesToCover()));
      saveCoverageMetricInternal(inputFile, UNCOVERED_LINES,
        new DefaultMeasure<Integer>().forMetric(UNCOVERED_LINES).withValue(defaultCoverage.linesToCover() - defaultCoverage.coveredLines()));
      saveCoverageMetricInternal(inputFile, COVERAGE_LINE_HITS_DATA,
        new DefaultMeasure<String>().forMetric(COVERAGE_LINE_HITS_DATA).withValue(KeyValueFormat.format(defaultCoverage.hitsByLine())));
    }
    if (defaultCoverage.conditions() > 0) {
      saveCoverageMetricInternal(inputFile, CONDITIONS_TO_COVER,
        new DefaultMeasure<Integer>().forMetric(CONDITIONS_TO_COVER).withValue(defaultCoverage.conditions()));
      saveCoverageMetricInternal(inputFile, UNCOVERED_CONDITIONS,
        new DefaultMeasure<Integer>().forMetric(UNCOVERED_CONDITIONS).withValue(defaultCoverage.conditions() - defaultCoverage.coveredConditions()));
      saveCoverageMetricInternal(inputFile, COVERED_CONDITIONS_BY_LINE,
        new DefaultMeasure<String>().forMetric(COVERED_CONDITIONS_BY_LINE).withValue(KeyValueFormat.format(defaultCoverage.coveredConditionsByLine())));
      saveCoverageMetricInternal(inputFile, CONDITIONS_BY_LINE,
        new DefaultMeasure<String>().forMetric(CONDITIONS_BY_LINE).withValue(KeyValueFormat.format(defaultCoverage.conditionsByLine())));
    }
  }

  @Override
  public void store(DefaultCpdTokens defaultCpdTokens) {
    DefaultInputFile inputFile = (DefaultInputFile) defaultCpdTokens.inputFile();
    if (shouldSkipStorage(inputFile)) {
      return;
    }
    inputFile.setPublished(true);
    PmdBlockChunker blockChunker = new PmdBlockChunker(getBlockSize(inputFile.language()));
    List<Block> blocks = blockChunker.chunk(inputFile.key(), defaultCpdTokens.getTokenLines());
    index.insert(inputFile, blocks);
  }

  @VisibleForTesting
  int getBlockSize(String languageKey) {
    return settings.getInt("sonar.cpd." + languageKey + ".minimumLines").orElse(DefaultCpdBlockIndexer.getDefaultBlockSize(languageKey));
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
}
