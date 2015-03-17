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

import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.core.event.EventDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.db.DbClient;

import java.util.List;

public class PersistEventsStep implements ComputationStep {

  private final DbClient dbClient;
  private final System2 system2;

  public PersistEventsStep(DbClient dbClient, System2 system2) {
    this.dbClient = dbClient;
    this.system2 = system2;
  }

  @Override
  public String[] supportedProjectQualifiers() {
    return new String[] {Qualifiers.PROJECT};
  }

  @Override
  public void execute(ComputationContext context) {
    DbSession session = dbClient.openSession(false);
    try {
      int rootComponentRef = context.getReportMetadata().getRootComponentRef();
      recursivelyProcessComponent(session, context, rootComponentRef);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void recursivelyProcessComponent(DbSession session, ComputationContext context, int componentRef) {
    BatchReportReader reportReader = context.getReportReader();
    BatchReport.Component component = reportReader.readComponent(componentRef);
    processEvents(session, component, context.getReportMetadata().getAnalysisDate());
    saveVersionEvent(session, component, context.getReportMetadata().getAnalysisDate());

    for (Integer childRef : component.getChildRefList()) {
      recursivelyProcessComponent(session, context, childRef);
    }
  }

  private void processEvents(DbSession session, BatchReport.Component component, Long analysisDate) {
    List<BatchReport.Event> events = component.getEventList();
    if (!events.isEmpty()) {
      for (BatchReport.Event event : component.getEventList()) {
        dbClient.eventDao().insert(session, newBaseEvent(component, analysisDate)
          .setName(event.getName())
          .setCategory(convertCategory(event.getCategory()))
          .setDescription(event.hasDescription() ? event.getDescription() : null)
          .setData(event.hasEventData() ? event.getEventData() : null)
          );
      }
    }
  }

  private void saveVersionEvent(DbSession session, BatchReport.Component component, Long analysisDate) {
    if (component.hasVersion()) {
      deletePreviousEventsHavingSameVersion(session, component);
      dbClient.eventDao().insert(session, newBaseEvent(component, analysisDate)
        .setName(component.getVersion())
        .setCategory(EventDto.CATEGORY_VERSION)
        );
    }
  }

  private void deletePreviousEventsHavingSameVersion(DbSession session, BatchReport.Component component) {
    for (EventDto dto : dbClient.eventDao().selectByComponentUuid(session, component.getUuid())) {
      if (dto.getCategory().equals(EventDto.CATEGORY_VERSION) && dto.getName().equals(component.getVersion())) {
        dbClient.eventDao().delete(session, dto.getId());
      }
    }
  }

  private EventDto newBaseEvent(BatchReport.Component component, Long analysisDate) {
    return new EventDto()
      .setComponentUuid(component.getUuid())
      .setSnapshotId(component.getSnapshotId())
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
