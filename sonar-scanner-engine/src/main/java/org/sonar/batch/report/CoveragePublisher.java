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
package org.sonar.batch.report;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.batch.index.BatchComponent;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.scan.measure.MeasureCache;
import org.sonar.scanner.protocol.output.ScannerReport.LineCoverage;
import org.sonar.scanner.protocol.output.ScannerReport.LineCoverage.Builder;
import org.sonar.scanner.protocol.output.ScannerReportWriter;

public class CoveragePublisher implements ReportPublisherStep {

  private final BatchComponentCache resourceCache;
  private final MeasureCache measureCache;

  public CoveragePublisher(BatchComponentCache resourceCache, MeasureCache measureCache) {
    this.resourceCache = resourceCache;
    this.measureCache = measureCache;
  }

  @Override
  public void publish(ScannerReportWriter writer) {
    for (final BatchComponent resource : resourceCache.all()) {
      if (!resource.isFile()) {
        continue;
      }
      Map<Integer, LineCoverage.Builder> coveragePerLine = new LinkedHashMap<>();

      int lineCount = ((InputFile) resource.inputComponent()).lines();
      applyLineMeasure(resource.key(), lineCount, CoreMetrics.COVERAGE_LINE_HITS_DATA_KEY, coveragePerLine,
        new MeasureOperation() {
          @Override
          public void apply(String value, LineCoverage.Builder builder) {
            builder.setUtHits(Integer.parseInt(value) > 0);
          }
        });
      applyLineMeasure(resource.key(), lineCount, CoreMetrics.IT_COVERAGE_LINE_HITS_DATA_KEY, coveragePerLine,
        new MeasureOperation() {
          @Override
          public void apply(String value, LineCoverage.Builder builder) {
            builder.setItHits(Integer.parseInt(value) > 0);
          }
        });
      // Just in case we have only IT and no UT, we first take conditions from ITs, then from UTs (UTs will override ITs).
      // Note that it would be very strange (and unsupported in scanner report) to have ITs and UTs don't report the same number of
      // conditions. Not even talking about overall conditions...
      applyLineMeasure(resource.key(), lineCount, CoreMetrics.IT_CONDITIONS_BY_LINE_KEY, coveragePerLine,
        new MeasureOperation() {
          @Override
          public void apply(String value, LineCoverage.Builder builder) {
            builder.setConditions(Integer.parseInt(value));
          }
        });
      applyLineMeasure(resource.key(), lineCount, CoreMetrics.CONDITIONS_BY_LINE_KEY, coveragePerLine,
        new MeasureOperation() {
          @Override
          public void apply(String value, LineCoverage.Builder builder) {
            builder.setConditions(Integer.parseInt(value));
          }
        });
      applyLineMeasure(resource.key(), lineCount, CoreMetrics.COVERED_CONDITIONS_BY_LINE_KEY, coveragePerLine,
        new MeasureOperation() {
          @Override
          public void apply(String value, LineCoverage.Builder builder) {
            builder.setUtCoveredConditions(Integer.parseInt(value));
          }
        });
      applyLineMeasure(resource.key(), lineCount, CoreMetrics.IT_COVERED_CONDITIONS_BY_LINE_KEY, coveragePerLine,
        new MeasureOperation() {
          @Override
          public void apply(String value, LineCoverage.Builder builder) {
            builder.setItCoveredConditions(Integer.parseInt(value));
          }
        });
      applyLineMeasure(resource.key(), lineCount, CoreMetrics.OVERALL_COVERED_CONDITIONS_BY_LINE_KEY, coveragePerLine,
        new MeasureOperation() {
          @Override
          public void apply(String value, LineCoverage.Builder builder) {
            builder.setOverallCoveredConditions(Integer.parseInt(value));
          }
        });
      writer.writeComponentCoverage(resource.batchId(), Iterables.transform(coveragePerLine.values(), BuildCoverage.INSTANCE));
    }
  }

  void applyLineMeasure(String inputFileKey, int lineCount, String metricKey, Map<Integer, LineCoverage.Builder> coveragePerLine, MeasureOperation op) {
    Measure measure = measureCache.byMetric(inputFileKey, metricKey);
    if (measure != null) {
      Map<Integer, String> lineMeasures = KeyValueFormat.parseIntString((String) measure.value());
      for (Map.Entry<Integer, String> lineMeasure : lineMeasures.entrySet()) {
        int lineIdx = lineMeasure.getKey();
        if (lineIdx <= lineCount) {
          String value = lineMeasure.getValue();
          if (StringUtils.isNotEmpty(value)) {
            LineCoverage.Builder coverageBuilder = coveragePerLine.get(lineIdx);
            if (coverageBuilder == null) {
              coverageBuilder = LineCoverage.newBuilder();
              coverageBuilder.setLine(lineIdx);
              coveragePerLine.put(lineIdx, coverageBuilder);
            }
            op.apply(value, coverageBuilder);
          }
        }
      }
    }
  }

  interface MeasureOperation {
    void apply(String value, LineCoverage.Builder builder);
  }

  private enum BuildCoverage implements Function<LineCoverage.Builder, LineCoverage> {
    INSTANCE;

    @Override
    public LineCoverage apply(@Nonnull Builder input) {
      return input.build();
    }
  }

}
