/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.sonar.api.utils.Paging;
import org.sonar.core.util.stream.Collectors;
import org.sonar.db.DbSession;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.event.EventDto;
import org.sonarqube.ws.client.projectanalysis.SearchRequest;

import static java.util.Objects.requireNonNull;

class SearchResults {
  final List<SnapshotDto> analyses;
  final ListMultimap<String, EventDto> eventsByAnalysis;
  final Paging paging;

  private SearchResults(Builder builder) {
    this.analyses = builder.analyses;
    this.eventsByAnalysis = buildEvents(builder.events);
    this.paging = Paging
      .forPageIndex(builder.getRequest().getPage())
      .withPageSize(builder.getRequest().getPageSize())
      .andTotal(builder.countAnalyses);
  }

  private ListMultimap<String, EventDto> buildEvents(List<EventDto> events) {
    return events.stream().collect(Collectors.index(EventDto::getAnalysisUuid));
  }

  static Builder builder(DbSession dbSession, SearchRequest request) {
    return new Builder(dbSession, request);
  }

  static class Builder {
    private final DbSession dbSession;
    private final SearchRequest request;
    private String projectUuid;
    private List<SnapshotDto> analyses;
    private int countAnalyses;
    private List<EventDto> events;

    private Builder(DbSession dbSession, SearchRequest request) {
      this.dbSession = dbSession;
      this.request = request;
    }

    Builder setProjectUuid(String projectUuid) {
      this.projectUuid = projectUuid;
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

      this.analyses = stream.collect(Collectors.toList());
      this.countAnalyses = analyses.size();
      return this;
    }

    Builder setEvents(List<EventDto> events) {
      this.events = events;
      return this;
    }

    DbSession getDbSession() {
      return dbSession;
    }

    SearchRequest getRequest() {
      return request;
    }

    String getProjectUuid() {
      return projectUuid;
    }

    List<SnapshotDto> getAnalyses() {
      return analyses;
    }

    private void filterByCategory() {
      ListMultimap<String, String> eventCategoriesByAnalysisUuid = events.stream()
        .collect(Collectors.index(EventDto::getAnalysisUuid, EventDto::getCategory));
      Predicate<SnapshotDto> byCategory = a -> eventCategoriesByAnalysisUuid.get(a.getUuid()).contains(request.getCategory().getLabel());
      this.countAnalyses = (int) analyses.stream().filter(byCategory).count();
      this.analyses = analyses.stream()
        .filter(byCategory)
        .skip(Paging.offset(request.getPage(), request.getPageSize()))
        .limit(request.getPageSize())
        .collect(Collectors.toList());
    }

    SearchResults build() {
      requireNonNull(analyses);
      requireNonNull(events);
      if (request.getCategory() != null) {
        filterByCategory();
      }
      return new SearchResults(this);
    }
  }
}
