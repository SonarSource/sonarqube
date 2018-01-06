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
package org.sonar.api.measures;

import java.util.Map;
import java.util.TreeMap;
import org.sonar.api.utils.KeyValueFormat;

/**
 * @since 1.10
 * @deprecated since 5.6. Use directly {@link KeyValueFormat}.
 */
@Deprecated
public class PropertiesBuilder<K, V> {
  private Metric metric;
  private Map<K, V> props;

  public PropertiesBuilder(Metric metric, Map<K, V> map) {
    this.props = new TreeMap<>(map);
    this.metric = metric;
  }

  public PropertiesBuilder(Metric metric) {
    this.props = new TreeMap<>();
    this.metric = metric;
  }

  public PropertiesBuilder() {
    this.props = new TreeMap<>();
  }

  public PropertiesBuilder<K, V> clear() {
    this.props.clear();
    return this;
  }

  public Map<K, V> getProps() {
    return props;
  }

  public Metric getMetric() {
    return metric;
  }

  public PropertiesBuilder<K, V> setMetric(Metric metric) {
    this.metric = metric;
    return this;
  }

  public PropertiesBuilder<K, V> add(K key, V value) {
    props.put(key, value);
    return this;
  }

  public PropertiesBuilder<K, V> addAll(Map<K, V> map) {
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
