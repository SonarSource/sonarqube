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

import java.util.List;
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

import static org.sonar.server.computation.component.DepthTraversalTypeAwareVisitor.Order.PRE_ORDER;

public class PersistEventsStep implements ComputationStep {

  private final DbClient dbClient;
  private final System2 system2;
  private final TreeRootHolder treeRootHolder;
  private final BatchReportReader reportReader;

  public PersistEventsStep(DbClient dbClient, System2 system2, TreeRootHolder treeRootHolder, BatchReportReader reportReader) {
    this.dbClient = dbClient;
    this.system2 = system2;
    this.treeRootHolder = treeRootHolder;
    this.reportReader = reportReader;
  }

  @Override
  public void execute() {
    DbSession session = dbClient.openSession(false);
    try {
      new EventVisitor(session, reportReader.readMetadata().getAnalysisDate()).visit(treeRootHolder.getRoot());
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private class EventVisitor extends DepthTraversalTypeAwareVisitor {

    private final DbSession session;
    private final long analysisDate;

    private EventVisitor(DbSession session, long analysisDate) {
      super(Component.Type.FILE, PRE_ORDER);
      this.session = session;
      this.analysisDate = analysisDate;
    }

    @Override
    public void visitModule(Component module) {
      visitProjectOrModule(module);
    }

    @Override
    public void visitProject(Component project) {
      visitProjectOrModule(project);
    }

    private void visitProjectOrModule(Component component) {
      BatchReport.Component batchComponent = reportReader.readComponent(component.getRef());
      processEvents(batchComponent, component);
      saveVersionEvent(batchComponent, component);
    }

    private void processEvents(BatchReport.Component batchComponent, Component component) {
      List<BatchReport.Event> events = batchComponent.getEventList();
      if (!events.isEmpty()) {
        for (BatchReport.Event event : events) {
          dbClient.eventDao().insert(session, newBaseEvent(component, batchComponent.getSnapshotId())
            .setName(event.getName())
            .setCategory(convertCategory(event.getCategory()))
            .setDescription(event.hasDescription() ? event.getDescription() : null)
            .setData(event.hasEventData() ? event.getEventData() : null)
            );
        }
      }
    }

    private void saveVersionEvent(BatchReport.Component batchComponent, Component component) {
      if (batchComponent.hasVersion()) {
        deletePreviousEventsHavingSameVersion(batchComponent, component);
        dbClient.eventDao().insert(session, newBaseEvent(component, batchComponent.getSnapshotId())
          .setName(batchComponent.getVersion())
          .setCategory(EventDto.CATEGORY_VERSION)
          );
      }
    }

    private void deletePreviousEventsHavingSameVersion(BatchReport.Component batchComponent, Component component) {
      for (EventDto dto : dbClient.eventDao().selectByComponentUuid(session, component.getUuid())) {
        if (dto.getCategory().equals(EventDto.CATEGORY_VERSION) && dto.getName().equals(batchComponent.getVersion())) {
          dbClient.eventDao().delete(session, dto.getId());
        }
      }
    }

    private EventDto newBaseEvent(Component component, long snapshotId) {
      return new EventDto()
        .setComponentUuid(component.getUuid())
        .setSnapshotId(snapshotId)
        .setCreatedAt(system2.now())
        .setDate(analysisDate);
    }

    private String convertCategory(Constants.EventCategory category) {
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

  @Override
  public String getDescription() {
    return "Persist component links";
  }
}
