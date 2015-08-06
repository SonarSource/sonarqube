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

package org.sonar.db.metric;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.ibatis.session.RowBounds;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;
import org.sonar.db.RowNotFoundException;

import static com.google.common.collect.Lists.newArrayList;

public class MetricDao implements Dao {

  @CheckForNull
  public MetricDto selectByKey(DbSession session, String key) {
    return mapper(session).selectByKey(key);
  }

  public List<MetricDto> selectByKeys(final DbSession session, List<String> keys) {
    return DatabaseUtils.executeLargeInputs(keys, new Function<List<String>, List<MetricDto>>() {
      @Override
      public List<MetricDto> apply(@Nonnull List<String> input) {
        return mapper(session).selectByKeys(input);
      }
    });
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

  public List<MetricDto> selectEnabled(DbSession session, @Nullable Boolean isCustom, int offset, int limit) {
    Map<String, Object> properties = Maps.newHashMapWithExpectedSize(1);
    if (isCustom != null) {
      properties.put("isCustom", isCustom);
    }

    return mapper(session).selectAllEnabled(properties, new RowBounds(offset, limit));
  }

  public int countEnabled(DbSession session, @Nullable Boolean isCustom) {
    return mapper(session).countEnabled(isCustom);
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

  public List<String> selectEnabledDomains(DbSession session) {
    return newArrayList(Collections2.filter(mapper(session).selectDomains(), new NotEmptyPredicate()));
  }

  public List<MetricDto> selectAvailableCustomMetricsByComponentUuid(DbSession session, String projectUuid) {
    return mapper(session).selectAvailableCustomMetricsByComponentUuid(projectUuid);
  }

  public List<MetricDto> selectByIds(final DbSession session, Set<Integer> idsSet) {
    List<Integer> ids = new ArrayList<>(idsSet);
    return DatabaseUtils.executeLargeInputs(ids, new Function<List<Integer>, List<MetricDto>>() {
      @Override
      public List<MetricDto> apply(@Nonnull List<Integer> ids) {
        return mapper(session).selectByIds(ids);
      }
    });
  }

  private static class NotEmptyPredicate implements Predicate<String> {

    @Override
    public boolean apply(@Nonnull String input) {
      return !input.isEmpty();
    }
  }

  private MetricMapper mapper(DbSession session) {
    return session.getMapper(MetricMapper.class);
  }

  public void disableCustomByIds(final DbSession session, List<Integer> ids) {
    DatabaseUtils.executeLargeInputsWithoutOutput(ids, new Function<List<Integer>, Void>() {
      @Override
      public Void apply(@Nonnull List<Integer> input) {
        mapper(session).disableByIds(input);
        return null;
      }
    });
  }

  public void disableCustomByKey(final DbSession session, String key) {
    mapper(session).disableByKey(key);
  }

  public void update(DbSession session, MetricDto metric) {
    mapper(session).update(metric);
  }

  @CheckForNull
  public MetricDto selectById(DbSession session, long id) {
    return mapper(session).selectById(id);
  }

  public MetricDto selectOrFailById(DbSession session, long id) {
    MetricDto metric = mapper(session).selectById(id);
    if (metric == null) {
      throw new RowNotFoundException(String.format("Metric id '%d' not found", id));
    }
    return metric;
  }
}
