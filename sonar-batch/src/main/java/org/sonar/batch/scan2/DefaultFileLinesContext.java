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
import org.sonar.api.batch.analyzer.measure.AnalyzerMeasure;
import org.sonar.api.batch.analyzer.measure.internal.DefaultAnalyzerMeasureBuilder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.utils.KeyValueFormat.Converter;
import org.sonar.core.component.ComponentKeys;

import java.util.Map;

public class DefaultFileLinesContext implements FileLinesContext {

  private final AnalyzerMeasureCache measureCache;
  private final InputFile inputFile;

  /**
   * metric key -> line -> value
   */
  private final Map<String, Map<Integer, Object>> map = Maps.newHashMap();
  private String projectKey;
  private MetricFinder metricFinder;

  public DefaultFileLinesContext(MetricFinder metricFinder, AnalyzerMeasureCache measureCache, String projectKey, InputFile inputFile) {
    this.metricFinder = metricFinder;
    this.projectKey = projectKey;
    Preconditions.checkNotNull(measureCache);
    this.measureCache = measureCache;
    this.inputFile = inputFile;
  }

  public void setIntValue(String metricKey, int line, int value) {
    Preconditions.checkNotNull(metricKey);
    Preconditions.checkArgument(line > 0);

    setValue(metricKey, line, value);
  }

  public Integer getIntValue(String metricKey, int line) {
    Preconditions.checkNotNull(metricKey);
    Preconditions.checkArgument(line > 0);

    Map lines = map.get(metricKey);
    if (lines == null) {
      // not in memory, so load
      lines = loadData(metricKey, KeyValueFormat.newIntegerConverter());
      map.put(metricKey, lines);
    }
    return (Integer) lines.get(line);
  }

  public void setStringValue(String metricKey, int line, String value) {
    Preconditions.checkNotNull(metricKey);
    Preconditions.checkArgument(line > 0);
    Preconditions.checkNotNull(value);

    setValue(metricKey, line, value);
  }

  public String getStringValue(String metricKey, int line) {
    Preconditions.checkNotNull(metricKey);
    Preconditions.checkArgument(line > 0);

    Map lines = map.get(metricKey);
    if (lines == null) {
      // not in memory, so load
      lines = loadData(metricKey, KeyValueFormat.newStringConverter());
      map.put(metricKey, lines);
    }
    return (String) lines.get(line);
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

  public void save() {
    for (Map.Entry<String, Map<Integer, Object>> entry : map.entrySet()) {
      String metricKey = entry.getKey();
      Metric metric = metricFinder.findByKey(metricKey);
      if (metric == null) {
        throw new IllegalStateException("Unable to find metric with key: " + metricKey);
      }
      Map<Integer, Object> lines = entry.getValue();
      if (shouldSave(lines)) {
        String data = KeyValueFormat.format(lines);
        measureCache.put(projectKey, ComponentKeys.createEffectiveKey(projectKey, inputFile), new DefaultAnalyzerMeasureBuilder<String>()
          .forMetric(metric)
          .onFile(inputFile)
          .withValue(data)
          .build());
        entry.setValue(ImmutableMap.copyOf(lines));
      }
    }
  }

  private Map loadData(String metricKey, Converter converter) {
    AnalyzerMeasure measure = measureCache.byMetric(projectKey, ComponentKeys.createEffectiveKey(projectKey, inputFile), metricKey);
    if (measure == null) {
      // no such measure
      return ImmutableMap.of();
    }
    return ImmutableMap.copyOf(KeyValueFormat.parse((String) measure.value(), KeyValueFormat.newIntegerConverter(), converter));
  }

  /**
   * Checks that measure was not saved.
   *
   * @see #loadData(String, Converter)
   * @see #save()
   */
  private boolean shouldSave(Map<Integer, Object> lines) {
    return !(lines instanceof ImmutableMap);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("map", map)
      .toString();
  }

}
