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
package org.sonar.server.metric;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import org.sonar.api.measures.Metric;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.metric.MetricDto;

public class MetricFinder {

  private final DbClient dbClient;

  public MetricFinder(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public Metric findByUuid(String uuid) {
    try (DbSession session = dbClient.openSession(false)) {
      MetricDto dto = dbClient.metricDao().selectByUuid(session, uuid);
      if (dto != null && dto.isEnabled()) {
        return ToMetric.INSTANCE.apply(dto);
      }
      return null;
    }
  }

  public Metric findByKey(String key) {
    try (DbSession session = dbClient.openSession(false)) {
      MetricDto dto = dbClient.metricDao().selectByKey(session, key);
      if (dto != null && dto.isEnabled()) {
        return ToMetric.INSTANCE.apply(dto);
      }
      return null;
    }
  }

  public Collection<Metric> findAll(List<String> metricKeys) {
    try (DbSession session = dbClient.openSession(false)) {
      List<MetricDto> dtos = dbClient.metricDao().selectByKeys(session, metricKeys);
      return dtos.stream().filter(IsEnabled.INSTANCE).map(ToMetric.INSTANCE).toList();
    }
  }

  public Collection<Metric> findAll() {
    try (DbSession session = dbClient.openSession(false)) {
      List<MetricDto> dtos = dbClient.metricDao().selectEnabled(session);
      return dtos.stream().map(ToMetric.INSTANCE).toList();
    }
  }

  private enum IsEnabled implements Predicate<MetricDto> {
    INSTANCE;
    @Override
    public boolean test(@Nonnull MetricDto dto) {
      return dto.isEnabled();
    }
  }

  private enum ToMetric implements Function<MetricDto, Metric> {
    INSTANCE;

    @Override
    public Metric apply(@Nonnull MetricDto dto) {
      Metric<Serializable> metric = new Metric<>();
      metric.setUuid(dto.getUuid());
      metric.setKey(dto.getKey());
      metric.setDescription(dto.getDescription());
      metric.setName(dto.getShortName());
      metric.setBestValue(dto.getBestValue());
      metric.setDomain(dto.getDomain());
      metric.setEnabled(dto.isEnabled());
      metric.setDirection(dto.getDirection());
      metric.setHidden(dto.isHidden());
      metric.setQualitative(dto.isQualitative());
      metric.setType(Metric.ValueType.valueOf(dto.getValueType()));
      metric.setOptimizedBestValue(dto.isOptimizedBestValue());
      metric.setWorstValue(dto.getWorstValue());
      return metric;
    }
  }
}
