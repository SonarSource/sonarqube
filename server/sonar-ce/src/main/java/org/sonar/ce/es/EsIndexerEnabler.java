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
package org.sonar.ce.es;

import org.picocontainer.Startable;
import org.sonar.server.es.BaseIndexer;
import org.sonar.server.es.IndexerStartupTask;

/**
 * Replaces the {@link IndexerStartupTask} to enable indexers but without triggering a full
 * indexation (it's the WebServer's responsibility).
 */
public class EsIndexerEnabler implements Startable {

  private final BaseIndexer[] indexers;

  public EsIndexerEnabler(BaseIndexer[] indexers) {
    this.indexers = indexers;
  }

  @Override
  public void start() {
    for (BaseIndexer indexer : indexers) {
      indexer.setEnabled(true);
    }
  }

  @Override
  public void stop() {
    // nothing to do at stop
  }
}
