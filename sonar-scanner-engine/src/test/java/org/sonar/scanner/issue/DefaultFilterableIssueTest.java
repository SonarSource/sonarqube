/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.scanner.ProjectAnalysisInfo;
import org.sonar.scanner.protocol.Constants.Severity;
import org.sonar.scanner.protocol.output.ScannerReport.Issue;
import org.sonar.scanner.protocol.output.ScannerReport.TextRange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultFilterableIssueTest {
  private DefaultFilterableIssue issue;
  private DefaultInputModule mockedProject;
  private ProjectAnalysisInfo projectAnalysisInfo;
  private String componentKey;
  private Issue rawIssue;

  @Before
  public void setUp() {
    mockedProject = mock(DefaultInputModule.class);
    projectAnalysisInfo = mock(ProjectAnalysisInfo.class);
    componentKey = "component";
  }

  private Issue createIssue() {
    Issue.Builder builder = Issue.newBuilder();

    builder.setGap(3.0);
    builder.setTextRange(TextRange.newBuilder().setStartLine(30));
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
    issue = new DefaultFilterableIssue(mockedProject, projectAnalysisInfo, rawIssue, componentKey);

    when(projectAnalysisInfo.analysisDate()).thenReturn(new Date(10_000));
    when(mockedProject.key()).thenReturn("projectKey");

    assertThat(issue.componentKey()).isEqualTo(componentKey);
    assertThat(issue.creationDate()).isEqualTo(new Date(10_000));
    assertThat(issue.line()).isEqualTo(30);
    assertThat(issue.projectKey()).isEqualTo("projectKey");
    assertThat(issue.effortToFix()).isEqualTo(3.0);
    assertThat(issue.severity()).isEqualTo("MAJOR");
  }

  @Test
  public void nullValues() {
    rawIssue = createIssueWithoutFields();
    issue = new DefaultFilterableIssue(mockedProject, projectAnalysisInfo, rawIssue, componentKey);

    assertThat(issue.line()).isNull();
    assertThat(issue.effortToFix()).isNull();
  }
}
