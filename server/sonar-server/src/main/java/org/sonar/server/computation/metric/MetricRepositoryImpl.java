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
package org.sonar.server.computation.metric;

import org.sonar.core.metric.db.MetricDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;

import static java.util.Objects.requireNonNull;

public class MetricRepositoryImpl implements MetricRepository {
  private final DbClient dbClient;

  public MetricRepositoryImpl(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public Metric getByKey(String key) {
    requireNonNull(key);

    try (DbSession dbSession = dbClient.openSession(false)) {
      MetricDto metricDto = dbClient.metricDao().selectNullableByKey(dbSession, key);
      if (metricDto == null) {
        throw new IllegalStateException(String.format("Metric with key '%s' does not exist", key));
      }

      return toMetric(metricDto);
    }
  }

  @Override
  public Metric getById(long id) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      MetricDto metricDto = dbClient.metricDao().selectNullableById(dbSession, id);
      if (metricDto == null) {
        throw new IllegalStateException(String.format("Metric with id '%s' does not exist", id));
      }

      return toMetric(metricDto);
    }
  }

  private static Metric toMetric(MetricDto metricDto) {
    return new MetricImpl(metricDto.getId(), metricDto.getKey(), metricDto.getShortName(), Metric.MetricType.valueOf(metricDto.getValueType()));
  }

}
