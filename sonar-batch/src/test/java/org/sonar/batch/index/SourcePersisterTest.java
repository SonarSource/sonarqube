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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.DuplicatedSourceException;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Resource;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.source.db.SnapshotSourceDao;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SourcePersisterTest extends AbstractDaoTestCase {

  private SourcePersister sourcePersister;

  @Before
  public void before() {
    setupData("shared");
    ResourcePersister resourcePersister = mock(ResourcePersister.class);
    Snapshot snapshot = new Snapshot();
    snapshot.setId(1000);
    when(resourcePersister.getSnapshotOrFail(any(Resource.class))).thenReturn(snapshot);
    sourcePersister = new SourcePersister(resourcePersister, new SnapshotSourceDao(getMyBatis()));
  }

  @Test
  public void shouldSaveSource() {
    sourcePersister.saveSource(new File("org/foo/Bar.java"), "this is the file content");
    checkTables("shouldSaveSource", "snapshot_sources");
  }

  @Test(expected = DuplicatedSourceException.class)
  public void shouldFailIfSourceSavedSeveralTimes() {
    File file = new File("org/foo/Bar.java");
    sourcePersister.saveSource(file, "this is the file content");
    sourcePersister.saveSource(file, "new content"); // fail
  }
}
