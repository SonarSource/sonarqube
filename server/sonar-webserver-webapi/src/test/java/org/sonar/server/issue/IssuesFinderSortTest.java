/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.issue.index.IssueQuery;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IssuesFinderSortTest {

  @Test
  public void should_sort_by_status() {
    IssueDto issue1 = new IssueDto().setKee("A").setStatus("CLOSED");
    IssueDto issue2 = new IssueDto().setKee("B").setStatus("REOPENED");
    IssueDto issue3 = new IssueDto().setKee("C").setStatus("OPEN");
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
    IssueDto issue1 = new IssueDto().setKee("A").setSeverity("INFO");
    IssueDto issue2 = new IssueDto().setKee("B").setSeverity("BLOCKER");
    IssueDto issue3 = new IssueDto().setKee("C").setSeverity("MAJOR");
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
    IssueDto issue1 = new IssueDto().setKee("A").setSeverity("INFO");
    IssueDto issue2 = new IssueDto().setKee("B").setSeverity("BLOCKER");
    IssueDto issue3 = new IssueDto().setKee("C").setSeverity("MAJOR");
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
    IssueDto issue1 = new IssueDto().setKee("A").setIssueCreationDate(date1);
    IssueDto issue2 = new IssueDto().setKee("B").setIssueCreationDate(date3);
    IssueDto issue3 = new IssueDto().setKee("C").setIssueCreationDate(date2);
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
    IssueDto issue1 = new IssueDto().setKee("A").setIssueUpdateDate(date1);
    IssueDto issue2 = new IssueDto().setKee("B").setIssueUpdateDate(date3);
    IssueDto issue3 = new IssueDto().setKee("C").setIssueUpdateDate(date2);
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
    IssueDto issue1 = new IssueDto().setKee("A").setIssueCloseDate(date1);
    IssueDto issue2 = new IssueDto().setKee("B").setIssueCloseDate(date3);
    IssueDto issue3 = new IssueDto().setKee("C").setIssueCloseDate(date2);
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
    IssueDto issue1 = new IssueDto().setKee("A").setAssigneeUuid("perceval");
    IssueDto issue2 = new IssueDto().setKee("B").setAssigneeUuid("arthur");
    IssueDto issue3 = new IssueDto().setKee("C").setAssigneeUuid("vincent");
    IssueDto issue4 = new IssueDto().setKee("D").setAssigneeUuid(null);
    List<IssueDto> dtoList = newArrayList(issue1, issue2, issue3, issue4);

    IssueQuery query = IssueQuery.builder().sort(null).build();
    IssuesFinderSort issuesFinderSort = new IssuesFinderSort(dtoList, query);

    List<IssueDto> result = newArrayList(issuesFinderSort.sort());

    assertThat(result).hasSize(4);
    assertThat(result.get(0).getAssigneeUuid()).isEqualTo("perceval");
    assertThat(result.get(1).getAssigneeUuid()).isEqualTo("arthur");
    assertThat(result.get(2).getAssigneeUuid()).isEqualTo("vincent");
    assertThat(result.get(3).getAssigneeUuid()).isNull();
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
