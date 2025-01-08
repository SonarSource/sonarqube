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
package org.sonar.ce.task.projectanalysis.step;

import java.util.List;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.event.Event;
import org.sonar.ce.task.projectanalysis.event.EventRepository;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.platform.SonarQubeVersion;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.event.EventDto;

/**
 * Computation of SQ Upgrade events
 */
public class SqUpgradeDetectionEventsStep implements ComputationStep {
  private final TreeRootHolder treeRootHolder;
  private final DbClient dbClient;
  private final EventRepository eventRepository;
  private final SonarQubeVersion sonarQubeVersion;

  public SqUpgradeDetectionEventsStep(TreeRootHolder treeRootHolder, DbClient dbClient,
    EventRepository eventRepository, SonarQubeVersion sonarQubeVersion) {
    this.treeRootHolder = treeRootHolder;
    this.dbClient = dbClient;
    this.eventRepository = eventRepository;
    this.sonarQubeVersion = sonarQubeVersion;
  }

  @Override
  public void execute(Context context) {
    executeForBranch(treeRootHolder.getRoot());
  }

  private void executeForBranch(Component branchComponent) {
    String currentSqVersion = sonarQubeVersion.get().toString();
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<EventDto> sqUpgradeEvents = dbClient.eventDao().selectSqUpgradesByMostRecentFirst(dbSession, branchComponent.getUuid());

      //We don't really care about newer versions, we want to log the events related to a version change.
      boolean isFirstAnalysisForSqVersion = sqUpgradeEvents.isEmpty() || !currentSqVersion.equals(sqUpgradeEvents.get(0).getName());

      if (isFirstAnalysisForSqVersion) {
        Event event = Event.createSqUpgrade(currentSqVersion);
        eventRepository.add(event);
      }
    }
  }

  @Override
  public String getDescription() {
    return "Generate SQ Upgrade analysis events";
  }
}
