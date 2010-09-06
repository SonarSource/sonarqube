/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.measures;

import org.sonar.api.utils.KeyValueFormat;

import java.util.Map;
import java.util.TreeMap;

/**
 * @since 1.10
 */
public class PropertiesBuilder<KEY, VALUE> {
  private Metric metric;
  private Map<KEY, VALUE> props;

  public PropertiesBuilder(Metric metric, Map<KEY, VALUE> map) {
    this.props = new TreeMap<KEY, VALUE>(map);
    this.metric = metric;
  }

  public PropertiesBuilder(Metric metric) {
    this.props = new TreeMap<KEY, VALUE>();
    this.metric = metric;
  }

  public PropertiesBuilder() {
    this.props = new TreeMap<KEY, VALUE>();
  }

  public PropertiesBuilder<KEY, VALUE> clear() {
    this.props.clear();
    return this;
  }

  public Map<KEY, VALUE> getProps() {
    return props;
  }

  public Metric getMetric() {
    return metric;
  }

  public PropertiesBuilder<KEY, VALUE> setMetric(Metric metric) {
    this.metric = metric;
    return this;
  }

  public PropertiesBuilder<KEY, VALUE> add(KEY key, VALUE value) {
    props.put(key, value);
    return this;
  }

  public PropertiesBuilder<KEY, VALUE> addAll(Map<KEY, VALUE> map) {
    props.putAll(map);
    return this;
  }

  public Measure build() {
    return new Measure(metric, buildData());
  }

  public String buildData() {
    return KeyValueFormat.format(props);
  }
}
