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
package org.sonar.ce.task.projectanalysis.scm;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.ce.task.projectanalysis.analysis.Analysis;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReferenceBranchComponentUuids;
import org.sonar.ce.task.projectanalysis.filemove.MutableMovedFilesRepositoryRule;
import org.sonar.ce.task.projectanalysis.period.NewCodeReferenceBranchComponentUuids;
import org.sonar.ce.task.projectanalysis.period.Period;
import org.sonar.ce.task.projectanalysis.period.PeriodHolderRule;
import org.sonar.core.hash.SourceHashComputer;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.source.FileSourceDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.log.LoggerLevel.TRACE;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;

public class ScmInfoDbLoaderTest {
  static final int FILE_REF = 1;
  static final Component FILE = builder(Component.Type.FILE, FILE_REF).setKey("FILE_KEY").setUuid("FILE_UUID").build();
  static final long DATE_1 = 123456789L;

  static Analysis baseProjectAnalysis = new Analysis.Builder()
    .setUuid("uuid_1")
    .setCreatedAt(123456789L)
    .build();

  @Rule
  public LogTester logTester = new LogTester();
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();
  @Rule
  public MutableMovedFilesRepositoryRule movedFiles = new MutableMovedFilesRepositoryRule();
  @Rule
  public PeriodHolderRule periodHolder = new PeriodHolderRule();

  private final Branch branch = mock(Branch.class);
  private final ReferenceBranchComponentUuids referenceBranchComponentUuids = mock(ReferenceBranchComponentUuids.class);
  private final NewCodeReferenceBranchComponentUuids newCodeReferenceBranchComponentUuids = mock(NewCodeReferenceBranchComponentUuids.class);

  private final ScmInfoDbLoader underTest = new ScmInfoDbLoader(analysisMetadataHolder, movedFiles, dbTester.getDbClient(), referenceBranchComponentUuids,
      newCodeReferenceBranchComponentUuids, periodHolder);

  @Before
  public void before() {
    periodHolder.setPeriod(new Period(NewCodePeriodType.PREVIOUS_VERSION.name(), null, null));
  }

  @Test
  public void returns_ScmInfo_from_DB() {
    analysisMetadataHolder.setBaseAnalysis(baseProjectAnalysis);
    analysisMetadataHolder.setBranch(null);

    String hash = computeSourceHash(1);
    addFileSourceInDb("henry", DATE_1, "rev-1", hash);

    DbScmInfo scmInfo = underTest.getScmInfo(FILE).get();
    assertThat(scmInfo.getAllChangesets()).hasSize(1);
    assertThat(scmInfo.fileHash()).isEqualTo(hash);

    assertThat(logTester.logs(TRACE)).containsOnly("Reading SCM info from DB for file 'FILE_UUID'");
  }

  @Test
  public void read_from_reference_branch_if_no_base() {
    analysisMetadataHolder.setBaseAnalysis(null);
    analysisMetadataHolder.setBranch(branch);

    String referenceFileUuid = "referenceFileUuid";
    String hash = computeSourceHash(1);

    when(referenceBranchComponentUuids.getComponentUuid(FILE.getKey())).thenReturn(referenceFileUuid);
    addFileSourceInDb("henry", DATE_1, "rev-1", hash, referenceFileUuid);

    DbScmInfo scmInfo = underTest.getScmInfo(FILE).get();
    assertThat(scmInfo.getAllChangesets()).hasSize(1);
    assertThat(scmInfo.fileHash()).isEqualTo(hash);
    assertThat(logTester.logs(TRACE)).containsOnly("Reading SCM info from DB for file 'referenceFileUuid'");
  }

  @Test
  public void read_from_target_if_pullrequest() {
    Branch branch = mock(Branch.class);
    when(branch.getType()).thenReturn(BranchType.PULL_REQUEST);
    analysisMetadataHolder.setBaseAnalysis(null);
    analysisMetadataHolder.setBranch(branch);

    String targetBranchFileUuid = "targetBranchFileUuid";
    String hash = computeSourceHash(1);

    when(referenceBranchComponentUuids.getComponentUuid(FILE.getKey())).thenReturn(targetBranchFileUuid);
    addFileSourceInDb("henry", DATE_1, "rev-1", hash, targetBranchFileUuid);

    DbScmInfo scmInfo = underTest.getScmInfo(FILE).get();
    assertThat(scmInfo.getAllChangesets()).hasSize(1);
    assertThat(scmInfo.fileHash()).isEqualTo(hash);
    assertThat(logTester.logs(TRACE)).containsOnly("Reading SCM info from DB for file 'targetBranchFileUuid'");
  }

  @Test
  public void read_from_target_if_reference_branch() {
    periodHolder.setPeriod(new Period(NewCodePeriodType.REFERENCE_BRANCH.name(), null, null));

    Branch branch = mock(Branch.class);
    when(branch.getType()).thenReturn(BranchType.BRANCH);
    analysisMetadataHolder.setBaseAnalysis(null);
    analysisMetadataHolder.setBranch(branch);

    String targetBranchFileUuid = "targetBranchFileUuid";
    String hash = computeSourceHash(1);

    when(newCodeReferenceBranchComponentUuids.getComponentUuid(FILE.getKey())).thenReturn(targetBranchFileUuid);
    addFileSourceInDb("henry", DATE_1, "rev-1", hash, targetBranchFileUuid);

    DbScmInfo scmInfo = underTest.getScmInfo(FILE).get();
    assertThat(scmInfo.getAllChangesets()).hasSize(1);
    assertThat(scmInfo.fileHash()).isEqualTo(hash);
    assertThat(logTester.logs(TRACE)).containsOnly("Reading SCM info from DB for file 'targetBranchFileUuid'");
  }

  @Test
  public void read_from_db_if_not_exist_in_reference_branch() {
    periodHolder.setPeriod(new Period(NewCodePeriodType.REFERENCE_BRANCH.name(), null, null));

    Branch branch = mock(Branch.class);
    when(branch.getType()).thenReturn(BranchType.BRANCH);
    analysisMetadataHolder.setBaseAnalysis(null);
    analysisMetadataHolder.setBranch(branch);

    String hash = computeSourceHash(1);

    addFileSourceInDb("henry", DATE_1, "rev-1", hash, FILE.getUuid());

    DbScmInfo scmInfo = underTest.getScmInfo(FILE).get();
    assertThat(scmInfo.getAllChangesets()).hasSize(1);
    assertThat(scmInfo.fileHash()).isEqualTo(hash);
    assertThat(logTester.logs(TRACE)).containsOnly("Reading SCM info from DB for file 'FILE_UUID'");
  }

  @Test
  public void return_empty_if_no_dto_available() {
    analysisMetadataHolder.setBaseAnalysis(baseProjectAnalysis);
    analysisMetadataHolder.setBranch(null);

    Optional<DbScmInfo> scmInfo = underTest.getScmInfo(FILE);

    assertThat(logTester.logs(TRACE)).containsOnly("Reading SCM info from DB for file 'FILE_UUID'");
    assertThat(scmInfo).isEmpty();
  }

  @Test
  public void do_not_read_from_db_on_first_analysis_if_there_is_no_reference_branch() {
    Branch branch = mock(Branch.class);
    when(branch.getType()).thenReturn(BranchType.PULL_REQUEST);
    analysisMetadataHolder.setBaseAnalysis(null);
    analysisMetadataHolder.setBranch(branch);

    assertThat(underTest.getScmInfo(FILE)).isEmpty();
    assertThat(logTester.logs(TRACE)).isEmpty();
  }

  private static List<String> generateLines(int lineCount) {
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    for (int i = 0; i < lineCount; i++) {
      builder.add("line " + i);
    }
    return builder.build();
  }

  private static String computeSourceHash(int lineCount) {
    SourceHashComputer sourceHashComputer = new SourceHashComputer();
    Iterator<String> lines = generateLines(lineCount).iterator();
    while (lines.hasNext()) {
      sourceHashComputer.addLine(lines.next(), lines.hasNext());
    }
    return sourceHashComputer.getHash();
  }

  private void addFileSourceInDb(@Nullable String author, @Nullable Long date, @Nullable String revision, String srcHash) {
    addFileSourceInDb(author, date, revision, srcHash, FILE.getUuid());
  }

  private void addFileSourceInDb(@Nullable String author, @Nullable Long date, @Nullable String revision, String srcHash, String fileUuid) {
    DbFileSources.Data.Builder fileDataBuilder = DbFileSources.Data.newBuilder();
    DbFileSources.Line.Builder builder = fileDataBuilder.addLinesBuilder()
      .setLine(1);
    if (author != null) {
      builder.setScmAuthor(author);
    }
    if (date != null) {
      builder.setScmDate(date);
    }
    if (revision != null) {
      builder.setScmRevision(revision);
    }
    dbTester.getDbClient().fileSourceDao().insert(dbTester.getSession(), new FileSourceDto()
      .setUuid(Uuids.createFast())
      .setLineHashes(Collections.singletonList("lineHash"))
      .setFileUuid(fileUuid)
      .setProjectUuid("PROJECT_UUID")
      .setSourceData(fileDataBuilder.build())
      .setSrcHash(srcHash));
    dbTester.commit();
  }

}
