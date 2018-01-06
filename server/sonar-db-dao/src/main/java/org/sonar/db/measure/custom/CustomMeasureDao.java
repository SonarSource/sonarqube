/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.db.measure.custom;

import java.util.List;
import javax.annotation.CheckForNull;
import org.apache.ibatis.session.RowBounds;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;
import org.sonar.db.RowNotFoundException;

public class CustomMeasureDao implements Dao {
  public void insert(DbSession session, CustomMeasureDto customMeasureDto) {
    mapper(session).insert(customMeasureDto);
  }

  public void update(DbSession session, CustomMeasureDto customMeasure) {
    mapper(session).update(customMeasure);
  }

  public void delete(DbSession session, long id) {
    mapper(session).delete(id);
  }

  public void deleteByMetricIds(DbSession session, List<Integer> metricIds) {
    DatabaseUtils.executeLargeInputsWithoutOutput(metricIds, input -> mapper(session).deleteByMetricIds(metricIds));
  }

  @CheckForNull
  public CustomMeasureDto selectById(DbSession session, long id) {
    return mapper(session).selectById(id);
  }

  public CustomMeasureDto selectOrFail(DbSession session, long id) {
    CustomMeasureDto customMeasure = selectById(session, id);
    if (customMeasure == null) {
      throw new RowNotFoundException(String.format("Custom measure '%d' not found.", id));
    }
    return customMeasure;
  }

  public List<CustomMeasureDto> selectByMetricId(DbSession session, int metricId) {
    return mapper(session).selectByMetricId(metricId);
  }

  public int countByComponentIdAndMetricId(DbSession session, String componentUuid, int metricId) {
    return mapper(session).countByComponentIdAndMetricId(componentUuid, metricId);
  }

  public List<CustomMeasureDto> selectByComponentUuid(DbSession session, String componentUuid, int offset, int limit) {
    return mapper(session).selectByComponentUuid(componentUuid, new RowBounds(offset, limit));
  }

  public List<CustomMeasureDto> selectByComponentUuid(DbSession session, String componentUuid) {
    return mapper(session).selectByComponentUuid(componentUuid);
  }

  /**
   * Used by Views plugin
   */
  public List<CustomMeasureDto> selectByMetricKeyAndTextValue(DbSession session, String metricKey, String textValue) {
    return mapper(session).selectByMetricKeyAndTextValue(metricKey, textValue);
  }

  private static CustomMeasureMapper mapper(DbSession session) {
    return session.getMapper(CustomMeasureMapper.class);
  }

  public int countByComponentUuid(DbSession dbSession, String uuid) {
    return mapper(dbSession).countByComponentUuid(uuid);
  }
}
