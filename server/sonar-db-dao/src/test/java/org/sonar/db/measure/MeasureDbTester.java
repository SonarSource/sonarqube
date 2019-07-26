/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db.measure;

import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.custom.CustomMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.user.UserDto;

import static org.sonar.db.measure.MeasureTesting.newLiveMeasure;
import static org.sonar.db.measure.MeasureTesting.newMeasureDto;
import static org.sonar.db.measure.custom.CustomMeasureTesting.newCustomMeasureDto;
import static org.sonar.db.metric.MetricTesting.newMetricDto;

public class MeasureDbTester {
  private final DbClient dbClient;
  private final DbSession dbSession;

  public MeasureDbTester(DbTester db) {
    this.dbClient = db.getDbClient();
    this.dbSession = db.getSession();
  }

  @SafeVarargs
  public final MeasureDto insertMeasure(ComponentDto component, SnapshotDto analysis, MetricDto metricDto, Consumer<MeasureDto>... consumers) {
    MeasureDto measureDto = newMeasureDto(metricDto, component, analysis);
    Arrays.stream(consumers).forEach(c -> c.accept(measureDto));
    dbClient.measureDao().insert(dbSession, measureDto);
    dbSession.commit();
    return measureDto;
  }

  @SafeVarargs
  public final LiveMeasureDto insertLiveMeasure(ComponentDto component, MetricDto metric, Consumer<LiveMeasureDto>... consumers) {
    LiveMeasureDto dto = newLiveMeasure(component, metric);
    Arrays.stream(consumers).forEach(c -> c.accept(dto));
    dbClient.liveMeasureDao().insert(dbSession, dto);
    dbSession.commit();
    return dto;
  }

  @SafeVarargs
  public final CustomMeasureDto insertCustomMeasure(@Nullable UserDto user, ComponentDto component, MetricDto metricDto, Consumer<CustomMeasureDto>... consumers) {
    Preconditions.checkArgument(metricDto.isUserManaged(),"Custom measure must be created from a custom metric");
    CustomMeasureDto dto = newCustomMeasureDto()
      .setComponentUuid(component.uuid())
      .setMetricId(metricDto.getId());
    if (user != null) {
      dto.setUserUuid(user.getUuid());
    }
    Arrays.stream(consumers).forEach(c -> c.accept(dto));
    dbClient.customMeasureDao().insert(dbSession, dto);
    dbSession.commit();
    return dto;
  }

  @SafeVarargs
  public final MetricDto insertMetric(Consumer<MetricDto>... consumers) {
    MetricDto metricDto = newMetricDto();
    Arrays.stream(consumers).forEach(c -> c.accept(metricDto));
    dbClient.metricDao().insert(dbSession, metricDto);
    dbSession.commit();
    return metricDto;
  }

}
