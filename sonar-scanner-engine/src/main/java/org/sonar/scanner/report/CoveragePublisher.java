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
package org.sonar.scanner.report;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.scanner.protocol.output.ScannerReport.LineCoverage;
import org.sonar.scanner.protocol.output.ScannerReport.LineCoverage.Builder;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.scan.filesystem.InputComponentStore;
import org.sonar.scanner.scan.measure.MeasureCache;

public class CoveragePublisher implements ReportPublisherStep {

  private final InputComponentStore componentStore;
  private final MeasureCache measureCache;

  public CoveragePublisher(InputComponentStore componentStore, MeasureCache measureCache) {
    this.componentStore = componentStore;
    this.measureCache = measureCache;
  }

  @Override
  public void publish(ScannerReportWriter writer) {
    for (final DefaultInputFile inputFile : componentStore.allFilesToPublish()) {
      Map<Integer, LineCoverage.Builder> coveragePerLine = new LinkedHashMap<>();

      int lineCount = inputFile.lines();
      applyLineMeasure(inputFile.key(), lineCount, CoreMetrics.COVERAGE_LINE_HITS_DATA_KEY, coveragePerLine,
        (value, builder) -> builder.setHits(Integer.parseInt(value) > 0));
      applyLineMeasure(inputFile.key(), lineCount, CoreMetrics.CONDITIONS_BY_LINE_KEY, coveragePerLine,
        (value, builder) -> builder.setConditions(Integer.parseInt(value)));
      applyLineMeasure(inputFile.key(), lineCount, CoreMetrics.COVERED_CONDITIONS_BY_LINE_KEY, coveragePerLine,
        (value, builder) -> builder.setCoveredConditions(Integer.parseInt(value)));
      writer.writeComponentCoverage(inputFile.batchId(), Iterables.transform(coveragePerLine.values(), BuildCoverage.INSTANCE));
    }
  }

  void applyLineMeasure(String inputFileKey, int lineCount, String metricKey, Map<Integer, LineCoverage.Builder> coveragePerLine, MeasureOperation op) {
    DefaultMeasure<?> measure = measureCache.byMetric(inputFileKey, metricKey);
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
