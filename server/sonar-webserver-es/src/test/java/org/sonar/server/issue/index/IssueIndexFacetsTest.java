/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.elasticsearch.action.search.SearchResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.issue.impact.Severity;
import org.sonar.core.rule.RuleType;
import org.sonar.api.server.rule.RulesDefinition.OwaspAsvsVersion;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.es.Facets;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.security.SecurityStandards.SQCategory;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.IntStream.rangeClosed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.doReturn;
import static org.sonar.api.issue.Issue.RESOLUTION_FALSE_POSITIVE;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_REMOVED;
import static org.sonar.api.issue.Issue.RESOLUTION_WONT_FIX;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_CONFIRMED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_REOPENED;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;
import static org.sonar.api.issue.impact.Severity.HIGH;
import static org.sonar.api.issue.impact.SoftwareQuality.MAINTAINABILITY;
import static org.sonar.api.issue.impact.SoftwareQuality.RELIABILITY;
import static org.sonar.api.issue.impact.SoftwareQuality.SECURITY;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rule.Severity.CRITICAL;
import static org.sonar.api.rule.Severity.INFO;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rule.Severity.MINOR;
import static org.sonar.api.rules.CleanCodeAttributeCategory.ADAPTABLE;
import static org.sonar.api.rules.CleanCodeAttributeCategory.CONSISTENT;
import static org.sonar.api.rules.CleanCodeAttributeCategory.INTENTIONAL;
import static org.sonar.api.rules.CleanCodeAttributeCategory.RESPONSIBLE;
import static org.sonar.api.server.rule.RulesDefinition.OwaspTop10Version.Y2017;
import static org.sonar.api.server.rule.RulesDefinition.OwaspTop10Version.Y2021;
import static org.sonar.api.server.rule.RulesDefinition.PciDssVersion.V3_2;
import static org.sonar.api.server.rule.RulesDefinition.PciDssVersion.V4_0;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.core.config.MQRModeConstants.MULTI_QUALITY_MODE_ENABLED;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.rule.RuleTesting.newRule;
import static org.sonar.server.issue.IssueDocTesting.newDoc;
import static org.sonar.server.issue.IssueDocTesting.newDocForProject;

class IssueIndexFacetsTest extends IssueIndexTestCommon {

  @Test
  void facet_on_projectUuids() {
    ComponentDto project = newPrivateProjectDto("ABCD");
    ComponentDto project2 = newPrivateProjectDto("EFGH");

    indexIssues(
      newDoc("I1", project.uuid(), newFileDto(project)),
      newDoc("I2", project.uuid(), newFileDto(project)),
      newDoc("I3", project2.uuid(), newFileDto(project2)));

    assertThatFacetHasExactly(IssueQuery.builder(), "projects", entry("ABCD", 2L), entry("EFGH", 1L));
  }

  @Test
  void facet_on_projectUuids_return_100_entries_plus_selected_values() {

    indexIssues(rangeClosed(1, 110).mapToObj(i -> newDocForProject(newPrivateProjectDto("a" + i))).toArray(IssueDoc[]::new));
    IssueDoc issue1 = newDocForProject(newPrivateProjectDto("project1"));
    IssueDoc issue2 = newDocForProject(newPrivateProjectDto("project2"));
    indexIssues(issue1, issue2);

    assertThatFacetHasSize(IssueQuery.builder().build(), "projects", 100);
    assertThatFacetHasSize(IssueQuery.builder().projectUuids(asList(issue1.projectUuid(), issue2.projectUuid())).build(), "projects", 102);
  }

  @Test
  void facets_on_files() {
    ComponentDto project = newPrivateProjectDto("A");
    ComponentDto dir = newDirectory(project, "src");
    ComponentDto file1 = newFileDto(project, dir, "ABCD");
    ComponentDto file2 = newFileDto(project, dir, "BCDE");
    ComponentDto file3 = newFileDto(project, dir, "CDEF");

    indexIssues(
      newDocForProject("I1", project),
      newDoc("I2", project.uuid(), file1),
      newDoc("I3", project.uuid(), file2),
      newDoc("I4", project.uuid(), file2),
      newDoc("I5", project.uuid(), file3));

    assertThatFacetHasOnly(IssueQuery.builder(), "files", entry("src/NAME_ABCD", 1L), entry("src/NAME_BCDE", 2L), entry("src/NAME_CDEF",
      1L));
  }

  @Test
  void facet_on_files_return_100_entries_plus_selected_values() {
    ComponentDto project = newPrivateProjectDto();
    indexIssues(rangeClosed(1, 110).mapToObj(i -> newDoc(newFileDto(project, null, "a" + i), project.uuid())).toArray(IssueDoc[]::new));
    IssueDoc issue1 = newDoc(newFileDto(project, null, "file1"), project.uuid());
    IssueDoc issue2 = newDoc(newFileDto(project, null, "file2"), project.uuid());
    indexIssues(issue1, issue2);

    assertThatFacetHasSize(IssueQuery.builder().build(), "files", 100);
    assertThatFacetHasSize(IssueQuery.builder().files(asList(issue1.filePath(), issue2.filePath())).build(), "files", 102);
  }

  @Test
  void facets_on_directories() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file1 = newFileDto(project).setPath("src/main/xoo/F1.xoo");
    ComponentDto file2 = newFileDto(project).setPath("F2.xoo");

    indexIssues(
      newDoc("I1", project.uuid(), file1).setDirectoryPath("/src/main/xoo"),
      newDoc("I2", project.uuid(), file2).setDirectoryPath("/"));

    assertThatFacetHasOnly(IssueQuery.builder(), "directories", entry("/src/main/xoo", 1L), entry("/", 1L));
  }

  @Test
  void facet_on_directories_return_100_entries_plus_selected_values() {
    ComponentDto project = newPrivateProjectDto();
    indexIssues(
      rangeClosed(1, 110).mapToObj(i -> newDoc(newFileDto(project, newDirectory(project, "dir" + i)), project.uuid()).setDirectoryPath("a"
        + i)).toArray(IssueDoc[]::new));
    IssueDoc issue1 = newDoc(newFileDto(project, newDirectory(project, "path1")), project.uuid()).setDirectoryPath("directory1");
    IssueDoc issue2 = newDoc(newFileDto(project, newDirectory(project, "path2")), project.uuid()).setDirectoryPath("directory2");
    indexIssues(issue1, issue2);

    assertThatFacetHasSize(IssueQuery.builder().build(), "directories", 100);
    assertThatFacetHasSize(IssueQuery.builder().directories(asList(issue1.directoryPath(), issue2.directoryPath())).build(), "directories"
      , 102);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void facets_on_cwe(boolean mqrMode) {
    doReturn(Optional.of(mqrMode)).when(config).getBoolean(MULTI_QUALITY_MODE_ENABLED);
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setCwe(asList("20", "564",
        "89", "943")),
      newDoc("I2", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setCwe(asList("943")),
      newDoc("I3", project.uuid(), file));

    assertThatFacetHasOnly(IssueQuery.builder(), "cwe",
      entry("943", 2L),
      entry("20", 1L),
      entry("564", 1L),
      entry("89", 1L));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void facets_on_pciDss32(boolean mqrMode) {
    doReturn(Optional.of(mqrMode)).when(config).getBoolean(MULTI_QUALITY_MODE_ENABLED);
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setPciDss32(asList("1", "2")),
      newDoc("I2", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setPciDss32(singletonList("3")),
      newDoc("I3", project.uuid(), file));

    assertThatFacetHasOnly(IssueQuery.builder(), V3_2.prefix(),
      entry("1", 1L),
      entry("2", 1L),
      entry("3", 1L));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void facets_on_pciDss40(boolean mqrMode) {
    doReturn(Optional.of(mqrMode)).when(config).getBoolean(MULTI_QUALITY_MODE_ENABLED);
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setPciDss40(asList("1", "2")),
      newDoc("I2", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setPciDss40(singletonList("3")),
      newDoc("I3", project.uuid(), file));

    assertThatFacetHasOnly(IssueQuery.builder(), V4_0.prefix(),
      entry("1", 1L),
      entry("2", 1L),
      entry("3", 1L));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void facets_on_owaspAsvs40(boolean mqrMode) {
    doReturn(Optional.of(mqrMode)).when(config).getBoolean(MULTI_QUALITY_MODE_ENABLED);
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setOwaspAsvs40(asList("1", "2"
      )),
      newDoc("I2", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setOwaspAsvs40(singletonList(
        "3")),
      newDoc("I3", project.uuid(), file));

    assertThatFacetHasOnly(IssueQuery.builder(), OwaspAsvsVersion.V4_0.prefix(),
      entry("1", 1L),
      entry("2", 1L),
      entry("3", 1L));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void facets_on_owaspTop10(boolean mqrMode) {
    doReturn(Optional.of(mqrMode)).when(config).getBoolean(MULTI_QUALITY_MODE_ENABLED);
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setOwaspTop10(asList("a1",
        "a2")),
      newDoc("I2", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setOwaspTop10(singletonList(
        "a3")),
      newDoc("I3", project.uuid(), file));

    assertThatFacetHasOnly(IssueQuery.builder(), Y2017.prefix(),
      entry("a1", 1L),
      entry("a2", 1L),
      entry("a3", 1L));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void facets_on_owaspTop10_2021(boolean mqrMode) {
    doReturn(Optional.of(mqrMode)).when(config).getBoolean(MULTI_QUALITY_MODE_ENABLED);
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setOwaspTop10For2021(asList(
        "a1", "a2")),
      newDoc("I2", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setOwaspTop10For2021(singletonList("a3")),
      newDoc("I3", project.uuid(), file));

    assertThatFacetHasExactly(IssueQuery.builder(), Y2021.prefix(),
      entry("a1", 1L),
      entry("a2", 1L),
      entry("a3", 1L));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void facets_on_owaspTop10_2021_stay_ordered(boolean mqrMode) {
    doReturn(Optional.of(mqrMode)).when(config).getBoolean(MULTI_QUALITY_MODE_ENABLED);
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setOwaspTop10For2021(asList(
        "a1", "a2")),
      newDoc("I2", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setOwaspTop10For2021(singletonList("a3")),
      newDoc("I3", project.uuid(), file));

    assertThatFacetHasExactly(IssueQuery.builder().owaspTop10For2021(Collections.singletonList("a3")), Y2021.prefix(),
      entry("a1", 1L),
      entry("a2", 1L),
      entry("a3", 1L));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void facets_on_sansTop25(boolean mqrMode) {
    doReturn(Optional.of(mqrMode)).when(config).getBoolean(MULTI_QUALITY_MODE_ENABLED);
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setSansTop25(asList("porous" +
          "-defenses", "risky-resource",
        "insecure-interaction")),
      newDoc("I2", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setSansTop25(singletonList(
        "porous-defenses")),
      newDoc("I3", project.uuid(), file));

    assertThatFacetHasOnly(IssueQuery.builder(), "sansTop25",
      entry("insecure-interaction", 1L),
      entry("porous-defenses", 2L),
      entry("risky-resource", 1L));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void facets_on_sonarSourceSecurity(boolean mqrMode) {
    doReturn(Optional.of(mqrMode)).when(config).getBoolean(MULTI_QUALITY_MODE_ENABLED);
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setSonarSourceSecurityCategory(SQCategory.BUFFER_OVERFLOW),
      newDoc("I2", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setSonarSourceSecurityCategory(SQCategory.DOS),
      newDoc("I3", project.uuid(), file));

    assertThatFacetHasOnly(IssueQuery.builder(), "sonarsourceSecurity",
      entry("buffer-overflow", 1L),
      entry("dos", 1L));
  }

  @Test
  void facets_on_severities() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setSeverity(INFO),
      newDoc("I2", project.uuid(), file).setSeverity(INFO),
      newDoc("I3", project.uuid(), file).setSeverity(MAJOR));

    assertThatFacetHasOnly(IssueQuery.builder(), "severities", entry("INFO", 2L), entry("MAJOR", 1L));
  }

  @Test
  void facet_on_severities_return_5_entries_max() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I2", project.uuid(), file).setSeverity(INFO),
      newDoc("I1", project.uuid(), file).setSeverity(MINOR),
      newDoc("I3", project.uuid(), file).setSeverity(MAJOR),
      newDoc("I4", project.uuid(), file).setSeverity(CRITICAL),
      newDoc("I5", project.uuid(), file).setSeverity(BLOCKER),
      newDoc("I6", project.uuid(), file).setSeverity(MAJOR));

    assertThatFacetHasSize(IssueQuery.builder().build(), "severities", 5);
  }

  @Test
  void facets_on_statuses() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setStatus(STATUS_CLOSED),
      newDoc("I2", project.uuid(), file).setStatus(STATUS_CLOSED),
      newDoc("I3", project.uuid(), file).setStatus(STATUS_OPEN));

    assertThatFacetHasOnly(IssueQuery.builder(), "statuses", entry("CLOSED", 2L), entry("OPEN", 1L));
  }

  @Test
  void facet_on_statuses_return_5_entries_max() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setStatus(STATUS_OPEN),
      newDoc("I2", project.uuid(), file).setStatus(STATUS_CONFIRMED),
      newDoc("I3", project.uuid(), file).setStatus(STATUS_REOPENED),
      newDoc("I4", project.uuid(), file).setStatus(STATUS_RESOLVED),
      newDoc("I5", project.uuid(), file).setStatus(STATUS_CLOSED),
      newDoc("I6", project.uuid(), file).setStatus(STATUS_OPEN));

    assertThatFacetHasSize(IssueQuery.builder().build(), "statuses", 5);
  }

  @Test
  void facets_on_resolutions() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setResolution(RESOLUTION_FALSE_POSITIVE),
      newDoc("I2", project.uuid(), file).setResolution(RESOLUTION_FALSE_POSITIVE),
      newDoc("I3", project.uuid(), file).setResolution(RESOLUTION_FIXED));

    assertThatFacetHasOnly(IssueQuery.builder(), "resolutions", entry("FALSE-POSITIVE", 2L), entry("FIXED", 1L));
  }

  @Test
  void search_shouldReturnIssueStatusesFacet() {
    ComponentDto mainBranch = newPrivateProjectDto();
    ComponentDto file = newFileDto(mainBranch);

    indexIssues(
      newDoc("I1", mainBranch.uuid(), file).setIssueStatus(IssueStatus.CONFIRMED.name()),
      newDoc("I2", mainBranch.uuid(), file).setIssueStatus(IssueStatus.FIXED.name()),
      newDoc("I3", mainBranch.uuid(), file).setIssueStatus(IssueStatus.OPEN.name()),
      newDoc("I4", mainBranch.uuid(), file).setIssueStatus(IssueStatus.OPEN.name()),
      newDoc("I5", mainBranch.uuid(), file).setIssueStatus(IssueStatus.ACCEPTED.name()),
      newDoc("I6", mainBranch.uuid(), file).setIssueStatus(IssueStatus.ACCEPTED.name()),
      newDoc("I7", mainBranch.uuid(), file).setIssueStatus(IssueStatus.ACCEPTED.name()),
      newDoc("I8", mainBranch.uuid(), file).setIssueStatus(IssueStatus.FALSE_POSITIVE.name()),
      newDoc("I9", mainBranch.uuid(), file).setIssueStatus(IssueStatus.FALSE_POSITIVE.name()));

    assertThatFacetHasSize(IssueQuery.builder().build(), "issueStatuses", 5);
    assertThatFacetHasOnly(IssueQuery.builder(), "issueStatuses",
      entry("OPEN", 2L),
      entry("CONFIRMED", 1L),
      entry("FALSE_POSITIVE", 2L),
      entry("ACCEPTED", 3L),
      entry("FIXED", 1L));
  }

  @Test
  void facets_on_resolutions_return_5_entries_max() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setResolution(RESOLUTION_FIXED),
      newDoc("I2", project.uuid(), file).setResolution(RESOLUTION_FALSE_POSITIVE),
      newDoc("I3", project.uuid(), file).setResolution(RESOLUTION_REMOVED),
      newDoc("I4", project.uuid(), file).setResolution(RESOLUTION_WONT_FIX),
      newDoc("I5", project.uuid(), file).setResolution(null));

    assertThatFacetHasSize(IssueQuery.builder().build(), "resolutions", 5);
  }

  @Test
  void facets_on_languages() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);
    RuleDto ruleDefinitionDto = newRule();
    db.rules().insert(ruleDefinitionDto);

    indexIssues(newDoc("I1", project.uuid(), file).setRuleUuid(ruleDefinitionDto.getUuid()).setLanguage("xoo"));

    assertThatFacetHasOnly(IssueQuery.builder(), "languages", entry("xoo", 1L));
  }

  @Test
  void facets_on_languages_return_100_entries_plus_selected_values() {
    ComponentDto project = newPrivateProjectDto();
    indexIssues(rangeClosed(1, 100).mapToObj(i -> newDoc(newFileDto(project), project.uuid()).setLanguage("a" + i)).toArray(IssueDoc[]::new));
    IssueDoc issue1 = newDoc(newFileDto(project), project.uuid()).setLanguage("language1");
    IssueDoc issue2 = newDoc(newFileDto(project), project.uuid()).setLanguage("language2");
    indexIssues(issue1, issue2);

    assertThatFacetHasSize(IssueQuery.builder().build(), "languages", 100);
    assertThatFacetHasSize(IssueQuery.builder().languages(asList(issue1.language(), issue2.language())).build(), "languages", 102);
  }

  @Test
  void facets_on_assignees() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setAssigneeUuid("steph-uuid"),
      newDoc("I2", project.uuid(), file).setAssigneeUuid("marcel-uuid"),
      newDoc("I3", project.uuid(), file).setAssigneeUuid("marcel-uuid"),
      newDoc("I4", project.uuid(), file).setAssigneeUuid(null));

    assertThatFacetHasOnly(IssueQuery.builder(), "assignees", entry("steph-uuid", 1L), entry("marcel-uuid", 2L), entry("", 1L));
  }

  @Test
  void facets_on_assignees_return_only_100_entries_plus_selected_values() {
    ComponentDto project = newPrivateProjectDto();
    indexIssues(rangeClosed(1, 110).mapToObj(i -> newDoc(newFileDto(project), project.uuid()).setAssigneeUuid("a" + i)).toArray(IssueDoc[]::new));
    IssueDoc issue1 = newDoc(newFileDto(project), project.uuid()).setAssigneeUuid("user1");
    IssueDoc issue2 = newDoc(newFileDto(project), project.uuid()).setAssigneeUuid("user2");
    indexIssues(issue1, issue2);

    assertThatFacetHasSize(IssueQuery.builder().build(), "assignees", 100);
    assertThatFacetHasSize(IssueQuery.builder().assigneeUuids(asList(issue1.assigneeUuid(), issue2.assigneeUuid())).build(), "assignees",
      102);
  }

  @Test
  void facets_on_assignees_supports_dashes() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setAssigneeUuid("j-b-uuid"),
      newDoc("I2", project.uuid(), file).setAssigneeUuid("marcel-uuid"),
      newDoc("I3", project.uuid(), file).setAssigneeUuid("marcel-uuid"),
      newDoc("I4", project.uuid(), file).setAssigneeUuid(null));

    assertThatFacetHasOnly(IssueQuery.builder().assigneeUuids(singletonList("j-b")),
      "assignees", entry("j-b-uuid", 1L), entry("marcel-uuid", 2L), entry("", 1L));
  }

  @Test
  void facets_on_author() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setAuthorLogin("steph"),
      newDoc("I2", project.uuid(), file).setAuthorLogin("marcel"),
      newDoc("I3", project.uuid(), file).setAuthorLogin("marcel"),
      newDoc("I4", project.uuid(), file).setAuthorLogin(null));

    assertThatFacetHasOnly(IssueQuery.builder(), "author", entry("steph", 1L), entry("marcel", 2L));
  }

  @Test
  void facets_on_prioritized_rule() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setPrioritizedRule(false),
      newDoc("I2", project.uuid(), file).setPrioritizedRule(true),
      newDoc("I3", project.uuid(), file).setPrioritizedRule(true)
    );

    assertThatFacetHasOnly(IssueQuery.builder(), "prioritizedRule", entry("true", 2L), entry("false", 1L));
  }

  @Test
  void facets_on_issues_from_analyzer_update() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setFromSonarQubeUpdate(false),
      newDoc("I2", project.uuid(), file).setFromSonarQubeUpdate(true),
      newDoc("I3", project.uuid(), file).setFromSonarQubeUpdate(true)
    );

    assertThatFacetHasOnly(IssueQuery.builder(), "fromSonarQubeUpdate", entry("true", 2L), entry("false", 1L));
  }

  @Test
  void facets_on_authors_return_100_entries_plus_selected_values() {
    ComponentDto project = newPrivateProjectDto();
    indexIssues(rangeClosed(1, 110).mapToObj(i -> newDoc(newFileDto(project), project.uuid()).setAuthorLogin("a" + i)).toArray(IssueDoc[]::new));
    IssueDoc issue1 = newDoc(newFileDto(project), project.uuid()).setAuthorLogin("user1");
    IssueDoc issue2 = newDoc(newFileDto(project), project.uuid()).setAuthorLogin("user2");
    indexIssues(issue1, issue2);

    assertThatFacetHasSize(IssueQuery.builder().build(), "author", 100);
    assertThatFacetHasSize(IssueQuery.builder().authors(asList(issue1.authorLogin(), issue2.authorLogin())).build(), "author", 102);
  }

  @Test
  void facet_on_created_at_with_less_than_20_days_use_system_timezone_by_default() {
    SearchOptions options = fixtureForCreatedAtFacet();

    IssueQuery query = IssueQuery.builder()
      .createdAfter(parseDateTime("2014-09-01T00:00:00+0100"))
      .createdBefore(parseDateTime("2014-09-08T00:00:00+0100"))
      .build();
    SearchResponse result = underTest.search(query, options);
    Map<String, Long> buckets = new Facets(result, system2.getDefaultTimeZone().toZoneId()).get("createdAt");
    assertThat(buckets).containsOnly(
      entry("2014-08-31", 0L),
      entry("2014-09-01", 2L),
      entry("2014-09-02", 1L),
      entry("2014-09-03", 0L),
      entry("2014-09-04", 0L),
      entry("2014-09-05", 1L),
      entry("2014-09-06", 0L),
      entry("2014-09-07", 0L));
  }

  @Test
  void facet_on_created_at_with_less_than_20_days_use_user_timezone_if_provided() {
    // Use timezones very far from each other in order to see some issues moving to a different calendar day
    final ZoneId plus14 = ZoneId.of("Pacific/Kiritimati");
    final ZoneId minus11 = ZoneId.of("Pacific/Pago_Pago");

    SearchOptions options = fixtureForCreatedAtFacet();

    final Date startDate = parseDateTime("2014-09-01T00:00:00+0000");
    final Date endDate = parseDateTime("2014-09-08T00:00:00+0000");

    IssueQuery queryPlus14 = IssueQuery.builder()
      .createdAfter(startDate)
      .createdBefore(endDate)
      .timeZone(plus14)
      .build();
    SearchResponse resultPlus14 = underTest.search(queryPlus14, options);
    Map<String, Long> bucketsPlus14 = new Facets(resultPlus14, plus14).get("createdAt");
    assertThat(bucketsPlus14).containsOnly(
      entry("2014-09-01", 0L),
      entry("2014-09-02", 2L),
      entry("2014-09-03", 1L),
      entry("2014-09-04", 0L),
      entry("2014-09-05", 0L),
      entry("2014-09-06", 1L),
      entry("2014-09-07", 0L),
      entry("2014-09-08", 0L));

    IssueQuery queryMinus11 = IssueQuery.builder()
      .createdAfter(startDate)
      .createdBefore(endDate)
      .timeZone(minus11)
      .build();
    SearchResponse resultMinus11 = underTest.search(queryMinus11, options);
    Map<String, Long> bucketsMinus11 = new Facets(resultMinus11, minus11).get("createdAt");
    assertThat(bucketsMinus11).containsOnly(
      entry("2014-08-31", 1L),
      entry("2014-09-01", 1L),
      entry("2014-09-02", 1L),
      entry("2014-09-03", 0L),
      entry("2014-09-04", 0L),
      entry("2014-09-05", 1L),
      entry("2014-09-06", 0L),
      entry("2014-09-07", 0L));
  }

  @Test
  void facet_on_created_at_with_less_than_20_weeks() {
    SearchOptions options = fixtureForCreatedAtFacet();

    SearchResponse result = underTest.search(IssueQuery.builder()
        .createdAfter(parseDateTime("2014-09-01T00:00:00+0100"))
        .createdBefore(parseDateTime("2014-09-21T00:00:00+0100")).build(),
      options);
    Map<String, Long> createdAt = new Facets(result, system2.getDefaultTimeZone().toZoneId()).get("createdAt");
    assertThat(createdAt).containsOnly(
      entry("2014-08-25", 0L),
      entry("2014-09-01", 4L),
      entry("2014-09-08", 0L),
      entry("2014-09-15", 1L));
  }

  @Test
  void facet_on_created_at_with_less_than_20_months() {
    SearchOptions options = fixtureForCreatedAtFacet();

    SearchResponse result = underTest.search(IssueQuery.builder()
        .createdAfter(parseDateTime("2014-09-01T00:00:00+0100"))
        .createdBefore(parseDateTime("2015-01-19T00:00:00+0100")).build(),
      options);
    Map<String, Long> createdAt = new Facets(result, system2.getDefaultTimeZone().toZoneId()).get("createdAt");
    assertThat(createdAt).containsOnly(
      entry("2014-08-01", 0L),
      entry("2014-09-01", 5L),
      entry("2014-10-01", 0L),
      entry("2014-11-01", 0L),
      entry("2014-12-01", 0L),
      entry("2015-01-01", 1L));
  }

  @Test
  void facet_on_created_at_with_more_than_20_months() {
    SearchOptions options = fixtureForCreatedAtFacet();

    SearchResponse result = underTest.search(IssueQuery.builder()
        .createdAfter(parseDateTime("2011-01-01T00:00:00+0100"))
        .createdBefore(parseDateTime("2016-01-01T00:00:00+0100")).build(),
      options);
    Map<String, Long> createdAt = new Facets(result, system2.getDefaultTimeZone().toZoneId()).get("createdAt");
    assertThat(createdAt).containsOnly(
      entry("2010-01-01", 0L),
      entry("2011-01-01", 1L),
      entry("2012-01-01", 0L),
      entry("2013-01-01", 0L),
      entry("2014-01-01", 5L),
      entry("2015-01-01", 1L));
  }

  @Test
  void facet_on_created_at_with_one_day() {
    SearchOptions options = fixtureForCreatedAtFacet();

    SearchResponse result = underTest.search(IssueQuery.builder()
        .createdAfter(parseDateTime("2014-09-01T00:00:00-0100"))
        .createdBefore(parseDateTime("2014-09-02T00:00:00-0100")).build(),
      options);
    Map<String, Long> createdAt = new Facets(result, system2.getDefaultTimeZone().toZoneId()).get("createdAt");
    assertThat(createdAt).containsOnly(
      entry("2014-09-01", 2L));
  }

  @Test
  void facet_on_created_at_with_bounds_outside_of_data() {
    SearchOptions options = fixtureForCreatedAtFacet();

    SearchResponse result = underTest.search(IssueQuery.builder()
      .createdAfter(parseDateTime("2009-01-01T00:00:00+0100"))
      .createdBefore(parseDateTime("2016-01-01T00:00:00+0100"))
      .build(), options);
    Map<String, Long> createdAt = new Facets(result, system2.getDefaultTimeZone().toZoneId()).get("createdAt");
    assertThat(createdAt).containsOnly(
      entry("2008-01-01", 0L),
      entry("2009-01-01", 0L),
      entry("2010-01-01", 0L),
      entry("2011-01-01", 1L),
      entry("2012-01-01", 0L),
      entry("2013-01-01", 0L),
      entry("2014-01-01", 5L),
      entry("2015-01-01", 1L));
  }

  @Test
  void facet_on_created_at_without_start_bound() {
    SearchOptions searchOptions = fixtureForCreatedAtFacet();

    SearchResponse result = underTest.search(IssueQuery.builder()
        .createdBefore(parseDateTime("2016-01-01T00:00:00+0100")).build(),
      searchOptions);
    Map<String, Long> createdAt = new Facets(result, system2.getDefaultTimeZone().toZoneId()).get("createdAt");
    assertThat(createdAt).containsOnly(
      entry("2011-01-01", 1L),
      entry("2012-01-01", 0L),
      entry("2013-01-01", 0L),
      entry("2014-01-01", 5L),
      entry("2015-01-01", 1L));
  }

  @Test
  void facet_on_created_at_without_issues() {
    SearchOptions searchOptions = new SearchOptions().addFacets("createdAt");

    SearchResponse result = underTest.search(IssueQuery.builder().build(), searchOptions);
    Map<String, Long> createdAt = new Facets(result, system2.getDefaultTimeZone().toZoneId()).get("createdAt");
    assertThat(createdAt).isNull();
  }

  @Test
  void search_shouldReturnCodeVariantsFacet() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setCodeVariants(asList("variant1", "variant2")),
      newDoc("I2", project.uuid(), file).setCodeVariants(singletonList("variant2")),
      newDoc("I3", project.uuid(), file).setCodeVariants(singletonList("variant3")),
      newDoc("I4", project.uuid(), file));

    assertThatFacetHasOnly(IssueQuery.builder(), "codeVariants",
      entry("variant1", 1L),
      entry("variant2", 2L),
      entry("variant3", 1L));
  }

  @Test
  void search_shouldReturnImpactSoftwareQualitiesFacet() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setImpacts(Map.of(
        MAINTAINABILITY, HIGH,
        RELIABILITY, org.sonar.api.issue.impact.Severity.MEDIUM)),
      newDoc("I2", project.uuid(), file).setImpacts(Map.of(
        MAINTAINABILITY, org.sonar.api.issue.impact.Severity.LOW)),
      newDoc("I3", project.uuid(), file).setImpacts(Map.of(
        RELIABILITY, HIGH)),
      newDoc("I4", project.uuid(), file).setImpacts(Map.of(
        MAINTAINABILITY, org.sonar.api.issue.impact.Severity.LOW)));

    assertThatFacetHasOnly(IssueQuery.builder(), "impactSoftwareQualities",
      entry("MAINTAINABILITY", 3L),
      entry("RELIABILITY", 2L),
      entry("SECURITY", 0L));
  }

  @Test
  void search_whenFilteredOnSeverity_shouldReturnImpactSoftwareQualitiesFacet() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setImpacts(Map.of(
          MAINTAINABILITY, HIGH,
          RELIABILITY, org.sonar.api.issue.impact.Severity.MEDIUM))
        .setTags(singletonList("my-tag")),
      newDoc("I2", project.uuid(), file).setImpacts(Map.of(
        MAINTAINABILITY, org.sonar.api.issue.impact.Severity.LOW)),
      newDoc("I3", project.uuid(), file).setImpacts(Map.of(
        RELIABILITY, HIGH)),
      newDoc("I4", project.uuid(), file).setImpacts(Map.of(
        MAINTAINABILITY, org.sonar.api.issue.impact.Severity.LOW)));

    assertThatFacetHasOnly(IssueQuery.builder().impactSoftwareQualities(Set.of(MAINTAINABILITY.name())).impactSeverities(Set.of(org.sonar.api.issue.impact.Severity.LOW.name())),
      "impactSoftwareQualities",
      entry("MAINTAINABILITY", 2L),
      entry("RELIABILITY", 0L),
      entry("SECURITY", 0L));

    assertThatFacetHasOnly(IssueQuery.builder().impactSoftwareQualities(Set.of(MAINTAINABILITY.name())), "impactSoftwareQualities",
      entry("MAINTAINABILITY", 3L),
      entry("RELIABILITY", 2L),
      entry("SECURITY", 0L));

    assertThatFacetHasOnly(IssueQuery.builder().impactSeverities(Set.of(Severity.MEDIUM.name())), "impactSoftwareQualities",
      entry("MAINTAINABILITY", 0L),
      entry("RELIABILITY", 1L),
      entry("SECURITY", 0L));

    assertThatFacetHasOnly(IssueQuery.builder().impactSeverities(Set.of(HIGH.name())),
      "impactSoftwareQualities",
      entry("MAINTAINABILITY", 1L),
      entry("RELIABILITY", 1L),
      entry("SECURITY", 0L));

    assertThatFacetHasOnly(IssueQuery.builder()
        .tags(singletonList("my-tag"))
        .impactSeverities(Set.of(HIGH.name())), "impactSoftwareQualities",
      entry("MAINTAINABILITY", 1L),
      entry("RELIABILITY", 0L),
      entry("SECURITY", 0L));
  }

  @Test
  void search_whenFilteredOnSeverityAndSoftwareQuality_shouldReturnImpactFacets() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setImpacts(Map.of(
        MAINTAINABILITY, org.sonar.api.issue.impact.Severity.LOW,
        RELIABILITY, org.sonar.api.issue.impact.Severity.LOW,
        SECURITY, org.sonar.api.issue.impact.Severity.BLOCKER)));

    assertThatFacetHasOnly(IssueQuery.builder()
        .impactSoftwareQualities(Set.of(MAINTAINABILITY.name()))
        .impactSeverities(Set.of(org.sonar.api.issue.impact.Severity.LOW.name())),
      "impactSoftwareQualities",
      entry("MAINTAINABILITY", 1L),
      entry("RELIABILITY", 1L),
      entry("SECURITY", 0L));

    assertThatFacetHasOnly(IssueQuery.builder()
        .impactSoftwareQualities(Set.of(MAINTAINABILITY.name()))
        .impactSeverities(Set.of(org.sonar.api.issue.impact.Severity.LOW.name())),
      "impactSeverities",
      entry("HIGH", 0L),
      entry("MEDIUM", 0L),
      entry("LOW", 1L),
      entry("INFO", 0L),
      entry("BLOCKER", 0L));
  }

  @Test
  void search_shouldReturnImpactSeverityFacet() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setImpacts(Map.of(
        MAINTAINABILITY, HIGH,
        RELIABILITY, org.sonar.api.issue.impact.Severity.MEDIUM)),
      newDoc("I2", project.uuid(), file).setImpacts(Map.of(
        MAINTAINABILITY, org.sonar.api.issue.impact.Severity.LOW)),
      newDoc("I3", project.uuid(), file).setImpacts(Map.of(
        RELIABILITY, HIGH,
        SECURITY, org.sonar.api.issue.impact.Severity.BLOCKER)),
      newDoc("I4", project.uuid(), file).setImpacts(Map.of(
        MAINTAINABILITY, org.sonar.api.issue.impact.Severity.LOW,
        RELIABILITY, org.sonar.api.issue.impact.Severity.INFO)));

    assertThatFacetHasOnly(IssueQuery.builder(), "impactSeverities",
      entry("HIGH", 2L),
      entry("MEDIUM", 1L),
      entry("LOW", 2L),
      entry("BLOCKER", 1L),
      entry("INFO", 1L));
  }

  @Test
  void search_whenFilteredOnSoftwareQuality_shouldReturnImpactSeverityFacet() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setImpacts(Map.of(
        MAINTAINABILITY, HIGH,
        RELIABILITY, org.sonar.api.issue.impact.Severity.MEDIUM)),
      newDoc("I2", project.uuid(), file).setImpacts(Map.of(
        MAINTAINABILITY, org.sonar.api.issue.impact.Severity.LOW)),
      newDoc("I3", project.uuid(), file).setImpacts(Map.of(
        RELIABILITY, HIGH)),
      newDoc("I4", project.uuid(), file).setImpacts(Map.of(
        MAINTAINABILITY, org.sonar.api.issue.impact.Severity.LOW)));

    assertThatFacetHasOnly(IssueQuery.builder().impactSoftwareQualities(Set.of(MAINTAINABILITY.name())), "impactSeverities",
      entry("HIGH", 1L),
      entry("MEDIUM", 0L),
      entry("LOW", 2L),
      entry("BLOCKER", 0L),
      entry("INFO", 0L));
  }

  @Test
  void search_shouldReturnCleanCodeAttributeCategoryFacet() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setCleanCodeAttributeCategory(ADAPTABLE.name()),
      newDoc("I2", project.uuid(), file).setCleanCodeAttributeCategory(ADAPTABLE.name()),
      newDoc("I3", project.uuid(), file).setCleanCodeAttributeCategory(CONSISTENT.name()),
      newDoc("I4", project.uuid(), file).setCleanCodeAttributeCategory(INTENTIONAL.name()),
      newDoc("I5", project.uuid(), file).setCleanCodeAttributeCategory(INTENTIONAL.name()),
      newDoc("I6", project.uuid(), file).setCleanCodeAttributeCategory(INTENTIONAL.name()),
      newDoc("I7", project.uuid(), file).setCleanCodeAttributeCategory(INTENTIONAL.name()),
      newDoc("I8", project.uuid(), file).setCleanCodeAttributeCategory(RESPONSIBLE.name()));

    assertThatFacetHasOnly(IssueQuery.builder(), "cleanCodeAttributeCategories",
      entry("INTENTIONAL", 4L),
      entry("ADAPTABLE", 2L),
      entry("CONSISTENT", 1L),
      entry("RESPONSIBLE", 1L));
  }

  @Test
  void search_whenFilteredByTags_shouldReturnCleanCodeAttributeCategoryFacet() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setCleanCodeAttributeCategory(ADAPTABLE.name()).setTags(singletonList("tag-1")),
      newDoc("I2", project.uuid(), file).setCleanCodeAttributeCategory(ADAPTABLE.name()).setTags(singletonList("tag-1")),
      newDoc("I3", project.uuid(), file).setCleanCodeAttributeCategory(CONSISTENT.name()),
      newDoc("I4", project.uuid(), file).setCleanCodeAttributeCategory(INTENTIONAL.name()).setTags(singletonList("tag-1")),
      newDoc("I5", project.uuid(), file).setCleanCodeAttributeCategory(INTENTIONAL.name()),
      newDoc("I6", project.uuid(), file).setCleanCodeAttributeCategory(INTENTIONAL.name()),
      newDoc("I7", project.uuid(), file).setCleanCodeAttributeCategory(INTENTIONAL.name()),
      newDoc("I8", project.uuid(), file).setCleanCodeAttributeCategory(RESPONSIBLE.name()).setTags(singletonList("tag-3")));

    assertThatFacetHasOnly(IssueQuery.builder().tags(singletonList("tag-1")), "cleanCodeAttributeCategories",
      entry("INTENTIONAL", 1L),
      entry("ADAPTABLE", 2L));
  }

  private SearchOptions fixtureForCreatedAtFacet() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    IssueDoc issue0 = newDoc("ISSUE0", project.uuid(), file).setFuncCreationDate(parseDateTime("2011-04-25T00:05:13+0000"));
    IssueDoc issue1 = newDoc("I1", project.uuid(), file).setFuncCreationDate(parseDateTime("2014-09-01T10:34:56+0000"));
    IssueDoc issue2 = newDoc("I2", project.uuid(), file).setFuncCreationDate(parseDateTime("2014-09-01T22:46:00+0000"));
    IssueDoc issue3 = newDoc("I3", project.uuid(), file).setFuncCreationDate(parseDateTime("2014-09-02T11:34:56+0000"));
    IssueDoc issue4 = newDoc("I4", project.uuid(), file).setFuncCreationDate(parseDateTime("2014-09-05T11:34:56+0000"));
    IssueDoc issue5 = newDoc("I5", project.uuid(), file).setFuncCreationDate(parseDateTime("2014-09-20T11:34:56+0000"));
    IssueDoc issue6 = newDoc("I6", project.uuid(), file).setFuncCreationDate(parseDateTime("2015-01-18T11:34:56+0000"));

    indexIssues(issue0, issue1, issue2, issue3, issue4, issue5, issue6);

    return new SearchOptions().addFacets("createdAt");
  }

  @SafeVarargs
  private void assertThatFacetHasExactly(IssueQuery.Builder query, String facet, Map.Entry<String, Long>... expectedEntries) {
    SearchResponse result = underTest.search(query.build(), new SearchOptions().addFacets(singletonList(facet)));
    Facets facets = new Facets(result, system2.getDefaultTimeZone().toZoneId());
    assertThat(facets.getNames()).containsOnly(facet, "effort");
    assertThat(facets.get(facet)).containsExactly(expectedEntries);
  }

  @SafeVarargs
  private void assertThatFacetHasOnly(IssueQuery.Builder query, String facet, Map.Entry<String, Long>... expectedEntries) {
    SearchResponse result = underTest.search(query.build(), new SearchOptions().addFacets(singletonList(facet)));
    Facets facets = new Facets(result, system2.getDefaultTimeZone().toZoneId());
    assertThat(facets.getNames()).containsOnly(facet, "effort");
    assertThat(facets.get(facet)).containsOnly(expectedEntries);
  }

  private void assertThatFacetHasSize(IssueQuery issueQuery, String facet, int expectedSize) {
    SearchResponse result = underTest.search(issueQuery, new SearchOptions().addFacets(singletonList(facet)));
    Facets facets = new Facets(result, system2.getDefaultTimeZone().toZoneId());
    assertThat(facets.get(facet)).hasSize(expectedSize);
  }
}
