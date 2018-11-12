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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.event.EventComponentChangeDto;
import org.sonar.db.event.EventDto;
import org.sonarqube.ws.ProjectAnalyses;
import org.sonarqube.ws.ProjectAnalyses.Analysis;
import org.sonarqube.ws.ProjectAnalyses.Event;
import org.sonarqube.ws.ProjectAnalyses.QualityGate;
import org.sonarqube.ws.ProjectAnalyses.SearchResponse;

import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.server.projectanalysis.ws.EventCategory.fromLabel;

class SearchResponseBuilder {
  private static final Logger LOGGER = Loggers.get(SearchResponseBuilder.class);

  private final Analysis.Builder wsAnalysis;
  private final Event.Builder wsEvent;
  private final SearchData searchData;
  private final QualityGate.Builder wsQualityGate;

  SearchResponseBuilder(SearchData searchData) {
    this.wsAnalysis = Analysis.newBuilder();
    this.wsEvent = Event.newBuilder();
    this.wsQualityGate = QualityGate.newBuilder();
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
    if (dbEvent.getCategory() != null) {
      switch (EventCategory.fromLabel(dbEvent.getCategory())) {
        case QUALITY_GATE:
          addQualityGateInformation(dbEvent);
          break;
        case VERSION:
        case OTHER:
        case QUALITY_PROFILE:
        default:
          break;
      }
    }
    return wsEvent;
  }

  private void addQualityGateInformation(EventDto event) {
    List<EventComponentChangeDto> eventComponentChangeDtos = searchData.componentChangesByEventUuid.get(event.getUuid());
    if (eventComponentChangeDtos.isEmpty()) {
      return;
    }

    if (event.getData() != null) {
      try {
        Gson gson = new Gson();
        Data data = gson.fromJson(event.getData(), Data.class);

        wsQualityGate.setStillFailing(data.isStillFailing());
        wsQualityGate.setStatus(data.getStatus());
      } catch (JsonSyntaxException ex) {
        LOGGER.error("Unable to retrieve data from event uuid=" + event.getUuid(), ex);
        return;
      }
    }

    wsQualityGate.addAllFailing(eventComponentChangeDtos.stream()
      .map(SearchResponseBuilder::toFailing)
      .collect(Collectors.toList()));
    wsEvent.setQualityGate(wsQualityGate.build());
  }

  private void addPagination(SearchResponse.Builder wsResponse) {
    wsResponse.getPagingBuilder()
      .setPageIndex(searchData.paging.pageIndex())
      .setPageSize(searchData.paging.pageSize())
      .setTotal(searchData.paging.total())
      .build();
  }

  private static ProjectAnalyses.Failing toFailing(EventComponentChangeDto change) {
    ProjectAnalyses.Failing.Builder builder = ProjectAnalyses.Failing.newBuilder()
      .setKey(change.getComponentKey())
      .setName(change.getComponentName());
    if (change.getComponentBranchKey() != null) {
      builder.setBranch(change.getComponentBranchKey());
    }
    return builder.build();
  }

  private static class Data {
    private boolean stillFailing;
    private String status;

    public Data() {
      // Empty constructor because it's used by GSon
    }

    boolean isStillFailing() {
      return stillFailing;
    }

    public Data setStillFailing(boolean stillFailing) {
      this.stillFailing = stillFailing;
      return this;
    }

    String getStatus() {
      return status;
    }

    public Data setStatus(String status) {
      this.status = status;
      return this;
    }
  }
}
