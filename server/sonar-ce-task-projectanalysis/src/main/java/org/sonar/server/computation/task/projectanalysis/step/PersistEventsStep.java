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
package org.sonar.server.computation.task.projectanalysis.step;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.event.EventDto;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.server.computation.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;
import org.sonar.server.computation.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.task.projectanalysis.event.Event;
import org.sonar.server.computation.task.projectanalysis.event.EventRepository;
import org.sonar.server.computation.task.step.ComputationStep;

public class PersistEventsStep implements ComputationStep {

  private final DbClient dbClient;
  private final System2 system2;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final TreeRootHolder treeRootHolder;
  private final EventRepository eventRepository;
  private final UuidFactory uuidFactory;

  public PersistEventsStep(DbClient dbClient, System2 system2, TreeRootHolder treeRootHolder, AnalysisMetadataHolder analysisMetadataHolder,
    EventRepository eventRepository, UuidFactory uuidFactory) {
    this.dbClient = dbClient;
    this.system2 = system2;
    this.treeRootHolder = treeRootHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.eventRepository = eventRepository;
    this.uuidFactory = uuidFactory;
  }

  @Override
  public void execute() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      long analysisDate = analysisMetadataHolder.getAnalysisDate();
      new DepthTraversalTypeAwareCrawler(new PersistEventComponentVisitor(dbSession, analysisDate))
        .visit(treeRootHolder.getRoot());
      dbSession.commit();
    }
  }

  @Override
  public String getDescription() {
    return "Persist events";
  }

  private class PersistEventComponentVisitor extends TypeAwareVisitorAdapter {
    private final DbSession session;
    private final long analysisDate;

    PersistEventComponentVisitor(DbSession session, long analysisDate) {
      super(CrawlerDepthLimit.PROJECT, Order.PRE_ORDER);
      this.session = session;
      this.analysisDate = analysisDate;
    }

    @Override
    public void visitProject(Component project) {
      processEvents(session, project, analysisDate);
      saveVersionEvent(session, project, analysisDate);
    }

    private void processEvents(DbSession session, Component component, Long analysisDate) {
      Function<Event, EventDto> eventToEventDto = event -> newBaseEvent(component, analysisDate)
        .setName(event.getName())
        .setCategory(convertCategory(event.getCategory()))
        .setDescription(event.getDescription())
        .setData(event.getData());
      // FIXME bulk insert
      for (EventDto batchEventDto : StreamSupport.stream(eventRepository.getEvents(component).spliterator(), false).map(eventToEventDto).collect(Collectors.toList())) {
        dbClient.eventDao().insert(session, batchEventDto);
      }
    }

    private void saveVersionEvent(DbSession session, Component component, Long analysisDate) {
      String version = component.getReportAttributes().getVersion();
      if (version != null) {
        deletePreviousEventsHavingSameVersion(session, version, component);
        dbClient.eventDao().insert(session, newBaseEvent(component, analysisDate)
          .setName(version)
          .setCategory(EventDto.CATEGORY_VERSION));
      }
    }

    private void deletePreviousEventsHavingSameVersion(DbSession session, String version, Component component) {
      for (EventDto dto : dbClient.eventDao().selectByComponentUuid(session, component.getUuid())) {
        if (Objects.equals(dto.getCategory(), EventDto.CATEGORY_VERSION) && Objects.equals(dto.getName(), version)) {
          dbClient.eventDao().delete(session, dto.getId());
        }
      }
    }

    private EventDto newBaseEvent(Component component, Long analysisDate) {
      return new EventDto()
        .setUuid(uuidFactory.create())
        .setAnalysisUuid(analysisMetadataHolder.getUuid())
        .setComponentUuid(component.getUuid())
        .setCreatedAt(system2.now())
        .setDate(analysisDate);
    }

    private String convertCategory(Event.Category category) {
      switch (category) {
        case ALERT:
          return EventDto.CATEGORY_ALERT;
        case PROFILE:
          return EventDto.CATEGORY_PROFILE;
        default:
          throw new IllegalArgumentException(String.format("Unsupported category %s", category.name()));
      }
    }

  }
}
