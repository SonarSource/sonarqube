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
package org.sonar.batch.scan2;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.batch.sensor.SensorStorage;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.utils.KeyValueFormat;

import java.util.Map;

public class DefaultFileLinesContext implements FileLinesContext {

  private final SensorStorage sensorStorage;
  private final InputFile inputFile;

  /**
   * metric key -> line -> value
   */
  private final Map<String, Map<Integer, Object>> map = Maps.newHashMap();
  private MetricFinder metricFinder;

  public DefaultFileLinesContext(MetricFinder metricFinder, SensorStorage sensorStorage, InputFile inputFile) {
    this.metricFinder = metricFinder;
    this.sensorStorage = sensorStorage;
    this.inputFile = inputFile;
  }

  @Override
  public void setIntValue(String metricKey, int line, int value) {
    Preconditions.checkNotNull(metricKey);
    Preconditions.checkArgument(line > 0);

    setValue(metricKey, line, value);
  }

  @Override
  public Integer getIntValue(String metricKey, int line) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setStringValue(String metricKey, int line, String value) {
    Preconditions.checkNotNull(metricKey);
    Preconditions.checkArgument(line > 0);
    Preconditions.checkNotNull(value);

    setValue(metricKey, line, value);
  }

  @Override
  public String getStringValue(String metricKey, int line) {
    throw new UnsupportedOperationException();
  }

  private Map<Integer, Object> getOrCreateLines(String metricKey) {
    Map<Integer, Object> lines = map.get(metricKey);
    if (lines == null) {
      lines = Maps.newHashMap();
      map.put(metricKey, lines);
    }
    return lines;
  }

  private void setValue(String metricKey, int line, Object value) {
    getOrCreateLines(metricKey).put(line, value);
  }

  @Override
  public void save() {
    for (Map.Entry<String, Map<Integer, Object>> entry : map.entrySet()) {
      String metricKey = entry.getKey();
      org.sonar.api.batch.measure.Metric<String> metric = metricFinder.findByKey(metricKey);
      if (metric == null) {
        throw new IllegalStateException("Unable to find metric with key: " + metricKey);
      }
      Map<Integer, Object> lines = entry.getValue();
      String data = KeyValueFormat.format(lines);
      new DefaultMeasure<String>(sensorStorage)
        .forMetric(metric)
        .onFile(inputFile)
        .withValue(data)
        .save();
      entry.setValue(ImmutableMap.copyOf(lines));
    }
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("map", map)
      .toString();
  }

}
