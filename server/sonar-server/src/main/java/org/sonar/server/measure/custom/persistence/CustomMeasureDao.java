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

package org.sonar.server.measure.custom.persistence;

import com.google.common.base.Function;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.sonar.api.server.ServerSide;
import org.sonar.core.custommeasure.db.CustomMeasureDto;
import org.sonar.core.custommeasure.db.CustomMeasureMapper;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DaoUtils;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.exceptions.NotFoundException;

@ServerSide
public class CustomMeasureDao implements DaoComponent {
  public void insert(DbSession session, CustomMeasureDto customMeasureDto) {
    mapper(session).insert(customMeasureDto);
  }

  public void delete(DbSession session, long id) {
    mapper(session).delete(id);
  }

  public void deleteByMetricIds(final DbSession session, final List<Integer> metricIds) {
    DaoUtils.executeLargeInputsWithoutOutput(metricIds, new Function<List<Integer>, Void>() {
      @Override
      public Void apply(@Nonnull List<Integer> input) {
        mapper(session).deleteByMetricIds(metricIds);
        return null;
      }
    });
  }

  @CheckForNull
  public CustomMeasureDto selectNullableById(DbSession session, long id) {
    return mapper(session).selectById(id);
  }

  public CustomMeasureDto selectById(DbSession session, long id) {
    CustomMeasureDto customMeasure = selectNullableById(session, id);
    if (customMeasure == null) {
      throw new NotFoundException(String.format("CustomMeasure '%d' not found", id));
    }
    return customMeasure;
  }

  public List<CustomMeasureDto> selectByMetricId(DbSession session, int id) {
    return mapper(session).selectByMetricId(id);
  }

  public List<CustomMeasureDto> selectByComponentId(DbSession session, long id) {
    return mapper(session).selectByComponentId(id);
  }

  private CustomMeasureMapper mapper(DbSession session) {
    return session.getMapper(CustomMeasureMapper.class);
  }
}
