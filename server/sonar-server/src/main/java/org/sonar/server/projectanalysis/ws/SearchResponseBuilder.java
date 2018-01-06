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
package org.sonar.server.projectanalysis.ws;

import org.sonar.db.component.SnapshotDto;
import org.sonar.db.event.EventDto;
import org.sonarqube.ws.ProjectAnalyses.Analysis;
import org.sonarqube.ws.ProjectAnalyses.Event;
import org.sonarqube.ws.ProjectAnalyses.SearchResponse;

import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.server.projectanalysis.ws.EventCategory.fromLabel;

class SearchResponseBuilder {
  private final Analysis.Builder wsAnalysis;
  private final Event.Builder wsEvent;
  private final SearchData searchData;

  SearchResponseBuilder(SearchData searchData) {
    this.wsAnalysis = Analysis.newBuilder();
    this.wsEvent = Event.newBuilder();
    this.searchData = searchData;
  }

  SearchResponse build() {
    SearchResponse.Builder wsResponse = SearchResponse.newBuilder();
    addAnalyses(wsResponse);
    addPagination(wsResponse);
    return wsResponse.build();
  }

  private void addAnalyses(SearchResponse.Builder wsResponse) {
    searchData.analyses.stream()
      .map(this::dbToWsAnalysis)
      .map(this::attachEvents)
      .forEach(wsResponse::addAnalyses);
  }

  private Analysis.Builder dbToWsAnalysis(SnapshotDto dbAnalysis) {
    return wsAnalysis.clear()
      .setKey(dbAnalysis.getUuid())
      .setDate(formatDateTime(dbAnalysis.getCreatedAt()));
  }

  private Analysis.Builder attachEvents(Analysis.Builder analysis) {
    searchData.eventsByAnalysis.get(analysis.getKey())
      .stream()
      .map(this::dbToWsEvent)
      .forEach(analysis::addEvents);
    return analysis;

  }

  private Event.Builder dbToWsEvent(EventDto dbEvent) {
    wsEvent.clear().setKey(dbEvent.getUuid());
    setNullable(dbEvent.getName(), wsEvent::setName);
    setNullable(dbEvent.getDescription(), wsEvent::setDescription);
    setNullable(dbEvent.getCategory(), cat -> wsEvent.setCategory(fromLabel(cat).name()));

    return wsEvent;
  }

  private void addPagination(SearchResponse.Builder wsResponse) {
    wsResponse.getPagingBuilder()
      .setPageIndex(searchData.paging.pageIndex())
      .setPageSize(searchData.paging.pageSize())
      .setTotal(searchData.paging.total())
      .build();
  }

}
