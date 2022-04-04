/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import java.util.Optional;
import org.apache.ibatis.session.RowBounds;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;

import static java.util.Optional.ofNullable;

public class CustomMeasureDao implements Dao {
  private final UuidFactory uuidFactory;

  public CustomMeasureDao(UuidFactory uuidFactory) {
    this.uuidFactory = uuidFactory;
  }

  public void insert(DbSession session, CustomMeasureDto customMeasureDto) {
    customMeasureDto.setUuid(uuidFactory.create());
    mapper(session).insert(customMeasureDto);
  }

  public void update(DbSession session, CustomMeasureDto customMeasure) {
    mapper(session).update(customMeasure);
  }

  public void delete(DbSession session, String uuid) {
    mapper(session).delete(uuid);
  }

  public void deleteByMetricUuids(DbSession session, List<String> metricUuids) {
    DatabaseUtils.executeLargeInputsWithoutOutput(metricUuids, input -> mapper(session).deleteByMetricUuids(metricUuids));
  }

  public Optional<CustomMeasureDto> selectByUuid(DbSession session, String uuid) {
    return ofNullable(mapper(session).selectByUuid(uuid));
  }

  public List<CustomMeasureDto> selectByMetricUuid(DbSession session, String metricUuid) {
    return mapper(session).selectByMetricUuid(metricUuid);
  }

  public int countByComponentIdAndMetricUuid(DbSession session, String componentUuid, String metricUuid) {
    return mapper(session).countByComponentIdAndMetricUuid(componentUuid, metricUuid);
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
