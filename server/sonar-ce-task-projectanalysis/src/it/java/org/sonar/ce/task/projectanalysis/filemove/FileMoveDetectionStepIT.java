/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.ce.task.projectanalysis.filemove;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.Analysis;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.FileAttributes;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.source.SourceLinesHashRepository;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.core.hash.SourceLineHashesComputer;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.source.FileSourceDto;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.ce.task.projectanalysis.filemove.FileMoveDetectionStep.MIN_REQUIRED_SCORE;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.db.component.BranchType.PULL_REQUEST;

public class FileMoveDetectionStepIT {

  private static final String SNAPSHOT_UUID = "uuid_1";
  private static final Analysis ANALYSIS = new Analysis.Builder()
    .setUuid(SNAPSHOT_UUID)
    .setCreatedAt(86521)
    .build();
  private static final int ROOT_REF = 1;
  private static final int FILE_1_REF = 2;
  private static final int FILE_2_REF = 3;
  private static final int FILE_3_REF = 4;
  private static final String[] CONTENT1 = {
    "package org.sonar.ce.task.projectanalysis.filemove;",
    "",
    "public class Foo {",
    "  public String bar() {",
    "    return \"Doh!\";",
    "  }",
    "}"
  };

  private static final String[] LESS_CONTENT1 = {
    "package org.sonar.ce.task.projectanalysis.filemove;",
    "",
    "public class Foo {",
    "  public String foo() {",
    "    return \"Donut!\";",
    "  }",
    "}"
  };
  private static final String[] CONTENT_EMPTY = {
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
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public MutableMovedFilesRepositoryRule movedFilesRepository = new MutableMovedFilesRepositoryRule();
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public LogTester logTester = new LogTester();

  private final DbClient dbClient = dbTester.getDbClient();
  private ComponentDto project;

  private final AnalysisMetadataHolderRule analysisMetadataHolder = mock(AnalysisMetadataHolderRule.class);
  private final SourceLinesHashRepository sourceLinesHash = mock(SourceLinesHashRepository.class);
  private final FileSimilarity fileSimilarity = new FileSimilarityImpl(new SourceSimilarityImpl());
  private final CapturingScoreMatrixDumper scoreMatrixDumper = new CapturingScoreMatrixDumper();
  private final RecordingMutableAddedFileRepository addedFileRepository = new RecordingMutableAddedFileRepository();

  private final FileMoveDetectionStep underTest = new FileMoveDetectionStep(analysisMetadataHolder, treeRootHolder, dbClient,
    fileSimilarity, movedFilesRepository, sourceLinesHash, scoreMatrixDumper, addedFileRepository, new NoOpHeapSizeChecker());

  @Before
  public void setUp() throws Exception {
    logTester.setLevel(Level.DEBUG);
    project = dbTester.components().insertPrivateProject().getMainBranchComponent();
    treeRootHolder.setRoot(builder(Component.Type.PROJECT, ROOT_REF).setUuid(project.uuid()).build());
  }

  @Test
  public void getDescription_returns_description() {
    assertThat(underTest.getDescription()).isEqualTo("Detect file moves");
  }

  @Test
  public void execute_detects_no_move_if_in_pull_request_scope() {
    prepareAnalysis(PULL_REQUEST, ANALYSIS);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertThat(movedFilesRepository.getComponentsWithOriginal()).isEmpty();
    verifyStatistics(context, null, null, null, null);
  }

  @Test
  public void execute_detects_no_move_on_first_analysis() {
    prepareAnalysis(BRANCH, null);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertThat(movedFilesRepository.getComponentsWithOriginal()).isEmpty();
    verifyStatistics(context, 0, null, null, null);
  }

  @Test
  public void execute_detects_no_move_if_baseSnapshot_has_no_file_and_report_has_no_file() {
    prepareBranchAnalysis(ANALYSIS);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertThat(movedFilesRepository.getComponentsWithOriginal()).isEmpty();
    assertThat(addedFileRepository.getComponents()).isEmpty();
    verifyStatistics(context, 0, null, null, null);
  }

  @Test
  public void execute_detects_no_move_if_baseSnapshot_has_no_file() {
    prepareBranchAnalysis(ANALYSIS);
    Component file1 = fileComponent(FILE_1_REF, null);
    Component file2 = fileComponent(FILE_2_REF, null);
    setFilesInReport(file1, file2);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertThat(movedFilesRepository.getComponentsWithOriginal()).isEmpty();
    assertThat(addedFileRepository.getComponents()).containsOnly(file1, file2);
    verifyStatistics(context, 2, 0, 2, null);
  }

  @Test
  public void execute_detects_no_move_if_there_is_no_file_in_report() {
    prepareBranchAnalysis(ANALYSIS);
    insertFiles( /* no components */);
    setFilesInReport();

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertThat(movedFilesRepository.getComponentsWithOriginal()).isEmpty();
    assertThat(addedFileRepository.getComponents()).isEmpty();
    verifyStatistics(context, 0, null, null, null);
  }

  @Test
  public void execute_detects_no_move_if_file_key_exists_in_both_DB_and_report() {
    prepareBranchAnalysis(ANALYSIS);
    Component file1 = fileComponent(FILE_1_REF, null);
    Component file2 = fileComponent(FILE_2_REF, null);
    insertFiles(file1.getUuid(), file2.getUuid());
    insertContentOfFileInDb(file1.getUuid(), CONTENT1);
    insertContentOfFileInDb(file2.getUuid(), CONTENT2);
    setFilesInReport(file2, file1);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertThat(movedFilesRepository.getComponentsWithOriginal()).isEmpty();
    assertThat(addedFileRepository.getComponents()).isEmpty();
    verifyStatistics(context, 2, 2, 0, null);
  }

  @Test
  public void execute_detects_move_if_content_of_file_is_same_in_DB_and_report() {
    prepareBranchAnalysis(ANALYSIS);
    Component file1 = fileComponent(FILE_1_REF, null);
    Component file2 = fileComponent(FILE_2_REF, CONTENT1);
    ComponentDto[] dtos = insertFiles(file1.getUuid());
    insertContentOfFileInDb(file1.getUuid(), CONTENT1);
    setFilesInReport(file2);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertThat(movedFilesRepository.getComponentsWithOriginal()).containsExactly(file2);
    MovedFilesRepository.OriginalFile originalFile = movedFilesRepository.getOriginalFile(file2).get();
    assertThat(originalFile.key()).isEqualTo(dtos[0].getKey());
    assertThat(originalFile.uuid()).isEqualTo(dtos[0].uuid());
    assertThat(addedFileRepository.getComponents()).isEmpty();
    verifyStatistics(context, 1, 1, 1, 1);
  }

  @Test
  public void execute_detects_no_move_if_content_of_file_is_not_similar_enough() {
    prepareBranchAnalysis(ANALYSIS);
    Component file1 = fileComponent(FILE_1_REF, null);
    Component file2 = fileComponent(FILE_2_REF, LESS_CONTENT1);
    insertFiles(file1.getKey());
    insertContentOfFileInDb(file1.getKey(), CONTENT1);
    setFilesInReport(file2);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertThat(movedFilesRepository.getComponentsWithOriginal()).isEmpty();
    assertThat(scoreMatrixDumper.scoreMatrix.getMaxScore())
      .isPositive()
      .isLessThan(MIN_REQUIRED_SCORE);
    assertThat(addedFileRepository.getComponents()).contains(file2);
    verifyStatistics(context, 1, 1, 1, 0);
  }

  @Test
  public void execute_detects_no_move_if_content_of_file_is_empty_in_DB() {
    prepareBranchAnalysis(ANALYSIS);
    Component file1 = fileComponent(FILE_1_REF, null);
    Component file2 = fileComponent(FILE_2_REF, CONTENT1);
    insertFiles(file1.getKey());
    insertContentOfFileInDb(file1.getKey(), CONTENT_EMPTY);
    setFilesInReport(file2);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertThat(movedFilesRepository.getComponentsWithOriginal()).isEmpty();
    assertThat(scoreMatrixDumper.scoreMatrix.getMaxScore()).isZero();
    assertThat(addedFileRepository.getComponents()).contains(file2);
    verifyStatistics(context, 1, 1, 1, 0);
  }

  @Test
  public void execute_detects_no_move_if_content_of_file_has_no_path_in_DB() {
    prepareBranchAnalysis(ANALYSIS);
    Component file1 = fileComponent(FILE_1_REF, null);
    Component file2 = fileComponent(FILE_2_REF, CONTENT1);
    insertFiles(key -> newComponentDto(key).setPath(null), file1.getKey());
    insertContentOfFileInDb(file1.getKey(), CONTENT1);
    setFilesInReport(file2);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertThat(movedFilesRepository.getComponentsWithOriginal()).isEmpty();
    assertThat(scoreMatrixDumper.scoreMatrix).isNull();
    assertThat(addedFileRepository.getComponents()).containsOnly(file2);
    verifyStatistics(context, 1, 0, 1, null);
  }

  @Test
  public void execute_detects_no_move_if_content_of_file_is_empty_in_report() {
    prepareBranchAnalysis(ANALYSIS);
    Component file1 = fileComponent(FILE_1_REF, null);
    Component file2 = fileComponent(FILE_2_REF, CONTENT_EMPTY);
    insertFiles(file1.getKey());
    insertContentOfFileInDb(file1.getKey(), CONTENT1);
    setFilesInReport(file2);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertThat(movedFilesRepository.getComponentsWithOriginal()).isEmpty();
    assertThat(scoreMatrixDumper.scoreMatrix.getMaxScore()).isZero();
    assertThat(addedFileRepository.getComponents()).contains(file2);
    verifyStatistics(context, 1, 1, 1, 0);
    assertThat(logTester.logs(Level.DEBUG)).contains("max score in matrix is less than min required score (85). Do nothing.");
  }

  @Test
  public void execute_detects_no_move_if_two_added_files_have_same_content_as_the_one_in_db() {
    prepareBranchAnalysis(ANALYSIS);
    Component file1 = fileComponent(FILE_1_REF, null);
    Component file2 = fileComponent(FILE_2_REF, CONTENT1);
    Component file3 = fileComponent(FILE_3_REF, CONTENT1);
    insertFiles(file1.getKey());
    insertContentOfFileInDb(file1.getKey(), CONTENT1);
    setFilesInReport(file2, file3);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertThat(movedFilesRepository.getComponentsWithOriginal()).isEmpty();
    assertThat(scoreMatrixDumper.scoreMatrix.getMaxScore()).isEqualTo(100);
    assertThat(addedFileRepository.getComponents()).containsOnly(file2, file3);
    verifyStatistics(context, 2, 1, 2, 0);
  }

  @Test
  public void execute_detects_no_move_if_two_deleted_files_have_same_content_as_the_one_added() {
    prepareBranchAnalysis(ANALYSIS);
    Component file1 = fileComponent(FILE_1_REF, null);
    Component file2 = fileComponent(FILE_2_REF, null);
    Component file3 = fileComponent(FILE_3_REF, CONTENT1);
    insertFiles(file1.getUuid(), file2.getUuid());
    insertContentOfFileInDb(file1.getUuid(), CONTENT1);
    insertContentOfFileInDb(file2.getUuid(), CONTENT1);
    setFilesInReport(file3);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertThat(movedFilesRepository.getComponentsWithOriginal()).isEmpty();
    assertThat(scoreMatrixDumper.scoreMatrix.getMaxScore()).isEqualTo(100);
    assertThat(addedFileRepository.getComponents()).containsOnly(file3);
    verifyStatistics(context, 1, 2, 1, 0);
  }

  @Test
  public void execute_detects_no_move_if_two_files_are_empty_in_DB() {
    prepareBranchAnalysis(ANALYSIS);
    Component file1 = fileComponent(FILE_1_REF, null);
    Component file2 = fileComponent(FILE_2_REF, null);
    insertFiles(file1.getUuid(), file2.getUuid());
    insertContentOfFileInDb(file1.getUuid(), null);
    insertContentOfFileInDb(file2.getUuid(), null);
    setFilesInReport(file1, file2);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertThat(movedFilesRepository.getComponentsWithOriginal()).isEmpty();
    assertThat(scoreMatrixDumper.scoreMatrix).isNull();
    assertThat(addedFileRepository.getComponents()).isEmpty();
    verifyStatistics(context, 2, 2, 0, null);
  }

  @Test
  public void execute_detects_several_moves() {
    // testing:
    // - file1 renamed to file3
    // - file2 deleted
    // - file4 untouched
    // - file5 renamed to file6 with a small change
    prepareBranchAnalysis(ANALYSIS);
    Component file1 = fileComponent(FILE_1_REF, null);
    Component file2 = fileComponent(FILE_2_REF, null);
    Component file3 = fileComponent(FILE_3_REF, CONTENT1);
    Component file4 = fileComponent(5, new String[] {"a", "b"});
    Component file5 = fileComponent(6, null);
    Component file6 = fileComponent(7, LESS_CONTENT2);
    ComponentDto[] dtos = insertFiles(file1.getUuid(), file2.getUuid(), file4.getUuid(), file5.getUuid());
    insertContentOfFileInDb(file1.getUuid(), CONTENT1);
    insertContentOfFileInDb(file2.getUuid(), LESS_CONTENT1);
    insertContentOfFileInDb(file4.getUuid(), new String[] {"e", "f", "g", "h", "i"});
    insertContentOfFileInDb(file5.getUuid(), CONTENT2);
    setFilesInReport(file3, file4, file6);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertThat(movedFilesRepository.getComponentsWithOriginal()).containsOnly(file3, file6);
    MovedFilesRepository.OriginalFile originalFile2 = movedFilesRepository.getOriginalFile(file3).get();
    assertThat(originalFile2.key()).isEqualTo(dtos[0].getKey());
    assertThat(originalFile2.uuid()).isEqualTo(dtos[0].uuid());
    MovedFilesRepository.OriginalFile originalFile5 = movedFilesRepository.getOriginalFile(file6).get();
    assertThat(originalFile5.key()).isEqualTo(dtos[3].getKey());
    assertThat(originalFile5.uuid()).isEqualTo(dtos[3].uuid());
    assertThat(scoreMatrixDumper.scoreMatrix.getMaxScore()).isGreaterThan(MIN_REQUIRED_SCORE);
    assertThat(addedFileRepository.getComponents()).isEmpty();
    verifyStatistics(context, 3, 4, 2, 2);
  }

  @Test
  public void execute_does_not_compute_any_distance_if_all_files_sizes_are_all_too_different() {
    prepareBranchAnalysis(ANALYSIS);
    Component file1 = fileComponent(FILE_1_REF, null);
    Component file2 = fileComponent(FILE_2_REF, null);
    Component file3 = fileComponent(FILE_3_REF, arrayOf(118));
    Component file4 = fileComponent(5, arrayOf(25));
    insertFiles(file1.getKey(), file2.getKey());
    insertContentOfFileInDb(file1.getKey(), arrayOf(100));
    insertContentOfFileInDb(file2.getKey(), arrayOf(30));
    setFilesInReport(file3, file4);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertThat(movedFilesRepository.getComponentsWithOriginal()).isEmpty();
    assertThat(scoreMatrixDumper.scoreMatrix.getMaxScore()).isZero();
    verifyStatistics(context, 2, 2, 2, 0);
  }

  /**
   * Creates an array of {@code numberOfElements} int values as String, starting with zero.
   */
  private static String[] arrayOf(int numberOfElements) {
    return IntStream.range(0, numberOfElements).mapToObj(String::valueOf).toArray(String[]::new);
  }

  /**
   * JH: A bug was encountered in the algorithm and I didn't manage to forge a simpler test case.
   */
  @Test
  public void real_life_use_case() throws Exception {
    prepareBranchAnalysis(ANALYSIS);
    for (File f : FileUtils.listFiles(new File("src/it/resources/org/sonar/ce/task/projectanalysis/filemove/FileMoveDetectionStepIT/v1"), null, false)) {
      insertFiles("uuid_" + f.getName().hashCode());
      insertContentOfFileInDb("uuid_" + f.getName().hashCode(), readLines(f));
    }

    Map<String, Component> comps = new HashMap<>();
    int i = 1;
    for (File f : FileUtils.listFiles(new File("src/it/resources/org/sonar/ce/task/projectanalysis/filemove/FileMoveDetectionStepIT/v2"), null, false)) {
      String[] lines = readLines(f);
      Component c = builder(Component.Type.FILE, i++)
        .setUuid("uuid_" + f.getName().hashCode())
        .setKey(f.getName())
        .setName(f.getName())
        .setFileAttributes(new FileAttributes(false, null, lines.length))
        .build();

      comps.put(f.getName(), c);
      setFileLineHashesInReport(c, lines);
    }

    setFilesInReport(comps.values().toArray(new Component[0]));

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    Component makeComponentUuidAndAnalysisUuidNotNullOnDuplicationsIndex = comps.get("MakeComponentUuidAndAnalysisUuidNotNullOnDuplicationsIndex.java");
    Component migrationRb1238 = comps.get("1238_make_component_uuid_and_analysis_uuid_not_null_on_duplications_index.rb");
    Component addComponentUuidAndAnalysisUuidColumnToDuplicationsIndex = comps.get("AddComponentUuidAndAnalysisUuidColumnToDuplicationsIndex.java");
    assertThat(movedFilesRepository.getComponentsWithOriginal()).containsOnly(
      makeComponentUuidAndAnalysisUuidNotNullOnDuplicationsIndex,
      migrationRb1238,
      addComponentUuidAndAnalysisUuidColumnToDuplicationsIndex);

    assertThat(movedFilesRepository.getOriginalFile(makeComponentUuidAndAnalysisUuidNotNullOnDuplicationsIndex).get().uuid())
      .isEqualTo("uuid_" + "MakeComponentUuidNotNullOnDuplicationsIndex.java".hashCode());
    assertThat(movedFilesRepository.getOriginalFile(migrationRb1238).get().uuid())
      .isEqualTo("uuid_" + "1242_make_analysis_uuid_not_null_on_duplications_index.rb".hashCode());
    assertThat(movedFilesRepository.getOriginalFile(addComponentUuidAndAnalysisUuidColumnToDuplicationsIndex).get().uuid())
      .isEqualTo("uuid_" + "AddComponentUuidColumnToDuplicationsIndex.java".hashCode());
    verifyStatistics(context, comps.values().size(), 12, 6, 3);
  }

  private String[] readLines(File filename) throws IOException {
    return FileUtils
      .readLines(filename, StandardCharsets.UTF_8)
      .toArray(new String[0]);
  }

  @CheckForNull
  private FileSourceDto insertContentOfFileInDb(String uuid, @Nullable String[] content) {
    return dbTester.getDbClient().componentDao().selectByUuid(dbTester.getSession(), uuid)
      .map(file -> {
        SourceLineHashesComputer linesHashesComputer = new SourceLineHashesComputer();
        if (content != null) {
          stream(content).forEach(linesHashesComputer::addLine);
        }
        FileSourceDto fileSourceDto = new FileSourceDto()
          .setUuid(Uuids.createFast())
          .setFileUuid(file.uuid())
          .setProjectUuid(file.branchUuid())
          .setLineHashes(linesHashesComputer.getLineHashes());
        dbTester.getDbClient().fileSourceDao().insert(dbTester.getSession(), fileSourceDto);
        dbTester.commit();
        return fileSourceDto;
      }).orElse(null);
  }

  private void setFilesInReport(Component... files) {
    treeRootHolder.setRoot(builder(Component.Type.PROJECT, ROOT_REF)
      .setUuid(project.uuid())
      .addChildren(files)
      .build());
  }

  private ComponentDto[] insertFiles(String... uuids) {
    return insertFiles(this::newComponentDto, uuids);
  }

  private ComponentDto[] insertFiles(Function<String, ComponentDto> newComponentDto, String... uuids) {
    return stream(uuids)
      .map(newComponentDto)
      .map(dto -> dbTester.components().insertComponent(dto))
      .toArray(ComponentDto[]::new);
  }

  private ComponentDto newComponentDto(String uuid) {
    return ComponentTesting.newFileDto(project)
      .setKey("key_" + uuid)
      .setUuid(uuid)
      .setPath("path_" + uuid);
  }

  private Component fileComponent(int ref, @Nullable String[] content) {
    ReportComponent component = builder(Component.Type.FILE, ref)
      .setName("report_path" + ref)
      .setFileAttributes(new FileAttributes(false, null, content == null ? 1 : content.length))
      .build();
    if (content != null) {
      setFileLineHashesInReport(component, content);
    }
    return component;
  }

  private void setFileLineHashesInReport(Component file, String[] content) {
    SourceLineHashesComputer computer = new SourceLineHashesComputer();
    for (String line : content) {
      computer.addLine(line);
    }
    when(sourceLinesHash.getLineHashesMatchingDBVersion(file)).thenReturn(computer.getLineHashes());
  }

  private static class CapturingScoreMatrixDumper implements ScoreMatrixDumper {
    private ScoreMatrix scoreMatrix;

    @Override
    public void dumpAsCsv(ScoreMatrix scoreMatrix) {
      this.scoreMatrix = scoreMatrix;
    }
  }

  private void prepareBranchAnalysis(Analysis analysis) {
    prepareAnalysis(BRANCH, analysis);
  }

  private void prepareAnalysis(BranchType branch, Analysis analysis) {
    mockBranchType(branch);
    analysisMetadataHolder.setBaseAnalysis(analysis);
  }

  private void mockBranchType(BranchType branchType) {
    when(analysisMetadataHolder.isPullRequest()).thenReturn(branchType == PULL_REQUEST);
  }

  public static void verifyStatistics(TestComputationStepContext context,
    @Nullable Integer expectedReportFiles, @Nullable Integer expectedDbFiles,
    @Nullable Integer expectedAddedFiles, @Nullable Integer expectedMovedFiles) {
    context.getStatistics().assertValue("reportFiles", expectedReportFiles);
    context.getStatistics().assertValue("dbFiles", expectedDbFiles);
    context.getStatistics().assertValue("addedFiles", expectedAddedFiles);
    context.getStatistics().assertValue("movedFiles", expectedMovedFiles);
  }

  public static class RecordingMutableAddedFileRepository implements MutableAddedFileRepository {
    private final List<Component> components = new ArrayList<>();

    @Override
    public void register(Component file) {
      components.add(file);
    }

    @Override
    public boolean isAdded(Component component) {
      throw new UnsupportedOperationException("isAdded should not be called");
    }

    public List<Component> getComponents() {
      return components;
    }
  }
}
