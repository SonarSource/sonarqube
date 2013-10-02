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

import com.google.common.base.Charsets;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.batch.components.PastSnapshot;
import org.sonar.batch.components.PastSnapshotFinder;
import org.sonar.core.source.SnapshotDataType;
import org.sonar.core.source.jdbc.SnapshotDataDao;
import org.sonar.core.source.jdbc.SnapshotDataDto;

import java.io.File;
import java.util.Arrays;
import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileHashCacheTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private FileHashCache cache;

  private PastSnapshotFinder pastSnapshotFinder;

  private Snapshot snapshot;

  private File baseDir;

  private SnapshotDataDao snapshotDataDao;

  private ModuleFileSystem moduleFileSystem;

  @Before
  public void prepare() throws Exception {
    pastSnapshotFinder = mock(PastSnapshotFinder.class);
    snapshot = mock(Snapshot.class);
    baseDir = temp.newFolder();
    snapshotDataDao = mock(SnapshotDataDao.class);
    moduleFileSystem = mock(ModuleFileSystem.class);
    cache = new FileHashCache(moduleFileSystem, ProjectDefinition.create().setBaseDir(baseDir), new PathResolver(), new HashBuilder(), snapshot,
      snapshotDataDao, pastSnapshotFinder);
  }

  @Test
  public void should_return_null_if_no_previous_snapshot() throws Exception {
    when(pastSnapshotFinder.findPreviousAnalysis(snapshot)).thenReturn(new PastSnapshot("foo"));

    cache.start();
    assertThat(cache.getPreviousHash(new File(baseDir, "src/main/java/foo/Bar.java"))).isNull();
  }

  @Test
  public void should_return_null_if_no_previous_snapshot_data() throws Exception {
    Snapshot previousSnapshot = mock(Snapshot.class);
    PastSnapshot pastSnapshot = new PastSnapshot("foo", new Date(), previousSnapshot);
    when(pastSnapshotFinder.findPreviousAnalysis(snapshot)).thenReturn(pastSnapshot);

    cache.start();
    assertThat(cache.getPreviousHash(new File(baseDir, "src/main/java/foo/Bar.java"))).isNull();
  }

  @Test
  public void should_return_previous_hash() throws Exception {
    Snapshot previousSnapshot = mock(Snapshot.class);
    when(previousSnapshot.getId()).thenReturn(123);
    PastSnapshot pastSnapshot = new PastSnapshot("foo", new Date(), previousSnapshot);
    when(pastSnapshotFinder.findPreviousAnalysis(snapshot)).thenReturn(pastSnapshot);

    SnapshotDataDto snapshotDataDto = new SnapshotDataDto();
    snapshotDataDto.setData("src/main/java/foo/Bar.java=abcd1234\n");
    when(snapshotDataDao.selectSnapshotData(123, Arrays.asList(SnapshotDataType.FILE_HASH.getValue())))
      .thenReturn(Arrays.asList(snapshotDataDto));

    File file = new File(baseDir, "src/main/java/foo/Bar.java");
    FileUtils.write(file, "foo");
    cache.start();
    assertThat(cache.getPreviousHash(file)).isEqualTo("abcd1234");
  }

  @Test
  public void should_compute_and_cache_current_hash() throws Exception {
    when(moduleFileSystem.sourceCharset()).thenReturn(Charsets.UTF_8);

    File file = new File(baseDir, "src/main/java/foo/Bar.java");
    FileUtils.write(file, "foo");
    String hash = "9a8742076ef9ffa5591f633704c2286b";
    assertThat(cache.getCurrentHash(file)).isEqualTo(hash);

    // Modify file
    FileUtils.write(file, "bar");
    assertThat(cache.getCurrentHash(file)).isEqualTo(hash);
  }
}
