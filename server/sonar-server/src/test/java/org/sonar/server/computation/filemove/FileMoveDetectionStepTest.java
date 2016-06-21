/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.filemove;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.core.hash.SourceHashComputer;
import org.sonar.core.hash.SourceLinesHashesComputer;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDtoWithSnapshotId;
import org.sonar.db.component.ComponentTreeQuery;
import org.sonar.db.source.FileSourceDao;
import org.sonar.db.source.FileSourceDto;
import org.sonar.server.computation.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.snapshot.Snapshot;
import org.sonar.server.computation.source.SourceLinesRepositoryRule;

import static com.google.common.base.Joiner.on;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.resources.Qualifiers.FILE;
import static org.sonar.api.resources.Qualifiers.UNIT_TEST_FILE;
import static org.sonar.server.computation.component.ReportComponent.builder;

public class FileMoveDetectionStepTest {
  private static final long SNAPSHOT_ID = 98765;
  private static final Snapshot SNAPSHOT = new Snapshot.Builder()
    .setId(SNAPSHOT_ID)
    .setUuid("uuid_1")
    .setCreatedAt(86521)
    .build();
  private static final int ROOT_REF = 1;
  private static final int FILE_1_REF = 2;
  private static final int FILE_2_REF = 3;
  private static final int FILE_3_REF = 4;
  private static final Component FILE_1 = fileComponent(FILE_1_REF);
  private static final Component FILE_2 = fileComponent(FILE_2_REF);
  private static final Component FILE_3 = fileComponent(FILE_3_REF);
  private static final String[] CONTENT1 = {
    "package org.sonar.server.computation.filemove;",
    "",
    "public class Foo {",
    "  public String bar() {",
    "    return \"Doh!\";",
    "  }",
    "}"
  };
  private static final String[] LESS_CONTENT1 = {
    "package org.sonar.server.computation.filemove;",
    "",
    "public class Foo {",
    "}"
  };
  public static final String[] CONTENT_EMPTY = {
    ""
  };
  private static final String[] CONTENT2 = {
    "package org.sonar.ce.queue;",
    "",
    "import com.google.common.base.MoreObjects;",
    "import javax.annotation.CheckForNull;",
    "import javax.annotation.Nullable;",
    "import javax.annotation.concurrent.Immutable;",
    "",
    "import static com.google.common.base.Strings.emptyToNull;",
    "import static java.util.Objects.requireNonNull;",
    "",
    "@Immutable",
    "public class CeTask {",
    "",
    ",  private final String type;",
    ",  private final String uuid;",
    ",  private final String componentUuid;",
    ",  private final String componentKey;",
    ",  private final String componentName;",
    ",  private final String submitterLogin;",
    "",
    ",  private CeTask(Builder builder) {",
    ",    this.uuid = requireNonNull(emptyToNull(builder.uuid));",
    ",    this.type = requireNonNull(emptyToNull(builder.type));",
    ",    this.componentUuid = emptyToNull(builder.componentUuid);",
    ",    this.componentKey = emptyToNull(builder.componentKey);",
    ",    this.componentName = emptyToNull(builder.componentName);",
    ",    this.submitterLogin = emptyToNull(builder.submitterLogin);",
    ",  }",
    "",
    ",  public String getUuid() {",
    ",    return uuid;",
    ",  }",
    "",
    ",  public String getType() {",
    ",    return type;",
    ",  }",
    "",
    ",  @CheckForNull",
    ",  public String getComponentUuid() {",
    ",    return componentUuid;",
    ",  }",
    "",
    ",  @CheckForNull",
    ",  public String getComponentKey() {",
    ",    return componentKey;",
    ",  }",
    "",
    ",  @CheckForNull",
    ",  public String getComponentName() {",
    ",    return componentName;",
    ",  }",
    "",
    ",  @CheckForNull",
    ",  public String getSubmitterLogin() {",
    ",    return submitterLogin;",
    ",  }",
    ",}",
  };
  // removed immutable annotation
  private static final String[] LESS_CONTENT2 = {
    "package org.sonar.ce.queue;",
    "",
    "import com.google.common.base.MoreObjects;",
    "import javax.annotation.CheckForNull;",
    "import javax.annotation.Nullable;",
    "",
    "import static com.google.common.base.Strings.emptyToNull;",
    "import static java.util.Objects.requireNonNull;",
    "",
    "public class CeTask {",
    "",
    ",  private final String type;",
    ",  private final String uuid;",
    ",  private final String componentUuid;",
    ",  private final String componentKey;",
    ",  private final String componentName;",
    ",  private final String submitterLogin;",
    "",
    ",  private CeTask(Builder builder) {",
    ",    this.uuid = requireNonNull(emptyToNull(builder.uuid));",
    ",    this.type = requireNonNull(emptyToNull(builder.type));",
    ",    this.componentUuid = emptyToNull(builder.componentUuid);",
    ",    this.componentKey = emptyToNull(builder.componentKey);",
    ",    this.componentName = emptyToNull(builder.componentName);",
    ",    this.submitterLogin = emptyToNull(builder.submitterLogin);",
    ",  }",
    "",
    ",  public String getUuid() {",
    ",    return uuid;",
    ",  }",
    "",
    ",  public String getType() {",
    ",    return type;",
    ",  }",
    "",
    ",  @CheckForNull",
    ",  public String getComponentUuid() {",
    ",    return componentUuid;",
    ",  }",
    "",
    ",  @CheckForNull",
    ",  public String getComponentKey() {",
    ",    return componentKey;",
    ",  }",
    "",
    ",  @CheckForNull",
    ",  public String getComponentName() {",
    ",    return componentName;",
    ",  }",
    "",
    ",  @CheckForNull",
    ",  public String getSubmitterLogin() {",
    ",    return submitterLogin;",
    ",  }",
    ",}",
  };

  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public SourceLinesRepositoryRule sourceLinesRepository = new SourceLinesRepositoryRule();
  @Rule
  public MutableMovedFilesRepositoryRule movedFilesRepository = new MutableMovedFilesRepositoryRule();

  private DbClient dbClient = mock(DbClient.class);
  private DbSession dbSession = mock(DbSession.class);
  private ComponentDao componentDao = mock(ComponentDao.class);
  private FileSourceDao fileSourceDao = mock(FileSourceDao.class);
  private FileSimilarity fileSimilarity = new FileSimilarityImpl(new SourceSimilarityImpl());
  private long dbIdGenerator = 0;

  private FileMoveDetectionStep underTest = new FileMoveDetectionStep(analysisMetadataHolder, treeRootHolder, dbClient,
    sourceLinesRepository, fileSimilarity, movedFilesRepository);

  @Before
  public void setUp() throws Exception {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.componentDao()).thenReturn(componentDao);
    when(dbClient.fileSourceDao()).thenReturn(fileSourceDao);
  }

  @Test
  public void getDescription_returns_description() {
    assertThat(underTest.getDescription()).isEqualTo("Detect file moves");
  }

  @Test
  public void execute_detects_no_move_if_baseProjectSnaphost_is_null() {
    analysisMetadataHolder.setBaseProjectSnapshot(null);

    underTest.execute();

    assertThat(movedFilesRepository.getComponentsWithOriginal()).isEmpty();
  }

  @Test
  public void execute_detects_no_move_if_baseSnapshot_has_no_file_and_report_has_no_file() {
    analysisMetadataHolder.setBaseProjectSnapshot(SNAPSHOT);

    underTest.execute();

    assertThat(movedFilesRepository.getComponentsWithOriginal()).isEmpty();
  }

  @Test
  public void execute_detects_no_move_if_baseSnapshot_has_no_file() {
    analysisMetadataHolder.setBaseProjectSnapshot(SNAPSHOT);
    setFilesInReport(FILE_1, FILE_2);

    underTest.execute();

    assertThat(movedFilesRepository.getComponentsWithOriginal()).isEmpty();
  }

  @Test
  public void execute_retrieves_only_file_and_unit_tests_from_last_snapshot() {
    analysisMetadataHolder.setBaseProjectSnapshot(SNAPSHOT);
    ArgumentCaptor<ComponentTreeQuery> captor = ArgumentCaptor.forClass(ComponentTreeQuery.class);
    when(componentDao.selectAllChildren(eq(dbSession), captor.capture()))
      .thenReturn(Collections.<ComponentDtoWithSnapshotId>emptyList());

    underTest.execute();

    ComponentTreeQuery query = captor.getValue();
    assertThat(query.getBaseSnapshot().getId()).isEqualTo(SNAPSHOT_ID);
    assertThat(query.getBaseSnapshot().getRootId()).isEqualTo(SNAPSHOT_ID);
    assertThat(query.getPage()).isEqualTo(1);
    assertThat(query.getPageSize()).isEqualTo(Integer.MAX_VALUE);
    assertThat(query.getSqlSort()).isEqualTo("LOWER(p.name) ASC, p.name ASC");
    assertThat(query.getQualifiers()).containsOnly(FILE, UNIT_TEST_FILE);
  }

  @Test
  public void execute_detects_no_move_if_there_is_no_file_in_report() {
    analysisMetadataHolder.setBaseProjectSnapshot(SNAPSHOT);
    mockComponentsForSnapshot(1);
    setFilesInReport();

    underTest.execute();

    assertThat(movedFilesRepository.getComponentsWithOriginal()).isEmpty();
  }

  @Test
  public void execute_detects_no_move_if_file_key_exists_in_both_DB_and_report() {
    analysisMetadataHolder.setBaseProjectSnapshot(SNAPSHOT);
    mockComponentsForSnapshot(FILE_1.getKey(), FILE_2.getKey());
    setFilesInReport(FILE_2, FILE_1);

    underTest.execute();

    assertThat(movedFilesRepository.getComponentsWithOriginal()).isEmpty();
  }

  @Test
  public void execute_detects_move_if_content_of_file_is_same_in_DB_and_report() {
    analysisMetadataHolder.setBaseProjectSnapshot(SNAPSHOT);
    ComponentDtoWithSnapshotId[] dtos = mockComponentsForSnapshot(FILE_1.getKey());
    mockContentOfFileIdDb(FILE_1.getKey(), CONTENT1);
    setFilesInReport(FILE_2);
    setFileContentInReport(FILE_2_REF, CONTENT1);

    underTest.execute();

    assertThat(movedFilesRepository.getComponentsWithOriginal()).containsExactly(FILE_2);
    MovedFilesRepository.OriginalFile originalFile = movedFilesRepository.getOriginalFile(FILE_2).get();
    assertThat(originalFile.getId()).isEqualTo(dtos[0].getId());
    assertThat(originalFile.getKey()).isEqualTo(dtos[0].getKey());
    assertThat(originalFile.getUuid()).isEqualTo(dtos[0].uuid());
  }

  @Test
  public void execute_detects_no_move_if_content_of_file_is_not_similar_enough() {
    analysisMetadataHolder.setBaseProjectSnapshot(SNAPSHOT);
    mockComponentsForSnapshot(FILE_1.getKey());
    mockContentOfFileIdDb(FILE_1.getKey(), CONTENT1);
    setFilesInReport(FILE_2);
    setFileContentInReport(FILE_2_REF, LESS_CONTENT1);

    underTest.execute();

    assertThat(movedFilesRepository.getComponentsWithOriginal()).isEmpty();
  }

  @Test
  public void execute_detects_no_move_if_content_of_file_is_empty_in_DB() {
    analysisMetadataHolder.setBaseProjectSnapshot(SNAPSHOT);
    mockComponentsForSnapshot(FILE_1.getKey());
    mockContentOfFileIdDb(FILE_1.getKey(), CONTENT_EMPTY);
    setFilesInReport(FILE_2);
    setFileContentInReport(FILE_2_REF, CONTENT1);

    underTest.execute();

    assertThat(movedFilesRepository.getComponentsWithOriginal()).isEmpty();
  }

  @Test
  public void execute_detects_no_move_if_content_of_file_is_empty_in_report() {
    analysisMetadataHolder.setBaseProjectSnapshot(SNAPSHOT);
    mockComponentsForSnapshot(FILE_1.getKey());
    mockContentOfFileIdDb(FILE_1.getKey(), CONTENT1);
    setFilesInReport(FILE_2);
    setFileContentInReport(FILE_2_REF, CONTENT_EMPTY);

    underTest.execute();

    assertThat(movedFilesRepository.getComponentsWithOriginal()).isEmpty();
  }

  @Test
  public void execute_detects_no_move_if_two_added_files_have_same_content_as_the_one_in_db() {
    analysisMetadataHolder.setBaseProjectSnapshot(SNAPSHOT);
    mockComponentsForSnapshot(FILE_1.getKey());
    mockContentOfFileIdDb(FILE_1.getKey(), CONTENT1);
    setFilesInReport(FILE_2, FILE_3);
    setFileContentInReport(FILE_2_REF, CONTENT1);
    setFileContentInReport(FILE_3_REF, CONTENT1);

    underTest.execute();

    assertThat(movedFilesRepository.getComponentsWithOriginal()).isEmpty();
  }

  @Test
  public void execute_detects_no_move_if_two_deleted_files_have_same_content_as_the_one_added() {
    analysisMetadataHolder.setBaseProjectSnapshot(SNAPSHOT);
    mockComponentsForSnapshot(FILE_1.getKey(), FILE_2.getKey());
    mockContentOfFileIdDb(FILE_1.getKey(), CONTENT1);
    mockContentOfFileIdDb(FILE_2.getKey(), CONTENT1);
    setFilesInReport(FILE_3);
    setFileContentInReport(FILE_3_REF, CONTENT1);

    underTest.execute();

    assertThat(movedFilesRepository.getComponentsWithOriginal()).isEmpty();
  }

  @Test
  public void execute_detects_several_moves() {
    // testing:
    // - file1 renamed to file3
    // - file2 deleted
    // - file4 untouched
    // - file5 renamed to file6 with a small change
    analysisMetadataHolder.setBaseProjectSnapshot(SNAPSHOT);
    Component file4 = fileComponent(5);
    Component file5 = fileComponent(6);
    Component file6 = fileComponent(7);
    ComponentDtoWithSnapshotId[] dtos = mockComponentsForSnapshot(FILE_1.getKey(), FILE_2.getKey(), file4.getKey(), file5.getKey());
    mockContentOfFileIdDb(FILE_1.getKey(), CONTENT1);
    mockContentOfFileIdDb(FILE_2.getKey(), LESS_CONTENT1);
    mockContentOfFileIdDb(file4.getKey(), new String[] {"e", "f", "g", "h", "i"});
    mockContentOfFileIdDb(file5.getKey(), CONTENT2);
    setFilesInReport(FILE_3, file4, file6);
    setFileContentInReport(FILE_3_REF, CONTENT1);
    setFileContentInReport(file4.getReportAttributes().getRef(), new String[] {"a", "b"});
    setFileContentInReport(file6.getReportAttributes().getRef(), LESS_CONTENT2);

    underTest.execute();

    assertThat(movedFilesRepository.getComponentsWithOriginal()).containsOnly(FILE_3, file6);
    MovedFilesRepository.OriginalFile originalFile2 = movedFilesRepository.getOriginalFile(FILE_3).get();
    assertThat(originalFile2.getId()).isEqualTo(dtos[0].getId());
    assertThat(originalFile2.getKey()).isEqualTo(dtos[0].getKey());
    assertThat(originalFile2.getUuid()).isEqualTo(dtos[0].uuid());
    MovedFilesRepository.OriginalFile originalFile5 = movedFilesRepository.getOriginalFile(file6).get();
    assertThat(originalFile5.getId()).isEqualTo(dtos[3].getId());
    assertThat(originalFile5.getKey()).isEqualTo(dtos[3].getKey());
    assertThat(originalFile5.getUuid()).isEqualTo(dtos[3].uuid());
  }

  private void setFileContentInReport(int ref, String[] content) {
    sourceLinesRepository.addLines(ref, content);
  }

  private void mockContentOfFileIdDb(String key, String[] content) {
    SourceLinesHashesComputer linesHashesComputer = new SourceLinesHashesComputer();
    SourceHashComputer sourceHashComputer = new SourceHashComputer();
    Iterator<String> lineIterator = Arrays.asList(content).iterator();
    while (lineIterator.hasNext()) {
      String line = lineIterator.next();
      linesHashesComputer.addLine(line);
      sourceHashComputer.addLine(line, lineIterator.hasNext());
    }

    when(fileSourceDao.selectSourceByFileUuid(dbSession, componentUuidOf(key)))
      .thenReturn(new FileSourceDto()
        .setLineHashes(on('\n').join(linesHashesComputer.getLineHashes()))
        .setSrcHash(sourceHashComputer.getHash()));
  }

  private void setFilesInReport(Component... files) {
    treeRootHolder.setRoot(builder(Component.Type.PROJECT, ROOT_REF)
      .addChildren(files)
      .build());
  }

  private ComponentDtoWithSnapshotId[] mockComponentsForSnapshot(String... componentKeys) {
    return mockComponentsForSnapshot(SNAPSHOT_ID, componentKeys);
  }

  private ComponentDtoWithSnapshotId[] mockComponentsForSnapshot(long snapshotId, String... componentKeys) {
    List<ComponentDtoWithSnapshotId> componentDtoWithSnapshotIds = stream(componentKeys)
      .map(key -> newComponentDto(snapshotId, key))
      .collect(toList());
    when(componentDao.selectAllChildren(eq(dbSession), any(ComponentTreeQuery.class)))
      .thenReturn(componentDtoWithSnapshotIds);
    return componentDtoWithSnapshotIds.toArray(new ComponentDtoWithSnapshotId[componentDtoWithSnapshotIds.size()]);
  }

  private ComponentDtoWithSnapshotId newComponentDto(long snapshotId, String key) {
    ComponentDtoWithSnapshotId res = new ComponentDtoWithSnapshotId();
    res.setSnapshotId(snapshotId)
      .setId(dbIdGenerator)
      .setKey(key)
      .setUuid(componentUuidOf(key))
      .setPath("path_" + key);
    dbIdGenerator++;
    return res;
  }

  private static String componentUuidOf(String key) {
    return "uuid_" + key;
  }

  private static Component fileComponent(int ref) {
    return builder(Component.Type.FILE, ref)
      .setPath("report_path" + ref)
      .build();
  }

}
