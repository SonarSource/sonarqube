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
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SourcePersisterTest extends AbstractDbUnitTestCase {

  private SourcePersister sourcePersister;

  @Before
  public void before() {
    setupData("shared");
    Snapshot snapshot = getSession().getSingleResult(Snapshot.class, "id", 1000);
    ResourcePersister resourcePersister = mock(ResourcePersister.class);
    when(resourcePersister.getSnapshotOrFail(any(Resource.class))).thenReturn(snapshot);
    sourcePersister = new SourcePersister(getSession(), resourcePersister);
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
