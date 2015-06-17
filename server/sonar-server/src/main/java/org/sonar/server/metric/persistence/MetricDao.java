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

package org.sonar.server.metric.persistence;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.ibatis.session.RowBounds;
import org.sonar.api.server.ServerSide;
import org.sonar.core.metric.db.MetricDto;
import org.sonar.core.metric.db.MetricMapper;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DaoUtils;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.exceptions.NotFoundException;

import static com.google.common.collect.Lists.newArrayList;

@ServerSide
public class MetricDao implements DaoComponent {

  @CheckForNull
  public MetricDto selectNullableByKey(DbSession session, String key) {
    return mapper(session).selectByKey(key);
  }

  public List<MetricDto> selectNullableByKeys(final DbSession session, List<String> keys) {
    return DaoUtils.executeLargeInputs(keys, new Function<List<String>, List<MetricDto>>() {
      @Override
      public List<MetricDto> apply(@Nonnull List<String> input) {
        return mapper(session).selectByKeys(input);
      }
    });
  }

  public MetricDto selectByKey(DbSession session, String key) {
    MetricDto metric = selectNullableByKey(session, key);
    if (metric == null) {
      throw new NotFoundException(String.format("Metric key '%s' not found", key));
    }
    return metric;
  }

  public List<MetricDto> selectEnabled(DbSession session) {
    return mapper(session).selectAllEnabled();
  }

  public List<MetricDto> selectEnabled(DbSession session, @Nullable Boolean isCustom, SearchOptions searchOptions) {
    Map<String, Object> properties = Maps.newHashMapWithExpectedSize(1);
    if (isCustom != null) {
      properties.put("isCustom", isCustom);
    }

    return mapper(session).selectAllEnabled(properties, new RowBounds(searchOptions.getOffset(), searchOptions.getLimit()));
  }

  public int countEnabled(DbSession session, @Nullable Boolean isCustom) {
    return mapper(session).countEnabled(isCustom);
  }

  public void insert(DbSession session, MetricDto dto) {
    mapper(session).insert(dto);
  }

  public void insert(DbSession session, Collection<MetricDto> items) {
    for (MetricDto item : items) {
      insert(session, item);
    }
  }

  public void insert(DbSession session, MetricDto item, MetricDto... others) {
    insert(session, Lists.asList(item, others));
  }

  public List<String> selectDomains(DbSession session) {
    return newArrayList(Collections2.filter(mapper(session).selectDomains(), new Predicate<String>() {
      @Override
      public boolean apply(@Nonnull String input) {
        return !input.isEmpty();
      }
    }));
  }

  private MetricMapper mapper(DbSession session) {
    return session.getMapper(MetricMapper.class);
  }

  public void disableByIds(final DbSession session, List<Integer> ids) {
    DaoUtils.executeLargeInputsWithoutOutput(ids, new Function<List<Integer>, Void>() {
      @Override
      public Void apply(@Nonnull List<Integer> input) {
        mapper(session).disableByIds(input);
        return null;
      }
    });
  }

  public void disableByKey(final DbSession session, String key) {
    mapper(session).disableByKey(key);
  }

  public void update(DbSession session, MetricDto metric) {
    mapper(session).update(metric);
  }

  public MetricDto selectNullableById(DbSession session, long id) {
    return mapper(session).selectById(id);
  }

  public MetricDto selectById(DbSession session, int id) {
    MetricDto metric = mapper(session).selectById(id);
    if (metric == null) {
      throw new NotFoundException(String.format("Metric id '%d' not found", id));
    }
    return metric;
  }
}
