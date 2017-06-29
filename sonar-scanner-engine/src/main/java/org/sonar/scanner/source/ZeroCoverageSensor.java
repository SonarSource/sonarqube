/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.scanner.source;

import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.StreamSupport;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.coverage.CoverageType;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.scanner.scan.measure.MeasureCache;
import org.sonar.scanner.sensor.coverage.CoverageExclusions;

@Phase(name = Phase.Name.POST)
public final class ZeroCoverageSensor implements Sensor {

  private static final class MeasureToMetricKey implements Function<DefaultMeasure<?>, String> {
    @Override
    public String apply(DefaultMeasure<?> input) {
      return input.metric().key();
    }
  }

  private static final class MetricToKey implements Function<Metric, String> {
    @Override
    public String apply(Metric input) {
      return input.key();
    }
  }

  private final MeasureCache measureCache;
  private final CoverageExclusions coverageExclusions;

  public ZeroCoverageSensor(MeasureCache measureCache, CoverageExclusions exclusions) {
    this.measureCache = measureCache;
    this.coverageExclusions = exclusions;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("Zero Coverage Sensor")
      .global();
  }

  @Override
  public void execute(final SensorContext context) {
    FileSystem fs = context.fileSystem();
    for (InputFile f : fs.inputFiles(fs.predicates().hasType(Type.MAIN))) {
      if (coverageExclusions.isExcluded(f)) {
        continue;
      }
      if (!isCoverageMeasuresAlreadyDefined(f)) {
        DefaultMeasure<String> execLines = (DefaultMeasure<String>) measureCache.byMetric(f.key(), CoreMetrics.EXECUTABLE_LINES_DATA_KEY);
        if (execLines != null) {
          storeZeroCoverageForEachExecutableLine(context, f, execLines);
        }
      }
    }
  }

  private static void storeZeroCoverageForEachExecutableLine(final SensorContext context, InputFile f, DefaultMeasure<String> execLines) {
    NewCoverage newCoverage = context.newCoverage().onFile(f);
    Map<Integer, Integer> lineMeasures = KeyValueFormat.parseIntInt((String) execLines.value());
    for (Map.Entry<Integer, Integer> lineMeasure : lineMeasures.entrySet()) {
      int lineIdx = lineMeasure.getKey();
      if (lineIdx <= f.lines() && lineMeasure.getValue() > 0) {
        newCoverage.lineHits(lineIdx, 0);
      }
    }
    newCoverage.save();
  }

  private boolean isCoverageMeasuresAlreadyDefined(InputFile f) {
    Set<String> metricKeys = StreamSupport.stream(measureCache.byComponentKey(f.key()).spliterator(), false)
      .map(new MeasureToMetricKey()).collect(MoreCollectors.toSet());
    Function<Metric, String> metricToKey = new MetricToKey();
    Set<String> allCoverageMetricKeys = CoverageType.UNIT.allMetrics().stream().map(metricToKey).collect(MoreCollectors.toSet());
    return !Sets.intersection(metricKeys, allCoverageMetricKeys).isEmpty();
  }

}
