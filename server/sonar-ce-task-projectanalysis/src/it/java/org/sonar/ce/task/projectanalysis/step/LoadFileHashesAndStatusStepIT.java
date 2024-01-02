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
package org.sonar.ce.task.projectanalysis.step;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.FileStatusesImpl;
import org.sonar.ce.task.projectanalysis.component.PreviousSourceHashRepositoryImpl;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.source.FileHashesDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LoadFileHashesAndStatusStepIT {
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  public PreviousSourceHashRepositoryImpl previousFileHashesRepository = new PreviousSourceHashRepositoryImpl();
  public FileStatusesImpl fileStatuses = mock(FileStatusesImpl.class);
  public AnalysisMetadataHolder analysisMetadataHolder = mock(AnalysisMetadataHolder.class);

  private final LoadFileHashesAndStatusStep underTest = new LoadFileHashesAndStatusStep(db.getDbClient(), previousFileHashesRepository, fileStatuses,
    db.getDbClient().fileSourceDao(), treeRootHolder);

  @Before
  public void before() {
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
  }

  @Test
  public void initialized_file_statuses() {
    Component project = ReportComponent.builder(Component.Type.PROJECT, 1, "project").build();
    treeRootHolder.setRoot(project);
    underTest.execute(new TestComputationStepContext());
    verify(fileStatuses).initialize();
  }

  @Test
  public void loads_file_hashes_for_project_branch() {
    ComponentDto project1 = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto project2 = db.components().insertPublicProject().getMainBranchComponent();

    ComponentDto dbFile1 = db.components().insertComponent(ComponentTesting.newFileDto(project1));
    ComponentDto dbFile2 = db.components().insertComponent(ComponentTesting.newFileDto(project1));

    insertFileSources(dbFile1, dbFile2);

    Component reportFile1 = ReportComponent.builder(Component.Type.FILE, 2, dbFile1.getKey()).setUuid(dbFile1.uuid()).build();
    Component reportFile2 = ReportComponent.builder(Component.Type.FILE, 3, dbFile2.getKey()).setUuid(dbFile2.uuid()).build();
    Component reportFile3 = ReportComponent.builder(Component.Type.FILE, 4, dbFile2.getKey()).build();

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1, project1.getKey()).setUuid(project1.uuid())
      .addChildren(reportFile1, reportFile2, reportFile3).build());
    underTest.execute(new TestComputationStepContext());

    assertThat(previousFileHashesRepository.getMap()).hasSize(2);
    assertThat(previousFileHashesRepository.getDbFile(reportFile1).get())
      .extracting(FileHashesDto::getSrcHash, FileHashesDto::getRevision, FileHashesDto::getDataHash)
      .containsOnly("srcHash" + dbFile1.getKey(), "revision" + dbFile1.getKey(), "dataHash" + dbFile1.getKey());
    assertThat(previousFileHashesRepository.getDbFile(reportFile2).get())
      .extracting(FileHashesDto::getSrcHash, FileHashesDto::getRevision, FileHashesDto::getDataHash)
      .containsOnly("srcHash" + dbFile2.getKey(), "revision" + dbFile2.getKey(), "dataHash" + dbFile2.getKey());
    assertThat(previousFileHashesRepository.getDbFile(reportFile3)).isEmpty();
  }

  @Test
  public void loads_high_number_of_files() {
    ComponentDto project1 = db.components().insertPublicProject().getMainBranchComponent();
    List<Component> files = new ArrayList<>(2000);

    for (int i = 0; i < 2000; i++) {
      ComponentDto dbFile = db.components().insertComponent(ComponentTesting.newFileDto(project1));
      insertFileSources(dbFile);
      files.add(ReportComponent.builder(Component.Type.FILE, 2, dbFile.getKey()).setUuid(dbFile.uuid()).build());
    }

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1, project1.getKey()).setUuid(project1.uuid())
      .addChildren(files.toArray(Component[]::new)).build());
    underTest.execute(new TestComputationStepContext());

    assertThat(previousFileHashesRepository.getMap()).hasSize(2000);
    for (Component file : files) {
      assertThat(previousFileHashesRepository.getDbFile(file)).isPresent();
    }
  }

  private void insertFileSources(ComponentDto... files) {
    for (ComponentDto file : files) {
      db.fileSources().insertFileSource(file, f -> f
        .setSrcHash("srcHash" + file.getKey())
        .setRevision("revision" + file.getKey())
        .setDataHash("dataHash" + file.getKey()));
    }
  }
}
