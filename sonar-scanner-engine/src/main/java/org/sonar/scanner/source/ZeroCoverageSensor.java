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
package org.sonar.scanner.source;

import com.google.common.base.Function;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
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
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.scanner.scan.measure.MeasureCache;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Sets.newHashSet;

@Phase(name = Phase.Name.POST)
public final class ZeroCoverageSensor implements Sensor {

  private static final class MeasureToMetricKey implements Function<Measure, String> {
    @Override
    public String apply(Measure input) {
      return input.getMetricKey();
    }
  }

  private static final class MetricToKey implements Function<Metric, String> {
    @Override
    public String apply(Metric input) {
      return input.key();
    }
  }

  private final MeasureCache measureCache;

  public ZeroCoverageSensor(MeasureCache measureCache) {
    this.measureCache = measureCache;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("Zero Coverage Sensor");
  }

  @Override
  public void execute(final SensorContext context) {
    FileSystem fs = context.fileSystem();
    for (InputFile f : fs.inputFiles(fs.predicates().hasType(Type.MAIN))) {
      if (!isCoverageMeasuresAlreadyDefined(f)) {
        Measure execLines = measureCache.byMetric(f.key(), CoreMetrics.EXECUTABLE_LINES_DATA_KEY);
        if (execLines != null) {
          storeZeroCoverageForEachExecutableLine(context, f, execLines);
        }

      }
    }
  }

  private static void storeZeroCoverageForEachExecutableLine(final SensorContext context, InputFile f, Measure execLines) {
    NewCoverage newCoverage = context.newCoverage().ofType(CoverageType.UNIT).onFile(f);
    Map<Integer, String> lineMeasures = KeyValueFormat.parseIntString((String) execLines.value());
    for (Map.Entry<Integer, String> lineMeasure : lineMeasures.entrySet()) {
      int lineIdx = lineMeasure.getKey();
      if (lineIdx <= f.lines()) {
        String value = lineMeasure.getValue();
        if (StringUtils.isNotEmpty(value) && Integer.parseInt(value) > 0) {
          newCoverage.lineHits(lineIdx, 0);
        }
      }
    }
    newCoverage.save();
  }

  private boolean isCoverageMeasuresAlreadyDefined(InputFile f) {
    Set<String> metricKeys = newHashSet(transform(measureCache.byComponentKey(f.key()), new MeasureToMetricKey()));
    Function<Metric, String> metricToKey = new MetricToKey();
    Set<String> allCoverageMetricKeys = newHashSet(concat(transform(CoverageType.UNIT.allMetrics(), metricToKey),
      transform(CoverageType.IT.allMetrics(), metricToKey),
      transform(CoverageType.OVERALL.allMetrics(), metricToKey)));
    return !Sets.intersection(metricKeys, allCoverageMetricKeys).isEmpty();
  }

}
