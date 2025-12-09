/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.Date;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.filemove.MovedFilesRepository;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.issue.IssueFieldsSetter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class MovedIssueVisitorTest {
  private static final long ANALYSIS_DATE = 894521;
  private static final String FILE_UUID = "file uuid";
  private static final Component FILE = ReportComponent.builder(Component.Type.FILE, 1)
    .setKey("key_1")
    .setUuid(FILE_UUID)
    .build();

  @org.junit.Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();

  private final MovedFilesRepository movedFilesRepository = mock(MovedFilesRepository.class);
  private final MovedIssueVisitor underTest = new MovedIssueVisitor(analysisMetadataHolder, movedFilesRepository, new IssueFieldsSetter());

  @Before
  public void setUp() {
    analysisMetadataHolder.setAnalysisDate(ANALYSIS_DATE);
    when(movedFilesRepository.getOriginalFile(any(Component.class)))
      .thenReturn(Optional.empty());
  }

  @Test
  public void onIssue_does_not_alter_issue_if_component_is_not_a_file() {
    DefaultIssue issue = mock(DefaultIssue.class);
    underTest.onIssue(ReportComponent.builder(Component.Type.DIRECTORY, 1).build(), issue);

    verifyNoInteractions(issue);
  }

  @Test
  public void onIssue_does_not_alter_issue_if_component_file_but_issue_has_the_same_component_uuid() {
    DefaultIssue issue = mockIssue(FILE_UUID);
    underTest.onIssue(FILE, issue);

    verify(issue).componentUuid();
    verifyNoMoreInteractions(issue);
  }

  @Test
  public void onIssue_throws_ISE_if_issue_has_different_component_uuid_but_component_has_no_original_file() {
    DefaultIssue issue = mockIssue("other component uuid");
    when(issue.toString()).thenReturn("[bad issue, bad!]");

    assertThatThrownBy(() -> underTest.onIssue(FILE, issue))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Issue [bad issue, bad!] for component ReportComponent{ref=1, key='key_1', type=FILE} " +
        "has a different component key but no original file exist in MovedFilesRepository");
  }

  @Test
  public void onIssue_throws_ISE_if_issue_has_different_component_uuid_from_component_but_it_is_not_the_one_of_original_file() {
    DefaultIssue issue = mockIssue("other component uuid");
    when(issue.toString()).thenReturn("[bad issue, bad!]");
    when(movedFilesRepository.getOriginalFile(FILE))
      .thenReturn(Optional.of(new MovedFilesRepository.OriginalFile("original uuid", "original key")));

    assertThatThrownBy(() -> underTest.onIssue(FILE, issue))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Issue [bad issue, bad!] doesn't belong to file original uuid registered as original " +
        "file of current file ReportComponent{ref=1, key='key_1', type=FILE}");
  }

  @Test
  public void onIssue_update_component_and_module_fields_to_component_and_flag_issue_has_changed() {
    MovedFilesRepository.OriginalFile originalFile = new MovedFilesRepository.OriginalFile("original uuid", "original key");
    DefaultIssue issue = mockIssue(originalFile.uuid());
    when(movedFilesRepository.getOriginalFile(FILE))
      .thenReturn(Optional.of(originalFile));

    underTest.onIssue(FILE, issue);

    verify(issue).setComponentUuid(FILE.getUuid());
    verify(issue).setComponentKey(FILE.getKey());
    verify(issue).setUpdateDate(new Date(ANALYSIS_DATE));
    verify(issue).setChanged(true);
  }

  private DefaultIssue mockIssue(String fileUuid) {
    DefaultIssue issue = mock(DefaultIssue.class);
    when(issue.componentUuid()).thenReturn(fileUuid);
    return issue;
  }
}
