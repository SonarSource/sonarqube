/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.batch.index.BatchResource;
import org.sonar.batch.index.ResourceCache;
import org.sonar.batch.protocol.output.BatchReport.Coverage;
import org.sonar.batch.protocol.output.BatchReport.Coverage.Builder;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.batch.scan.measure.MeasureCache;

import java.util.LinkedHashMap;
import java.util.Map;

public class CoveragePublisher implements ReportPublisherStep {

  private final ResourceCache resourceCache;
  private final MeasureCache measureCache;

  public CoveragePublisher(ResourceCache resourceCache, MeasureCache measureCache) {
    this.resourceCache = resourceCache;
    this.measureCache = measureCache;
  }

  @Override
  public void publish(BatchReportWriter writer) {
    for (final BatchResource resource : resourceCache.all()) {
      if (!resource.isFile()) {
        continue;
      }
      Map<Integer, Coverage.Builder> coveragePerLine = new LinkedHashMap<>();

      applyLineMeasure(resource.key(), ((InputFile) resource.inputPath()).lines(), CoreMetrics.COVERAGE_LINE_HITS_DATA_KEY, coveragePerLine, new MeasureOperation() {
        @Override
        public void apply(String value, Coverage.Builder builder) {
          builder.setUtHits(Integer.parseInt(value) > 0);
        }
      });
      applyLineMeasure(resource.key(), ((InputFile) resource.inputPath()).lines(), CoreMetrics.CONDITIONS_BY_LINE_KEY, coveragePerLine, new MeasureOperation() {
        @Override
        public void apply(String value, Coverage.Builder builder) {
          builder.setConditions(Integer.parseInt(value));
        }
      });
      applyLineMeasure(resource.key(), ((InputFile) resource.inputPath()).lines(), CoreMetrics.COVERED_CONDITIONS_BY_LINE_KEY, coveragePerLine, new MeasureOperation() {
        @Override
        public void apply(String value, Coverage.Builder builder) {
          builder.setUtCoveredConditions(Integer.parseInt(value));
        }
      });
      applyLineMeasure(resource.key(), ((InputFile) resource.inputPath()).lines(), CoreMetrics.IT_COVERAGE_LINE_HITS_DATA_KEY, coveragePerLine, new MeasureOperation() {
        @Override
        public void apply(String value, Coverage.Builder builder) {
          builder.setItHits(Integer.parseInt(value) > 0);
        }
      });
      applyLineMeasure(resource.key(), ((InputFile) resource.inputPath()).lines(), CoreMetrics.IT_COVERED_CONDITIONS_BY_LINE_KEY, coveragePerLine, new MeasureOperation() {
        @Override
        public void apply(String value, Coverage.Builder builder) {
          builder.setItCoveredConditions(Integer.parseInt(value));
        }
      });
      applyLineMeasure(resource.key(), ((InputFile) resource.inputPath()).lines(), CoreMetrics.OVERALL_COVERED_CONDITIONS_BY_LINE_KEY, coveragePerLine, new MeasureOperation() {
        @Override
        public void apply(String value, Coverage.Builder builder) {
          builder.setOverallCoveredConditions(Integer.parseInt(value));
        }
      });

      writer.writeComponentCoverage(resource.batchId(), Iterables.transform(coveragePerLine.values(), new Function<Coverage.Builder, Coverage>() {
        @Override
        public Coverage apply(Builder input) {
          return input.build();
        }
      }));
    }
  }

  void applyLineMeasure(String inputFileKey, int lineCount, String metricKey, Map<Integer, Coverage.Builder> coveragePerLine, MeasureOperation op) {
    Iterable<Measure> measures = measureCache.byMetric(inputFileKey, metricKey);
    if (measures.iterator().hasNext()) {
      Measure measure = measures.iterator().next();
      Map<Integer, String> lineMeasures = KeyValueFormat.parseIntString((String) measure.value());
      for (Map.Entry<Integer, String> lineMeasure : lineMeasures.entrySet()) {
        int lineIdx = lineMeasure.getKey();
        if (lineIdx <= lineCount) {
          String value = lineMeasure.getValue();
          if (StringUtils.isNotEmpty(value)) {
            Coverage.Builder coverageBuilder = coveragePerLine.get(lineIdx);
            if (coverageBuilder == null) {
              coverageBuilder = Coverage.newBuilder();
              coverageBuilder.setLine(lineIdx);
              coveragePerLine.put(lineIdx, coverageBuilder);
            }
            op.apply(value, coverageBuilder);
          }
        }
      }
    }
  }

  static interface MeasureOperation {
    void apply(String value, Coverage.Builder builder);
  }

}
