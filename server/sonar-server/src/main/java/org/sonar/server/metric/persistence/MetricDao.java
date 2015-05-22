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

import com.google.common.collect.Maps;
import org.apache.ibatis.session.RowBounds;
import org.sonar.api.server.ServerSide;
import org.sonar.core.metric.db.MetricDto;
import org.sonar.core.metric.db.MetricMapper;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.es.SearchOptions;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

@ServerSide
public class MetricDao implements DaoComponent {

  @CheckForNull
  public MetricDto selectByKey(DbSession session, String key) {
    return session.getMapper(MetricMapper.class).selectByKey(key);
  }

  public List<MetricDto> selectEnabled(DbSession session) {
    return session.getMapper(MetricMapper.class).selectAllEnabled();
  }

  public List<MetricDto> selectEnabled(DbSession session, @Nullable Boolean isCustom, SearchOptions searchOptions) {
    Map<String, Object> properties = Maps.newHashMapWithExpectedSize(1);
    if (isCustom != null) {
      properties.put("isCustom", isCustom);
    }

    return session.getMapper(MetricMapper.class).selectAllEnabled(properties, new RowBounds(searchOptions.getOffset(), searchOptions.getLimit()));
  }

  public void insert(DbSession session, MetricDto dto) {
    session.getMapper(MetricMapper.class).insert(dto);
  }
}
