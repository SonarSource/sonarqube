/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;

import static com.google.common.base.Preconditions.checkArgument;
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

  /**
   * returns the last analysis of any branch of a project
   */
  public Optional<Long> selectLastAnalysisDateByProject(DbSession session, String projectUuid) {
    return Optional.ofNullable(mapper(session).selectLastAnalysisDateByProject(projectUuid));
  }

  /**
   * returns the last analysis of any branch for each existing project
   */
  public List<ProjectLastAnalysisDateDto> selectLastAnalysisDateByProjectUuids(DbSession session, Collection<String> projectUuids) {
    if (projectUuids.isEmpty()) {
      return Collections.emptyList();
    }
    return mapper(session).selectLastAnalysisDateByProjectUuids(projectUuids);
  }

  public Optional<SnapshotDto> selectLastAnalysisByRootComponentUuid(DbSession session, String rootComponentUuid) {
    return Optional.ofNullable(mapper(session).selectLastSnapshotByRootComponentUuid(rootComponentUuid));
  }

  public List<SnapshotDto> selectLastAnalysesByRootComponentUuids(DbSession dbSession, Collection<String> rootComponentUuids) {
    return executeLargeInputs(rootComponentUuids, mapper(dbSession)::selectLastSnapshotsByRootComponentUuids);
  }

  public List<SnapshotDto> selectAnalysesByQuery(DbSession session, SnapshotQuery query) {
    return mapper(session).selectSnapshotsByQuery(query);
  }

  public Optional<SnapshotDto> selectOldestAnalysis(DbSession session, String rootComponentUuid) {
    return mapper(session).selectOldestSnapshots(rootComponentUuid, SnapshotDto.STATUS_PROCESSED, Pagination.first())
      .stream()
      .findFirst();
  }

  /**
   * Returned finished analysis from a list of projects and dates.
   * "Finished" analysis means that the status in the CE_ACTIVITY table is SUCCESS => the goal is to be sure that the CE task is completely finished.
   * Note that branches analysis of projects are also returned.
   */
  public List<SnapshotDto> selectFinishedByProjectUuidsAndFromDates(DbSession dbSession, List<String> projectUuids, List<Long> fromDates) {
    checkArgument(projectUuids.size() == fromDates.size(), "The number of components (%s) and from dates (%s) must be the same.",
      String.valueOf(projectUuids.size()),
      String.valueOf(fromDates.size()));
    List<ProjectUuidFromDatePair> projectUuidFromDatePairs = IntStream.range(0, projectUuids.size())
      .mapToObj(i -> new ProjectUuidFromDatePair(projectUuids.get(i), fromDates.get(i)))
      .toList();

    return executeLargeInputs(projectUuidFromDatePairs, partition -> mapper(dbSession).selectFinishedByProjectUuidsAndFromDates(partition), i -> i / 2);
  }

  public void switchIsLastFlagAndSetProcessedStatus(DbSession dbSession, String rootComponentUuid, String analysisUuid) {
    SnapshotMapper mapper = mapper(dbSession);
    mapper.unsetIsLastFlagForRootComponentUuid(rootComponentUuid);
    mapper(dbSession).setIsLastFlagForAnalysisUuid(analysisUuid);
  }

  public SnapshotDto insert(DbSession session, SnapshotDto item) {
    mapper(session).insert(item);
    return item;
  }

  @VisibleForTesting
  public void insert(DbSession session, Collection<SnapshotDto> items) {
    for (SnapshotDto item : items) {
      insert(session, item);
    }
  }

  @VisibleForTesting
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
  public ViewsSnapshotDto selectSnapshotBefore(String rootComponentUuid, long date, DbSession dbSession) {
    return mapper(dbSession).selectSnapshotBefore(rootComponentUuid, date).stream().findFirst().orElse(null);
  }

  private static SnapshotMapper mapper(DbSession session) {
    return session.getMapper(SnapshotMapper.class);
  }

  static class ProjectUuidFromDatePair implements Comparable<ProjectUuidFromDatePair> {
    private final String projectUuid;
    private final long from;

    ProjectUuidFromDatePair(String projectUuid, long from) {
      this.projectUuid = requireNonNull(projectUuid);
      this.from = from;
    }

    @Override
    public int compareTo(ProjectUuidFromDatePair other) {
      if (this == other) {
        return 0;
      }

      int c = projectUuid.compareTo(other.projectUuid);
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

      ProjectUuidFromDatePair other = (ProjectUuidFromDatePair) o;
      return projectUuid.equals(other.projectUuid) && from == other.from;
    }

    @Override
    public int hashCode() {
      return Objects.hash(projectUuid, from);
    }
  }
}
