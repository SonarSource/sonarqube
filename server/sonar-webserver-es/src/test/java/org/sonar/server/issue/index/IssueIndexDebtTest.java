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

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.Severity;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.es.Facets;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.issue.IssueDocTesting;
import org.sonar.server.issue.index.IssueQuery.Builder;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.FACET_MODE_EFFORT;

class IssueIndexDebtTest extends IssueIndexTestCommon {

  @Test
  void facets_on_projects() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto("ABCD");
    ComponentDto project2 = ComponentTesting.newPrivateProjectDto("EFGH");

    indexIssues(
      IssueDocTesting.newDoc("I1", project.uuid(), ComponentTesting.newFileDto(project)).setEffort(10L),
      IssueDocTesting.newDoc("I2", project.uuid(), ComponentTesting.newFileDto(project)).setEffort(10L),
      IssueDocTesting.newDoc("I3", project2.uuid(), ComponentTesting.newFileDto(project2)).setEffort(10L));

    Facets facets = search("projects");
    assertThat(facets.getNames()).containsOnly("projects", FACET_MODE_EFFORT);
    assertThat(facets.get("projects")).containsOnly(entry("ABCD", 20L), entry("EFGH", 10L));
    assertThat(facets.get(FACET_MODE_EFFORT)).containsOnly(entry("total", 30L));
  }

  @Test
  void facets_on_components() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto("A");
    ComponentDto file1 = ComponentTesting.newFileDto(project, null, "ABCD");
    ComponentDto file2 = ComponentTesting.newFileDto(project, null, "BCDE");
    ComponentDto file3 = ComponentTesting.newFileDto(project, null, "CDEF");

    indexIssues(
      IssueDocTesting.newDocForProject("I1", project).setEffort(10L),
      IssueDocTesting.newDoc("I2", project.uuid(), file1).setEffort(10L),
      IssueDocTesting.newDoc("I3", project.uuid(), file2).setEffort(10L),
      IssueDocTesting.newDoc("I4", project.uuid(), file2).setEffort(10L),
      IssueDocTesting.newDoc("I5", project.uuid(), file3).setEffort(10L));

    Facets facets = search("files");
    assertThat(facets.getNames()).containsOnly("files", FACET_MODE_EFFORT);
    assertThat(facets.get("files"))
      .containsOnly(entry(file1.path(), 10L), entry(file2.path(), 20L), entry(file3.path(), 10L));
    assertThat(facets.get(FACET_MODE_EFFORT)).containsOnly(entry("total", 50L));
  }

  @Test
  void facets_on_directories() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto();
    ComponentDto file1 = ComponentTesting.newFileDto(project).setPath("src/main/xoo/F1.xoo");
    ComponentDto file2 = ComponentTesting.newFileDto(project).setPath("F2.xoo");

    indexIssues(
      IssueDocTesting.newDoc("I1", project.uuid(), file1).setDirectoryPath("/src/main/xoo").setEffort(10L),
      IssueDocTesting.newDoc("I2", project.uuid(), file2).setDirectoryPath("/").setEffort(10L));

    Facets facets = search("directories");
    assertThat(facets.getNames()).containsOnly("directories", FACET_MODE_EFFORT);
    assertThat(facets.get("directories")).containsOnly(entry("/src/main/xoo", 10L), entry("/", 10L));
    assertThat(facets.get(FACET_MODE_EFFORT)).containsOnly(entry("total", 20L));
  }

  @Test
  void facets_on_severities() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueDocTesting.newDoc("I1", project.uuid(), file).setSeverity(Severity.INFO).setEffort(10L),
      IssueDocTesting.newDoc("I2", project.uuid(), file).setSeverity(Severity.INFO).setEffort(10L),
      IssueDocTesting.newDoc("I3", project.uuid(), file).setSeverity(Severity.MAJOR).setEffort(10L));

    Facets facets = search("severities");
    assertThat(facets.getNames()).containsOnly("severities", FACET_MODE_EFFORT);
    assertThat(facets.get("severities")).containsOnly(entry("INFO", 20L), entry("MAJOR", 10L));
    assertThat(facets.get(FACET_MODE_EFFORT)).containsOnly(entry("total", 30L));
  }

  @Test
  void facets_on_statuses() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueDocTesting.newDoc("I1", project.uuid(), file).setStatus(STATUS_CLOSED).setEffort(10L),
      IssueDocTesting.newDoc("I2", project.uuid(), file).setStatus(STATUS_CLOSED).setEffort(10L),
      IssueDocTesting.newDoc("I3", project.uuid(), file).setStatus(STATUS_OPEN).setEffort(10L));

    Facets facets = search("statuses");
    assertThat(facets.getNames()).containsOnly("statuses", FACET_MODE_EFFORT);
    assertThat(facets.get("statuses")).containsOnly(entry("CLOSED", 20L), entry("OPEN", 10L));
    assertThat(facets.get(FACET_MODE_EFFORT)).containsOnly(entry("total", 30L));
  }

  @Test
  void facets_on_resolutions() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueDocTesting.newDoc("I1", project.uuid(), file).setResolution(Issue.RESOLUTION_FALSE_POSITIVE).setEffort(10L),
      IssueDocTesting.newDoc("I2", project.uuid(), file).setResolution(Issue.RESOLUTION_FALSE_POSITIVE).setEffort(10L),
      IssueDocTesting.newDoc("I3", project.uuid(), file).setResolution(Issue.RESOLUTION_FIXED).setEffort(10L));

    Facets facets = search("resolutions");
    assertThat(facets.getNames()).containsOnly("resolutions", FACET_MODE_EFFORT);
    assertThat(facets.get("resolutions")).containsOnly(entry("FALSE-POSITIVE", 20L), entry("FIXED", 10L));
    assertThat(facets.get(FACET_MODE_EFFORT)).containsOnly(entry("total", 30L));
  }

  @Test
  void facets_on_languages() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(IssueDocTesting.newDoc("I1", project.uuid(), file).setLanguage("xoo").setEffort(10L));

    Facets facets = search("languages");
    assertThat(facets.getNames()).containsOnly("languages", FACET_MODE_EFFORT);
    assertThat(facets.get("languages")).containsOnly(entry("xoo", 10L));
    assertThat(facets.get(FACET_MODE_EFFORT)).containsOnly(entry("total", 10L));
  }

  @Test
  void facets_on_assignees() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueDocTesting.newDoc("I1", project.uuid(), file).setAssigneeUuid("uuid-steph").setEffort(10L),
      IssueDocTesting.newDoc("I2", project.uuid(), file).setAssigneeUuid("uuid-simon").setEffort(10L),
      IssueDocTesting.newDoc("I3", project.uuid(), file).setAssigneeUuid("uuid-simon").setEffort(10L),
      IssueDocTesting.newDoc("I4", project.uuid(), file).setAssigneeUuid(null).setEffort(10L));

    Facets facets = new Facets(underTest.search(newQueryBuilder().build(), new SearchOptions().addFacets(singletonList("assignees"))), system2.getDefaultTimeZone().toZoneId());
    assertThat(facets.getNames()).containsOnly("assignees", FACET_MODE_EFFORT);
    assertThat(facets.get("assignees")).containsOnly(entry("uuid-steph", 10L), entry("uuid-simon", 20L), entry("", 10L));
    assertThat(facets.get(FACET_MODE_EFFORT)).containsOnly(entry("total", 40L));
  }

  @Test
  void facets_on_author() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueDocTesting.newDoc("I1", project.uuid(), file).setAuthorLogin("steph").setEffort(10L),
      IssueDocTesting.newDoc("I2", project.uuid(), file).setAuthorLogin("simon").setEffort(10L),
      IssueDocTesting.newDoc("I3", project.uuid(), file).setAuthorLogin("simon").setEffort(10L),
      IssueDocTesting.newDoc("I4", project.uuid(), file).setAuthorLogin(null).setEffort(10L));

    Facets facets = new Facets(underTest.search(newQueryBuilder().build(), new SearchOptions().addFacets(singletonList("author"))), system2.getDefaultTimeZone().toZoneId());
    assertThat(facets.getNames()).containsOnly("author", FACET_MODE_EFFORT);
    assertThat(facets.get("author")).containsOnly(entry("steph", 10L), entry("simon", 20L));
    assertThat(facets.get(FACET_MODE_EFFORT)).containsOnly(entry("total", 40L));
  }

  @Test
  void facet_on_created_at() {
    SearchOptions searchOptions = fixtureForCreatedAtFacet();

    Builder query = newQueryBuilder().createdBefore(parseDateTime("2016-01-01T00:00:00+0100"));
    Map<String, Long> createdAt = new Facets(underTest.search(query.build(), searchOptions), system2.getDefaultTimeZone().toZoneId()).get("createdAt");
    assertThat(createdAt).containsOnly(
      entry("2011-01-01", 10L),
      entry("2012-01-01", 0L),
      entry("2013-01-01", 0L),
      entry("2014-01-01", 50L),
      entry("2015-01-01", 10L));
  }

  private SearchOptions fixtureForCreatedAtFacet() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    IssueDoc issue0 = IssueDocTesting.newDoc("ISSUE0", project.uuid(), file).setEffort(10L).setFuncCreationDate(parseDateTime("2011-04-25T01:05:13+0100"));
    IssueDoc issue1 = IssueDocTesting.newDoc("I1", project.uuid(), file).setEffort(10L).setFuncCreationDate(parseDateTime("2014-09-01T12:34:56+0100"));
    IssueDoc issue2 = IssueDocTesting.newDoc("I2", project.uuid(), file).setEffort(10L).setFuncCreationDate(parseDateTime("2014-09-01T23:46:00+0100"));
    IssueDoc issue3 = IssueDocTesting.newDoc("I3", project.uuid(), file).setEffort(10L).setFuncCreationDate(parseDateTime("2014-09-02T12:34:56+0100"));
    IssueDoc issue4 = IssueDocTesting.newDoc("I4", project.uuid(), file).setEffort(10L).setFuncCreationDate(parseDateTime("2014-09-05T12:34:56+0100"));
    IssueDoc issue5 = IssueDocTesting.newDoc("I5", project.uuid(), file).setEffort(10L).setFuncCreationDate(parseDateTime("2014-09-20T12:34:56+0100"));
    IssueDoc issue6 = IssueDocTesting.newDoc("I6", project.uuid(), file).setEffort(10L).setFuncCreationDate(parseDateTime("2015-01-18T12:34:56+0100"));

    indexIssues(issue0, issue1, issue2, issue3, issue4, issue5, issue6);

    return new SearchOptions().addFacets("createdAt");
  }

  private Facets search(String additionalFacet) {
    return new Facets(underTest.search(newQueryBuilder().build(), new SearchOptions().addFacets(singletonList(additionalFacet))), system2.getDefaultTimeZone().toZoneId());
  }

  private Builder newQueryBuilder() {
    return IssueQuery.builder().facetMode(FACET_MODE_EFFORT);
  }
}
