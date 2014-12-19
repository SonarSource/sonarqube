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
package org.sonar.batch.scan.filesystem;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.batch.components.PastSnapshot;
import org.sonar.batch.components.PastSnapshotFinder;
import org.sonar.batch.index.ResourceCache;
import org.sonar.core.source.SnapshotDataTypes;
import org.sonar.core.source.db.SnapshotDataDao;
import org.sonar.core.source.db.SnapshotDataDto;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PreviousFileHashLoaderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private PastSnapshotFinder pastSnapshotFinder = mock(PastSnapshotFinder.class);
  private Snapshot snapshot = mock(Snapshot.class);
  private SnapshotDataDao snapshotDataDao = mock(SnapshotDataDao.class);
  private Project project = new Project("foo");
  private ResourceCache resourceCache;
  private PreviousFileHashLoader loader;

  @Before
  public void prepare() {
    resourceCache = new ResourceCache();
    resourceCache.add(project, null, snapshot);
    loader = new PreviousFileHashLoader(project, resourceCache, snapshotDataDao, pastSnapshotFinder);
  }

  @Test
  public void should_return_null_if_no_previous_snapshot() throws Exception {
    when(pastSnapshotFinder.findPreviousAnalysis(snapshot)).thenReturn(new PastSnapshot("foo"));

    Map<String, String> hashByRelativePath = loader.hashByRelativePath();
    assertThat(hashByRelativePath.get("src/main/java/foo/Bar.java")).isNull();
  }

  @Test
  public void should_return_null_if_no_remote_hashes() throws Exception {
    Snapshot previousSnapshot = mock(Snapshot.class);
    PastSnapshot pastSnapshot = new PastSnapshot("foo", new Date(), previousSnapshot);
    when(pastSnapshotFinder.findPreviousAnalysis(snapshot)).thenReturn(pastSnapshot);

    Map<String, String> hashByRelativePath = loader.hashByRelativePath();
    assertThat(hashByRelativePath.get("src/main/java/foo/Bar.java")).isNull();
  }

  @Test
  public void should_return_remote_hash() throws Exception {
    Snapshot previousSnapshot = mock(Snapshot.class);
    when(previousSnapshot.getId()).thenReturn(123);
    PastSnapshot pastSnapshot = new PastSnapshot("foo", new Date(), previousSnapshot);
    when(pastSnapshotFinder.findPreviousAnalysis(snapshot)).thenReturn(pastSnapshot);

    SnapshotDataDto snapshotDataDto = new SnapshotDataDto();
    snapshotDataDto.setData("src/main/java/foo/Bar.java=abcd1234");
    when(snapshotDataDao.selectSnapshotData(123, Arrays.asList(SnapshotDataTypes.FILE_HASHES)))
      .thenReturn(Arrays.asList(snapshotDataDto));

    Map<String, String> hashByRelativePath = loader.hashByRelativePath();
    assertThat(hashByRelativePath.get("src/main/java/foo/Bar.java")).isEqualTo("abcd1234");
  }
}
