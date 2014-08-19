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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.System2;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.core.measure.db.MeasureKey;
import org.sonar.core.measure.db.MeasureMapper;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.BaseDao;

import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class MeasureDao extends BaseDao<MeasureMapper, MeasureDto, MeasureKey> implements ServerComponent, DaoComponent {

  public MeasureDao() {
    this(System2.INSTANCE);
  }

  @VisibleForTesting
  public MeasureDao(System2 system) {
    super(MeasureMapper.class, system);
  }

  @Override
  protected MeasureDto doGetNullableByKey(DbSession session, MeasureKey key) {
    return mapper(session).selectByKey(key);
  }

  public boolean existsByKey(MeasureKey key, DbSession session) {
    return mapper(session).countByKey(key) > 0;
  }

  public List<MeasureDto> findByComponentKeyAndMetricKeys(String componentKey, List<String> metricKeys, DbSession session) {
    if (metricKeys.isEmpty()) {
      return Collections.emptyList();
    }
    List<MeasureDto> dtos = newArrayList();
    List<List<String>> partitions = Lists.partition(newArrayList(metricKeys), 1000);
    for (List<String> partition : partitions) {
      dtos.addAll(mapper(session).selectByComponentAndMetrics(componentKey, partition));
    }
    return dtos;
  }

  public MeasureDto findByComponentKeyAndMetricKey(String componentKey, String metricKey, DbSession session) {
    return mapper(session).selectByComponentAndMetric(componentKey, metricKey);
  }
}
