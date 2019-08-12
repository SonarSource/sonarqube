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
package org.sonar.server.issue.index;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.permission.index.IndexPermissions;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.tester.UserSessionRule;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.TimeZone.getTimeZone;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.organization.OrganizationTesting.newOrganizationDto;
import static org.sonar.server.issue.IssueDocTesting.newDoc;

public class IssueIndexSortTest {

  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = none();
  private System2 system2 = new TestSystem2().setNow(1_500_000_000_000L).setDefaultTimeZone(getTimeZone("GMT-01:00"));
  @Rule
  public DbTester db = DbTester.create(system2);

  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), db.getDbClient(), new IssueIteratorFactory(db.getDbClient()));
  private PermissionIndexerTester authorizationIndexer = new PermissionIndexerTester(es, issueIndexer);

  private IssueIndex underTest = new IssueIndex(es.client(), system2, userSessionRule, new WebAuthorizationTypeSupport(userSessionRule));

  @Test
  public void sort_by_status() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setStatus(Issue.STATUS_OPEN),
      newDoc("I2", file).setStatus(Issue.STATUS_CLOSED),
      newDoc("I3", file).setStatus(Issue.STATUS_REOPENED));

    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_STATUS).asc(true);
    assertThatSearchReturnsOnly(query, "I2", "I1", "I3");

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_STATUS).asc(false);
    assertThatSearchReturnsOnly(query, "I3", "I1", "I2");
  }

  @Test
  public void sort_by_severity() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setSeverity(Severity.BLOCKER),
      newDoc("I2", file).setSeverity(Severity.INFO),
      newDoc("I3", file).setSeverity(Severity.MINOR),
      newDoc("I4", file).setSeverity(Severity.CRITICAL),
      newDoc("I5", file).setSeverity(Severity.MAJOR));

    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_SEVERITY).asc(true);
    assertThatSearchReturnsOnly(query, "I2", "I3", "I5", "I4", "I1");

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_SEVERITY).asc(false);
    assertThatSearchReturnsOnly(query, "I1", "I4", "I5", "I3", "I2");
  }

  @Test
  public void sort_by_creation_date() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setFuncCreationDate(parseDateTime("2014-09-23T00:00:00+0100")),
      newDoc("I2", file).setFuncCreationDate(parseDateTime("2014-09-24T00:00:00+0100")));

    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_CREATION_DATE).asc(true);
    SearchResponse result = underTest.search(query.build(), new SearchOptions());
    assertThatSearchReturnsOnly(query, "I1", "I2");

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_CREATION_DATE).asc(false);
    assertThatSearchReturnsOnly(query, "I2", "I1");
  }

  @Test
  public void sort_by_update_date() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setFuncUpdateDate(parseDateTime("2014-09-23T00:00:00+0100")),
      newDoc("I2", file).setFuncUpdateDate(parseDateTime("2014-09-24T00:00:00+0100")));

    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_UPDATE_DATE).asc(true);
    SearchResponse result = underTest.search(query.build(), new SearchOptions());
    assertThatSearchReturnsOnly(query, "I1", "I2");

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_UPDATE_DATE).asc(false);
    assertThatSearchReturnsOnly(query, "I2", "I1");
  }

  @Test
  public void sort_by_close_date() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setFuncCloseDate(parseDateTime("2014-09-23T00:00:00+0100")),
      newDoc("I2", file).setFuncCloseDate(parseDateTime("2014-09-24T00:00:00+0100")),
      newDoc("I3", file).setFuncCloseDate(null));

    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_CLOSE_DATE).asc(true);
    SearchResponse result = underTest.search(query.build(), new SearchOptions());
    assertThatSearchReturnsOnly(query, "I3", "I1", "I2");

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_CLOSE_DATE).asc(false);
    assertThatSearchReturnsOnly(query, "I2", "I1", "I3");
  }

  @Test
  public void sort_by_file_and_line() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file1 = newFileDto(project, null, "F1").setPath("src/main/xoo/org/sonar/samples/File.xoo");
    ComponentDto file2 = newFileDto(project, null, "F2").setPath("src/main/xoo/org/sonar/samples/File2.xoo");

    indexIssues(
      // file F1
      newDoc("F1_2", file1).setLine(20),
      newDoc("F1_1", file1).setLine(null),
      newDoc("F1_3", file1).setLine(25),

      // file F2
      newDoc("F2_1", file2).setLine(9),
      newDoc("F2_2", file2).setLine(109),
      // two issues on the same line -> sort by key
      newDoc("F2_3", file2).setLine(109));

    // ascending sort -> F1 then F2. Line "0" first.
    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_FILE_LINE).asc(true);
    assertThatSearchReturnsOnly(query, "F1_1", "F1_2", "F1_3", "F2_1", "F2_2", "F2_3");

    // descending sort -> F2 then F1
    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_FILE_LINE).asc(false);
    assertThatSearchReturnsOnly(query, "F2_3", "F2_2", "F2_1", "F1_3", "F1_2", "F1_1");
  }

  @Test
  public void default_sort_is_by_creation_date_then_project_then_file_then_line_then_issue_key() {
    OrganizationDto organizationDto = newOrganizationDto();
    ComponentDto project1 = newPrivateProjectDto(organizationDto, "P1");
    ComponentDto file1 = newFileDto(project1, null, "F1").setPath("src/main/xoo/org/sonar/samples/File.xoo");
    ComponentDto file2 = newFileDto(project1, null, "F2").setPath("src/main/xoo/org/sonar/samples/File2.xoo");

    ComponentDto project2 = newPrivateProjectDto(organizationDto, "P2");
    ComponentDto file3 = newFileDto(project2, null, "F3").setPath("src/main/xoo/org/sonar/samples/File3.xoo");

    indexIssues(
      // file F1 from project P1
      newDoc("F1_1", file1).setLine(20).setFuncCreationDate(parseDateTime("2014-09-23T00:00:00+0100")),
      newDoc("F1_2", file1).setLine(null).setFuncCreationDate(parseDateTime("2014-09-23T00:00:00+0100")),
      newDoc("F1_3", file1).setLine(25).setFuncCreationDate(parseDateTime("2014-09-23T00:00:00+0100")),

      // file F2 from project P1
      newDoc("F2_1", file2).setLine(9).setFuncCreationDate(parseDateTime("2014-09-23T00:00:00+0100")),
      newDoc("F2_2", file2).setLine(109).setFuncCreationDate(parseDateTime("2014-09-23T00:00:00+0100")),
      // two issues on the same line -> sort by key
      newDoc("F2_3", file2).setLine(109).setFuncCreationDate(parseDateTime("2014-09-23T00:00:00+0100")),

      // file F3 from project P2
      newDoc("F3_1", file3).setLine(20).setFuncCreationDate(parseDateTime("2014-09-24T00:00:00+0100")),
      newDoc("F3_2", file3).setLine(20).setFuncCreationDate(parseDateTime("2014-09-23T00:00:00+0100")));

    assertThatSearchReturnsOnly(IssueQuery.builder(), "F3_1", "F1_2", "F1_1", "F1_3", "F2_1", "F2_2", "F2_3", "F3_2");
  }

  private void indexIssues(IssueDoc... issues) {
    issueIndexer.index(asList(issues).iterator());
    authorizationIndexer.allow(stream(issues).map(issue -> new IndexPermissions(issue.projectUuid(), PROJECT).allowAnyone()).collect(toList()));
  }

  /**
   * Execute the search request and return the document ids of results.
   */
  private List<String> searchAndReturnKeys(IssueQuery.Builder query) {
    return Arrays.stream(underTest.search(query.build(), new SearchOptions()).getHits().getHits())
      .map(SearchHit::getId)
      .collect(Collectors.toList());
  }

  private void assertThatSearchReturnsOnly(IssueQuery.Builder query, String... expectedIssueKeys) {
    List<String> keys = searchAndReturnKeys(query);
    assertThat(keys).containsExactlyInAnyOrder(expectedIssueKeys);
  }

}
