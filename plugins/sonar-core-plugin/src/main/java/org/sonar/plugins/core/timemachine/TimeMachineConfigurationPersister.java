/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.timemachine;

import org.sonar.api.BatchExtension;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.Snapshot;

import java.util.List;

public final class TimeMachineConfigurationPersister implements BatchExtension {

  private TimeMachineConfiguration configuration;
  private Snapshot projectSnapshot;
  private DatabaseSession session;

  public TimeMachineConfigurationPersister(TimeMachineConfiguration configuration, Snapshot projectSnapshot, DatabaseSession session) {
    this.configuration = configuration;
    this.projectSnapshot = projectSnapshot;
    this.session = session;
  }

  public void start() {
    List<PastSnapshot> pastSnapshots = configuration.getProjectPastSnapshots();
    for (PastSnapshot pastSnapshot : pastSnapshots) {
      projectSnapshot = session.reattach(Snapshot.class, projectSnapshot.getId());
      projectSnapshot.setPeriodMode(pastSnapshot.getIndex(), pastSnapshot.getMode());
      projectSnapshot.setPeriodModeParameter(pastSnapshot.getIndex(), pastSnapshot.getModeParameter());
      projectSnapshot.setPeriodDate(pastSnapshot.getIndex(), pastSnapshot.getTargetDate());
      session.save(projectSnapshot);
    }
    session.commit();
  }
}
