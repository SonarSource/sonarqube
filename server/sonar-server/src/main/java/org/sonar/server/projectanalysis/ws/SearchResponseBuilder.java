/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.event.EventDto;
import org.sonarqube.ws.ProjectAnalyses.Analysis;
import org.sonarqube.ws.ProjectAnalyses.Event;
import org.sonarqube.ws.ProjectAnalyses.SearchResponse;

import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonarqube.ws.client.projectanalysis.EventCategory.fromLabel;

class SearchResponseBuilder {
  private final Analysis.Builder analysisBuilder;
  private final Event.Builder eventBuilder;

  SearchResponseBuilder() {
    this.analysisBuilder = Analysis.newBuilder();
    this.eventBuilder = Event.newBuilder();
  }

  Function<SearchResults, SearchResponse> buildWsResponse() {
    return searchResults -> Stream.of(SearchResponse.newBuilder())
      .peek(addAnalyses(searchResults))
      .peek(addPagination(searchResults))
      .map(SearchResponse.Builder::build)
      .collect(MoreCollectors.toOneElement());
  }

  private Consumer<SearchResponse.Builder> addAnalyses(SearchResults searchResults) {
    return response -> searchResults.analyses.stream()
      .map(dbToWsAnalysis())
      .peek(addEvents(searchResults))
      .forEach(response::addAnalyses);
  }

  private Function<SnapshotDto, Analysis.Builder> dbToWsAnalysis() {
    return dbAnalysis -> analysisBuilder.clear()
      .setKey(dbAnalysis.getUuid())
      .setDate(formatDateTime(dbAnalysis.getCreatedAt()));
  }

  private Consumer<Analysis.Builder> addEvents(SearchResults searchResults) {
    return wsAnalysis -> searchResults.eventsByAnalysis.get(wsAnalysis.getKey()).stream()
      .map(dbToWsEvent())
      .forEach(wsAnalysis::addEvents);
  }

  private Function<EventDto, Event.Builder> dbToWsEvent() {
    return dbEvent -> {
      Event.Builder wsEvent = eventBuilder.clear()
        .setKey(dbEvent.getUuid());
      setNullable(dbEvent.getName(), wsEvent::setName);
      setNullable(dbEvent.getDescription(), wsEvent::setDescription);
      setNullable(dbEvent.getCategory(), cat -> wsEvent.setCategory(fromLabel(cat).name()));
      return wsEvent;
    };
  }

  private static Consumer<SearchResponse.Builder> addPagination(SearchResults searchResults) {
    return response -> response.getPagingBuilder()
      .setPageIndex(searchResults.paging.pageIndex())
      .setPageSize(searchResults.paging.pageSize())
      .setTotal(searchResults.paging.total())
      .build();
  }

}
