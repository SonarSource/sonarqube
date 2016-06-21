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
package org.sonar.server.computation.step;

import com.google.common.base.Function;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.event.EventDto;
import org.sonar.server.computation.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ComponentVisitor;
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.component.DbIdsRepository;
import org.sonar.server.computation.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.event.Event;
import org.sonar.server.computation.event.EventRepository;

import static com.google.common.collect.Iterables.transform;

public class PersistEventsStep implements ComputationStep {

  private final DbClient dbClient;
  private final System2 system2;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final TreeRootHolder treeRootHolder;
  private final EventRepository eventRepository;
  private final DbIdsRepository dbIdsRepository;

  public PersistEventsStep(DbClient dbClient, System2 system2, TreeRootHolder treeRootHolder, AnalysisMetadataHolder analysisMetadataHolder,
    EventRepository eventRepository, DbIdsRepository dbIdsRepository) {
    this.dbClient = dbClient;
    this.system2 = system2;
    this.treeRootHolder = treeRootHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.eventRepository = eventRepository;
    this.dbIdsRepository = dbIdsRepository;
  }

  @Override
  public void execute() {
    final DbSession session = dbClient.openSession(false);
    try {
      long analysisDate = analysisMetadataHolder.getAnalysisDate();
      new DepthTraversalTypeAwareCrawler(new PersistEventComponentVisitor(session, analysisDate))
        .visit(treeRootHolder.getRoot());
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void processEvents(DbSession session, Component component, Long analysisDate) {
    Function<Event, EventDto> eventToEventDto = event -> newBaseEvent(component, analysisDate)
      .setName(event.getName())
      .setCategory(convertCategory(event.getCategory()))
      .setDescription(event.getDescription())
      .setData(event.getData());
    // FIXME bulk insert
    for (EventDto batchEventDto : transform(eventRepository.getEvents(component), eventToEventDto)) {
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
      if (dto.getCategory().equals(EventDto.CATEGORY_VERSION) && dto.getName().equals(version)) {
        dbClient.eventDao().delete(session, dto.getId());
      }
    }
  }

  private EventDto newBaseEvent(Component component, Long analysisDate) {
    return new EventDto()
      .setComponentUuid(component.getUuid())
      .setSnapshotId(dbIdsRepository.getSnapshotId(component))
      .setCreatedAt(system2.now())
      .setDate(analysisDate);
  }

  private static String convertCategory(Event.Category category) {
    switch (category) {
      case ALERT:
        return EventDto.CATEGORY_ALERT;
      case PROFILE:
        return EventDto.CATEGORY_PROFILE;
      default:
        throw new IllegalArgumentException(String.format("Unsupported category %s", category.name()));
    }
  }

  @Override
  public String getDescription() {
    return "Persist events";
  }

  private class PersistEventComponentVisitor extends TypeAwareVisitorAdapter {
    private final DbSession session;
    private final long analysisDate;

    public PersistEventComponentVisitor(DbSession session, long analysisDate) {
      super(CrawlerDepthLimit.PROJECT, ComponentVisitor.Order.PRE_ORDER);
      this.session = session;
      this.analysisDate = analysisDate;
    }

    @Override
    public void visitProject(Component project) {
      processEvents(session, project, analysisDate);
      saveVersionEvent(session, project, analysisDate);
    }

  }
}
