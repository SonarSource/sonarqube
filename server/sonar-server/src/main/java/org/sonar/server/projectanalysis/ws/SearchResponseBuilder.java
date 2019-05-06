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
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
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

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.core.util.stream.MoreCollectors.index;
import static org.sonar.server.projectanalysis.ws.EventCategory.fromLabel;

class SearchResponseBuilder {
  private static final Logger LOGGER = Loggers.get(SearchResponseBuilder.class);

  private final Analysis.Builder wsAnalysis;
  private final Event.Builder wsEvent;
  private final SearchData searchData;
  private final QualityGate.Builder wsQualityGate;
  private final ProjectAnalyses.DefinitionChange.Builder wsDefinitionChange;

  SearchResponseBuilder(SearchData searchData) {
    this.wsAnalysis = Analysis.newBuilder();
    this.wsEvent = Event.newBuilder();
    this.wsQualityGate = QualityGate.newBuilder();
    this.wsDefinitionChange = ProjectAnalyses.DefinitionChange.newBuilder();
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
    Analysis.Builder builder = wsAnalysis.clear();
    builder
      .setKey(dbAnalysis.getUuid())
      .setDate(formatDateTime(dbAnalysis.getCreatedAt()))
      .setManualNewCodePeriodBaseline(searchData.getManualBaseline().filter(dbAnalysis.getUuid()::equals).isPresent());
    ofNullable(dbAnalysis.getProjectVersion()).ifPresent(builder::setProjectVersion);
    ofNullable(dbAnalysis.getBuildString()).ifPresent(builder::setBuildString);
    ofNullable(dbAnalysis.getRevision()).ifPresent(builder::setRevision);

    return builder;
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
    ofNullable(dbEvent.getName()).ifPresent(wsEvent::setName);
    ofNullable(dbEvent.getDescription()).ifPresent(wsEvent::setDescription);
    ofNullable(dbEvent.getCategory()).ifPresent(cat -> wsEvent.setCategory(fromLabel(cat).name()));
    if (dbEvent.getCategory() != null) {
      switch (EventCategory.fromLabel(dbEvent.getCategory())) {
        case DEFINITION_CHANGE:
          addDefinitionChange(dbEvent);
          break;
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
    wsQualityGate.clear();
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
      .collect(toList()));
    wsEvent.setQualityGate(wsQualityGate.build());
  }

  private void addDefinitionChange(EventDto event) {
    wsDefinitionChange.clear();
    List<EventComponentChangeDto> eventComponentChangeDtos = searchData.componentChangesByEventUuid.get(event.getUuid());
    if (eventComponentChangeDtos.isEmpty()) {
      return;
    }

    ListMultimap<String, EventComponentChangeDto> componentChangeByKey = eventComponentChangeDtos.stream()
      .collect(index(EventComponentChangeDto::getComponentKey));

    try {
      wsDefinitionChange.addAllProjects(
        componentChangeByKey.asMap().values().stream()
          .map(SearchResponseBuilder::addChange)
          .map(Project::toProject)
          .collect(toList())
      );
      wsEvent.setDefinitionChange(wsDefinitionChange.build());
    } catch (IllegalStateException e) {
      LOGGER.error(e.getMessage(), e);
    }
  }

  private static Project addChange(Collection<EventComponentChangeDto> changes) {
    if (changes.size() == 1) {
      return addSingleChange(changes.iterator().next());
    } else {
      return addBranchChange(changes);
    }
  }

  private static Project addSingleChange(EventComponentChangeDto componentChange) {
    Project project = new Project()
      .setKey(componentChange.getComponentKey())
      .setName(componentChange.getComponentName())
      .setBranch(componentChange.getComponentBranchKey());

    switch (componentChange.getCategory()) {
      case ADDED:
        project.setChangeType("ADDED");
        break;
      case REMOVED:
        project.setChangeType("REMOVED");
        break;
      default:
        throw new IllegalStateException(format("Unknown change %s for eventComponentChange uuid: %s", componentChange.getCategory(), componentChange.getUuid()));
    }

    return project;
  }

  private static Project addBranchChange(Collection<EventComponentChangeDto> changes) {
    if (changes.size() != 2) {
      throw new IllegalStateException(format("Too many changes on same project (%d) for eventComponentChange uuids : %s",
        changes.size(),
        changes.stream().map(EventComponentChangeDto::getUuid).collect(Collectors.joining(","))));
    }

    Optional<EventComponentChangeDto> addedChange = changes.stream().filter(c -> c.getCategory().equals(EventComponentChangeDto.ChangeCategory.ADDED)).findFirst();
    Optional<EventComponentChangeDto> removedChange = changes.stream().filter(c -> c.getCategory().equals(EventComponentChangeDto.ChangeCategory.REMOVED)).findFirst();

    if (!addedChange.isPresent() || !removedChange.isPresent() || addedChange.equals(removedChange)) {
      Iterator<EventComponentChangeDto> iterator = changes.iterator();
      // We are missing two different ADDED and REMOVED changes
      EventComponentChangeDto firstChange = iterator.next();
      EventComponentChangeDto secondChange = iterator.next();
      throw new IllegalStateException(format("Incorrect changes : [uuid=%s change=%s, branch=%s] and [uuid=%s, change=%s, branch=%s]",
        firstChange.getUuid(), firstChange.getCategory().name(), firstChange.getComponentBranchKey(),
        secondChange.getUuid(), secondChange.getCategory().name(), secondChange.getComponentBranchKey()));
    }

    return new Project()
      .setName(addedChange.get().getComponentName())
      .setKey(addedChange.get().getComponentKey())
      .setChangeType("BRANCH_CHANGED")
      .setNewBranch(addedChange.get().getComponentBranchKey())
      .setOldBranch(removedChange.get().getComponentBranchKey());
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

  private static class Project {
    private String key;
    private String name;
    private String changeType;
    private String branch;
    private String oldBranch;
    private String newBranch;

    public Project setKey(String key) {
      this.key = key;
      return this;
    }

    public Project setName(String name) {
      this.name = name;
      return this;
    }

    public Project setChangeType(String changeType) {
      this.changeType = changeType;
      return this;
    }

    public Project setBranch(@Nullable String branch) {
      this.branch = branch;
      return this;
    }

    public Project setOldBranch(@Nullable String oldBranch) {
      this.oldBranch = oldBranch;
      return this;
    }

    public Project setNewBranch(@Nullable String newBranch) {
      this.newBranch = newBranch;
      return this;
    }

    private ProjectAnalyses.Project toProject() {
      ProjectAnalyses.Project.Builder builder = ProjectAnalyses.Project.newBuilder();
      builder
        .setKey(key)
        .setName(name)
        .setChangeType(changeType);
      ofNullable(branch).ifPresent(builder::setBranch);
      ofNullable(oldBranch).ifPresent(builder::setOldBranch);
      ofNullable(newBranch).ifPresent(builder::setNewBranch);
      return builder.build();
    }
  }
}
