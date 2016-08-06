/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.batch.sensor.coverage.CoverageType;
import org.sonar.api.batch.sensor.coverage.internal.DefaultCoverage;
import org.sonar.api.batch.sensor.cpd.internal.DefaultCpdTokens;
import org.sonar.api.batch.sensor.error.AnalysisError;
import org.sonar.api.batch.sensor.highlighting.internal.DefaultHighlighting;
import org.sonar.api.batch.sensor.highlighting.internal.SyntaxHighlightingRule;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.batch.sensor.symbol.internal.DefaultSymbolTable;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.utils.SonarException;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.internal.pmd.PmdBlockChunker;
import org.sonar.scanner.cpd.deprecated.DefaultCpdBlockIndexer;
import org.sonar.scanner.cpd.index.SonarCpdBlockIndex;
import org.sonar.scanner.index.BatchComponent;
import org.sonar.scanner.index.BatchComponentCache;
import org.sonar.scanner.issue.ModuleIssues;
import org.sonar.scanner.protocol.output.FileStructure;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.report.ReportPublisher;
import org.sonar.scanner.report.ScannerReportUtils;
import org.sonar.scanner.repository.ContextPropertiesCache;
import org.sonar.scanner.scan.measure.MeasureCache;
import org.sonar.scanner.sensor.coverage.CoverageExclusions;

public class DefaultSensorStorage implements SensorStorage {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultSensorStorage.class);

  private static final List<Metric> INTERNAL_METRICS = Arrays.<Metric>asList(
    // Computed by LinesSensor
    CoreMetrics.LINES);

  private static final List<String> DEPRECATED_METRICS_KEYS = Arrays.asList(
    CoreMetrics.DEPENDENCY_MATRIX_KEY,
    CoreMetrics.DIRECTORY_CYCLES_KEY,
    CoreMetrics.DIRECTORY_EDGES_WEIGHT_KEY,
    CoreMetrics.DIRECTORY_FEEDBACK_EDGES_KEY,
    CoreMetrics.DIRECTORY_TANGLE_INDEX_KEY,
    CoreMetrics.DIRECTORY_TANGLES_KEY,
    CoreMetrics.FILE_CYCLES_KEY,
    CoreMetrics.FILE_EDGES_WEIGHT_KEY,
    CoreMetrics.FILE_FEEDBACK_EDGES_KEY,
    CoreMetrics.FILE_TANGLE_INDEX_KEY,
    CoreMetrics.FILE_TANGLES_KEY);

  private final MetricFinder metricFinder;
  private final ModuleIssues moduleIssues;
  private final CoverageExclusions coverageExclusions;
  private final BatchComponentCache componentCache;
  private final ReportPublisher reportPublisher;
  private final MeasureCache measureCache;
  private final SonarCpdBlockIndex index;
  private final ContextPropertiesCache contextPropertiesCache;
  private final Settings settings;

  public DefaultSensorStorage(MetricFinder metricFinder, ModuleIssues moduleIssues,
    Settings settings,
    CoverageExclusions coverageExclusions, BatchComponentCache componentCache, ReportPublisher reportPublisher,
    MeasureCache measureCache, SonarCpdBlockIndex index,
    ContextPropertiesCache contextPropertiesCache) {
    this.metricFinder = metricFinder;
    this.moduleIssues = moduleIssues;
    this.settings = settings;
    this.coverageExclusions = coverageExclusions;
    this.componentCache = componentCache;
    this.reportPublisher = reportPublisher;
    this.measureCache = measureCache;
    this.index = index;
    this.contextPropertiesCache = contextPropertiesCache;
  }

  private Metric findMetricOrFail(String metricKey) {
    Metric m = (Metric) metricFinder.findByKey(metricKey);
    if (m == null) {
      throw new IllegalStateException("Unknow metric with key: " + metricKey);
    }
    return m;
  }

  @Override
  public void store(Measure newMeasure) {
    DefaultMeasure<?> measure = (DefaultMeasure<?>) newMeasure;
    org.sonar.api.measures.Metric m = findMetricOrFail(measure.metric().key());
    org.sonar.api.measures.Measure measureToSave = new org.sonar.api.measures.Measure(m);
    setValueAccordingToMetricType(newMeasure, m, measureToSave);
    measureToSave.setFromCore(measure.isFromCore());
    InputComponent inputComponent = newMeasure.inputComponent();
    Resource resource = componentCache.get(inputComponent).resource();
    if (coverageExclusions.accept(resource, measureToSave)) {
      saveMeasure(resource, measureToSave);
    }
  }

  public org.sonar.api.measures.Measure saveMeasure(Resource resource, org.sonar.api.measures.Measure measure) {
    if (DEPRECATED_METRICS_KEYS.contains(measure.getMetricKey())) {
      // Ignore deprecated metrics
      return null;
    }
    org.sonar.api.batch.measure.Metric metric = metricFinder.findByKey(measure.getMetricKey());
    if (metric == null) {
      throw new SonarException("Unknown metric: " + measure.getMetricKey());
    }
    if (!measure.isFromCore() && INTERNAL_METRICS.contains(metric)) {
      LOG.debug("Metric " + metric.key() + " is an internal metric computed by SonarQube. Provided value is ignored.");
      return measure;
    }
    if (measureCache.contains(resource, measure)) {
      throw new SonarException("Can not add the same measure twice on " + resource + ": " + measure);
    }
    measureCache.put(resource, measure);
    return measure;
  }

  private void setValueAccordingToMetricType(Measure<?> measure, org.sonar.api.measures.Metric<?> m, org.sonar.api.measures.Measure measureToSave) {
    switch (m.getType()) {
      case BOOL:
        measureToSave.setValue(Boolean.TRUE.equals(measure.value()) ? 1.0 : 0.0);
        break;
      case INT:
      case MILLISEC:
      case WORK_DUR:
      case FLOAT:
      case PERCENT:
      case RATING:
        measureToSave.setValue(((Number) measure.value()).doubleValue());
        break;
      case STRING:
      case LEVEL:
      case DATA:
      case DISTRIB:
        measureToSave.setData((String) measure.value());
        break;
      default:
        throw new UnsupportedOperationException("Unsupported type :" + m.getType());
    }
  }

  @Override
  public void store(Issue issue) {
    moduleIssues.initAndAddIssue(issue);
  }

  private File getFile(InputFile file) {
    BatchComponent r = componentCache.get(file);
    if (r == null) {
      throw new IllegalStateException("Provided input file is not indexed");
    }
    return (File) r.resource();
  }

  @Override
  public void store(DefaultHighlighting highlighting) {
    ScannerReportWriter writer = reportPublisher.getWriter();
    DefaultInputFile inputFile = (DefaultInputFile) highlighting.inputFile();
    int componentRef = componentCache.get(inputFile).batchId();
    if (writer.hasComponentData(FileStructure.Domain.SYNTAX_HIGHLIGHTINGS, componentRef)) {
      throw new UnsupportedOperationException("Trying to save highlighting twice for the same file is not supported: " + inputFile.absolutePath());
    }
    writer.writeComponentSyntaxHighlighting(componentRef,
      Iterables.transform(highlighting.getSyntaxHighlightingRuleSet(), new BuildSyntaxHighlighting()));
  }

  @Override
  public void store(DefaultSymbolTable symbolTable) {
    ScannerReportWriter writer = reportPublisher.getWriter();
    int componentRef = componentCache.get(symbolTable.inputFile()).batchId();
    if (writer.hasComponentData(FileStructure.Domain.SYMBOLS, componentRef)) {
      throw new UnsupportedOperationException("Trying to save symbol table twice for the same file is not supported: " + symbolTable.inputFile().absolutePath());
    }
    writer.writeComponentSymbols(componentRef,
      Iterables.transform(symbolTable.getReferencesBySymbol().entrySet(), new Function<Map.Entry<TextRange, Set<TextRange>>, ScannerReport.Symbol>() {
        private ScannerReport.Symbol.Builder builder = ScannerReport.Symbol.newBuilder();
        private ScannerReport.TextRange.Builder rangeBuilder = ScannerReport.TextRange.newBuilder();

        @Override
        public ScannerReport.Symbol apply(Map.Entry<TextRange, Set<TextRange>> input) {
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
        }

      }));
  }

  @Override
  public void store(DefaultCoverage defaultCoverage) {
    File file = getFile(defaultCoverage.inputFile());
    if (coverageExclusions.hasMatchingPattern(file)) {
      return;
    }
    CoverageType type = defaultCoverage.type();
    if (defaultCoverage.linesToCover() > 0) {
      saveMeasure(file, new org.sonar.api.measures.Measure(type.linesToCover(), (double) defaultCoverage.linesToCover()));
      saveMeasure(file, new org.sonar.api.measures.Measure(type.uncoveredLines(), (double) (defaultCoverage.linesToCover() - defaultCoverage.coveredLines())));
      saveMeasure(file, new org.sonar.api.measures.Measure(type.lineHitsData()).setData(KeyValueFormat.format(defaultCoverage.hitsByLine())));
    }
    if (defaultCoverage.conditions() > 0) {
      saveMeasure(file, new org.sonar.api.measures.Measure(type.conditionsToCover(), (double) defaultCoverage.conditions()));
      saveMeasure(file, new org.sonar.api.measures.Measure(type.uncoveredConditions(), (double) (defaultCoverage.conditions() - defaultCoverage.coveredConditions())));
      saveMeasure(file, new org.sonar.api.measures.Measure(type.coveredConditionsByLine()).setData(KeyValueFormat.format(defaultCoverage.coveredConditionsByLine())));
      saveMeasure(file, new org.sonar.api.measures.Measure(type.conditionsByLine()).setData(KeyValueFormat.format(defaultCoverage.conditionsByLine())));
    }
  }

  private static class BuildSyntaxHighlighting implements Function<SyntaxHighlightingRule, ScannerReport.SyntaxHighlightingRule> {
    private ScannerReport.SyntaxHighlightingRule.Builder builder = ScannerReport.SyntaxHighlightingRule.newBuilder();
    private ScannerReport.TextRange.Builder rangeBuilder = ScannerReport.TextRange.newBuilder();

    @Override
    public ScannerReport.SyntaxHighlightingRule apply(@Nonnull SyntaxHighlightingRule input) {
      builder.setRange(rangeBuilder.setStartLine(input.range().start().line())
        .setStartOffset(input.range().start().lineOffset())
        .setEndLine(input.range().end().line())
        .setEndOffset(input.range().end().lineOffset())
        .build());
      builder.setType(ScannerReportUtils.toProtocolType(input.getTextType()));
      return builder.build();
    }
  }

  @Override
  public void store(DefaultCpdTokens defaultCpdTokens) {
    InputFile inputFile = defaultCpdTokens.inputFile();
    PmdBlockChunker blockChunker = new PmdBlockChunker(getBlockSize(inputFile.language()));
    List<Block> blocks = blockChunker.chunk(inputFile.key(), defaultCpdTokens.getTokenLines());
    index.insert(inputFile, blocks);
  }

  @VisibleForTesting
  int getBlockSize(String languageKey) {
    int blockSize = settings.getInt("sonar.cpd." + languageKey + ".minimumLines");
    if (blockSize == 0) {
      blockSize = DefaultCpdBlockIndexer.getDefaultBlockSize(languageKey);
    }
    return blockSize;
  }

  @Override
  public void store(AnalysisError analysisError) {
    // no op
  }

  @Override
  public void storeProperty(String key, String value) {
    contextPropertiesCache.put(key, value);
  }
}
