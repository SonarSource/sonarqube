/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.base.Optional;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.filemove.MovedFilesRepository;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.server.issue.IssueFieldsSetter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class MovedIssueVisitorTest {
  private static final long ANALYSIS_DATE = 894521;
  private static final String FILE_UUID = "file uuid";
  private static final Component FILE = ReportComponent.builder(Component.Type.FILE, 1)
    .setKey("key_1")
    .setPublicKey("public_key_1")
    .setUuid(FILE_UUID)
    .build();

  @org.junit.Rule
  public ExpectedException expectedException = ExpectedException.none();
  @org.junit.Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();

  private MovedFilesRepository movedFilesRepository = mock(MovedFilesRepository.class);
  private MovedIssueVisitor underTest = new MovedIssueVisitor(analysisMetadataHolder, movedFilesRepository, new IssueFieldsSetter());

  @Before
  public void setUp() throws Exception {
    analysisMetadataHolder.setAnalysisDate(ANALYSIS_DATE);
    when(movedFilesRepository.getOriginalFile(any(Component.class)))
      .thenReturn(Optional.absent());
  }

  @Test
  public void onIssue_does_not_alter_issue_if_component_is_not_a_file() {
    DefaultIssue issue = mock(DefaultIssue.class);
    underTest.onIssue(ReportComponent.builder(Component.Type.DIRECTORY, 1).build(), issue);

    verifyZeroInteractions(issue);
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

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Issue [bad issue, bad!] for component ReportComponent{ref=1, key='key_1', type=FILE} " +
      "has a different component key but no original file exist in MovedFilesRepository");

    underTest.onIssue(FILE, issue);
  }

  @Test
  public void onIssue_throws_ISE_if_issue_has_different_component_uuid_from_component_but_it_is_not_the_one_of_original_file() {
    DefaultIssue issue = mockIssue("other component uuid");
    when(issue.toString()).thenReturn("[bad issue, bad!]");
    when(movedFilesRepository.getOriginalFile(FILE))
      .thenReturn(Optional.of(new MovedFilesRepository.OriginalFile(6451, "original uuid", "original key")));

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Issue [bad issue, bad!] doesn't belong to file original uuid registered as original " +
      "file of current file ReportComponent{ref=1, key='key_1', type=FILE}");

    underTest.onIssue(FILE, issue);
  }

  @Test
  public void onIssue_update_component_and_module_fields_to_component_and_flag_issue_has_changed() {
    MovedFilesRepository.OriginalFile originalFile = new MovedFilesRepository.OriginalFile(6451, "original uuid", "original key");
    DefaultIssue issue = mockIssue(originalFile.getUuid());
    when(movedFilesRepository.getOriginalFile(FILE))
      .thenReturn(Optional.of(originalFile));

    underTest.onIssue(FILE, issue);

    verify(issue).setComponentUuid(FILE.getUuid());
    verify(issue).setComponentKey(FILE.getKey());
    verify(issue).setModuleUuid(null);
    verify(issue).setModuleUuidPath(null);
    verify(issue).setChanged(true);
    ArgumentCaptor<IssueChangeContext> issueChangeContextCaptor = ArgumentCaptor.forClass(IssueChangeContext.class);
    verify(issue).setFieldChange(issueChangeContextCaptor.capture(), eq("file"), eq(originalFile.getUuid()), eq(FILE.getUuid()));
    assertThat(issueChangeContextCaptor.getValue().date()).isEqualTo(new Date(ANALYSIS_DATE));
    assertThat(issueChangeContextCaptor.getValue().userUuid()).isNull();
    assertThat(issueChangeContextCaptor.getValue().scan()).isFalse();
  }

  private DefaultIssue mockIssue(String fileUuid) {
    DefaultIssue issue = mock(DefaultIssue.class);
    when(issue.componentUuid()).thenReturn(fileUuid);
    return issue;
  }
}
