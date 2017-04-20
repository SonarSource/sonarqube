/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.tester.UserSessionRule;
import org.sonarqube.ws.client.issue.SearchWsRequest;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IssueQueryFactoryTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create();

  private System2 system = mock(System2.class);
  private IssueQueryFactory underTest = new IssueQueryFactory(db.getDbClient(), system, userSession);

  @Test
  public void create_from_parameters() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(organization);
    ComponentDto module = db.components().insertComponent(ComponentTesting.newModuleDto(project));
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project));

    SearchWsRequest request = new SearchWsRequest()
      .setIssues(asList("anIssueKey"))
      .setSeverities(asList("MAJOR", "MINOR"))
      .setStatuses(asList("CLOSED"))
      .setResolutions(asList("FALSE-POSITIVE"))
      .setResolved(true)
      .setProjectKeys(asList(project.key()))
      .setModuleUuids(asList(module.uuid()))
      .setDirectories(asList("aDirPath"))
      .setFileUuids(asList(file.uuid()))
      .setAssignees(asList("joanna"))
      .setLanguages(asList("xoo"))
      .setTags(asList("tag1", "tag2"))
      .setOrganization(organization.getKey())
      .setAssigned(true)
      .setCreatedAfter("2013-04-16T09:08:24+0200")
      .setCreatedBefore("2013-04-17T09:08:24+0200")
      .setRules(asList("squid:AvoidCycle", "findbugs:NullReference"))
      .setSort("CREATION_DATE")
      .setAsc(true);

    IssueQuery query = underTest.create(request);

    assertThat(query.issueKeys()).containsOnly("anIssueKey");
    assertThat(query.severities()).containsOnly("MAJOR", "MINOR");
    assertThat(query.statuses()).containsOnly("CLOSED");
    assertThat(query.resolutions()).containsOnly("FALSE-POSITIVE");
    assertThat(query.resolved()).isTrue();
    assertThat(query.projectUuids()).containsOnly(project.uuid());
    assertThat(query.moduleUuids()).containsOnly(module.uuid());
    assertThat(query.fileUuids()).containsOnly(file.uuid());
    assertThat(query.assignees()).containsOnly("joanna");
    assertThat(query.languages()).containsOnly("xoo");
    assertThat(query.tags()).containsOnly("tag1", "tag2");
    assertThat(query.organizationUuid()).isEqualTo(organization.getUuid());
    assertThat(query.onComponentOnly()).isFalse();
    assertThat(query.assigned()).isTrue();
    assertThat(query.rules()).hasSize(2);
    assertThat(query.directories()).containsOnly("aDirPath");
    assertThat(query.createdAfter()).isEqualTo(DateUtils.parseDateTime("2013-04-16T09:08:24+0200"));
    assertThat(query.createdBefore()).isEqualTo(DateUtils.parseDateTime("2013-04-17T09:08:24+0200"));
    assertThat(query.sort()).isEqualTo(IssueQuery.SORT_BY_CREATION_DATE);
    assertThat(query.asc()).isTrue();
  }

  @Test
  public void dates_are_inclusive() {
    SearchWsRequest request = new SearchWsRequest()
      .setCreatedAfter("2013-04-16")
      .setCreatedBefore("2013-04-17");

    IssueQuery query = underTest.create(request);

    assertThat(query.createdAfter()).isEqualTo(DateUtils.parseDate("2013-04-16"));
    assertThat(query.createdBefore()).isEqualTo(DateUtils.parseDate("2013-04-18"));
  }

  @Test
  public void add_unknown_when_no_component_found() {
    SearchWsRequest request = new SearchWsRequest()
      .setComponentKeys(asList("does_not_exist"));

    IssueQuery query = underTest.create(request);

    assertThat(query.componentUuids()).containsOnly("<UNKNOWN>");
  }

  @Test
  public void parse_list_of_rules() {
    assertThat(IssueQueryFactory.toRules(null)).isNull();
    assertThat(IssueQueryFactory.toRules("")).isEmpty();
    assertThat(IssueQueryFactory.toRules("squid:AvoidCycle")).containsOnly(RuleKey.of("squid", "AvoidCycle"));
    assertThat(IssueQueryFactory.toRules("squid:AvoidCycle,findbugs:NullRef")).containsOnly(RuleKey.of("squid", "AvoidCycle"), RuleKey.of("findbugs", "NullRef"));
    assertThat(IssueQueryFactory.toRules(asList("squid:AvoidCycle", "findbugs:NullRef"))).containsOnly(RuleKey.of("squid", "AvoidCycle"), RuleKey.of("findbugs", "NullRef"));
  }

  @Test
  public void fail_if_components_and_components_uuid_params_are_set_at_the_same_time() {
    SearchWsRequest request = new SearchWsRequest()
      .setComponentKeys(asList("foo"))
      .setComponentUuids(asList("bar"));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("At most one of the following parameters can be provided: componentKeys, componentUuids, components, componentRoots, componentUuids");

    underTest.create(request);
  }

  @Test
  public void fail_if_both_projects_and_projectUuids_params_are_set() {
    SearchWsRequest request = new SearchWsRequest()
      .setProjectKeys(asList("foo"))
      .setProjectUuids(asList("bar"));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Parameters projects and projectUuids cannot be set simultaneously");

    underTest.create(request);
  }

  @Test
  public void fail_if_both_componentRoots_and_componentRootUuids_params_are_set() {
    SearchWsRequest request = new SearchWsRequest()
      .setComponentRoots(asList("foo"))
      .setComponentRootUuids(asList("bar"));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("At most one of the following parameters can be provided: componentKeys, componentUuids, components, componentRoots, componentUuids");

    underTest.create(request);
  }

  @Test
  public void fail_if_componentRoots_references_components_with_different_qualifier() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project));
    SearchWsRequest request = new SearchWsRequest()
      .setComponentRoots(asList(project.key(), file.key()));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("All components must have the same qualifier, found FIL,TRK");

    underTest.create(request);
  }

  @Test
  public void param_componentRootUuids_enables_search_in_view_tree_if_user_has_permission_on_view() {
    ComponentDto view = db.components().insertView();
    SearchWsRequest request = new SearchWsRequest()
      .setComponentRootUuids(asList(view.uuid()));
    userSession.registerComponents(view);

    IssueQuery query = underTest.create(request);

    assertThat(query.viewUuids()).containsOnly(view.uuid());
    assertThat(query.onComponentOnly()).isFalse();
  }

  @Test
  public void return_empty_results_if_not_allowed_to_search_for_subview() {
    ComponentDto view = db.components().insertView();
    ComponentDto subView = db.components().insertComponent(ComponentTesting.newSubView(view));
    SearchWsRequest request = new SearchWsRequest()
      .setComponentRootUuids(asList(subView.uuid()));

    IssueQuery query = underTest.create(request);

    assertThat(query.viewUuids()).containsOnly("<UNKNOWN>");
  }

  @Test
  public void param_componentUuids_enables_search_on_project_tree_by_default() {
    ComponentDto project = db.components().insertPrivateProject();
    SearchWsRequest request = new SearchWsRequest()
      .setComponentUuids(asList(project.uuid()));

    IssueQuery query = underTest.create(request);
    assertThat(query.projectUuids()).containsExactly(project.uuid());
    assertThat(query.onComponentOnly()).isFalse();
  }

  @Test
  public void onComponentOnly_restricts_search_to_specified_componentKeys() {
    ComponentDto project = db.components().insertPrivateProject();
    SearchWsRequest request = new SearchWsRequest()
      .setComponentKeys(asList(project.key()))
      .setOnComponentOnly(true);

    IssueQuery query = underTest.create(request);

    assertThat(query.projectUuids()).isEmpty();
    assertThat(query.componentUuids()).containsExactly(project.uuid());
    assertThat(query.onComponentOnly()).isTrue();
  }

  @Test
  public void should_search_in_tree_with_module_uuid() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto module = db.components().insertComponent(ComponentTesting.newModuleDto(project));
    SearchWsRequest request = new SearchWsRequest()
      .setComponentUuids(asList(module.uuid()));

    IssueQuery query = underTest.create(request);
    assertThat(query.moduleRootUuids()).containsExactly(module.uuid());
    assertThat(query.onComponentOnly()).isFalse();
  }

  @Test
  public void param_componentUuids_enables_search_in_directory_tree() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto dir = db.components().insertComponent(ComponentTesting.newDirectory(project, "src/main/java/foo"));
    SearchWsRequest request = new SearchWsRequest()
      .setComponentUuids(asList(dir.uuid()));

    IssueQuery query = underTest.create(request);

    assertThat(query.moduleUuids()).containsOnly(dir.moduleUuid());
    assertThat(query.directories()).containsOnly(dir.path());
    assertThat(query.onComponentOnly()).isFalse();
  }

  @Test
  public void param_componentUuids_enables_search_by_file() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project));
    SearchWsRequest request = new SearchWsRequest()
      .setComponentUuids(asList(file.uuid()));

    IssueQuery query = underTest.create(request);

    assertThat(query.fileUuids()).containsExactly(file.uuid());
  }

  @Test
  public void param_componentUuids_enables_search_by_test_file() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project).setQualifier(Qualifiers.UNIT_TEST_FILE));
    SearchWsRequest request = new SearchWsRequest()
      .setComponentUuids(asList(file.uuid()));

    IssueQuery query = underTest.create(request);

    assertThat(query.fileUuids()).containsExactly(file.uuid());
  }

  @Test
  public void fail_if_created_after_and_created_since_are_both_set() {
    SearchWsRequest request = new SearchWsRequest()
      .setCreatedAfter("2013-07-25T07:35:00+0100")
      .setCreatedInLast("palap");

    try {
      underTest.create(request);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Parameters createdAfter and createdInLast cannot be set simultaneously");
    }
  }

  @Test
  public void set_created_after_from_created_since() {
    Date now = DateUtils.parseDateTime("2013-07-25T07:35:00+0100");
    when(system.now()).thenReturn(now.getTime());
    SearchWsRequest request = new SearchWsRequest()
      .setCreatedInLast("1y2m3w4d");
    assertThat(underTest.create(request).createdAfter()).isEqualTo(DateUtils.parseDateTime("2012-04-30T07:35:00+0100"));
  }

  @Test
  public void fail_if_since_leak_period_and_created_after_set_at_the_same_time() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Parameters 'createdAfter' and 'sinceLeakPeriod' cannot be set simultaneously");

    underTest.create(new SearchWsRequest()
      .setSinceLeakPeriod(true)
      .setCreatedAfter("2013-07-25T07:35:00+0100"));
  }

  @Test
  public void fail_if_no_component_provided_with_since_leak_period() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("One and only one component must be provided when searching since leak period");

    underTest.create(new SearchWsRequest().setSinceLeakPeriod(true));
  }

  @Test
  public void fail_if_several_components_provided_with_since_leak_period() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("One and only one component must be provided when searching since leak period");

    underTest.create(new SearchWsRequest()
      .setSinceLeakPeriod(true)
      .setComponentUuids(newArrayList("foo", "bar")));
  }

  @Test
  public void fail_if_date_is_not_formatted_correctly() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("'unknown-date' cannot be parsed as either a date or date+time");

    underTest.create(new SearchWsRequest()
      .setCreatedAfter("unknown-date"));
  }

  @Test
  public void return_empty_results_if_organization_with_specified_key_does_not_exist() {
    SearchWsRequest request = new SearchWsRequest()
      .setOrganization("does_not_exist");

    IssueQuery query = underTest.create(request);

    assertThat(query.organizationUuid()).isEqualTo("<UNKNOWN>");
  }

}
