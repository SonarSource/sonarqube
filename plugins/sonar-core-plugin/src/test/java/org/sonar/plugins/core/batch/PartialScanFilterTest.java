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
package org.sonar.plugins.core.batch;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.scan.filesystem.FileSystemFilter;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.components.PastSnapshot;
import org.sonar.batch.components.PastSnapshotFinder;
import org.sonar.core.source.SnapshotDataType;
import org.sonar.core.source.jdbc.SnapshotDataDao;
import org.sonar.core.source.jdbc.SnapshotDataDto;
import org.sonar.plugins.core.utils.HashBuilder;

import java.io.File;
import java.util.Arrays;
import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PartialScanFilterTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private PartialScanFilter filter;

  private Settings settings;

  private PastSnapshotFinder pastSnapshotFinder;

  private Snapshot snapshot;

  private File baseDir;

  private SnapshotDataDao snapshotDataDao;

  @Before
  public void prepare() throws Exception {
    settings = new Settings();
    pastSnapshotFinder = mock(PastSnapshotFinder.class);
    snapshot = mock(Snapshot.class);
    baseDir = temp.newFolder();
    snapshotDataDao = mock(SnapshotDataDao.class);
    filter = new PartialScanFilter(settings, ProjectDefinition.create().setBaseDir(baseDir), new PathResolver(), new HashBuilder(), snapshot,
      snapshotDataDao, pastSnapshotFinder);
  }

  @Test
  public void should_not_run_by_default() throws Exception {
    filter.start();
    assertThat(filter.accept(temp.newFile(), mock(FileSystemFilter.Context.class))).isTrue();
  }

  @Test
  public void should_throw_if_partial_mode_and_not_in_dryrun() throws Exception {
    settings.setProperty(CoreProperties.PARTIAL_ANALYSIS, true);

    thrown.expect(SonarException.class);
    thrown.expectMessage("Partial analysis is only supported with dry run mode");
    filter.start();
  }

  @Test
  public void should_include_if_no_previous_snapshot() throws Exception {
    settings.setProperty(CoreProperties.PARTIAL_ANALYSIS, true);
    settings.setProperty(CoreProperties.DRY_RUN, true);

    when(pastSnapshotFinder.findPreviousAnalysis(snapshot)).thenReturn(new PastSnapshot("foo"));

    filter.start();
    assertThat(filter.accept(new File(baseDir, "src/main/java/foo/Bar.java"), mock(FileSystemFilter.Context.class))).isTrue();
  }

  @Test
  public void should_include_if_no_previous_snapshot_data() throws Exception {
    settings.setProperty(CoreProperties.PARTIAL_ANALYSIS, true);
    settings.setProperty(CoreProperties.DRY_RUN, true);

    Snapshot previousSnapshot = mock(Snapshot.class);
    PastSnapshot pastSnapshot = new PastSnapshot("foo", new Date(), previousSnapshot);
    when(pastSnapshotFinder.findPreviousAnalysis(snapshot)).thenReturn(pastSnapshot);

    filter.start();
    assertThat(filter.accept(new File(baseDir, "src/main/java/foo/Bar.java"), mock(FileSystemFilter.Context.class))).isTrue();
  }

  @Test
  public void should_include_if_different_hash() throws Exception {
    settings.setProperty(CoreProperties.PARTIAL_ANALYSIS, true);
    settings.setProperty(CoreProperties.DRY_RUN, true);

    Snapshot previousSnapshot = mock(Snapshot.class);
    when(previousSnapshot.getId()).thenReturn(123);
    PastSnapshot pastSnapshot = new PastSnapshot("foo", new Date(), previousSnapshot);
    when(pastSnapshotFinder.findPreviousAnalysis(snapshot)).thenReturn(pastSnapshot);

    SnapshotDataDto snapshotDataDto = new SnapshotDataDto();
    snapshotDataDto.setData("src/main/java/foo/Bar.java=abcd1234\n");
    when(snapshotDataDao.selectSnapshotData(123, Arrays.asList(SnapshotDataType.FILE_HASH.getValue())))
      .thenReturn(Arrays.asList(snapshotDataDto));

    filter.start();
    File file = new File(baseDir, "src/main/java/foo/Bar.java");
    FileUtils.write(file, "foo");
    assertThat(filter.accept(file, mock(FileSystemFilter.Context.class))).isTrue();
  }

  @Test
  public void should_exclude_if_same_hash() throws Exception {
    settings.setProperty(CoreProperties.PARTIAL_ANALYSIS, true);
    settings.setProperty(CoreProperties.DRY_RUN, true);

    Snapshot previousSnapshot = mock(Snapshot.class);
    when(previousSnapshot.getId()).thenReturn(123);
    PastSnapshot pastSnapshot = new PastSnapshot("foo", new Date(), previousSnapshot);
    when(pastSnapshotFinder.findPreviousAnalysis(snapshot)).thenReturn(pastSnapshot);

    SnapshotDataDto snapshotDataDto = new SnapshotDataDto();
    snapshotDataDto.setData("src/main/java/foo/Bar.java=acbd18db4cc2f85cedef654fccc4a4d8\n");
    when(snapshotDataDao.selectSnapshotData(123, Arrays.asList(SnapshotDataType.FILE_HASH.getValue())))
      .thenReturn(Arrays.asList(snapshotDataDto));

    filter.start();
    File file = new File(baseDir, "src/main/java/foo/Bar.java");
    FileUtils.write(file, "foo");
    assertThat(filter.accept(file, mock(FileSystemFilter.Context.class))).isFalse();
  }
}
