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
package org.sonar.server.issue;

import java.util.Date;
import java.util.List;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;
import org.sonar.db.issue.IssueDto;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IssuesFinderSortTest {

  @Test
  public void should_sort_by_assignee() {
    IssueDto issue1 = new IssueDto().setId(1L).setAssignee("perceval");
    IssueDto issue2 = new IssueDto().setId(2L).setAssignee("arthur");
    IssueDto issue3 = new IssueDto().setId(3L).setAssignee("vincent");
    IssueDto issue4 = new IssueDto().setId(4L).setAssignee(null);
    List<IssueDto> dtoList = newArrayList(issue1, issue2, issue3, issue4);

    IssueQuery query = IssueQuery.builder().sort(IssueQuery.SORT_BY_ASSIGNEE).asc(true).build();
    IssuesFinderSort issuesFinderSort = new IssuesFinderSort(dtoList, query);

    List<IssueDto> result = newArrayList(issuesFinderSort.sort());

    assertThat(result).hasSize(4);
    assertThat(result.get(0).getAssignee()).isEqualTo("arthur");
    assertThat(result.get(1).getAssignee()).isEqualTo("perceval");
    assertThat(result.get(2).getAssignee()).isEqualTo("vincent");
    assertThat(result.get(3).getAssignee()).isNull();
  }

  @Test
  public void should_sort_by_status() {
    IssueDto issue1 = new IssueDto().setId(1L).setStatus("CLOSED");
    IssueDto issue2 = new IssueDto().setId(2L).setStatus("REOPENED");
    IssueDto issue3 = new IssueDto().setId(3L).setStatus("OPEN");
    List<IssueDto> dtoList = newArrayList(issue1, issue2, issue3);

    IssueQuery query = IssueQuery.builder().sort(IssueQuery.SORT_BY_STATUS).asc(false).build();
    IssuesFinderSort issuesFinderSort = new IssuesFinderSort(dtoList, query);

    List<IssueDto> result = newArrayList(issuesFinderSort.sort());

    assertThat(result).hasSize(3);
    assertThat(result.get(0).getStatus()).isEqualTo("REOPENED");
    assertThat(result.get(1).getStatus()).isEqualTo("OPEN");
    assertThat(result.get(2).getStatus()).isEqualTo("CLOSED");
  }

  @Test
  public void should_sort_by_severity() {
    IssueDto issue1 = new IssueDto().setId(1L).setSeverity("INFO");
    IssueDto issue2 = new IssueDto().setId(2L).setSeverity("BLOCKER");
    IssueDto issue3 = new IssueDto().setId(3L).setSeverity("MAJOR");
    List<IssueDto> dtoList = newArrayList(issue1, issue2, issue3);

    IssueQuery query = IssueQuery.builder().sort(IssueQuery.SORT_BY_SEVERITY).asc(true).build();
    IssuesFinderSort issuesFinderSort = new IssuesFinderSort(dtoList, query);

    List<IssueDto> result = newArrayList(issuesFinderSort.sort());

    assertThat(result).hasSize(3);
    assertThat(result.get(0).getSeverity()).isEqualTo("INFO");
    assertThat(result.get(1).getSeverity()).isEqualTo("MAJOR");
    assertThat(result.get(2).getSeverity()).isEqualTo("BLOCKER");
  }

  @Test
  public void should_sort_by_desc_severity() {
    IssueDto issue1 = new IssueDto().setId(1L).setSeverity("INFO");
    IssueDto issue2 = new IssueDto().setId(2L).setSeverity("BLOCKER");
    IssueDto issue3 = new IssueDto().setId(3L).setSeverity("MAJOR");
    List<IssueDto> dtoList = newArrayList(issue1, issue2, issue3);

    IssueQuery query = IssueQuery.builder().sort(IssueQuery.SORT_BY_SEVERITY).asc(false).build();
    IssuesFinderSort issuesFinderSort = new IssuesFinderSort(dtoList, query);

    List<IssueDto> result = newArrayList(issuesFinderSort.sort());

    assertThat(result).hasSize(3);
    assertThat(result.get(0).getSeverity()).isEqualTo("BLOCKER");
    assertThat(result.get(1).getSeverity()).isEqualTo("MAJOR");
    assertThat(result.get(2).getSeverity()).isEqualTo("INFO");
  }

  @Test
  public void should_sort_by_creation_date() {
    Date date = new Date();
    Date date1 = DateUtils.addDays(date, -3);
    Date date2 = DateUtils.addDays(date, -2);
    Date date3 = DateUtils.addDays(date, -1);
    IssueDto issue1 = new IssueDto().setId(1L).setIssueCreationDate(date1);
    IssueDto issue2 = new IssueDto().setId(2L).setIssueCreationDate(date3);
    IssueDto issue3 = new IssueDto().setId(3L).setIssueCreationDate(date2);
    List<IssueDto> dtoList = newArrayList(issue1, issue2, issue3);

    IssueQuery query = IssueQuery.builder().sort(IssueQuery.SORT_BY_CREATION_DATE).asc(false).build();
    IssuesFinderSort issuesFinderSort = new IssuesFinderSort(dtoList, query);

    List<IssueDto> result = newArrayList(issuesFinderSort.sort());

    assertThat(result).hasSize(3);
    assertThat(result.get(0).getIssueCreationDate()).isEqualTo(date3);
    assertThat(result.get(1).getIssueCreationDate()).isEqualTo(date2);
    assertThat(result.get(2).getIssueCreationDate()).isEqualTo(date1);
  }

  @Test
  public void should_sort_by_update_date() {
    Date date = new Date();
    Date date1 = DateUtils.addDays(date, -3);
    Date date2 = DateUtils.addDays(date, -2);
    Date date3 = DateUtils.addDays(date, -1);
    IssueDto issue1 = new IssueDto().setId(1L).setIssueUpdateDate(date1);
    IssueDto issue2 = new IssueDto().setId(2L).setIssueUpdateDate(date3);
    IssueDto issue3 = new IssueDto().setId(3L).setIssueUpdateDate(date2);
    List<IssueDto> dtoList = newArrayList(issue1, issue2, issue3);

    IssueQuery query = IssueQuery.builder().sort(IssueQuery.SORT_BY_UPDATE_DATE).asc(false).build();
    IssuesFinderSort issuesFinderSort = new IssuesFinderSort(dtoList, query);

    List<IssueDto> result = newArrayList(issuesFinderSort.sort());

    assertThat(result).hasSize(3);
    assertThat(result.get(0).getIssueUpdateDate()).isEqualTo(date3);
    assertThat(result.get(1).getIssueUpdateDate()).isEqualTo(date2);
    assertThat(result.get(2).getIssueUpdateDate()).isEqualTo(date1);
  }

  @Test
  public void should_sort_by_close_date() {
    Date date = new Date();
    Date date1 = DateUtils.addDays(date, -3);
    Date date2 = DateUtils.addDays(date, -2);
    Date date3 = DateUtils.addDays(date, -1);
    IssueDto issue1 = new IssueDto().setId(1L).setIssueCloseDate(date1);
    IssueDto issue2 = new IssueDto().setId(2L).setIssueCloseDate(date3);
    IssueDto issue3 = new IssueDto().setId(3L).setIssueCloseDate(date2);
    List<IssueDto> dtoList = newArrayList(issue1, issue2, issue3);

    IssueQuery query = IssueQuery.builder().sort(IssueQuery.SORT_BY_CLOSE_DATE).asc(false).build();
    IssuesFinderSort issuesFinderSort = new IssuesFinderSort(dtoList, query);

    List<IssueDto> result = newArrayList(issuesFinderSort.sort());

    assertThat(result).hasSize(3);
    assertThat(result.get(0).getIssueCloseDate()).isEqualTo(date3);
    assertThat(result.get(1).getIssueCloseDate()).isEqualTo(date2);
    assertThat(result.get(2).getIssueCloseDate()).isEqualTo(date1);
  }

  @Test
  public void should_not_sort_with_null_sort() {
    IssueDto issue1 = new IssueDto().setId(1L).setAssignee("perceval");
    IssueDto issue2 = new IssueDto().setId(2L).setAssignee("arthur");
    IssueDto issue3 = new IssueDto().setId(3L).setAssignee("vincent");
    IssueDto issue4 = new IssueDto().setId(4L).setAssignee(null);
    List<IssueDto> dtoList = newArrayList(issue1, issue2, issue3, issue4);

    IssueQuery query = IssueQuery.builder().sort(null).build();
    IssuesFinderSort issuesFinderSort = new IssuesFinderSort(dtoList, query);

    List<IssueDto> result = newArrayList(issuesFinderSort.sort());

    assertThat(result).hasSize(4);
    assertThat(result.get(0).getAssignee()).isEqualTo("perceval");
    assertThat(result.get(1).getAssignee()).isEqualTo("arthur");
    assertThat(result.get(2).getAssignee()).isEqualTo("vincent");
    assertThat(result.get(3).getAssignee()).isNull();
  }

  @Test
  public void should_fail_to_sort_with_unknown_sort() {
    IssueQuery query = mock(IssueQuery.class);
    when(query.sort()).thenReturn("unknown");
    IssuesFinderSort issuesFinderSort = new IssuesFinderSort(null, query);
    try {
      issuesFinderSort.sort();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Cannot sort on field : unknown");
    }
  }

}
