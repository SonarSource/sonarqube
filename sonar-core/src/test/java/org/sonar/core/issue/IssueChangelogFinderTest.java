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

package org.sonar.core.issue;

import com.google.common.collect.Multimap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.core.issue.db.IssueChangeDao;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IssueChangelogFinderTest {

  @Mock
  private IssueChangeDao dao;

  private IssueChangelogFinder finder;

  @Before
  public void before() {
    finder = new IssueChangelogFinder(dao);
  }

  @Test
  public void find_by_issues() {
    when(dao.selectChangelogByIssues(newArrayList("ABCD"))).thenReturn(newArrayList(new FieldDiffs().setIssueKey("ABCD")));

    Multimap<Issue, FieldDiffs> result = finder.findByIssues(newArrayList((Issue) new DefaultIssue().setKey("ABCD")));
    assertThat(result.entries()).hasSize(1);
  }
}
