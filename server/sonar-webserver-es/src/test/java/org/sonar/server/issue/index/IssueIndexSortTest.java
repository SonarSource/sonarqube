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
package org.sonar.server.issue.index;

import org.elasticsearch.action.search.SearchResponse;
import org.junit.jupiter.api.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.Severity;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.es.SearchOptions;

import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.server.issue.IssueDocTesting.newDoc;

class IssueIndexSortTest extends IssueIndexTestCommon {

  @Test
  void sort_by_status() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setStatus(Issue.STATUS_OPEN),
      newDoc("I2", project.uuid(), file).setStatus(Issue.STATUS_CLOSED),
      newDoc("I3", project.uuid(), file).setStatus(Issue.STATUS_REOPENED));

    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_STATUS).asc(true);
    assertThatSearchReturnsOnly(query, "I2", "I1", "I3");

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_STATUS).asc(false);
    assertThatSearchReturnsOnly(query, "I3", "I1", "I2");
  }

  @Test
  void sort_by_severity() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setSeverity(Severity.BLOCKER),
      newDoc("I2", project.uuid(), file).setSeverity(Severity.INFO),
      newDoc("I3", project.uuid(), file).setSeverity(Severity.MINOR),
      newDoc("I4", project.uuid(), file).setSeverity(Severity.CRITICAL),
      newDoc("I5", project.uuid(), file).setSeverity(Severity.MAJOR));

    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_SEVERITY).asc(true);
    assertThatSearchReturnsOnly(query, "I2", "I3", "I5", "I4", "I1");

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_SEVERITY).asc(false);
    assertThatSearchReturnsOnly(query, "I1", "I4", "I5", "I3", "I2");
  }

  @Test
  void sort_by_creation_date() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setFuncCreationDate(parseDateTime("2014-09-23T00:00:00+0100")),
      newDoc("I2", project.uuid(), file).setFuncCreationDate(parseDateTime("2014-09-24T00:00:00+0100")));

    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_CREATION_DATE).asc(true);
    SearchResponse result = underTest.search(query.build(), new SearchOptions());
    assertThatSearchReturnsOnly(query, "I1", "I2");

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_CREATION_DATE).asc(false);
    assertThatSearchReturnsOnly(query, "I2", "I1");
  }

  @Test
  void sort_by_update_date() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setFuncUpdateDate(parseDateTime("2014-09-23T00:00:00+0100")),
      newDoc("I2", project.uuid(), file).setFuncUpdateDate(parseDateTime("2014-09-24T00:00:00+0100")));

    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_UPDATE_DATE).asc(true);
    SearchResponse result = underTest.search(query.build(), new SearchOptions());
    assertThatSearchReturnsOnly(query, "I1", "I2");

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_UPDATE_DATE).asc(false);
    assertThatSearchReturnsOnly(query, "I2", "I1");
  }

  @Test
  void sort_by_close_date() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setFuncCloseDate(parseDateTime("2014-09-23T00:00:00+0100")),
      newDoc("I2", project.uuid(), file).setFuncCloseDate(parseDateTime("2014-09-24T00:00:00+0100")),
      newDoc("I3", project.uuid(), file).setFuncCloseDate(null));

    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_CLOSE_DATE).asc(true);
    SearchResponse result = underTest.search(query.build(), new SearchOptions());
    assertThatSearchReturnsOnly(query, "I3", "I1", "I2");

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_CLOSE_DATE).asc(false);
    assertThatSearchReturnsOnly(query, "I2", "I1", "I3");
  }

  @Test
  void sort_by_file_and_line() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file1 = newFileDto(project, null, "F1").setPath("src/main/xoo/org/sonar/samples/File.xoo");
    ComponentDto file2 = newFileDto(project, null, "F2").setPath("src/main/xoo/org/sonar/samples/File2.xoo");

    indexIssues(
      // file F1
      newDoc("F1_2", project.uuid(), file1).setLine(20),
      newDoc("F1_1", project.uuid(), file1).setLine(null),
      newDoc("F1_3", project.uuid(), file1).setLine(25),

      // file F2
      newDoc("F2_1", project.uuid(), file2).setLine(9),
      newDoc("F2_2", project.uuid(), file2).setLine(109),
      // two issues on the same line -> sort by key
      newDoc("F2_3", project.uuid(), file2).setLine(109));

    // ascending sort -> F1 then F2. Line "0" first.
    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_FILE_LINE).asc(true);
    assertThatSearchReturnsOnly(query, "F1_1", "F1_2", "F1_3", "F2_1", "F2_2", "F2_3");

    // descending sort -> F2 then F1
    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_FILE_LINE).asc(false);
    assertThatSearchReturnsOnly(query, "F2_3", "F2_2", "F2_1", "F1_3", "F1_2", "F1_1");
  }

  @Test
  void default_sort_is_by_creation_date_then_project_then_file_then_line_then_issue_key() {
    ComponentDto project1 = newPrivateProjectDto("P1");
    ComponentDto file1 = newFileDto(project1, null, "F1").setPath("src/main/xoo/org/sonar/samples/File.xoo");
    ComponentDto file2 = newFileDto(project1, null, "F2").setPath("src/main/xoo/org/sonar/samples/File2.xoo");

    ComponentDto project2 = newPrivateProjectDto("P2");
    ComponentDto file3 = newFileDto(project2, null, "F3").setPath("src/main/xoo/org/sonar/samples/File3.xoo");

    indexIssues(
      // file F1 from project P1
      newDoc("F1_1", project1.uuid(), file1).setLine(20).setFuncCreationDate(parseDateTime("2014-09-23T00:00:00+0100")),
      newDoc("F1_2", project1.uuid(), file1).setLine(null).setFuncCreationDate(parseDateTime("2014-09-23T00:00:00+0100")),
      newDoc("F1_3", project1.uuid(), file1).setLine(25).setFuncCreationDate(parseDateTime("2014-09-23T00:00:00+0100")),

      // file F2 from project P1
      newDoc("F2_1", project1.uuid(), file2).setLine(9).setFuncCreationDate(parseDateTime("2014-09-23T00:00:00+0100")),
      newDoc("F2_2", project1.uuid(), file2).setLine(109).setFuncCreationDate(parseDateTime("2014-09-23T00:00:00+0100")),
      // two issues on the same line -> sort by key
      newDoc("F2_3", project1.uuid(), file2).setLine(109).setFuncCreationDate(parseDateTime("2014-09-23T00:00:00+0100")),

      // file F3 from project P2
      newDoc("F3_1", project2.uuid(), file3).setLine(20).setFuncCreationDate(parseDateTime("2014-09-24T00:00:00+0100")),
      newDoc("F3_2", project2.uuid(), file3).setLine(20).setFuncCreationDate(parseDateTime("2014-09-23T00:00:00+0100")));

    assertThatSearchReturnsOnly(IssueQuery.builder(), "F3_1", "F1_2", "F1_1", "F1_3", "F2_1", "F2_2", "F2_3", "F3_2");
  }

}
