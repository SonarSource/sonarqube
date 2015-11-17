/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import org.sonar.batch.protocol.Constants.Severity;

import java.util.Date;

import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.Project;
import org.sonar.batch.protocol.output.BatchReport.Issue;

public class DefaultFilterableIssueTest {
  private DefaultFilterableIssue issue;
  private Project mockedProject;
  private String componentKey;
  private Issue rawIssue;

  @Before
  public void setUp() {
    mockedProject = mock(Project.class);
    componentKey = "component";
  }

  private Issue createIssue() {
    Issue.Builder builder = Issue.newBuilder();

    builder.setEffortToFix(3.0);
    builder.setLine(30);
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
    issue = new DefaultFilterableIssue(mockedProject, rawIssue, componentKey);

    when(mockedProject.getAnalysisDate()).thenReturn(new Date(10_000));
    when(mockedProject.getEffectiveKey()).thenReturn("projectKey");

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
    issue = new DefaultFilterableIssue(mockedProject, rawIssue, componentKey);

    assertThat(issue.line()).isNull();
    assertThat(issue.effortToFix()).isNull();
  }
}
