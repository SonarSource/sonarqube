/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nonnull;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.event.EventDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DepthTraversalTypeAwareVisitor;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.db.DbClient;

import static com.google.common.collect.Iterables.transform;

public class PersistEventsStep implements ComputationStep {

  private final DbClient dbClient;
  private final System2 system2;
  private final BatchReportReader reportReader;
  private final TreeRootHolder treeRootHolder;

  public PersistEventsStep(DbClient dbClient, System2 system2, TreeRootHolder treeRootHolder, BatchReportReader reportReader) {
    this.dbClient = dbClient;
    this.system2 = system2;
    this.treeRootHolder = treeRootHolder;
    this.reportReader = reportReader;
  }

  @Override
  public void execute() {
    final DbSession session = dbClient.openSession(false);
    try {
      final long analysisDate = reportReader.readMetadata().getAnalysisDate();
      new DepthTraversalTypeAwareVisitor(Component.Type.FILE, DepthTraversalTypeAwareVisitor.Order.PRE_ORDER) {
        @Override
        public void visitProject(Component project) {
          processComponent(project, session, analysisDate);
        }

        @Override
        public void visitModule(Component module) {
          processComponent(module, session, analysisDate);
        }

        @Override
        public void visitDirectory(Component directory) {
          processComponent(directory, session, analysisDate);
        }

        @Override
        public void visitFile(Component file) {
          processComponent(file, session, analysisDate);
        }
      }.visit(treeRootHolder.getRoot());
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void processComponent(Component component, DbSession session, long analysisDate) {
    BatchReport.Component batchComponent = reportReader.readComponent(component.getRef());
    processEvents(session, batchComponent, component, analysisDate);
    saveVersionEvent(session, batchComponent, component, analysisDate);
  }

  private void processEvents(DbSession session, final BatchReport.Component batchComponent, final Component component, final Long analysisDate) {
    List<BatchReport.Event> events = batchComponent.getEventList();
    if (events.isEmpty()) {
      return;
    }

    List<EventDto> batchEventDtos = Lists.newArrayList(transform(events, new Function<BatchReport.Event, EventDto>() {
      @Override
      public EventDto apply(@Nonnull BatchReport.Event event) {
        return newBaseEvent(batchComponent, component, analysisDate)
            .setName(event.getName())
            .setCategory(convertCategory(event.getCategory()))
            .setDescription(event.hasDescription() ? event.getDescription() : null)
            .setData(event.hasEventData() ? event.getEventData() : null);
      }
    }));

    for (EventDto batchEventDto : batchEventDtos) {
      dbClient.eventDao().insert(session, batchEventDto);
    }
  }

  private void saveVersionEvent(DbSession session, BatchReport.Component batchComponent, Component component, Long analysisDate) {
    if (batchComponent.hasVersion()) {
      deletePreviousEventsHavingSameVersion(session, batchComponent, component);
      dbClient.eventDao().insert(session, newBaseEvent(batchComponent, component, analysisDate)
        .setName(batchComponent.getVersion())
        .setCategory(EventDto.CATEGORY_VERSION)
        );
    }
  }

  private void deletePreviousEventsHavingSameVersion(DbSession session, BatchReport.Component batchComponent, Component component) {
    for (EventDto dto : dbClient.eventDao().selectByComponentUuid(session, component.getUuid())) {
      if (dto.getCategory().equals(EventDto.CATEGORY_VERSION) && dto.getName().equals(batchComponent.getVersion())) {
        dbClient.eventDao().delete(session, dto.getId());
      }
    }
  }

  private EventDto newBaseEvent(BatchReport.Component batchComponent, Component component, Long analysisDate) {
    return new EventDto()
      .setComponentUuid(component.getUuid())
      .setSnapshotId(batchComponent.getSnapshotId())
      .setCreatedAt(system2.now())
      .setDate(analysisDate);
  }

  private static String convertCategory(Constants.EventCategory category) {
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
    return "Persist component links";
  }
}
