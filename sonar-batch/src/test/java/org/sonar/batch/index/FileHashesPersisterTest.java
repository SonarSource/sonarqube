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
package org.sonar.batch.index;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.source.SnapshotDataTypes;
import org.sonar.core.source.db.SnapshotDataDao;

public class FileHashesPersisterTest extends AbstractDaoTestCase {

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  ResourceCache resourceCache;
  ComponentDataCache data;
  Caches caches;

  @Before
  public void start() throws Exception {
    resourceCache = new ResourceCache();
    caches = CachesTest.createCacheOnTemp(temp);
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
    resourceCache.add(new Project("myProject").setId(200), null, snapshot);

    data = new ComponentDataCache(caches);
    data.setStringData("myProject", SnapshotDataTypes.FILE_HASHES, "org/struts/Action.java=123ABC");

    SnapshotDataDao dataDao = new SnapshotDataDao(getMyBatis());
    FileHashesPersister persister = new FileHashesPersister(data, resourceCache, dataDao, getMyBatis());
    persister.persist();

    checkTables("should_persist_component_data", new String[] {"id", "created_at", "updated_at"}, "snapshot_data");
  }
}
