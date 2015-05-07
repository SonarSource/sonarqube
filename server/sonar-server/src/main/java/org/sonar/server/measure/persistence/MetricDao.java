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

package org.sonar.server.measure.persistence;

import org.sonar.api.ServerSide;
import org.sonar.core.measure.db.MetricDto;
import org.sonar.core.measure.db.MetricMapper;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DbSession;

import javax.annotation.CheckForNull;

import java.util.List;

@ServerSide
public class MetricDao implements DaoComponent {

  @CheckForNull
  public MetricDto selectByKey(DbSession session, String key) {
    return session.getMapper(MetricMapper.class).selectByKey(key);
  }

  public List<MetricDto> selectEnabled(DbSession session) {
    return session.getMapper(MetricMapper.class).selectAllEnabled();
  }

  public void insert(DbSession session, MetricDto dto) {
    session.getMapper(MetricMapper.class).insert(dto);
  }
}
