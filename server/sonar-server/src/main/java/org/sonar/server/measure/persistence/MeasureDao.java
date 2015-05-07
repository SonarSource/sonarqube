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

import com.google.common.base.Function;
import org.sonar.api.ServerSide;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.core.measure.db.MeasureMapper;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DaoUtils;
import org.sonar.core.persistence.DbSession;

import javax.annotation.CheckForNull;

import java.util.List;

@ServerSide
public class MeasureDao implements DaoComponent {

  public boolean existsByKey(DbSession session, String componentKey, String metricKey) {
    return mapper(session).countByComponentAndMetric(componentKey, metricKey) > 0;
  }

  @CheckForNull
  public MeasureDto findByComponentKeyAndMetricKey(DbSession session, String componentKey, String metricKey) {
    return mapper(session).selectByComponentAndMetric(componentKey, metricKey);
  }

  public List<MeasureDto> findByComponentKeyAndMetricKeys(final DbSession session, final String componentKey, List<String> metricKeys) {
    return DaoUtils.executeLargeInputs(metricKeys, new Function<List<String>, List<MeasureDto>>() {
      @Override
      public List<MeasureDto> apply(List<String> keys) {
        return mapper(session).selectByComponentAndMetrics(componentKey, keys);
      }
    });
  }

  public void insert(DbSession session, MeasureDto measureDto) {
    mapper(session).insert(measureDto);
  }

  public List<String> selectMetricKeysForSnapshot(DbSession session, long snapshotId) {
    return mapper(session).selectMetricKeysForSnapshot(snapshotId);
  }

  private MeasureMapper mapper(DbSession session) {
    return session.getMapper(MeasureMapper.class);
  }
}
