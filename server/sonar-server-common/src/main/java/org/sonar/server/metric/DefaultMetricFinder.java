/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.metric.MetricDto;

import static com.google.common.collect.FluentIterable.from;

public class DefaultMetricFinder implements MetricFinder {

  private final DbClient dbClient;

  public DefaultMetricFinder(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public Metric findById(int id) {
    try (DbSession session = dbClient.openSession(false)) {
      MetricDto dto = dbClient.metricDao().selectById(session, id);
      if (dto != null && dto.isEnabled()) {
        return ToMetric.INSTANCE.apply(dto);
      }
      return null;
    }
  }

  @Override
  public Metric findByKey(String key) {
    try (DbSession session = dbClient.openSession(false)) {
      MetricDto dto = dbClient.metricDao().selectByKey(session, key);
      if (dto != null && dto.isEnabled()) {
        return ToMetric.INSTANCE.apply(dto);
      }
      return null;
    }
  }

  @Override
  public Collection<Metric> findAll(List<String> metricKeys) {
    try (DbSession session = dbClient.openSession(false)) {
      List<MetricDto> dtos = dbClient.metricDao().selectByKeys(session, metricKeys);
      return from(dtos).filter(IsEnabled.INSTANCE).transform(ToMetric.INSTANCE).toList();
    }
  }

  @Override
  public Collection<Metric> findAll() {
    try (DbSession session = dbClient.openSession(false)) {
      List<MetricDto> dtos = dbClient.metricDao().selectEnabled(session);
      return from(dtos).transform(ToMetric.INSTANCE).toList();
    }
  }

  private enum IsEnabled implements Predicate<MetricDto> {
    INSTANCE;
    @Override
    public boolean apply(@Nonnull MetricDto dto) {
      return dto.isEnabled();
    }
  }

  private enum ToMetric implements Function<MetricDto, Metric> {
    INSTANCE;

    @Override
    public Metric apply(@Nonnull MetricDto dto) {
      Metric<Serializable> metric = new Metric<>();
      metric.setId(dto.getId());
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
      metric.setUserManaged(dto.isUserManaged());
      metric.setWorstValue(dto.getWorstValue());
      return metric;
    }
  }
}
