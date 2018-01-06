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
package org.sonar.db.measure;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.component.SnapshotDto;

import static java.util.Objects.requireNonNull;

public class PastMeasureQuery {
  private final String componentUuid;
  private final List<Integer> metricIds;
  private final Long from;
  private final Long to;
  private final String status;

  public PastMeasureQuery(String componentUuid, List<Integer> metricIds, @Nullable Long from, @Nullable Long to) {
    this.componentUuid = requireNonNull(componentUuid);
    this.metricIds = requireNonNull(metricIds);
    this.from = from;
    this.to = to;
    this.status = SnapshotDto.STATUS_PROCESSED;
  }

  public String getComponentUuid() {
    return componentUuid;
  }

  public List<Integer> getMetricIds() {
    return metricIds;
  }

  @CheckForNull
  public Long getFrom() {
    return from;
  }

  @CheckForNull
  public Long getTo() {
    return to;
  }

  public String getStatus() {
    return status;
  }
}
