/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.database.model.Snapshot;
import org.sonar.batch.components.PastSnapshot;
import org.sonar.batch.components.PastSnapshotFinder;
import org.sonar.core.source.SnapshotDataTypes;
import org.sonar.core.source.jdbc.SnapshotDataDao;
import org.sonar.core.source.jdbc.SnapshotDataDto;

import java.util.Arrays;
import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RemoteFileHashesTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  PastSnapshotFinder pastSnapshotFinder = mock(PastSnapshotFinder.class);
  Snapshot snapshot = mock(Snapshot.class);
  SnapshotDataDao snapshotDataDao = mock(SnapshotDataDao.class);
  RemoteFileHashes hashes = new RemoteFileHashes(snapshot, snapshotDataDao, pastSnapshotFinder);

  @Test
  public void should_return_null_if_no_remote_snapshot() throws Exception {
    when(pastSnapshotFinder.findPreviousAnalysis(snapshot)).thenReturn(new PastSnapshot("foo"));

    hashes.start();
    assertThat(hashes.remoteHash("src/main/java/foo/Bar.java")).isNull();
    hashes.stop();
  }

  @Test
  public void should_return_null_if_no_remote_hashes() throws Exception {
    Snapshot previousSnapshot = mock(Snapshot.class);
    PastSnapshot pastSnapshot = new PastSnapshot("foo", new Date(), previousSnapshot);
    when(pastSnapshotFinder.findPreviousAnalysis(snapshot)).thenReturn(pastSnapshot);

    hashes.start();
    assertThat(hashes.remoteHash("src/main/java/foo/Bar.java")).isNull();
    hashes.stop();
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

    hashes.start();
    assertThat(hashes.remoteHash("src/main/java/foo/Bar.java")).isEqualTo("abcd1234");
    hashes.stop();
  }
}
