/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.plugins.core.issue;

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.issue.IssueDto;

import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class InitialOpenIssuesStackTest {

  private InitialOpenIssuesStack initialOpenIssuesStack;

  @Before
  public void before() {
    initialOpenIssuesStack = new InitialOpenIssuesStack();
  }

  @Test
  public void should_get_and_remove() {
    Date loadedDate = new Date();
    IssueDto issueDto = new IssueDto().setResourceId(10).setId(1L);
    initialOpenIssuesStack.setIssues(newArrayList(issueDto), loadedDate);

    List<IssueDto> issueDtos = initialOpenIssuesStack.selectAndRemove(10);
    assertThat(issueDtos).hasSize(1);
    assertThat(issueDtos.get(0).getId()).isEqualTo(1L);

    assertThat(initialOpenIssuesStack.getAllIssues()).isEmpty();
    assertThat(initialOpenIssuesStack.getLoadedDate()).isEqualTo(loadedDate);
  }

  @Test
  public void should_get_and_remove_with_many_issues_on_same_resource() {
    initialOpenIssuesStack.setIssues(newArrayList(
        new IssueDto().setResourceId(10).setId(1L),
        new IssueDto().setResourceId(10).setId(2L)
    ), new Date());

    List<IssueDto> issueDtos = initialOpenIssuesStack.selectAndRemove(10);
    assertThat(issueDtos).hasSize(2);

    assertThat(initialOpenIssuesStack.getAllIssues()).isEmpty();
  }

  @Test
  public void should_do_nothing_if_resource_not_found() {
    IssueDto issueDto = new IssueDto().setResourceId(10).setId(1L);
    initialOpenIssuesStack.setIssues(newArrayList(issueDto), new Date());

    List<IssueDto> issueDtos = initialOpenIssuesStack.selectAndRemove(999);
    assertThat(issueDtos).hasSize(0);

    assertThat(initialOpenIssuesStack.getAllIssues()).hasSize(1);
  }
}
