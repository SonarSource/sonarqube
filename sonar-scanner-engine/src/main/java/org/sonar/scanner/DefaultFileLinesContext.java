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
package org.sonar.scanner;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.utils.KeyValueFormat;

import static java.util.stream.Collectors.toMap;

public class DefaultFileLinesContext implements FileLinesContext {
  private final InputFile inputFile;
  private final MetricFinder metricFinder;

  /**
   * metric key -> line -> value
   */
  private final Map<String, Map<Integer, Object>> map = new HashMap<>();
  private final SensorStorage sensorStorage;

  public DefaultFileLinesContext(SensorStorage sensorStorage, InputFile inputFile, MetricFinder metricFinder) {
    this.sensorStorage = sensorStorage;
    this.inputFile = inputFile;
    this.metricFinder = metricFinder;
  }

  @Override
  public void setIntValue(String metricKey, int line, int value) {
    Preconditions.checkNotNull(metricKey);
    checkLineRange(line);

    setValue(metricKey, line, value);
  }

  private void checkLineRange(int line) {
    Preconditions.checkArgument(line > 0, "Line number should be positive for file %s.", inputFile);
    Preconditions.checkArgument(line <= inputFile.lines(), "Line %s is out of range for file %s. File has %s lines.", line, inputFile, inputFile.lines());
  }

  @Override
  public void setStringValue(String metricKey, int line, String value) {
    Preconditions.checkNotNull(metricKey);
    checkLineRange(line);
    Preconditions.checkNotNull(value);

    setValue(metricKey, line, value);
  }

  private void setValue(String metricKey, int line, Object value) {
    map.computeIfAbsent(metricKey, k -> new HashMap<>())
      .put(line, value);
  }

  @Override
  public void save() {
    for (Map.Entry<String, Map<Integer, Object>> entry : map.entrySet()) {
      String metricKey = entry.getKey();
      Map<Integer, Object> lines = entry.getValue();
      if (shouldSave(lines)) {
        String data = KeyValueFormat.format(optimizeStorage(metricKey, lines));
        new DefaultMeasure<String>(sensorStorage)
          .on(inputFile)
          .forMetric(metricFinder.findByKey(metricKey))
          .withValue(data)
          .save();
        entry.setValue(ImmutableMap.copyOf(lines));
      }
    }
  }

  private static Map<Integer, Object> optimizeStorage(String metricKey, Map<Integer, Object> lines) {
    // SONAR-7464 Don't store 0 because this is default value anyway
    if (CoreMetrics.NCLOC_DATA_KEY.equals(metricKey) || CoreMetrics.EXECUTABLE_LINES_DATA_KEY.equals(metricKey)) {
      return lines.entrySet().stream()
        .filter(entry -> !entry.getValue().equals(0))
        .collect(toMap(Entry::getKey, Entry::getValue));
    }
    return lines;
  }

  /**
   * Checks that measure was not saved.
   *
   * @see #save()
   */
  private static boolean shouldSave(Map<Integer, Object> lines) {
    return !(lines instanceof ImmutableMap);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("map", map)
      .toString();
  }

}
