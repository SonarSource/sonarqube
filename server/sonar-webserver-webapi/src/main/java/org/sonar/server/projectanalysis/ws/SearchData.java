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
package org.sonar.server.projectanalysis.ws;

import com.google.common.collect.ListMultimap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.utils.Paging;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.event.EventComponentChangeDto;
import org.sonar.db.event.EventDto;

import static java.util.Objects.requireNonNull;

class SearchData {
  final List<SnapshotDto> analyses;
  final ListMultimap<String, EventDto> eventsByAnalysis;
  final ListMultimap<String, EventComponentChangeDto> componentChangesByEventUuid;
  final Paging paging;
  @CheckForNull
  private final String manualBaseline;

  private SearchData(Builder builder) {
    this.analyses = builder.analyses;
    this.eventsByAnalysis = buildEvents(builder.events);
    this.componentChangesByEventUuid = buildComponentChanges(builder.componentChanges);
    this.paging = Paging
      .forPageIndex(builder.getRequest().getPage())
      .withPageSize(builder.getRequest().getPageSize())
      .andTotal(builder.countAnalyses);
    this.manualBaseline = builder.manualBaseline;
  }

  private static ListMultimap<String, EventDto> buildEvents(List<EventDto> events) {
    return events.stream().collect(MoreCollectors.index(EventDto::getAnalysisUuid));
  }

  private static ListMultimap<String, EventComponentChangeDto> buildComponentChanges(List<EventComponentChangeDto> changes) {
    return changes.stream().collect(MoreCollectors.index(EventComponentChangeDto::getEventUuid));
  }

  static Builder builder(DbSession dbSession, SearchRequest request) {
    return new Builder(dbSession, request);
  }

  public Optional<String> getManualBaseline() {
    return Optional.ofNullable(manualBaseline);
  }

  static class Builder {
    private final DbSession dbSession;
    private final SearchRequest request;
    private ComponentDto project;
    private List<SnapshotDto> analyses;
    private int countAnalyses;
    private String manualBaseline;
    private List<EventDto> events;
    private List<EventComponentChangeDto> componentChanges;

    private Builder(DbSession dbSession, SearchRequest request) {
      this.dbSession = dbSession;
      this.request = request;
    }

    Builder setProject(ComponentDto project) {
      this.project = project;
      return this;
    }

    Builder setAnalyses(List<SnapshotDto> analyses) {
      Stream<SnapshotDto> stream = analyses.stream();
      // no filter by category, the pagination can be applied
      if (request.getCategory() == null) {
        stream = stream
          .skip(Paging.offset(request.getPage(), request.getPageSize()))
          .limit(request.getPageSize());
      }

      this.analyses = stream.collect(MoreCollectors.toList());
      this.countAnalyses = analyses.size();
      return this;
    }

    Builder setEvents(List<EventDto> events) {
      this.events = events;
      return this;
    }

    public List<EventComponentChangeDto> getComponentChanges() {
      return componentChanges;
    }

    Builder setComponentChanges(List<EventComponentChangeDto> componentChanges) {
      this.componentChanges = componentChanges;
      return this;
    }

    DbSession getDbSession() {
      return dbSession;
    }

    SearchRequest getRequest() {
      return request;
    }

    ComponentDto getProject() {
      return project;
    }

    List<SnapshotDto> getAnalyses() {
      return analyses;
    }

    public Builder setManualBaseline(@Nullable String manualBaseline) {
      this.manualBaseline = manualBaseline;
      return this;
    }

    private void filterByCategory() {
      ListMultimap<String, String> eventCategoriesByAnalysisUuid = events.stream()
        .collect(MoreCollectors.index(EventDto::getAnalysisUuid, EventDto::getCategory));
      Predicate<SnapshotDto> byCategory = a -> eventCategoriesByAnalysisUuid.get(a.getUuid()).contains(request.getCategory().getLabel());
      this.countAnalyses = (int) analyses.stream().filter(byCategory).count();
      this.analyses = analyses.stream()
        .filter(byCategory)
        .skip(Paging.offset(request.getPage(), request.getPageSize()))
        .limit(request.getPageSize())
        .collect(MoreCollectors.toList());
    }

    SearchData build() {
      requireNonNull(analyses);
      requireNonNull(events);
      if (request.getCategory() != null) {
        filterByCategory();
      }
      return new SearchData(this);
    }
  }
}
