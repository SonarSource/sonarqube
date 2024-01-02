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

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.apache.ibatis.session.RowBounds;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.RowNotFoundException;

import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class MetricDao implements Dao {

  @CheckForNull
  public MetricDto selectByKey(DbSession session, String key) {
    return mapper(session).selectByKey(key);
  }

  public List<MetricDto> selectByKeys(final DbSession session, Collection<String> keys) {
    return executeLargeInputs(keys, mapper(session)::selectByKeys);
  }

  public MetricDto selectOrFailByKey(DbSession session, String key) {
    MetricDto metric = selectByKey(session, key);
    if (metric == null) {
      throw new RowNotFoundException(String.format("Metric key '%s' not found", key));
    }
    return metric;
  }

  public List<MetricDto> selectAll(DbSession session) {
    return mapper(session).selectAll();
  }

  public List<MetricDto> selectEnabled(DbSession session) {
    return mapper(session).selectAllEnabled();
  }

  public List<MetricDto> selectEnabled(DbSession session, int offset, int limit) {
    return mapper(session).selectAllEnabled(new RowBounds(offset, limit));
  }

  public int countEnabled(DbSession session) {
    return mapper(session).countEnabled();
  }

  public MetricDto insert(DbSession session, MetricDto dto) {
    mapper(session).insert(dto);

    return dto;
  }

  public void insert(DbSession session, Collection<MetricDto> items) {
    for (MetricDto item : items) {
      insert(session, item);
    }
  }

  public void insert(DbSession session, MetricDto item, MetricDto... others) {
    insert(session, Lists.asList(item, others));
  }

  public List<MetricDto> selectByUuids(DbSession session, Set<String> uuidsSet) {
    return executeLargeInputs(new ArrayList<>(uuidsSet), mapper(session)::selectByUuids);
  }

  private static MetricMapper mapper(DbSession session) {
    return session.getMapper(MetricMapper.class);
  }

  /**
   * Disable a metric and return {@code false} if the metric does not exist
   * or is already disabled.
   */
  public boolean disableByKey(DbSession session, String key) {
    return mapper(session).disableByKey(key) == 1;
  }

  public void update(DbSession session, MetricDto metric) {
    mapper(session).update(metric);
  }

  @CheckForNull
  public MetricDto selectByUuid(DbSession session, String uuid) {
    return mapper(session).selectByUuid(uuid);
  }

}
