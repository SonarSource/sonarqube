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
import org.sonar.batch.highlighting.SyntaxHighlightingDataBuilder;
import org.sonar.batch.scan.filesystem.InputPathCache;
import org.sonar.batch.scan.measure.MeasureCache;
import org.sonar.batch.source.CodeColorizers;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.source.SnapshotDataTypes;
import org.sonar.core.source.db.FileSourceDao;
import org.sonar.core.source.db.FileSourceDto;
import org.sonar.core.source.db.SnapshotSourceDao;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;
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
      resourceCache, mock(CodeColorizers.class));
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
    Date now = DateUtils.parseDateTime("2014-10-29T16:44:02+0100");
    when(system2.newDate()).thenReturn(now);

    String relativePathSame = "src/changed.java";
    java.io.File sameFile = new java.io.File(basedir, relativePathSame);
    FileUtils.write(sameFile, "changed\ncontent");
    DefaultInputFile inputFileNew = new DefaultInputFile(PROJECT_KEY, relativePathSame).setLines(2)
      .setAbsolutePath(sameFile.getAbsolutePath())
      .setLineHashes(new String[] {"foo", "bar"});
    when(inputPathCache.all()).thenReturn(Arrays.<InputPath>asList(inputFileNew));

    mockResourceCache(relativePathSame, PROJECT_KEY, "uuidsame");

    sourcePersister.persist();

    FileSourceDto fileSourceDto = new FileSourceDao(getMyBatis()).select("uuidsame");
    assertThat(fileSourceDto.getCreatedAt()).isEqualTo(DateUtils.parseDateTime("2014-10-10T16:44:02+0200").getTime());
    assertThat(fileSourceDto.getUpdatedAt()).isEqualTo(now.getTime());
    assertThat(fileSourceDto.getData()).isEqualTo(
      ",,,,changed\r\n,,,,content\r\n");
    assertThat(fileSourceDto.getLineHashes()).isEqualTo("foo\nbar");
    assertThat(fileSourceDto.getDataHash()).isEqualTo("e41cca9c51ff853c748f708f39dfc035");
  }

  @Test
  public void testPersistEmptyFile() throws Exception {
    setupData("file_sources");
    when(system2.newDate()).thenReturn(DateUtils.parseDateTime("2014-10-29T16:44:02+0100"));

    String relativePathEmpty = "src/empty.java";
    DefaultInputFile inputFileEmpty = new DefaultInputFile(PROJECT_KEY, relativePathEmpty)
      .setLines(0)
      .setLineHashes(new String[] {});
    when(inputPathCache.all()).thenReturn(Arrays.<InputPath>asList(inputFileEmpty));

    mockResourceCache(relativePathEmpty, PROJECT_KEY, "uuidempty");

    sourcePersister.persist();
    checkTables("testPersistEmptyFile", "file_sources");
  }

  @Test
  public void testPersistNewFileNoScmNoHighlighting() throws Exception {
    setupData("file_sources");
    Date now = DateUtils.parseDateTime("2014-10-29T16:44:02+0100");
    when(system2.newDate()).thenReturn(now);

    String relativePathNew = "src/new.java";
    java.io.File newFile = new java.io.File(basedir, relativePathNew);
    FileUtils.write(newFile, "foo\nbar\nbiz");
    DefaultInputFile inputFileNew = new DefaultInputFile(PROJECT_KEY, relativePathNew)
      .setLines(3)
      .setAbsolutePath(newFile.getAbsolutePath())
      .setLineHashes(new String[] {"foo", "bar", "bee"});
    when(inputPathCache.all()).thenReturn(Arrays.<InputPath>asList(inputFileNew));

    mockResourceCache(relativePathNew, PROJECT_KEY, "uuidnew");

    sourcePersister.persist();
    FileSourceDto fileSourceDto = new FileSourceDao(getMyBatis()).select("uuidnew");
    assertThat(fileSourceDto.getCreatedAt()).isEqualTo(now.getTime());
    assertThat(fileSourceDto.getUpdatedAt()).isEqualTo(now.getTime());
    assertThat(fileSourceDto.getData()).isEqualTo(
      ",,,,foo\r\n,,,,bar\r\n,,,,biz\r\n");
    assertThat(fileSourceDto.getLineHashes()).isEqualTo("foo\nbar\nbee");
    assertThat(fileSourceDto.getDataHash()).isEqualTo("0c43ed6418d690ee0ffc3e43e6660967");

  }

  @Test
  public void testPersistNewFileWithScmAndHighlighting() throws Exception {
    setupData("file_sources");
    Date now = DateUtils.parseDateTime("2014-10-29T16:44:02+0100");
    when(system2.newDate()).thenReturn(now);

    String relativePathNew = "src/new.java";
    java.io.File newFile = new java.io.File(basedir, relativePathNew);
    FileUtils.write(newFile, "foo\nbar\nbiz");
    DefaultInputFile inputFileNew = new DefaultInputFile(PROJECT_KEY, relativePathNew)
      .setLines(3)
      .setAbsolutePath(newFile.getAbsolutePath())
      .setOriginalLineOffsets(new long[] {0, 4, 7})
      .setLineHashes(new String[] {"foo", "bar", "bee"});
    when(inputPathCache.all()).thenReturn(Arrays.<InputPath>asList(inputFileNew));

    mockResourceCache(relativePathNew, PROJECT_KEY, "uuidnew");

    when(measureCache.byMetric(PROJECT_KEY + ":" + relativePathNew, CoreMetrics.SCM_AUTHORS_BY_LINE_KEY))
      .thenReturn(Arrays.asList(new Measure(CoreMetrics.SCM_AUTHORS_BY_LINE, "1=julien;2=simon;3=julien")));
    when(measureCache.byMetric(PROJECT_KEY + ":" + relativePathNew, CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE_KEY))
      .thenReturn(Arrays.asList(new Measure(CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE, "1=2014-10-11T16:44:02+0100;2=2014-10-12T16:44:02+0100;3=2014-10-13T16:44:02+0100")));
    when(measureCache.byMetric(PROJECT_KEY + ":" + relativePathNew, CoreMetrics.SCM_REVISIONS_BY_LINE_KEY))
      .thenReturn(Arrays.asList(new Measure(CoreMetrics.SCM_REVISIONS_BY_LINE, "1=123;2=234;3=345")));

    SyntaxHighlightingData highlighting = new SyntaxHighlightingDataBuilder()
      .registerHighlightingRule(0, 3, TypeOfText.ANNOTATION)
      .registerHighlightingRule(4, 5, TypeOfText.COMMENT)
      .registerHighlightingRule(7, 16, TypeOfText.CONSTANT)
      .build();
    when(componentDataCache.getData(PROJECT_KEY + ":" + relativePathNew, SnapshotDataTypes.SYNTAX_HIGHLIGHTING))
      .thenReturn(highlighting);

    sourcePersister.persist();

    FileSourceDto fileSourceDto = new FileSourceDao(getMyBatis()).select("uuidnew");
    assertThat(fileSourceDto.getCreatedAt()).isEqualTo(now.getTime());
    assertThat(fileSourceDto.getUpdatedAt()).isEqualTo(now.getTime());
    assertThat(fileSourceDto.getLineHashes()).isEqualTo("foo\nbar\nbee");
    assertThat(fileSourceDto.getData()).isEqualTo(
      "123,julien,2014-10-11T16:44:02+0100,\"0,3,a\",foo\r\n"
        + "234,simon,2014-10-12T16:44:02+0100,\"0,1,cd\",bar\r\n"
        + "345,julien,2014-10-13T16:44:02+0100,\"0,9,c\",biz\r\n");
    assertThat(fileSourceDto.getDataHash()).isEqualTo("a2aaee165e33957a67331fb9f869e0f1");
  }

  @Test
  public void testSimpleConversionOfHighlightingOffset() {
    DefaultInputFile file = new DefaultInputFile(PROJECT_KEY, "src/foo.java")
      .setLines(3)
      .setOriginalLineOffsets(new long[] {0, 4, 7});

    SyntaxHighlightingData highlighting = new SyntaxHighlightingDataBuilder()
      .registerHighlightingRule(0, 3, TypeOfText.ANNOTATION)
      .registerHighlightingRule(4, 5, TypeOfText.COMMENT)
      .registerHighlightingRule(7, 16, TypeOfText.CONSTANT)
      .build();

    String[] highlightingPerLine = sourcePersister.computeHighlightingPerLine(file, highlighting);

    assertThat(highlightingPerLine).containsOnly("0,3,a", "0,1,cd", "0,9,c");
  }

  @Test
  public void testConversionOfHighlightingOffsetMultiLine() {
    DefaultInputFile file = new DefaultInputFile(PROJECT_KEY, "src/foo.java")
      .setLines(3)
      .setOriginalLineOffsets(new long[] {0, 4, 7});

    SyntaxHighlightingData highlighting = new SyntaxHighlightingDataBuilder()
      .registerHighlightingRule(0, 3, TypeOfText.ANNOTATION)
      .registerHighlightingRule(4, 9, TypeOfText.COMMENT)
      .registerHighlightingRule(10, 16, TypeOfText.CONSTANT)
      .build();

    String[] highlightingPerLine = sourcePersister.computeHighlightingPerLine(file, highlighting);

    assertThat(highlightingPerLine).containsOnly("0,3,a", "0,3,cd", "0,2,cd;3,9,c");
  }

  @Test
  public void testConversionOfHighlightingNestedRules() {
    DefaultInputFile file = new DefaultInputFile(PROJECT_KEY, "src/foo.java")
      .setLines(3)
      .setOriginalLineOffsets(new long[] {0, 4, 7});

    SyntaxHighlightingData highlighting = new SyntaxHighlightingDataBuilder()
      .registerHighlightingRule(0, 3, TypeOfText.ANNOTATION)
      .registerHighlightingRule(4, 6, TypeOfText.COMMENT)
      .registerHighlightingRule(7, 16, TypeOfText.CONSTANT)
      .registerHighlightingRule(8, 15, TypeOfText.KEYWORD)
      .build();

    String[] highlightingPerLine = sourcePersister.computeHighlightingPerLine(file, highlighting);

    assertThat(highlightingPerLine).containsOnly("0,3,a", "0,2,cd", "0,9,c;1,8,k");
  }

  @Test
  public void testConversionOfHighlightingNestedRulesMultiLine() {
    DefaultInputFile file = new DefaultInputFile(PROJECT_KEY, "src/foo.java")
      .setLines(3)
      .setOriginalLineOffsets(new long[] {0, 4, 7});

    SyntaxHighlightingData highlighting = new SyntaxHighlightingDataBuilder()
      .registerHighlightingRule(0, 3, TypeOfText.ANNOTATION)
      .registerHighlightingRule(4, 6, TypeOfText.COMMENT)
      .registerHighlightingRule(4, 16, TypeOfText.CONSTANT)
      .registerHighlightingRule(8, 15, TypeOfText.KEYWORD)
      .build();

    String[] highlightingPerLine = sourcePersister.computeHighlightingPerLine(file, highlighting);

    assertThat(highlightingPerLine).containsOnly("0,3,a", "0,3,c;0,2,cd", "0,9,c;1,8,k");
  }

  private void mockResourceCache(String relativePathEmpty, String projectKey, String uuid) {
    File sonarFile = File.create(relativePathEmpty);
    sonarFile.setUuid(uuid);
    when(resourceCache.get(projectKey + ":" + relativePathEmpty)).thenReturn(sonarFile);
  }
}
