/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.measure;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.measure.NewMeasure;
import org.sonar.api.utils.KeyValueFormat;

public class ModuleMeasureSensor implements Sensor {

  private static final String PROPERTY = "sonar.measure.valueByMetric";

  private final MetricFinder metricFinder;

  public ModuleMeasureSensor(MetricFinder metricFinder) {
    this.metricFinder = metricFinder;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("Generate measure on each module");
  }

  @Override
  public void execute(SensorContext context) {
    Optional<String> property = context.config().get(PROPERTY);
    if (!property.isPresent()) {
      return;
    }
    Map<String, String> valueByMetric = KeyValueFormat.parse(property.get());
    valueByMetric.forEach((metricKey, value) -> {
      org.sonar.api.batch.measure.Metric<Serializable> metric = metricFinder.findByKey(metricKey);
      if (metric == null) {
        throw new IllegalStateException(String.format("Metric '%s' doesn't exist", metricKey));
      }
      NewMeasure<Serializable> newMeasure = context.newMeasure()
        .forMetric(metric)
        .on(context.module())
        .withValue(Integer.parseInt(value));
      newMeasure.save();
    });
  }

}