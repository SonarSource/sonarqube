/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.project;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.event.EventDto;
import org.sonar.server.qualitygate.EvaluatedQualityGate;
import org.sonar.server.qualitygate.changeevent.QGChangeEvent;
import org.sonar.server.qualitygate.changeevent.QGChangeEventListener;

import static org.sonar.db.event.EventDto.CATEGORY_ALERT;

public class ProjectQGChangeEventListener implements QGChangeEventListener {

  private final DbClient dbClient;

  public ProjectQGChangeEventListener(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void onIssueChanges(QGChangeEvent qualityGateEvent, Set<ChangedIssue> changedIssues) {
    Optional<EvaluatedQualityGate> evaluatedQualityGate = qualityGateEvent.getQualityGateSupplier().get();
    Optional<Metric.Level> previousStatusOptional = qualityGateEvent.getPreviousStatus();
    if (evaluatedQualityGate.isEmpty() || previousStatusOptional.isEmpty()) {
      return;
    }
    Metric.Level currentStatus = evaluatedQualityGate.get().getStatus();
    Metric.Level previousStatus = previousStatusOptional.get();
    if (previousStatus.getColorName().equals(currentStatus.getColorName())) {
      // QG status didn't change - no action
      return;
    }

    addQualityGateEventToProject(qualityGateEvent, currentStatus);
  }

  private void addQualityGateEventToProject(QGChangeEvent qualityGateEvent, Metric.Level currentStatus) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String componentUuid = qualityGateEvent.getAnalysis().getRootComponentUuid();

      SnapshotDto liveMeasureSnapshotDto = createLiveMeasureSnapshotDto(qualityGateEvent.getAnalysis().getProjectVersion(), componentUuid);
      dbClient.snapshotDao().insert(dbSession, liveMeasureSnapshotDto);

      EventDto eventDto = createEventDto(componentUuid, liveMeasureSnapshotDto.getUuid(), currentStatus);
      dbClient.eventDao().insert(dbSession, eventDto);

      dbSession.commit();
    }
  }

  private static EventDto createEventDto(String componentUuid, String snapshotUuid, Metric.Level currentStatus) {
    EventDto eventDto = new EventDto();
    eventDto.setComponentUuid(componentUuid);
    eventDto.setCreatedAt(System2.INSTANCE.now());
    eventDto.setCategory(CATEGORY_ALERT);
    eventDto.setDate(System2.INSTANCE.now());
    eventDto.setAnalysisUuid(snapshotUuid);
    eventDto.setUuid(UUID.randomUUID().toString());
    eventDto.setName(currentStatus.getColorName().equals(Metric.Level.OK.getColorName()) ? Measure.Level.OK.getLabel() : Measure.Level.ERROR.getLabel());

    return eventDto;
  }

  private static SnapshotDto createLiveMeasureSnapshotDto(@Nullable String projectVersion, String componentUuid) {
    SnapshotDto dto = new SnapshotDto();

    dto.setUuid(UUID.randomUUID().toString());
    dto.setCreatedAt(System2.INSTANCE.now());
    dto.setProjectVersion(projectVersion);
    dto.setLast(false);
    dto.setStatus(SnapshotDto.STATUS_LIVE_MEASURE_COMPUTED);
    dto.setRootComponentUuid(componentUuid);

    return dto;
  }
}
