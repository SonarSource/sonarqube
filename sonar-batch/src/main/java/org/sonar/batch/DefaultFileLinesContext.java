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
package org.sonar.batch;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.utils.KeyValueFormat.Converter;

public class DefaultFileLinesContext implements FileLinesContext {

  private final SonarIndex index;
  private final Resource resource;

  /**
   * metric key -> line -> value
   */
  private final Map<String, Map<Integer, Object>> map = Maps.newHashMap();

  public DefaultFileLinesContext(SonarIndex index, Resource resource) {
    Preconditions.checkNotNull(index);
    Preconditions.checkArgument(ResourceUtils.isFile(resource));
    this.index = index;
    this.resource = resource;
  }

  @Override
  public void setIntValue(String metricKey, int line, int value) {
    Preconditions.checkNotNull(metricKey);
    Preconditions.checkArgument(line > 0);

    setValue(metricKey, line, value);
  }

  @Override
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

  @Override
  public void setStringValue(String metricKey, int line, String value) {
    Preconditions.checkNotNull(metricKey);
    Preconditions.checkArgument(line > 0);
    Preconditions.checkNotNull(value);

    setValue(metricKey, line, value);
  }

  @Override
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

  @Override
  public void save() {
    for (Map.Entry<String, Map<Integer, Object>> entry : map.entrySet()) {
      String metricKey = entry.getKey();
      Map<Integer, Object> lines = entry.getValue();
      if (shouldSave(lines)) {
        String data = KeyValueFormat.format(lines);
        Measure measure = new Measure(metricKey)
          .setPersistenceMode(PersistenceMode.DATABASE)
          .setData(data);
        index.addMeasure(resource, measure);
        entry.setValue(ImmutableMap.copyOf(lines));
      }
    }
  }

  private Map loadData(String metricKey, Converter converter) {
    // FIXME no way to load measure only by key
    Measure measure = index.getMeasure(resource, new Metric(metricKey));
    String data = measure != null ? measure.getData() : null;
    if (data != null) {
      return ImmutableMap.copyOf(KeyValueFormat.parse(data, KeyValueFormat.newIntegerConverter(), converter));
    }
    // no such measure
    return ImmutableMap.of();
  }

  /**
   * Checks that measure was not saved.
   *
   * @see #loadData(String, Converter)
   * @see #save()
   */
  private static boolean shouldSave(Map<Integer, Object> lines) {
    return !(lines instanceof ImmutableMap);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("map", map)
      .toString();
  }

}
