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
package org.sonar.scanner.scan.measure;

import com.persistit.Value;
import com.persistit.encoding.CoderContext;
import com.persistit.encoding.ValueCoder;
import java.io.Serializable;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;

class MeasureValueCoder implements ValueCoder {

  private final MetricFinder metricFinder;

  public MeasureValueCoder(MetricFinder metricFinder) {
    this.metricFinder = metricFinder;
  }

  @Override
  public void put(Value value, Object object, CoderContext context) {
    DefaultMeasure<?> m = (DefaultMeasure<?>) object;
    org.sonar.api.batch.measure.Metric<?> metric = m.metric();
    value.putString(metric.key());
    value.put(m.value());
  }

  @Override
  public Object get(Value value, Class clazz, CoderContext context) {
    String metricKey = value.getString();
    org.sonar.api.batch.measure.Metric<?> metric = metricFinder.findByKey(metricKey);
    if (metric == null) {
      throw new IllegalStateException("Unknow metric with key " + metricKey);
    }
    return new DefaultMeasure()
      .forMetric(metric)
      .withValue((Serializable) value.get());
  }
}
