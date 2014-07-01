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
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.technicaldebt.batch.Characteristic;
import org.sonar.api.technicaldebt.batch.Requirement;
import org.sonar.api.technicaldebt.batch.TechnicalDebtModel;

class MeasureValueCoder implements ValueCoder {

  private final MetricFinder metricFinder;
  private final TechnicalDebtModel techDebtModel;

  public MeasureValueCoder(MetricFinder metricFinder, TechnicalDebtModel techDebtModel) {
    this.metricFinder = metricFinder;
    this.techDebtModel = techDebtModel;
  }

  public void put(Value value, Object object, CoderContext context) {
    Measure<?> m = (Measure) object;
    value.putString(m.getMetricKey());
    value.put(m.getValue());
    value.putString(m.getData());
    value.putString(m.getDescription());
    value.putString(m.getAlertStatus() != null ? m.getAlertStatus().name() : null);
    value.putString(m.getAlertText());
    value.put(m.getTendency());
    value.putDate(m.getDate());
    value.put(m.getVariation1());
    value.put(m.getVariation2());
    value.put(m.getVariation3());
    value.put(m.getVariation4());
    value.put(m.getVariation5());
    value.putString(m.getUrl());
    Characteristic characteristic = m.getCharacteristic();
    value.put(characteristic != null ? characteristic.id() : null);
    Requirement requirement = m.getRequirement();
    value.put(requirement != null ? requirement.id() : null);
    Integer personId = m.getPersonId();
    value.put(personId != null ? personId.intValue() : null);
    PersistenceMode persistenceMode = m.getPersistenceMode();
    value.putString(persistenceMode != null ? persistenceMode.name() : null);
  }

  public Object get(Value value, Class clazz, CoderContext context) {
    Measure<?> m = new Measure();
    String metricKey = value.getString();
    m.setMetric(metricFinder.findByKey(metricKey));
    m.setRawValue(value.isNull(true) ? null : value.getDouble());
    m.setData(value.getString());
    m.setDescription(value.getString());
    m.setAlertStatus(value.isNull(true) ? null : Metric.Level.valueOf(value.getString()));
    m.setAlertText(value.getString());
    m.setTendency(value.isNull(true) ? null : value.getInt());
    m.setDate(value.getDate());
    m.setVariation1(value.isNull(true) ? null : value.getDouble());
    m.setVariation2(value.isNull(true) ? null : value.getDouble());
    m.setVariation3(value.isNull(true) ? null : value.getDouble());
    m.setVariation4(value.isNull(true) ? null : value.getDouble());
    m.setVariation5(value.isNull(true) ? null : value.getDouble());
    m.setUrl(value.getString());
    m.setCharacteristic(value.isNull(true) ? null : techDebtModel.characteristicById(value.getInt()));
    m.setRequirement(value.isNull(true) ? null : techDebtModel.requirementsById(value.getInt()));
    m.setPersonId(value.isNull(true) ? null : value.getInt());
    m.setPersistenceMode(value.isNull(true) ? null : PersistenceMode.valueOf(value.getString()));
    return m;
  }
}
