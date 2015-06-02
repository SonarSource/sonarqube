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

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import javax.annotation.Nonnull;
import org.sonar.api.measures.Metric;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.db.DbClient;

public class MeasureRepositoryImpl implements MeasureRepository {
  private final DbClient dbClient;
  private final BatchReportReader reportReader;

  public MeasureRepositoryImpl(DbClient dbClient, BatchReportReader reportReader) {
    this.dbClient = dbClient;
    this.reportReader = reportReader;
  }

  @Override
  public Optional<MeasureDto> findPrevious(Component component, Metric<?> metric) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return Optional.fromNullable(
        dbClient.measureDao().findByComponentKeyAndMetricKey(dbSession, component.getKey(), metric.getKey())
        );
    }
  }

  @Override
  public Optional<BatchReport.Measure> findCurrent(Component component, final Metric<?> metric) {
    return Optional.fromNullable(Iterables.find(
        reportReader.readComponentMeasures(component.getRef()),
        new Predicate<BatchReport.Measure>() {
          @Override
          public boolean apply(@Nonnull BatchReport.Measure input) {
            return input.getMetricKey().equals(metric.getKey());
          }
        }
    ));
  }
}
