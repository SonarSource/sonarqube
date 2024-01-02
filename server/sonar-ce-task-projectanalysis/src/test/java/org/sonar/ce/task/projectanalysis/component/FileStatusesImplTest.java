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
package org.sonar.ce.task.projectanalysis.component;

import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.analysis.Analysis;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.source.SourceHashRepository;
import org.sonar.db.source.FileHashesDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FileStatusesImplTest {
  private static final String PROJECT_KEY = "PROJECT_KEY";
  private static final String PROJECT_UUID = "UUID-1234";

  @Rule
  public final TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  private final PreviousSourceHashRepository previousSourceHashRepository = mock(PreviousSourceHashRepository.class);
  private final SourceHashRepository sourceHashRepository = mock(SourceHashRepository.class);
  @Rule
  public final AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();
  private final FileStatusesImpl fileStatuses = new FileStatusesImpl(analysisMetadataHolder, treeRootHolder, previousSourceHashRepository, sourceHashRepository);

  @Before
  public void before() {
    analysisMetadataHolder.setBaseAnalysis(new Analysis.Builder().setUuid(PROJECT_UUID).setCreatedAt(1000L).build());
  }

  @Test
  public void file_is_unchanged_only_if_status_is_SAME_and_hashes_equal() {
    Component file1 = ReportComponent.builder(Component.Type.FILE, 2, "FILE1_KEY").setStatus(Component.Status.SAME).build();
    Component file2 = ReportComponent.builder(Component.Type.FILE, 3, "FILE2_KEY").setStatus(Component.Status.SAME).build();
    Component file3 = ReportComponent.builder(Component.Type.FILE, 4, "FILE3_KEY").setStatus(Component.Status.CHANGED).build();

    addDbFileHash(file1, "hash1");
    addDbFileHash(file2, "different");
    addDbFileHash(file3, "hash3");

    addReportFileHash(file1, "hash1");
    addReportFileHash(file2, "hash2");
    addReportFileHash(file3, "hash3");

    Component project = ReportComponent.builder(Component.Type.PROJECT, 1)
      .setUuid(PROJECT_UUID)
      .setKey(PROJECT_KEY)
      .addChildren(file1, file2, file3)
      .build();
    treeRootHolder.setRoot(project);
  fileStatuses.initialize();
    assertThat(fileStatuses.isUnchanged(file1)).isTrue();
    assertThat(fileStatuses.isUnchanged(file2)).isFalse();
    assertThat(fileStatuses.isUnchanged(file2)).isFalse();
  }

  @Test
  public void isDataUnchanged_returns_false_if_any_SAME_status_is_incorrect() {
    Component file1 = ReportComponent.builder(Component.Type.FILE, 2, "FILE1_KEY").setStatus(Component.Status.SAME)
      .setFileAttributes(new FileAttributes(false, null, 10, true, null)).build();
    Component file2 = ReportComponent.builder(Component.Type.FILE, 3, "FILE2_KEY").setStatus(Component.Status.SAME).build();

    addDbFileHash(file1, "hash1");
    addDbFileHash(file2, "different");

    addReportFileHash(file1, "hash1");
    addReportFileHash(file2, "hash2");

    Component project = ReportComponent.builder(Component.Type.PROJECT, 1)
      .setUuid(PROJECT_UUID)
      .setKey(PROJECT_KEY)
      .addChildren(file1, file2)
      .build();
    treeRootHolder.setRoot(project);
    fileStatuses.initialize();
    assertThat(fileStatuses.isDataUnchanged(file1)).isFalse();
    assertThat(fileStatuses.isDataUnchanged(file2)).isFalse();
  }

  @Test
  public void isDataUnchanged_returns_false_no_previous_analysis() {
    analysisMetadataHolder.setBaseAnalysis(null);

    Component file1 = ReportComponent.builder(Component.Type.FILE, 2, "FILE1_KEY").setStatus(Component.Status.SAME)
      .setFileAttributes(new FileAttributes(false, null, 10, true, null)).build();
    Component file2 = ReportComponent.builder(Component.Type.FILE, 3, "FILE2_KEY").setStatus(Component.Status.SAME).build();

    addReportFileHash(file1, "hash1");
    addReportFileHash(file2, "hash2");

    Component project = ReportComponent.builder(Component.Type.PROJECT, 1)
      .setUuid(PROJECT_UUID)
      .setKey(PROJECT_KEY)
      .addChildren(file1, file2)
      .build();
    treeRootHolder.setRoot(project);
    fileStatuses.initialize();

    assertThat(fileStatuses.isDataUnchanged(file1)).isFalse();
    assertThat(fileStatuses.isDataUnchanged(file2)).isFalse();
  }

  @Test
  public void isDataUnchanged_returns_false_if_not_set_by_analyzer() {
    Component file1 = ReportComponent.builder(Component.Type.FILE, 2, "FILE1_KEY").setStatus(Component.Status.SAME)
      .setFileAttributes(new FileAttributes(false, null, 10, false,null)).build();
    Component file2 = ReportComponent.builder(Component.Type.FILE, 3, "FILE2_KEY").setStatus(Component.Status.SAME).build();

    addDbFileHash(file1, "hash1");
    addDbFileHash(file2, "hash2");

    addReportFileHash(file1, "hash1");
    addReportFileHash(file2, "hash2");

    Component project = ReportComponent.builder(Component.Type.PROJECT, 1)
      .setUuid(PROJECT_UUID)
      .setKey(PROJECT_KEY)
      .addChildren(file1, file2)
      .build();
    treeRootHolder.setRoot(project);
    fileStatuses.initialize();
    assertThat(fileStatuses.isDataUnchanged(file1)).isFalse();
    assertThat(fileStatuses.isDataUnchanged(file2)).isFalse();
  }

  @Test
  public void isDataUnchanged_returns_true_if_set_by_analyzer_and_all_SAME_status_are_correct() {
    Component file1 = ReportComponent.builder(Component.Type.FILE, 2, "FILE1_KEY").setStatus(Component.Status.SAME)
      .setFileAttributes(new FileAttributes(false, null, 10, true,null)).build();
    Component file2 = ReportComponent.builder(Component.Type.FILE, 3, "FILE2_KEY").setStatus(Component.Status.SAME).build();
    Component file3 = ReportComponent.builder(Component.Type.FILE, 4, "FILE3_KEY").setStatus(Component.Status.CHANGED).build();

    addDbFileHash(file1, "hash1");
    addDbFileHash(file2, "hash2");
    addDbFileHash(file3, "hash3");

    addReportFileHash(file1, "hash1");
    addReportFileHash(file2, "hash2");
    addReportFileHash(file3, "different");

    Component project = ReportComponent.builder(Component.Type.PROJECT, 1)
      .setUuid(PROJECT_UUID)
      .setKey(PROJECT_KEY)
      .addChildren(file1, file2, file3)
      .build();
    treeRootHolder.setRoot(project);
    fileStatuses.initialize();
    assertThat(fileStatuses.isDataUnchanged(file1)).isTrue();
    assertThat(fileStatuses.isDataUnchanged(file2)).isFalse();

    verify(previousSourceHashRepository).getDbFile(file1);
  }

  @Test
  public void getFileUuidsMarkedAsUnchanged_whenNotInitialized_shouldFail() {
    assertThatThrownBy(fileStatuses::getFileUuidsMarkedAsUnchanged)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Not initialized");
  }

  @Test
  public void getFileUuidsMarkedAsUnchanged_shouldReturnMarkAsUnchangedFileUuids() {
    Component file1 = ReportComponent.builder(Component.Type.FILE, 2, "FILE1_KEY").setStatus(Component.Status.SAME)
      .setFileAttributes(new FileAttributes(false, null, 10, true, null)).build();
    Component file2 = ReportComponent.builder(Component.Type.FILE, 3, "FILE2_KEY").setStatus(Component.Status.SAME).build();
    addDbFileHash(file1, "hash1");
    addDbFileHash(file2, "hash2");
    addReportFileHash(file1, "hash1");
    addReportFileHash(file2, "hash2");
    Component project = ReportComponent.builder(Component.Type.PROJECT, 1)
      .setUuid(PROJECT_UUID)
      .setKey(PROJECT_KEY)
      .addChildren(file1, file2)
      .build();
    treeRootHolder.setRoot(project);
    fileStatuses.initialize();

    assertThat(fileStatuses.getFileUuidsMarkedAsUnchanged()).contains(file1.getUuid());
  }

  private void addDbFileHash(Component file, String hash) {
    FileHashesDto fileHashesDto = new FileHashesDto().setSrcHash(hash);
    when(previousSourceHashRepository.getDbFile(file)).thenReturn(Optional.of(fileHashesDto));
  }

  private void addReportFileHash(Component file, String hash) {
    when(sourceHashRepository.getRawSourceHash(file)).thenReturn(hash);
  }
}
