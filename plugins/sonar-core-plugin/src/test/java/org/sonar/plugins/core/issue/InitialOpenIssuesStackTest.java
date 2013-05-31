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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.batch.index.Caches;
import org.sonar.core.issue.db.IssueDto;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class InitialOpenIssuesStackTest {

  InitialOpenIssuesStack stack;
  Caches caches = new Caches();

  @Before
  public void setUp() {
    caches.start();
    stack = new InitialOpenIssuesStack(caches);
  }

  @After
  public void tearDown() {
    caches.stop();
  }

  @Test
  public void should_get_and_remove() {
    IssueDto issueDto = new IssueDto().setComponentKey_unit_test_only("org.struts.Action").setKee("ISSUE-1");
    stack.addIssue(issueDto);

    List<IssueDto> issueDtos = stack.selectAndRemove("org.struts.Action");
    assertThat(issueDtos).hasSize(1);
    assertThat(issueDtos.get(0).getKee()).isEqualTo("ISSUE-1");

    assertThat(stack.selectAll()).isEmpty();
  }

  @Test
  public void should_get_and_remove_with_many_issues_on_same_resource() {
    stack.addIssue(new IssueDto().setComponentKey_unit_test_only("org.struts.Action").setKee("ISSUE-1"));
    stack.addIssue(new IssueDto().setComponentKey_unit_test_only("org.struts.Action").setKee("ISSUE-2"));

    List<IssueDto> issueDtos = stack.selectAndRemove("org.struts.Action");
    assertThat(issueDtos).hasSize(2);

    assertThat(stack.selectAll()).isEmpty();
  }

  @Test
  public void should_do_nothing_if_resource_not_found() {
    stack.addIssue(new IssueDto().setComponentKey_unit_test_only("org.struts.Action").setKee("ISSUE-1"));

    List<IssueDto> issueDtos = stack.selectAndRemove("Other");
    assertThat(issueDtos).hasSize(0);

    assertThat(stack.selectAll()).hasSize(1);
  }

  @Test
  public void should_clear() {
    stack.addIssue(new IssueDto().setComponentKey_unit_test_only("org.struts.Action").setKee("ISSUE-1"));

    assertThat(stack.selectAll()).hasSize(1);

    // issues are not removed
    assertThat(stack.selectAll()).hasSize(1);

    stack.clear();
    assertThat(stack.selectAll()).hasSize(0);
  }
}
