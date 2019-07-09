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
package org.sonar.scanner.issue;

import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.scanner.ProjectInfo;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.scanner.protocol.Constants.Severity;
import org.sonar.scanner.protocol.output.ScannerReport.Issue;
import org.sonar.scanner.protocol.output.ScannerReport.TextRange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultFilterableIssueTest {
  private DefaultFilterableIssue issue;
  private DefaultInputProject mockedProject;
  private ProjectInfo projectInfo;
  private InputComponent component;
  private Issue rawIssue;

  @Before
  public void setUp() {
    mockedProject = mock(DefaultInputProject.class);
    projectInfo = mock(ProjectInfo.class);
    component = mock(InputComponent.class);
    when(component.key()).thenReturn("foo");
  }

  private Issue createIssue() {
    Issue.Builder builder = Issue.newBuilder();

    builder.setGap(3.0);
    builder.setTextRange(TextRange.newBuilder()
      .setStartLine(30)
      .setStartOffset(10)
      .setEndLine(31)
      .setEndOffset(3));
    builder.setSeverity(Severity.MAJOR);
    return builder.build();
  }

  private Issue createIssueWithoutFields() {
    Issue.Builder builder = Issue.newBuilder();
    builder.setSeverity(Severity.MAJOR);
    return builder.build();
  }

  @Test
  public void testRoundTrip() {
    rawIssue = createIssue();
    issue = new DefaultFilterableIssue(mockedProject, projectInfo, rawIssue, component);

    when(projectInfo.getAnalysisDate()).thenReturn(new Date(10_000));
    when(mockedProject.key()).thenReturn("projectKey");

    assertThat(issue.componentKey()).isEqualTo(component.key());
    assertThat(issue.creationDate()).isEqualTo(new Date(10_000));
    assertThat(issue.line()).isEqualTo(30);
    assertThat(issue.textRange().start().line()).isEqualTo(30);
    assertThat(issue.textRange().start().lineOffset()).isEqualTo(10);
    assertThat(issue.textRange().end().line()).isEqualTo(31);
    assertThat(issue.textRange().end().lineOffset()).isEqualTo(3);
    assertThat(issue.projectKey()).isEqualTo("projectKey");
    assertThat(issue.gap()).isEqualTo(3.0);
    assertThat(issue.severity()).isEqualTo("MAJOR");
  }

  @Test
  public void nullValues() {
    rawIssue = createIssueWithoutFields();
    issue = new DefaultFilterableIssue(mockedProject, projectInfo, rawIssue, component);

    assertThat(issue.line()).isNull();
    assertThat(issue.gap()).isNull();
  }
}
