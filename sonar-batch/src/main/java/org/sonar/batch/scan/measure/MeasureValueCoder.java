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
package org.sonar.batch.scan.measure;

import com.persistit.Value;
import com.persistit.encoding.CoderContext;
import com.persistit.encoding.ValueCoder;
import javax.annotation.Nullable;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.PersistenceMode;

class MeasureValueCoder implements ValueCoder {

  private final MetricFinder metricFinder;

  public MeasureValueCoder(MetricFinder metricFinder) {
    this.metricFinder = metricFinder;
  }

  @Override
  public void put(Value value, Object object, CoderContext context) {
    Measure<?> m = (Measure) object;
    value.putUTF(m.getMetricKey());
    value.put(m.getValue());
    putUTFOrNull(value, m.getData());
    putUTFOrNull(value, m.getDescription());
    value.putString(m.getAlertStatus() != null ? m.getAlertStatus().name() : null);
    putUTFOrNull(value, m.getAlertText());
    value.putDate(m.getDate());
    value.put(m.getVariation1());
    value.put(m.getVariation2());
    value.put(m.getVariation3());
    value.put(m.getVariation4());
    value.put(m.getVariation5());
    putUTFOrNull(value, m.getUrl());
    Integer personId = m.getPersonId();
    value.put(personId != null ? personId.intValue() : null);
    PersistenceMode persistenceMode = m.getPersistenceMode();
    value.putString(persistenceMode != null ? persistenceMode.name() : null);
  }

  private static void putUTFOrNull(Value value, @Nullable String utfOrNull) {
    if (utfOrNull != null) {
      value.putUTF(utfOrNull);
    } else {
      value.putNull();
    }
  }

  @Override
  public Object get(Value value, Class clazz, CoderContext context) {
    Measure<?> m = new Measure();
    String metricKey = value.getString();
    org.sonar.api.batch.measure.Metric metric = metricFinder.findByKey(metricKey);
    if (metric == null) {
      throw new IllegalStateException("Unknow metric with key " + metricKey);
    }
    m.setMetric((org.sonar.api.measures.Metric) metric);
    m.setRawValue(value.isNull(true) ? null : value.getDouble());
    m.setData(value.getString());
    m.setDescription(value.getString());
    m.setAlertStatus(value.isNull(true) ? null : Metric.Level.valueOf(value.getString()));
    m.setAlertText(value.getString());
    m.setDate(value.getDate());
    m.setVariation1(value.isNull(true) ? null : value.getDouble());
    m.setVariation2(value.isNull(true) ? null : value.getDouble());
    m.setVariation3(value.isNull(true) ? null : value.getDouble());
    m.setVariation4(value.isNull(true) ? null : value.getDouble());
    m.setVariation5(value.isNull(true) ? null : value.getDouble());
    m.setUrl(value.getString());
    m.setPersonId(value.isNull(true) ? null : value.getInt());
    m.setPersistenceMode(value.isNull(true) ? null : PersistenceMode.valueOf(value.getString()));
    return m;
  }
}
