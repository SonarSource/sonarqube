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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.assertj.core.groups.Tuple;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.issue.Issue;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.permission.index.IndexPermissions;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.tester.UserSessionRule;

import static com.google.common.collect.ImmutableSortedSet.of;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.TimeZone.getTimeZone;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.rules.ExpectedException.none;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.api.rules.RuleType.VULNERABILITY;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.organization.OrganizationTesting.newOrganizationDto;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.issue.IssueDocTesting.newDoc;

public class IssueIndexTest {

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
  private RuleIndexer ruleIndexer = new RuleIndexer(es.client(), db.getDbClient());
  private PermissionIndexerTester authorizationIndexer = new PermissionIndexerTester(es, issueIndexer);

  private IssueIndex underTest = new IssueIndex(es.client(), system2, userSessionRule, new WebAuthorizationTypeSupport(userSessionRule));

  @Test
  public void paging() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);
    for (int i = 0; i < 12; i++) {
      indexIssues(newDoc("I" + i, file));
    }

    IssueQuery.Builder query = IssueQuery.builder();
    // There are 12 issues in total, with 10 issues per page, the page 2 should only contain 2 elements
    SearchResponse result = underTest.search(query.build(), new SearchOptions().setPage(2, 10));
    assertThat(result.getHits().getHits()).hasSize(2);
    assertThat(result.getHits().getTotalHits()).isEqualTo(12);

    result = underTest.search(IssueQuery.builder().build(), new SearchOptions().setOffset(0).setLimit(5));
    assertThat(result.getHits().getHits()).hasSize(5);
    assertThat(result.getHits().getTotalHits()).isEqualTo(12);

    result = underTest.search(IssueQuery.builder().build(), new SearchOptions().setOffset(2).setLimit(0));
    assertThat(result.getHits().getHits()).hasSize(10);
    assertThat(result.getHits().getTotalHits()).isEqualTo(12);
  }

  @Test
  public void search_with_max_limit() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);
    List<IssueDoc> issues = new ArrayList<>();
    for (int i = 0; i < 500; i++) {
      String key = "I" + i;
      issues.add(newDoc(key, file));
    }
    indexIssues(issues.toArray(new IssueDoc[] {}));

    IssueQuery.Builder query = IssueQuery.builder();
    SearchResponse result = underTest.search(query.build(), new SearchOptions().setLimit(Integer.MAX_VALUE));
    assertThat(result.getHits().getHits()).hasSize(SearchOptions.MAX_LIMIT);
  }

  @Test
  public void authorized_issues_on_groups() {
    OrganizationDto org = newOrganizationDto();
    ComponentDto project1 = newPrivateProjectDto(org);
    ComponentDto project2 = newPrivateProjectDto(org);
    ComponentDto project3 = newPrivateProjectDto(org);
    ComponentDto file1 = newFileDto(project1, null);
    ComponentDto file2 = newFileDto(project2, null);
    ComponentDto file3 = newFileDto(project3, null);
    GroupDto group1 = newGroupDto();
    GroupDto group2 = newGroupDto();

    // project1 can be seen by group1
    indexIssue(newDoc("I1", file1));
    authorizationIndexer.allowOnlyGroup(project1, group1);
    // project2 can be seen by group2
    indexIssue(newDoc("I2", file2));
    authorizationIndexer.allowOnlyGroup(project2, group2);
    // project3 can be seen by nobody but root
    indexIssue(newDoc("I3", file3));

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
    assertThatSearchReturnsEmpty(IssueQuery.builder().projectUuids(singletonList(project3.uuid())));

    userSessionRule.setRoot();
    assertThatSearchReturnsOnly(IssueQuery.builder(), "I1", "I2", "I3");
  }

  @Test
  public void authorized_issues_on_user() {
    OrganizationDto org = newOrganizationDto();
    ComponentDto project1 = newPrivateProjectDto(org);
    ComponentDto project2 = newPrivateProjectDto(org);
    ComponentDto project3 = newPrivateProjectDto(org);
    ComponentDto file1 = newFileDto(project1, null);
    ComponentDto file2 = newFileDto(project2, null);
    ComponentDto file3 = newFileDto(project3, null);
    UserDto user1 = newUserDto();
    UserDto user2 = newUserDto();

    // project1 can be seen by john, project2 by max, project3 cannot be seen by anyone
    indexIssue(newDoc("I1", file1));
    authorizationIndexer.allowOnlyUser(project1, user1);
    indexIssue(newDoc("I2", file2));
    authorizationIndexer.allowOnlyUser(project2, user2);
    indexIssue(newDoc("I3", file3));

    userSessionRule.logIn(user1);
    assertThatSearchReturnsOnly(IssueQuery.builder(), "I1");
    assertThatSearchReturnsEmpty(IssueQuery.builder().projectUuids(asList(project3.getDbKey())));

    userSessionRule.logIn(user2);
    assertThatSearchReturnsOnly(IssueQuery.builder(), "I2");

    // another user
    userSessionRule.logIn(newUserDto());
    assertThatSearchReturnsEmpty(IssueQuery.builder());

    userSessionRule.setRoot();
    assertThatSearchReturnsOnly(IssueQuery.builder(), "I1", "I2", "I3");
  }

  @Test
  public void root_user_is_authorized_to_access_all_issues() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    indexIssue(newDoc("I1", project));
    userSessionRule.logIn().setRoot();

    assertThatSearchReturnsOnly(IssueQuery.builder(), "I1");
  }

  @Test
  public void list_tags() {
    RuleDefinitionDto r1 = db.rules().insert();
    RuleDefinitionDto r2 = db.rules().insert();
    ruleIndexer.commitAndIndex(db.getSession(), asList(r1.getId(), r2.getId()));

    OrganizationDto org = db.organizations().insert();
    OrganizationDto anotherOrg = db.organizations().insert();
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);
    indexIssues(
      newDoc("I42", file).setOrganizationUuid(anotherOrg.getUuid()).setRuleId(r1.getId()).setTags(of("another")),
      newDoc("I1", file).setOrganizationUuid(org.getUuid()).setRuleId(r1.getId()).setTags(of("convention", "java8", "bug")),
      newDoc("I2", file).setOrganizationUuid(org.getUuid()).setRuleId(r1.getId()).setTags(of("convention", "bug")),
      newDoc("I3", file).setOrganizationUuid(org.getUuid()).setRuleId(r2.getId()),
      newDoc("I4", file).setOrganizationUuid(org.getUuid()).setRuleId(r1.getId()).setTags(of("convention")));

    assertThat(underTest.searchTags(IssueQuery.builder().organizationUuid(org.getUuid()).build(), null, 100)).containsOnly("convention", "java8", "bug");
    assertThat(underTest.searchTags(IssueQuery.builder().organizationUuid(org.getUuid()).build(), null, 2)).containsOnly("bug", "convention");
    assertThat(underTest.searchTags(IssueQuery.builder().organizationUuid(org.getUuid()).build(), "vent", 100)).containsOnly("convention");
    assertThat(underTest.searchTags(IssueQuery.builder().organizationUuid(org.getUuid()).build(), null, 1)).containsOnly("bug");
    assertThat(underTest.searchTags(IssueQuery.builder().organizationUuid(org.getUuid()).build(), null, 100)).containsOnly("convention", "java8", "bug");
    assertThat(underTest.searchTags(IssueQuery.builder().organizationUuid(org.getUuid()).build(), "invalidRegexp[", 100)).isEmpty();
    assertThat(underTest.searchTags(IssueQuery.builder().build(), null, 100)).containsExactlyInAnyOrder("another", "convention", "java8", "bug");
  }

  @Test
  public void list_authors() {
    OrganizationDto org = newOrganizationDto();
    ComponentDto project = newPrivateProjectDto(org);
    indexIssues(
      newDoc("issue1", project).setAuthorLogin("luke.skywalker"),
      newDoc("issue2", project).setAuthorLogin("luke@skywalker.name"),
      newDoc("issue3", project).setAuthorLogin(null),
      newDoc("issue4", project).setAuthorLogin("anakin@skywalker.name"));
    IssueQuery query = IssueQuery.builder().build();

    assertThat(underTest.searchAuthors(query, null, 5)).containsExactly("anakin@skywalker.name", "luke.skywalker", "luke@skywalker.name");
    assertThat(underTest.searchAuthors(query, null, 2)).containsExactly("anakin@skywalker.name", "luke.skywalker");
    assertThat(underTest.searchAuthors(query, "uke", 5)).containsExactly("luke.skywalker", "luke@skywalker.name");
    assertThat(underTest.searchAuthors(query, null, 1)).containsExactly("anakin@skywalker.name");
    assertThat(underTest.searchAuthors(query, null, Integer.MAX_VALUE)).containsExactly("anakin@skywalker.name", "luke.skywalker", "luke@skywalker.name");
  }

  @Test
  public void list_authors_escapes_regexp_special_characters() {
    OrganizationDto org = newOrganizationDto();
    ComponentDto project = newPrivateProjectDto(org);
    indexIssues(
      newDoc("issue1", project).setAuthorLogin("name++"));
    IssueQuery query = IssueQuery.builder().build();

    assertThat(underTest.searchAuthors(query, "invalidRegexp[", 5)).isEmpty();
    assertThat(underTest.searchAuthors(query, "nam+", 5)).isEmpty();
    assertThat(underTest.searchAuthors(query, "name+", 5)).containsExactly("name++");
    assertThat(underTest.searchAuthors(query, ".*", 5)).isEmpty();
  }

  @Test
  public void countTags() {
    OrganizationDto org = newOrganizationDto();
    ComponentDto project = newPrivateProjectDto(org);
    indexIssues(
      newDoc("issue1", project).setTags(ImmutableSet.of("convention", "java8", "bug")),
      newDoc("issue2", project).setTags(ImmutableSet.of("convention", "bug")),
      newDoc("issue3", project).setTags(emptyList()),
      newDoc("issue4", project).setTags(ImmutableSet.of("convention", "java8", "bug")).setResolution(Issue.RESOLUTION_FIXED),
      newDoc("issue5", project).setTags(ImmutableSet.of("convention")));

    assertThat(underTest.countTags(projectQuery(project.uuid()), 5)).containsOnly(entry("convention", 3L), entry("bug", 2L), entry("java8", 1L));
    assertThat(underTest.countTags(projectQuery(project.uuid()), 2)).contains(entry("convention", 3L), entry("bug", 2L)).doesNotContainEntry("java8", 1L);
    assertThat(underTest.countTags(projectQuery("other"), 10)).isEmpty();
  }

  @Test
  public void searchBranchStatistics() {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch1 = db.components().insertProjectBranch(project);
    ComponentDto branch2 = db.components().insertProjectBranch(project);
    ComponentDto branch3 = db.components().insertProjectBranch(project);
    ComponentDto fileOnBranch3 = db.components().insertComponent(newFileDto(branch3));
    indexIssues(newDoc(project),
      newDoc(branch1).setType(BUG).setResolution(null), newDoc(branch1).setType(VULNERABILITY).setResolution(null), newDoc(branch1).setType(CODE_SMELL).setResolution(null),
      newDoc(branch1).setType(CODE_SMELL).setResolution(RESOLUTION_FIXED),
      newDoc(branch3).setType(CODE_SMELL).setResolution(null), newDoc(branch3).setType(CODE_SMELL).setResolution(null),
      newDoc(fileOnBranch3).setType(CODE_SMELL).setResolution(null), newDoc(fileOnBranch3).setType(CODE_SMELL).setResolution(RESOLUTION_FIXED));

    List<BranchStatistics> branchStatistics = underTest.searchBranchStatistics(project.uuid(), asList(branch1.uuid(), branch2.uuid(), branch3.uuid()));

    assertThat(branchStatistics).extracting(BranchStatistics::getBranchUuid, BranchStatistics::getBugs, BranchStatistics::getVulnerabilities, BranchStatistics::getCodeSmells)
      .containsExactlyInAnyOrder(
        tuple(branch1.uuid(), 1L, 1L, 1L),
        tuple(branch3.uuid(), 0L, 0L, 3L));
  }

  @Test
  public void searchBranchStatistics_on_many_branches() {
    ComponentDto project = db.components().insertMainBranch();
    List<String> branchUuids = new ArrayList<>();
    List<Tuple> expectedResult = new ArrayList<>();
    IntStream.range(0, 15).forEach(i -> {
      ComponentDto branch = db.components().insertProjectBranch(project);
      addIssues(branch, 1 + i, 2 + i, 3 + i);
      expectedResult.add(tuple(branch.uuid(), 1L + i, 2L + i, 3L + i));
      branchUuids.add(branch.uuid());
    });

    List<BranchStatistics> branchStatistics = underTest.searchBranchStatistics(project.uuid(), branchUuids);

    assertThat(branchStatistics)
      .extracting(BranchStatistics::getBranchUuid, BranchStatistics::getBugs, BranchStatistics::getVulnerabilities, BranchStatistics::getCodeSmells)
      .hasSize(15)
      .containsAll(expectedResult);
  }

  @Test
  public void searchBranchStatistics_on_empty_list() {
    ComponentDto project = db.components().insertMainBranch();

    assertThat(underTest.searchBranchStatistics(project.uuid(), emptyList())).isEmpty();
    assertThat(underTest.searchBranchStatistics(project.uuid(), singletonList("unknown"))).isEmpty();
  }

  private void addIssues(ComponentDto component, int bugs, int vulnerabilities, int codeSmelles) {
    List<IssueDoc> issues = new ArrayList<>();
    IntStream.range(0, bugs).forEach(b -> issues.add(newDoc(component).setType(BUG).setResolution(null)));
    IntStream.range(0, vulnerabilities).forEach(v -> issues.add(newDoc(component).setType(VULNERABILITY).setResolution(null)));
    IntStream.range(0, codeSmelles).forEach(c -> issues.add(newDoc(component).setType(CODE_SMELL).setResolution(null)));
    indexIssues(issues.toArray(new IssueDoc[issues.size()]));
  }

  private IssueQuery projectQuery(String projectUuid) {
    return IssueQuery.builder().projectUuids(singletonList(projectUuid)).resolved(false).build();
  }

  private void indexIssues(IssueDoc... issues) {
    issueIndexer.index(asList(issues).iterator());
    authorizationIndexer.allow(stream(issues).map(issue -> new IndexPermissions(issue.projectUuid(), PROJECT).allowAnyone()).collect(toList()));
  }

  private void indexIssue(IssueDoc issue) {
    issueIndexer.index(Iterators.singletonIterator(issue));
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

  private void assertThatSearchReturnsEmpty(IssueQuery.Builder query) {
    List<String> keys = searchAndReturnKeys(query);
    assertThat(keys).isEmpty();
  }

}
