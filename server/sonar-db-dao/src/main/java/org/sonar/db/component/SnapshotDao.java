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
package org.sonar.db.component;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.ibatis.session.RowBounds;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto.Status;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;
import static java.util.Objects.requireNonNull;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class SnapshotDao implements Dao {

  public static boolean isLast(SnapshotDto snapshotTested, @Nullable SnapshotDto previousLastSnapshot) {
    return previousLastSnapshot == null || previousLastSnapshot.getCreatedAt() < snapshotTested.getCreatedAt();
  }

  public Optional<SnapshotDto> selectByUuid(DbSession dbSession, String analysisUuid) {
    List<SnapshotDto> dtos = mapper(dbSession).selectByUuids(Collections.singletonList(analysisUuid));
    if (dtos.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(dtos.iterator().next());
  }

  public List<SnapshotDto> selectByUuids(DbSession dbSession, Collection<String> analysisUuids) {
    return executeLargeInputs(analysisUuids, mapper(dbSession)::selectByUuids);
  }

  public Optional<SnapshotDto> selectLastAnalysisByComponentUuid(DbSession session, String componentUuid) {
    return Optional.ofNullable(mapper(session).selectLastSnapshotByComponentUuid(componentUuid));
  }

  public Optional<SnapshotDto> selectLastAnalysisByRootComponentUuid(DbSession session, String componentUuid) {
    return Optional.ofNullable(mapper(session).selectLastSnapshotByRootComponentUuid(componentUuid));
  }

  public List<SnapshotDto> selectLastAnalysesByRootComponentUuids(DbSession dbSession, Collection<String> componentUuids) {
    return executeLargeInputs(componentUuids, mapper(dbSession)::selectLastSnapshotsByRootComponentUuids);
  }

  public List<SnapshotDto> selectAnalysesByQuery(DbSession session, SnapshotQuery query) {
    return mapper(session).selectSnapshotsByQuery(query);
  }

  @CheckForNull
  public SnapshotDto selectAnalysisByQuery(DbSession session, SnapshotQuery query) {
    List<SnapshotDto> dtos = mapper(session).selectSnapshotsByQuery(query);
    if (dtos.isEmpty()) {
      return null;
    }
    checkState(dtos.size() == 1, "Expected one analysis to be returned, got %s", dtos.size());
    return dtos.get(0);
  }

  /**
   * Since this relies on tables EVENTS, this can return results only for root components (PROJECT, VIEW or DEVELOPER).
   */
  public List<SnapshotDto> selectPreviousVersionSnapshots(DbSession session, String componentUuid, String lastVersion) {
    return mapper(session).selectPreviousVersionSnapshots(componentUuid, lastVersion);
  }

  @CheckForNull
  public SnapshotDto selectOldestSnapshot(DbSession session, String componentUuid) {
    List<SnapshotDto> snapshotDtos = mapper(session).selectOldestSnapshots(componentUuid, new RowBounds(0, 1));
    return snapshotDtos.isEmpty() ? null : snapshotDtos.get(0);
  }

  /**
   * Returned finished analysis from a list of projects and dates.
   * "Finished" analysis means that the status in the CE_ACTIVITY table is SUCCESS => the goal is to be sure that the CE task is completely finished.
   *
   * Note that branches analysis of projects are also returned.
   */
  public List<SnapshotDto> selectFinishedByComponentUuidsAndFromDates(DbSession dbSession, List<String> componentUuids, List<Long> fromDates) {
    checkArgument(componentUuids.size() == fromDates.size(), "The number of components (%s) and from dates (%s) must be the same.",
      String.valueOf(componentUuids.size()),
      String.valueOf(fromDates.size()));
    List<ComponentUuidFromDatePair> componentUuidFromDatePairs = IntStream.range(0, componentUuids.size())
      .mapToObj(i -> new ComponentUuidFromDatePair(componentUuids.get(i), fromDates.get(i)))
      .collect(MoreCollectors.toList(componentUuids.size()));

    return executeLargeInputs(componentUuidFromDatePairs, partition -> mapper(dbSession).selectFinishedByComponentUuidsAndFromDates(partition, Status.SUCCESS), i -> i / 2);
  }

  public void switchIsLastFlagAndSetProcessedStatus(DbSession dbSession, String componentUuid, String analysisUuid) {
    SnapshotMapper mapper = mapper(dbSession);
    mapper.unsetIsLastFlagForComponentUuid(componentUuid);
    mapper(dbSession).setIsLastFlagForAnalysisUuid(analysisUuid);
  }

  public SnapshotDto insert(DbSession session, SnapshotDto item) {
    mapper(session).insert(item);
    return item;
  }

  public void insert(DbSession session, Collection<SnapshotDto> items) {
    for (SnapshotDto item : items) {
      insert(session, item);
    }
  }

  public void insert(DbSession session, SnapshotDto item, SnapshotDto... others) {
    insert(session, Lists.asList(item, others));
  }

  public void update(DbSession dbSession, SnapshotDto analysis) {
    mapper(dbSession).update(analysis);
  }

  /**
   * Used by Governance
   */
  @CheckForNull
  public ViewsSnapshotDto selectSnapshotBefore(String componentUuid, long date, DbSession dbSession) {
    return from(mapper(dbSession).selectSnapshotBefore(componentUuid, date))
      .first()
      .orNull();
  }

  private static SnapshotMapper mapper(DbSession session) {
    return session.getMapper(SnapshotMapper.class);
  }

  static class ComponentUuidFromDatePair implements Comparable<ComponentUuidFromDatePair> {
    private final String componentUuid;
    private final long from;

    ComponentUuidFromDatePair(String componentUuid, long from) {
      this.componentUuid = requireNonNull(componentUuid);
      this.from = from;
    }

    @Override
    public int compareTo(ComponentUuidFromDatePair other) {
      if (this == other) {
        return 0;
      }

      int c = componentUuid.compareTo(other.componentUuid);
      if (c == 0) {
        c = Long.compare(from, other.from);
      }

      return c;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      ComponentUuidFromDatePair other = (ComponentUuidFromDatePair) o;
      return componentUuid.equals(other.componentUuid)
        && from == other.from;
    }

    @Override
    public int hashCode() {
      return Objects.hash(componentUuid, from);
    }
  }
}
