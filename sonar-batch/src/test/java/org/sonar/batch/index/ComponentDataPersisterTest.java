/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.batch.index;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.database.model.Snapshot;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.source.jdbc.SnapshotDataDao;

public class ComponentDataPersisterTest extends AbstractDaoTestCase {

  SnapshotCache snapshots = new SnapshotCache();
  ComponentDataCache data;
  Caches caches = new Caches();

  @Before
  public void start() {
    caches.start();
  }

  @After
  public void stop() {
    caches.stop();
  }

  @Test
  public void should_persist_component_data() throws Exception {
    setupData("should_persist_component_data");
    Snapshot snapshot = new Snapshot();
    snapshot.setId(100);
    snapshot.setResourceId(200);
    snapshots.put("org/struts/Action.java", snapshot);

    data = new ComponentDataCache(caches);
    data.setStringData("org/struts/Action.java", "SYMBOL", "content of symbol");
    data.setStringData("org/struts/Action.java", "SYNTAX", "content of syntax");
    data.setStringData("org/struts/Other.java", "SYMBOL", "unregistered component, should not be persisted");

    SnapshotDataDao dataDao = new SnapshotDataDao(getMyBatis());
    ComponentDataPersister persister = new ComponentDataPersister(data, snapshots, dataDao);
    persister.persist();

    checkTables("should_persist_component_data", new String[]{"id", "created_at", "updated_at"}, "snapshot_data");
  }
}
