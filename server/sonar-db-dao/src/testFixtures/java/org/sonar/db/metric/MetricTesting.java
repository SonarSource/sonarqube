/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.security.SecureRandom;
import java.util.Random;
import org.apache.commons.lang3.RandomStringUtils;
import org.sonar.api.measures.Metric;

public class MetricTesting {

  private static final Random RANDOM = new SecureRandom();

  private MetricTesting() {
    // static stuff only
  }

  public static MetricDto newMetricDto() {
    Metric.ValueType[] metricTypes = Metric.ValueType.values();
    return new MetricDto()
      .setUuid(RandomStringUtils.randomAlphanumeric(40))
      .setKey(RandomStringUtils.randomAlphanumeric(64))
      .setShortName(RandomStringUtils.randomAlphanumeric(64))
      .setValueType(metricTypes[RANDOM.nextInt(metricTypes.length - 1)].name())
      .setDomain(RandomStringUtils.randomAlphanumeric(64))
      .setDescription(RandomStringUtils.randomAlphanumeric(250))
      .setBestValue(RANDOM.nextDouble())
      .setDeleteHistoricalData(RANDOM.nextBoolean())
      .setDirection(RANDOM.nextInt(Integer.MAX_VALUE))
      .setHidden(RANDOM.nextBoolean())
      .setEnabled(true)
      .setOptimizedBestValue(RANDOM.nextBoolean())
      .setQualitative(RANDOM.nextBoolean())
      .setWorstValue(RANDOM.nextDouble());
  }
}
