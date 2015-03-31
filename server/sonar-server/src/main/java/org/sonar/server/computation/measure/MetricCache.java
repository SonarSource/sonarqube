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

package org.sonar.server.computation.measure;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import org.sonar.core.measure.db.MetricDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.NotFoundException;

import java.util.List;
import java.util.Map;

public class MetricCache {
  private final Map<String, MetricDto> metrics;

  public MetricCache(DbClient dbClient) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      List<MetricDto> metricList = dbClient.metricDao().selectEnabled(dbSession);
      this.metrics = Maps.uniqueIndex(metricList, new Function<MetricDto, String>() {
        @Override
        public String apply(MetricDto metric) {
          return metric.getKey();
        }
      });
    } finally {
      MyBatis.closeQuietly(dbSession);
    }
  }

  public MetricDto get(String key) {
    MetricDto metric = metrics.get(key);
    if (metric == null) {
      throw new NotFoundException(String.format("Not found: '%s'", key));
    }

    return metric;
  }
}
