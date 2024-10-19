/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.ce.task.projectanalysis.analysis.Analysis;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.FileAttributes;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.filemove.FileMoveDetectionStepIT.RecordingMutableAddedFileRepository;
import org.sonar.ce.task.projectanalysis.filemove.MovedFilesRepository.OriginalFile;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ProjectData;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.source.FileSourceDto;
import org.sonar.server.project.Project;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.ce.task.projectanalysis.filemove.FileMoveDetectionStepIT.verifyStatistics;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.db.component.BranchType.PULL_REQUEST;

public class PullRequestFileMoveDetectionStepIT {
  private static final String ROOT_REF = "0";
  private static final String FILE_1_REF = "1";
  private static final String FILE_2_REF = "2";
  private static final String FILE_3_REF = "3";
  private static final String FILE_4_REF = "4";
  private static final String FILE_5_REF = "5";
  private static final String FILE_6_REF = "6";
  private static final String FILE_7_REF = "7";
  private static final String TARGET_BRANCH = "target_branch";
  private static final String BRANCH_UUID = "branch_uuid";
  private static final String SNAPSHOT_UUID = "uuid_1";

  private static final Analysis ANALYSIS = new Analysis.Builder()
    .setUuid(SNAPSHOT_UUID)
    .setCreatedAt(86521)
    .build();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public MutableMovedFilesRepositoryRule movedFilesRepository = new MutableMovedFilesRepositoryRule();
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public LogTester logTester = new LogTester();

  private ComponentDto branch;
  private ComponentDto mainBranch;
  private ProjectDto project;

  private final DbClient dbClient = dbTester.getDbClient();
  private final AnalysisMetadataHolderRule analysisMetadataHolder = mock(AnalysisMetadataHolderRule.class);
  private final RecordingMutableAddedFileRepository addedFileRepository = new RecordingMutableAddedFileRepository();
  private final PullRequestFileMoveDetectionStep underTest = new PullRequestFileMoveDetectionStep(analysisMetadataHolder, treeRootHolder, dbClient, movedFilesRepository, addedFileRepository);

  @Before
  public void setUp() throws Exception {
    ProjectData projectData = dbTester.components().insertPrivateProject();
    mainBranch = projectData.getMainBranchComponent();
    project = projectData.getProjectDto();
    branch = dbTester.components().insertProjectBranch(mainBranch, branchDto -> branchDto.setUuid(BRANCH_UUID).setKey(TARGET_BRANCH));
    treeRootHolder.setRoot(builder(Component.Type.PROJECT, Integer.parseInt(ROOT_REF)).setUuid(mainBranch.uuid()).build());
  }

  @Test
  public void getDescription_returns_description() {
    assertThat(underTest.getDescription()).isEqualTo("Detect file moves in Pull Request scope");
  }

  @Test
  public void execute_does_not_detect_any_files_if_not_in_pull_request_scope() {
    prepareAnalysis(BRANCH, ANALYSIS);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertThat(movedFilesRepository.getComponentsWithOriginal()).isEmpty();
    verifyStatistics(context, null, null, null, null);
  }

  @Test
  public void execute_detects_no_move_if_report_has_no_file() {
    preparePullRequestAnalysis(ANALYSIS);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertThat(movedFilesRepository.getComponentsWithOriginal()).isEmpty();
    assertThat(addedFileRepository.getComponents()).isEmpty();
    verifyStatistics(context, 0, null, null, null);
  }

  @Test
  public void execute_detects_no_move_if_target_branch_has_no_files() {
    preparePullRequestAnalysis(ANALYSIS);
    Set<FileReference> fileReferences = Set.of(FileReference.of(FILE_1_REF), FileReference.of(FILE_2_REF));
    Map<String, Component> reportFilesByUuid = initializeAnalysisReportComponents(fileReferences);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertThat(movedFilesRepository.getComponentsWithOriginal()).isEmpty();
    assertThat(addedFileRepository.getComponents()).containsOnlyOnceElementsOf(reportFilesByUuid.values());
    verifyStatistics(context, 2, 0, 2, null);
  }

  @Test
  public void execute_detects_no_move_if_there_are_no_files_in_report() {
    preparePullRequestAnalysis(ANALYSIS);
    initializeAnalysisReportComponents(Set.of());

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertThat(movedFilesRepository.getComponentsWithOriginal()).isEmpty();
    assertThat(addedFileRepository.getComponents()).isEmpty();
    verifyStatistics(context, 0, null, null, null);
  }

  @Test
  public void execute_detects_no_move_if_file_key_exists_in_both_database_and_report() {
    preparePullRequestAnalysis(ANALYSIS);

    Set<FileReference> fileReferences = Set.of(FileReference.of(FILE_1_REF), FileReference.of(FILE_2_REF));
    initializeAnalysisReportComponents(fileReferences);
    initializeTargetBranchDatabaseComponents(fileReferences);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertThat(movedFilesRepository.getComponentsWithOriginal()).isEmpty();
    assertThat(addedFileRepository.getComponents()).isEmpty();
    verifyStatistics(context, 2, 2, 0, 0);
  }

  @Test
  public void execute_detects_renamed_file() {
    // - FILE_1_REF on target branch is renamed to FILE_2_REF on Pull Request
    preparePullRequestAnalysis(ANALYSIS);

    Set<FileReference> reportFileReferences = Set.of(FileReference.of(FILE_2_REF, FILE_1_REF));
    Set<FileReference> databaseFileReferences = Set.of(FileReference.of(FILE_1_REF));

    Map<String, Component> reportFilesByUuid = initializeAnalysisReportComponents(reportFileReferences);
    Map<String, Component> databaseFilesByUuid = initializeTargetBranchDatabaseComponents(databaseFileReferences);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertThat(addedFileRepository.getComponents()).isEmpty();
    assertThat(movedFilesRepository.getComponentsWithOriginal()).hasSize(1);
    assertThatFileRenameHasBeenDetected(reportFilesByUuid, databaseFilesByUuid, FILE_2_REF, FILE_1_REF);
    verifyStatistics(context, 1, 1, 0, 1);
  }

  @Test
  public void execute_detects_several_renamed_file() {
    // - FILE_1_REF has been renamed to FILE_3_REF on Pull Request
    // - FILE_2_REF has been deleted on Pull Request
    // - FILE_4_REF has been left untouched
    // - FILE_5_REF has been renamed to FILE_6_REF on Pull Request
    // - FILE_7_REF has been added on Pull Request
    preparePullRequestAnalysis(ANALYSIS);

    Set<FileReference> reportFileReferences = Set.of(
      FileReference.of(FILE_3_REF, FILE_1_REF),
      FileReference.of(FILE_4_REF),
      FileReference.of(FILE_6_REF, FILE_5_REF),
      FileReference.of(FILE_7_REF));

    Set<FileReference> databaseFileReferences = Set.of(
      FileReference.of(FILE_1_REF),
      FileReference.of(FILE_2_REF),
      FileReference.of(FILE_4_REF),
      FileReference.of(FILE_5_REF));

    Map<String, Component> reportFilesByUuid = initializeAnalysisReportComponents(reportFileReferences);
    Map<String, Component> databaseFilesByUuid = initializeTargetBranchDatabaseComponents(databaseFileReferences);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertThat(addedFileRepository.getComponents()).hasSize(1);
    assertThat(movedFilesRepository.getComponentsWithOriginal()).hasSize(2);
    assertThatFileAdditionHasBeenDetected(reportFilesByUuid, FILE_7_REF);
    assertThatFileRenameHasBeenDetected(reportFilesByUuid, databaseFilesByUuid, FILE_3_REF, FILE_1_REF);
    assertThatFileRenameHasBeenDetected(reportFilesByUuid, databaseFilesByUuid, FILE_6_REF, FILE_5_REF);
    verifyStatistics(context, 4, 4, 1, 2);
  }

  private void assertThatFileAdditionHasBeenDetected(Map<String, Component> reportFilesByUuid, String fileInReportReference) {
    Component fileInReport = reportFilesByUuid.get(fileInReportReference);

    assertThat(addedFileRepository.getComponents()).contains(fileInReport);
    assertThat(movedFilesRepository.getOriginalPullRequestFile(fileInReport)).isEmpty();
  }


  private void assertThatFileRenameHasBeenDetected(Map<String, Component> reportFilesByUuid, Map<String, Component> databaseFilesByUuid, String fileInReportReference, String originalFileInDatabaseReference) {
    Component fileInReport = reportFilesByUuid.get(fileInReportReference);
    Component originalFileInDatabase = databaseFilesByUuid.get(originalFileInDatabaseReference);

    assertThat(movedFilesRepository.getComponentsWithOriginal()).contains(fileInReport);
    assertThat(movedFilesRepository.getOriginalPullRequestFile(fileInReport)).isPresent();

    OriginalFile detectedOriginalFile = movedFilesRepository.getOriginalPullRequestFile(fileInReport).get();
    assertThat(detectedOriginalFile.key()).isEqualTo(originalFileInDatabase.getKey());
    assertThat(detectedOriginalFile.uuid()).isEqualTo(originalFileInDatabase.getUuid());
  }

  private Map<String, Component> initializeTargetBranchDatabaseComponents(Set<FileReference> references) {
    Set<Component> fileComponents = createFileComponents(references);
    insertFileComponentsInDatabase(fileComponents);
    return toFileComponentsByUuidMap(fileComponents);
  }

  private Map<String, Component> initializeAnalysisReportComponents(Set<FileReference> refs) {
    Set<Component> fileComponents = createFileComponents(refs);
    insertFileComponentsInReport(fileComponents);
    return toFileComponentsByUuidMap(fileComponents);
  }

  private Map<String, Component> toFileComponentsByUuidMap(Set<Component> fileComponents) {
    return fileComponents
      .stream()
      .collect(toMap(Component::getUuid, identity()));
  }

  private static Set<Component> createFileComponents(Set<FileReference> references) {
    return references
      .stream()
      .map(PullRequestFileMoveDetectionStepIT::createReportFileComponent)
      .collect(toSet());
  }

  private static Component createReportFileComponent(FileReference fileReference) {
    return builder(FILE, Integer.parseInt(fileReference.getReference()))
      .setUuid(fileReference.getReference())
      .setName("report_path" + fileReference.getReference())
      .setFileAttributes(new FileAttributes(false, null, 1, false, composeComponentPath(fileReference.getPastReference())))
      .build();
  }

  private void insertFileComponentsInReport(Set<Component> files) {
    treeRootHolder
      .setRoot(builder(PROJECT, Integer.parseInt(ROOT_REF))
      .setUuid(mainBranch.uuid())
      .addChildren(files.toArray(Component[]::new))
      .build());
  }

  private Set<ComponentDto> insertFileComponentsInDatabase(Set<Component> files) {
    return files
      .stream()
      .map(Component::getUuid)
      .map(this::composeComponentDto)
      .peek(this::insertComponentDto)
      .peek(this::insertContentOfFileInDatabase)
      .collect(toSet());
  }

  private void insertComponentDto(ComponentDto component) {
    dbTester.components().insertComponent(component);
  }

  private ComponentDto composeComponentDto(String uuid) {
    return ComponentTesting
      .newFileDto(mainBranch)
      .setBranchUuid(branch.uuid())
      .setKey("key_" + uuid)
      .setUuid(uuid)
      .setPath(composeComponentPath(uuid));
  }

  @CheckForNull
  private static String composeComponentPath(@Nullable String reference) {
    return Optional.ofNullable(reference)
      .map(r -> String.join("_", "path", r))
      .orElse(null);
  }

  private FileSourceDto insertContentOfFileInDatabase(ComponentDto file) {
    FileSourceDto fileSourceDto = composeFileSourceDto(file);
    persistFileSourceDto(fileSourceDto);
    return fileSourceDto;
  }

  private static FileSourceDto composeFileSourceDto(ComponentDto file) {
    return new FileSourceDto()
      .setUuid(Uuids.createFast())
      .setFileUuid(file.uuid())
      .setProjectUuid(file.branchUuid());
  }

  private void persistFileSourceDto(FileSourceDto fileSourceDto) {
    dbTester.getDbClient().fileSourceDao().insert(dbTester.getSession(), fileSourceDto);
    dbTester.commit();
  }

  private void preparePullRequestAnalysis(Analysis analysis) {
    prepareAnalysis(PULL_REQUEST, analysis);
  }

  private void prepareAnalysis(BranchType branch, Analysis analysis) {
    mockBranchType(branch);
    analysisMetadataHolder.setBaseAnalysis(analysis);
  }

  private void mockBranchType(BranchType branchType) {
    Branch branch = mock(Branch.class);
    when(analysisMetadataHolder.getBranch()).thenReturn(branch);
    when(analysisMetadataHolder.getBranch().getTargetBranchName()).thenReturn(TARGET_BRANCH);
    when(analysisMetadataHolder.isPullRequest()).thenReturn(branchType == PULL_REQUEST);
    when(analysisMetadataHolder.getProject()).thenReturn(Project.from(project));
  }

  @Immutable
  private static class FileReference {
    private final String reference;
    private final String pastReference;

    private FileReference(String reference, @Nullable String pastReference) {
      this.reference = reference;
      this.pastReference = pastReference;
    }

    public String getReference() {
      return reference;
    }

    @CheckForNull
    public String getPastReference() {
      return pastReference;
    }

    public static FileReference of(String reference, String pastReference) {
      return new FileReference(reference, pastReference);
    }

    public static FileReference of(String reference) {
      return new FileReference(reference, null);
    }
  }
}
