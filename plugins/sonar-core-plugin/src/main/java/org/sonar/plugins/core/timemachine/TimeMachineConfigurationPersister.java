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
package org.sonar.plugins.core.timemachine;

import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorBarriers;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.batch.components.PastSnapshot;
import org.sonar.batch.components.TimeMachineConfiguration;
import org.sonar.batch.index.ResourceCache;

import java.util.List;

import static org.sonar.api.utils.DateUtils.dateToLong;

@DependedUpon(DecoratorBarriers.END_OF_TIME_MACHINE)
public final class TimeMachineConfigurationPersister implements Decorator {

  private final TimeMachineConfiguration timeMachineConfiguration;
  private ResourceCache resourceCache;
  private DatabaseSession session;

  public TimeMachineConfigurationPersister(TimeMachineConfiguration timeMachineConfiguration, ResourceCache resourceCache, DatabaseSession session) {
    this.timeMachineConfiguration = timeMachineConfiguration;
    this.resourceCache = resourceCache;
    this.session = session;
  }

  @Override
  public void decorate(Resource resource, DecoratorContext context) {
    if (ResourceUtils.isProject(resource)) {
      persistConfiguration(resource);
    }
  }

  void persistConfiguration(Resource module) {
    List<PastSnapshot> pastSnapshots = timeMachineConfiguration.getProjectPastSnapshots();
    Snapshot projectSnapshot = resourceCache.get(module).snapshot();
    for (PastSnapshot pastSnapshot : pastSnapshots) {
      Snapshot snapshot = session.reattach(Snapshot.class, projectSnapshot.getId());
      updatePeriodParams(snapshot, pastSnapshot);
      updatePeriodParams(projectSnapshot, pastSnapshot);
      session.save(snapshot);
    }
    session.commit();
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  private void updatePeriodParams(Snapshot snapshot, PastSnapshot pastSnapshot) {
    int periodIndex = pastSnapshot.getIndex();
    snapshot.setPeriodMode(periodIndex, pastSnapshot.getMode());
    snapshot.setPeriodModeParameter(periodIndex, pastSnapshot.getModeParameter());
    snapshot.setPeriodDateMs(periodIndex, dateToLong(pastSnapshot.getDate()));
  }
}
