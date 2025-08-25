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

import com.google.common.collect.Iterators;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.lucene.search.TotalHits;
import org.apache.lucene.search.TotalHits.Relation;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.junit.jupiter.api.Test;
import org.sonar.api.issue.Issue;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.SearchOptions;

import static com.google.common.collect.ImmutableSortedSet.of;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.issue.IssueDocTesting.newDoc;
import static org.sonar.server.issue.IssueDocTesting.newDocForProject;

class IssueIndexTest extends IssueIndexTestCommon {

  @Test
  void paging() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);
    for (int i = 0; i < 12; i++) {
      indexIssues(newDoc("I" + i, project.uuid(), file));
    }

    IssueQuery.Builder query = IssueQuery.builder();
    // There are 12 issues in total, with 10 issues per page, the page 2 should only contain 2 elements
    SearchResponse result = underTest.search(query.build(), new SearchOptions().setPage(2, 10));
    assertThat(result.getHits().getHits()).hasSize(2);
    assertThat(result.getHits().getTotalHits()).isEqualTo(new TotalHits(12, TotalHits.Relation.EQUAL_TO));

    result = underTest.search(IssueQuery.builder().build(), new SearchOptions().setOffset(0).setLimit(5));
    assertThat(result.getHits().getHits()).hasSize(5);
    assertThat(result.getHits().getTotalHits()).isEqualTo(new TotalHits(12, TotalHits.Relation.EQUAL_TO));

    result = underTest.search(IssueQuery.builder().build(), new SearchOptions().setOffset(2).setLimit(10));
    assertThat(result.getHits().getHits()).hasSize(10);
    assertThat(result.getHits().getTotalHits()).isEqualTo(new TotalHits(12, TotalHits.Relation.EQUAL_TO));
  }

  @Test
  void search_with_max_limit() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);
    List<IssueDoc> issues = new ArrayList<>();
    for (int i = 0; i < 500; i++) {
      String key = "I" + i;
      issues.add(newDoc(key, project.uuid(), file));
    }
    indexIssues(issues.toArray(new IssueDoc[]{}));

    IssueQuery.Builder query = IssueQuery.builder();
    SearchResponse result = underTest.search(query.build(), new SearchOptions().setLimit(500));
    assertThat(result.getHits().getHits()).hasSize(SearchOptions.MAX_PAGE_SIZE);
  }

  // SONAR-14224
  @Test
  void search_exceeding_default_index_max_window() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);
    List<IssueDoc> issues = new ArrayList<>();
    for (int i = 0; i < 11_000; i++) {
      String key = "I" + i;
      issues.add(newDoc(key, project.uuid(), file));
    }
    indexIssues(issues.toArray(new IssueDoc[]{}));

    IssueQuery.Builder query = IssueQuery.builder();
    SearchResponse result = underTest.search(query.build(), new SearchOptions().setLimit(500));
    assertThat(result.getHits().getHits()).hasSize(SearchOptions.MAX_PAGE_SIZE);
    assertThat(result.getHits().getTotalHits().value).isEqualTo(11_000L);
    assertThat(result.getHits().getTotalHits().relation).isEqualTo(Relation.EQUAL_TO);
  }

  @Test
  void search_directory_should_include_all_descendants() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);
    List<IssueDoc> issues = new ArrayList<>();
    String parentPath = "src/main/java/org/sonar";
    String subDirectoryPath = parentPath + "/server";
    String subSubDirectoryPath = subDirectoryPath + "/issue";
    issues.add(newDoc("i1", project.uuid(), file).setDirectoryPath(parentPath));
    issues.add(newDoc("i2", project.uuid(), file).setDirectoryPath(subDirectoryPath));
    issues.add(newDoc("i3", project.uuid(), file).setDirectoryPath(subSubDirectoryPath));
    indexIssues(issues.toArray(new IssueDoc[]{}));

    IssueQuery.Builder query = IssueQuery.builder();
    query.directories(singletonList(parentPath));

    SearchResponse result = underTest.search(query.build(), new SearchOptions().setLimit(500));

    assertThat(result.getHits().getHits())
      .hasSize(3)
      .extracting(SearchHit::getId)
      .containsExactlyInAnyOrder("i1", "i2", "i3");
  }

  @Test
  void search_nine_issues_with_same_creation_date_sorted_by_creation_date_order_is_sorted_also_by_key() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);
    List<IssueDoc> issues = new ArrayList<>();
    // we are adding issues in reverse order to see if the sort is actually doing anything
    for (int i = 9; i >= 1; i--) {
      String key = "I" + i;
      issues.add(newDoc(key, project.uuid(), file));
    }
    indexIssues(issues.toArray(new IssueDoc[]{}));
    IssueQuery.Builder query = IssueQuery.builder().asc(true);

    SearchResponse result = underTest.search(query.sort(IssueQuery.SORT_BY_CREATION_DATE).build(), new SearchOptions());

    SearchHit[] hits = result.getHits().getHits();
    for (int i = 1; i <= 9; i++) {
      assertThat(hits[i - 1].getId()).isEqualTo("I" + i);
    }
  }

  @Test
  void search_nine_issues_5_times_with_same_creation_date_sorted_by_creation_date_returned_issues_same_order() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);
    List<IssueDoc> issues = new ArrayList<>();
    // we are adding issues in reverse order to see if the sort is actually doing anything
    for (int i = 9; i >= 1; i--) {
      String key = "I" + i;
      issues.add(newDoc(key, project.uuid(), file));
    }
    indexIssues(issues.toArray(new IssueDoc[]{}));
    IssueQuery.Builder query = IssueQuery.builder().asc(true);

    SearchResponse result = underTest.search(query.sort(IssueQuery.SORT_BY_CREATION_DATE).build(), new SearchOptions());

    SearchHit[] originalHits = result.getHits().getHits();
    for (int i = 0; i < 4; i++) {
      result = underTest.search(query.sort(IssueQuery.SORT_BY_CREATION_DATE).build(), new SearchOptions());
      for (int j = 0; j < originalHits.length; j++) {
        SearchHit[] hits = result.getHits().getHits();
        assertThat(originalHits[j].getId()).isEqualTo(hits[j].getId());
      }
    }

  }

  @Test
  void authorized_issues_on_groups() {
    ProjectData project1 = db.components().insertPublicProject();
    ProjectData project2 = db.components().insertPublicProject();
    ProjectData project3 = db.components().insertPublicProject();
    ComponentDto file1 = newFileDto(project1.getMainBranchComponent());
    ComponentDto file2 = newFileDto(project2.getMainBranchComponent());
    ComponentDto file3 = newFileDto(project3.getMainBranchComponent());
    GroupDto group1 = newGroupDto();
    GroupDto group2 = newGroupDto();

    // project1 can be seen by group1
    indexIssue(newDoc("I1", project1.projectUuid(), true, file1));
    authorizationIndexer.allowOnlyGroup(project1.getProjectDto(), group1);
    // project2 can be seen by group2
    indexIssue(newDoc("I2", project2.projectUuid(), true, file2));
    authorizationIndexer.allowOnlyGroup(project2.getProjectDto(), group2);
    // project3 can be seen by nobody but root
    indexIssue(newDoc("I3", project3.projectUuid(), true, file3));

    userSessionRule.logIn().setGroups(group1);
    assertThatSearchReturnsOnly(IssueQuery.builder(), "I1");

    userSessionRule.logIn().setGroups(group2);
    assertThatSearchReturnsOnly(IssueQuery.builder(), "I2");

    userSessionRule.logIn().setGroups(group1, group2);
    assertThatSearchReturnsOnly(IssueQuery.builder(), "I1", "I2");

    GroupDto otherGroup = newGroupDto();
    userSessionRule.logIn().setGroups(otherGroup);
    assertThatSearchReturnsEmpty(IssueQuery.builder());

    userSessionRule.logIn().setGroups(group1, group2);
    assertThatSearchReturnsEmpty(IssueQuery.builder().projectUuids(singletonList(project3.projectUuid())));
  }

  @Test
  void authorized_issues_on_user() {
    ProjectData project1 = db.components().insertPublicProject();
    ProjectData project2 = db.components().insertPublicProject();
    ProjectData project3 = db.components().insertPublicProject();
    ComponentDto file1 = newFileDto(project1.getMainBranchComponent());
    ComponentDto file2 = newFileDto(project2.getMainBranchComponent());
    ComponentDto file3 = newFileDto(project3.getMainBranchComponent());
    UserDto user1 = newUserDto();
    UserDto user2 = newUserDto();

    // project1 can be seen by john, project2 by max, project3 cannot be seen by anyone
    indexIssue(newDoc("I1", project1.projectUuid(), true, file1));
    authorizationIndexer.allowOnlyUser(project1.getProjectDto(), user1);
    indexIssue(newDoc("I2", project2.projectUuid(), true, file2));
    authorizationIndexer.allowOnlyUser(project2.getProjectDto(), user2);
    indexIssue(newDoc("I3", project3.projectUuid(), true, file3));

    userSessionRule.logIn(user1);
    assertThatSearchReturnsOnly(IssueQuery.builder(), "I1");
    assertThatSearchReturnsEmpty(IssueQuery.builder().projectUuids(singletonList(project3.projectUuid())));

    userSessionRule.logIn(user2);
    assertThatSearchReturnsOnly(IssueQuery.builder(), "I2");

    // another user
    userSessionRule.logIn(newUserDto());
    assertThatSearchReturnsEmpty(IssueQuery.builder());
  }

  @Test
  void list_tags() {
    RuleDto r1 = db.rules().insert();
    RuleDto r2 = db.rules().insert();
    ruleIndexer.commitAndIndex(db.getSession(), asList(r1.getUuid(), r2.getUuid()));
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);
    indexIssues(
      newDoc("I42", project.uuid(), file).setRuleUuid(r1.getUuid()).setTags(of("another")),
      newDoc("I1", project.uuid(), file).setRuleUuid(r1.getUuid()).setTags(of("convention", "java8", "bug")),
      newDoc("I2", project.uuid(), file).setRuleUuid(r1.getUuid()).setTags(of("convention", "bug")),
      newDoc("I3", project.uuid(), file).setRuleUuid(r2.getUuid()),
      newDoc("I4", project.uuid(), file).setRuleUuid(r1.getUuid()).setTags(of("convention")));

    assertThat(underTest.searchTags(IssueQuery.builder().build(), null, 100)).containsExactlyInAnyOrder("convention", "java8", "bug", "another");
    assertThat(underTest.searchTags(IssueQuery.builder().build(), null, 2)).containsOnly("another", "bug");
    assertThat(underTest.searchTags(IssueQuery.builder().build(), "vent", 100)).containsOnly("convention");
    assertThat(underTest.searchTags(IssueQuery.builder().build(), null, 1)).containsOnly("another");
    assertThat(underTest.searchTags(IssueQuery.builder().build(), "invalidRegexp[", 100)).isEmpty();
  }

  @Test
  void list_authors() {
    ComponentDto project = newPrivateProjectDto();
    indexIssues(
      newDocForProject("issue1", project).setAuthorLogin("luke.skywalker"),
      newDocForProject("issue2", project).setAuthorLogin("luke@skywalker.name"),
      newDocForProject("issue3", project).setAuthorLogin(null),
      newDocForProject("issue4", project).setAuthorLogin("anakin@skywalker.name"));
    IssueQuery query = IssueQuery.builder().build();

    assertThat(underTest.searchAuthors(query, null, 5)).containsExactly("anakin@skywalker.name", "luke.skywalker", "luke@skywalker.name");
    assertThat(underTest.searchAuthors(query, null, 2)).containsExactly("anakin@skywalker.name", "luke.skywalker");
    assertThat(underTest.searchAuthors(query, "uke", 5)).containsExactly("luke.skywalker", "luke@skywalker.name");
    assertThat(underTest.searchAuthors(query, null, 1)).containsExactly("anakin@skywalker.name");
    assertThat(underTest.searchAuthors(query, null, Integer.MAX_VALUE)).containsExactly("anakin@skywalker.name", "luke.skywalker", "luke@skywalker.name");
  }

  @Test
  void prioritized_rules() {
    ComponentDto project = newPrivateProjectDto();
    indexIssues(
      newDocForProject("issue1", project).setPrioritizedRule(true),
      newDocForProject("issue2", project).setPrioritizedRule(false));

    assertThatSearchReturnsOnly(IssueQuery.builder(), "issue1", "issue2");
    assertThatSearchReturnsOnly(IssueQuery.builder().prioritizedRule(true), "issue1");
    assertThatSearchReturnsOnly(IssueQuery.builder().prioritizedRule(false), "issue2");
  }

  @Test
  void issues_from_analyzer_update() {
    ComponentDto project = newPrivateProjectDto();
    indexIssues(
      newDocForProject("issue1", project).setFromSonarQubeUpdate(true),
      newDocForProject("issue2", project).setFromSonarQubeUpdate(false));

    assertThatSearchReturnsOnly(IssueQuery.builder(), "issue1", "issue2");
    assertThatSearchReturnsOnly(IssueQuery.builder().fromSonarQubeUpdate(true), "issue1");
    assertThatSearchReturnsOnly(IssueQuery.builder().fromSonarQubeUpdate(false), "issue2");
  }

  @Test
  void list_authors_escapes_regexp_special_characters() {
    ComponentDto project = newPrivateProjectDto();
    indexIssues(
      newDocForProject("issue1", project).setAuthorLogin("name++"));
    IssueQuery query = IssueQuery.builder().build();

    assertThat(underTest.searchAuthors(query, "invalidRegexp[", 5)).isEmpty();
    assertThat(underTest.searchAuthors(query, "nam+", 5)).isEmpty();
    assertThat(underTest.searchAuthors(query, "name+", 5)).containsExactly("name++");
    assertThat(underTest.searchAuthors(query, ".*", 5)).isEmpty();
  }

  @Test
  void countTags() {
    ComponentDto project = newPrivateProjectDto();
    indexIssues(
      newDocForProject("issue1", project).setTags(Set.of("convention", "java8", "bug")),
      newDocForProject("issue2", project).setTags(Set.of("convention", "bug")),
      newDocForProject("issue3", project).setTags(emptyList()),
      newDocForProject("issue4", project).setTags(Set.of("convention", "java8", "bug")).setResolution(Issue.RESOLUTION_FIXED),
      newDocForProject("issue5", project).setTags(Set.of("convention")));

    assertThat(underTest.countTags(projectQuery(project.uuid()), 5)).containsOnly(entry("convention", 3L), entry("bug", 2L), entry("java8", 1L));
    assertThat(underTest.countTags(projectQuery(project.uuid()), 2)).contains(entry("convention", 3L), entry("bug", 2L)).doesNotContainEntry("java8", 1L);
    assertThat(underTest.countTags(projectQuery("other"), 10)).isEmpty();
  }

  private IssueQuery projectQuery(String projectUuid) {
    return IssueQuery.builder().projectUuids(singletonList(projectUuid)).resolved(false).build();
  }

  private void indexIssue(IssueDoc issue) {
    issueIndexer.index(Iterators.singletonIterator(issue));
  }
}
