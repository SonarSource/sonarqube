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
package org.sonar.batch.phases;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchSide;
import org.sonar.batch.bootstrap.DefaultAnalysisMode;
import org.sonar.batch.events.EventBus;
import org.sonar.batch.index.ScanPersister;

import java.util.Arrays;

@BatchSide
public class PersistersExecutor {

  private static final Logger LOG = LoggerFactory.getLogger(PersistersExecutor.class);

  private final ScanPersister[] persisters;
  private final DefaultAnalysisMode analysisMode;
  private final EventBus eventBus;

  public PersistersExecutor(DefaultAnalysisMode analysisMode, EventBus eventBus, ScanPersister[] persisters) {
    this.analysisMode = analysisMode;
    this.eventBus = eventBus;
    this.persisters = persisters;
  }

  public PersistersExecutor(DefaultAnalysisMode analysisMode, EventBus eventBus) {
    this(analysisMode, eventBus, new ScanPersister[0]);
  }

  public void execute() {
    if (analysisMode.isDb()) {
      LOG.info("Store results in database");
      eventBus.fireEvent(new PersistersPhaseEvent(Arrays.asList(persisters), true));
      for (ScanPersister persister : persisters) {
        LOG.debug("Execute {}", persister.getClass().getName());
        eventBus.fireEvent(new PersisterExecutionEvent(persister, true));
        persister.persist();
        eventBus.fireEvent(new PersisterExecutionEvent(persister, false));
      }

      eventBus.fireEvent(new PersistersPhaseEvent(Arrays.asList(persisters), false));
    }
  }

}
