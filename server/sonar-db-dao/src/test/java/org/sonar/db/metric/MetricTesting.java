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
package org.sonar.db.metric;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.sonar.api.measures.Metric;

public class MetricTesting {
  private MetricTesting() {
    // static stuff only
  }

  public static MetricDto newMetricDto() {
    Metric.ValueType[] metricTypes = Metric.ValueType.values();
    return new MetricDto()
      .setId(RandomUtils.nextInt())
      .setKey(RandomStringUtils.randomAlphanumeric(64))
      .setShortName(RandomStringUtils.randomAlphanumeric(64))
      .setValueType(metricTypes[RandomUtils.nextInt(metricTypes.length - 1)].name())
      .setDomain(RandomStringUtils.randomAlphanumeric(64))
      .setDescription(RandomStringUtils.randomAlphanumeric(250))
      .setBestValue(RandomUtils.nextDouble())
      .setDeleteHistoricalData(RandomUtils.nextBoolean())
      .setDirection(RandomUtils.nextInt())
      .setHidden(RandomUtils.nextBoolean())
      .setEnabled(true)
      .setOptimizedBestValue(RandomUtils.nextBoolean())
      .setQualitative(RandomUtils.nextBoolean())
      .setUserManaged(RandomUtils.nextBoolean())
      .setWorstValue(RandomUtils.nextDouble());
  }
}
