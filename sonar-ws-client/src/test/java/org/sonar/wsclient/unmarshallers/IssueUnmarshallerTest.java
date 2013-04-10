/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.wsclient.unmarshallers;

import org.junit.Test;
import org.sonar.wsclient.services.Issue;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class IssueUnmarshallerTest extends UnmarshallerTestCase {

  @Test
  public void should_return_nothing() {
    Issue issue = new IssueUnmarshaller().toModel("[]");
    assertThat(issue).isNull();
  }

  @Test
  public void should_return_one_issue() {
    Issue issue = new IssueUnmarshaller().toModel(loadFile("/issues/single_issue.json"));
    assertThat(issue.getKey()).isEqualTo("029d283a-072b-4ef8-bdda-e4b212aa39e3");
    assertThat(issue.getComponentKey()).isEqualTo("com.sonarsource.it.samples:simple-sample:sample");
    assertThat(issue.getRuleKey()).isEqualTo("NM_FIELD_NAMING_CONVENTION");
    assertThat(issue.getRuleRepositoryKey()).isEqualTo("findbugs");
    assertThat(issue.getSeverity()).isEqualTo("MAJOR");
    assertThat(issue.getTitle()).isEqualTo("title");
    assertThat(issue.getMessage()).isEqualTo("message");
    assertThat(issue.getLine()).isEqualTo(1);
    assertThat(issue.getCost()).isEqualTo(1.2);
    assertThat(issue.getStatus()).isEqualTo("OPEN");
    assertThat(issue.getResolution()).isEqualTo("FIXED");
    assertThat(issue.getUserLogin()).isEqualTo("admin");
    assertThat(issue.getAssigneeLogin()).isEqualTo("admin");
    assertThat(issue.getCreatedAt()).isNotNull();
    assertThat(issue.getUpdatedAt()).isNotNull();
    assertThat(issue.getClosedAt()).isNull();
  }

  @Test
  public void should_return_all_issues() {
    List<Issue> issues = new IssueUnmarshaller().toModels(loadFile("/issues/all_issues.json"));
    assertThat(issues).hasSize(2);
  }

}
