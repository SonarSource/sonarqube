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

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.batch.ProjectTree;
import org.sonar.batch.highlighting.SyntaxHighlightingData;
import org.sonar.batch.highlighting.SyntaxHighlightingRule;
import org.sonar.batch.scan.filesystem.InputPathCache;
import org.sonar.batch.scan.measure.MeasureCache;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.source.SnapshotDataTypes;
import org.sonar.core.source.db.SnapshotSourceDao;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SourcePersisterTest extends AbstractDaoTestCase {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private SourcePersister sourcePersister;
  private InputPathCache inputPathCache;
  private ResourceCache resourceCache;
  private ProjectTree projectTree;
  private System2 system2;
  private MeasureCache measureCache;
  private ComponentDataCache componentDataCache;

  private static final String PROJECT_KEY = "foo";

  private java.io.File basedir;

  @Before
  public void before() throws IOException {
    ResourcePersister resourcePersister = mock(ResourcePersister.class);
    Snapshot snapshot = new Snapshot();
    snapshot.setId(1000);
    when(resourcePersister.getSnapshotOrFail(any(Resource.class))).thenReturn(snapshot);
    inputPathCache = mock(InputPathCache.class);
    resourceCache = mock(ResourceCache.class);
    projectTree = mock(ProjectTree.class);
    system2 = mock(System2.class);
    measureCache = mock(MeasureCache.class);
    when(measureCache.byMetric(anyString(), anyString())).thenReturn(Collections.<org.sonar.api.measures.Measure>emptyList());
    componentDataCache = mock(ComponentDataCache.class);
    sourcePersister = new SourcePersister(resourcePersister, new SnapshotSourceDao(getMyBatis()), inputPathCache,
      getMyBatis(), measureCache, componentDataCache, projectTree, system2,
      resourceCache);
    Project project = new Project(PROJECT_KEY);
    project.setUuid("projectUuid");
    when(projectTree.getRootProject()).thenReturn(project);
    basedir = temp.newFolder();
  }

  @Test
  public void shouldSaveSource() {
    setupData("shouldSaveSource");
    sourcePersister.saveSource(new File("org/foo/Bar.java"), "this is the file content", DateUtils.parseDateTime("2014-10-31T16:44:02+0100"));
    checkTables("shouldSaveSource", "snapshot_sources");
  }

  @Test
  public void testPersistDontTouchUnchanged() throws Exception {
    setupData("file_sources");
    when(system2.newDate()).thenReturn(DateUtils.parseDateTime("2014-10-29T16:44:02+0100"));

    String relativePathSame = "src/same.java";
    java.io.File sameFile = new java.io.File(basedir, relativePathSame);
    FileUtils.write(sameFile, "unchanged\ncontent");
    DefaultInputFile inputFileNew = new DefaultInputFile(PROJECT_KEY, relativePathSame).setLines(2).setAbsolutePath(sameFile.getAbsolutePath());
    when(inputPathCache.all()).thenReturn(Arrays.<InputPath>asList(inputFileNew));

    mockResourceCache(relativePathSame, PROJECT_KEY, "uuidsame");

    sourcePersister.persist();
    checkTables("testPersistDontTouchUnchanged", "file_sources");
  }

  @Test
  public void testPersistUpdateChanged() throws Exception {
    setupData("file_sources");
    when(system2.newDate()).thenReturn(DateUtils.parseDateTime("2014-10-29T16:44:02+0100"));

    String relativePathSame = "src/changed.java";
    java.io.File sameFile = new java.io.File(basedir, relativePathSame);
    FileUtils.write(sameFile, "changed\ncontent");
    DefaultInputFile inputFileNew = new DefaultInputFile(PROJECT_KEY, relativePathSame).setLines(2).setAbsolutePath(sameFile.getAbsolutePath());
    when(inputPathCache.all()).thenReturn(Arrays.<InputPath>asList(inputFileNew));

    mockResourceCache(relativePathSame, PROJECT_KEY, "uuidsame");

    sourcePersister.persist();
    checkTables("testPersistUpdateChanged", "file_sources");
  }

  @Test
  public void testPersistEmptyFile() throws Exception {
    setupData("file_sources");
    when(system2.newDate()).thenReturn(DateUtils.parseDateTime("2014-10-29T16:44:02+0100"));

    String relativePathEmpty = "src/empty.java";
    DefaultInputFile inputFileEmpty = new DefaultInputFile(PROJECT_KEY, relativePathEmpty).setLines(0);
    when(inputPathCache.all()).thenReturn(Arrays.<InputPath>asList(inputFileEmpty));

    mockResourceCache(relativePathEmpty, PROJECT_KEY, "uuidempty");

    sourcePersister.persist();
    checkTables("testPersistEmptyFile", "file_sources");
  }

  @Test
  public void testPersistNewFileNoScmNoHighlighting() throws Exception {
    setupData("file_sources");
    when(system2.newDate()).thenReturn(DateUtils.parseDateTime("2014-10-29T16:44:02+0100"));

    String relativePathNew = "src/new.java";
    java.io.File newFile = new java.io.File(basedir, relativePathNew);
    FileUtils.write(newFile, "foo\nbar\nbiz");
    DefaultInputFile inputFileNew = new DefaultInputFile(PROJECT_KEY, relativePathNew).setLines(3).setAbsolutePath(newFile.getAbsolutePath());
    when(inputPathCache.all()).thenReturn(Arrays.<InputPath>asList(inputFileNew));

    mockResourceCache(relativePathNew, PROJECT_KEY, "uuidnew");

    sourcePersister.persist();
    checkTables("testPersistNewFileNoScmNoHighlighting", "file_sources");
  }

  @Test
  public void testPersistNewFileWithScmAndHighlighting() throws Exception {
    setupData("file_sources");
    when(system2.newDate()).thenReturn(DateUtils.parseDateTime("2014-10-29T16:44:02+0100"));

    String relativePathNew = "src/new.java";
    java.io.File newFile = new java.io.File(basedir, relativePathNew);
    FileUtils.write(newFile, "foo\nbar\nbiz");
    DefaultInputFile inputFileNew = new DefaultInputFile(PROJECT_KEY, relativePathNew)
      .setLines(3)
      .setAbsolutePath(newFile.getAbsolutePath())
      .setOriginalLineOffsets(new long[] {0, 4, 7});
    when(inputPathCache.all()).thenReturn(Arrays.<InputPath>asList(inputFileNew));

    mockResourceCache(relativePathNew, PROJECT_KEY, "uuidnew");

    when(measureCache.byMetric(PROJECT_KEY + ":" + relativePathNew, CoreMetrics.SCM_AUTHORS_BY_LINE_KEY))
      .thenReturn(Arrays.asList(new Measure(CoreMetrics.SCM_AUTHORS_BY_LINE, "1=julien;2=simon;3=julien")));
    when(measureCache.byMetric(PROJECT_KEY + ":" + relativePathNew, CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE_KEY))
      .thenReturn(Arrays.asList(new Measure(CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE, "1=2014-10-11T16:44:02+0100;2=2014-10-12T16:44:02+0100;3=2014-10-13T16:44:02+0100")));
    when(measureCache.byMetric(PROJECT_KEY + ":" + relativePathNew, CoreMetrics.SCM_REVISIONS_BY_LINE_KEY))
      .thenReturn(Arrays.asList(new Measure(CoreMetrics.SCM_REVISIONS_BY_LINE, "1=123;2=234;3=345")));

    SyntaxHighlightingData highlighting = new SyntaxHighlightingData(Arrays.asList(
      SyntaxHighlightingRule.create(0, 3, TypeOfText.ANNOTATION),
      SyntaxHighlightingRule.create(4, 5, TypeOfText.COMMENT),
      SyntaxHighlightingRule.create(7, 16, TypeOfText.CONSTANT)
      ));
    when(componentDataCache.getData(PROJECT_KEY + ":" + relativePathNew, SnapshotDataTypes.SYNTAX_HIGHLIGHTING))
      .thenReturn(highlighting);

    sourcePersister.persist();
    checkTables("testPersistNewFileWithScmAndHighlighting", "file_sources");
  }

  private void mockResourceCache(String relativePathEmpty, String projectKey, String uuid) {
    File sonarFile = File.create(relativePathEmpty);
    sonarFile.setUuid(uuid);
    when(resourceCache.get(projectKey + ":" + relativePathEmpty)).thenReturn(sonarFile);
  }
}
