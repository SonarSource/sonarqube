/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.hotspot.ws;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.issue.Issue;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.permission.index.PermissionIndexer;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.security.SecurityStandards;
import org.sonar.server.security.SecurityStandards.SQCategory;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.view.index.ViewIndexer;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Hotspots;
import org.sonarqube.ws.Hotspots.Component;
import org.sonarqube.ws.Hotspots.SearchWsResponse;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_SAFE;
import static org.sonar.api.issue.Issue.STATUSES;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_REVIEWED;
import static org.sonar.api.issue.Issue.STATUS_TO_REVIEW;
import static org.sonar.api.rules.RuleType.SECURITY_HOTSPOT;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.issue.IssueTesting.newIssue;

@RunWith(DataProviderRunner.class)
public class SearchActionTest {
  private static final Random RANDOM = new Random();
  private static final int ONE_MINUTE = 60_000;

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private DbClient dbClient = dbTester.getDbClient();
  private TestDefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(dbTester);

  private IssueIndex issueIndex = new IssueIndex(es.client(), System2.INSTANCE, userSessionRule, new WebAuthorizationTypeSupport(userSessionRule));
  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), dbClient, new IssueIteratorFactory(dbClient));
  private ViewIndexer viewIndexer = new ViewIndexer(dbClient, es.client());
  private PermissionIndexer permissionIndexer = new PermissionIndexer(dbClient, es.client(), issueIndexer);
  private HotspotWsResponseFormatter responseFormatter = new HotspotWsResponseFormatter(defaultOrganizationProvider);

  private SearchAction underTest = new SearchAction(dbClient, userSessionRule, issueIndex, responseFormatter);
  private WsActionTester actionTester = new WsActionTester(underTest);

  @Test
  public void verify_ws_def() {
    assertThat(actionTester.getDef().isInternal()).isTrue();
    assertThat(actionTester.getDef().param("onlyMine").isRequired()).isFalse();
    assertThat(actionTester.getDef().param("onlyMine").possibleValues())
      .containsExactlyInAnyOrder("yes", "no", "true", "false");
  }

  @Test
  public void fails_with_IAE_if_parameters_projectKey_and_hotspots_are_missing() {
    TestRequest request = actionTester.newRequest();

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("A value must be provided for either parameter 'projectKey' or parameter 'hotspots'");
  }

  @Test
  public void fail_with_IAE_if_parameter_branch_is_used_without_parameter_projectKey() {
    TestRequest request = actionTester.newRequest()
      .setParam("hotspots", randomAlphabetic(2))
      .setParam("branch", randomAlphabetic(1));

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Parameter 'branch' must be used with parameter 'projectKey'");
  }

  @Test
  public void fail_with_IAE_if_parameter_pullRequest_is_used_without_parameter_projectKey() {
    TestRequest request = actionTester.newRequest()
      .setParam("hotspots", randomAlphabetic(2))
      .setParam("pullRequest", randomAlphabetic(1));

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Parameter 'pullRequest' must be used with parameter 'projectKey'");
  }

  @Test
  public void fail_with_IAE_if_both_parameters_pullRequest_and_branch_are_provided() {
    TestRequest request = actionTester.newRequest()
      .setParam("projectKey", randomAlphabetic(2))
      .setParam("branch", randomAlphabetic(1))
      .setParam("pullRequest", randomAlphabetic(1));

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Only one of parameters 'branch' and 'pullRequest' can be provided");
  }

  @Test
  @UseDataProvider("badStatuses")
  public void fails_with_IAE_if_status_parameter_is_neither_TO_REVIEW_or_REVIEWED(String badStatus) {
    TestRequest request = actionTester.newRequest()
      .setParam("projectKey", randomAlphabetic(13))
      .setParam("status", badStatus);

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value of parameter 'status' (" + badStatus + ") must be one of: [TO_REVIEW, REVIEWED]");
  }

  @DataProvider
  public static Object[][] badStatuses() {
    return Stream.concat(
      Issue.STATUSES.stream(),
      Stream.of(randomAlphabetic(3)))
      .filter(t -> !STATUS_REVIEWED.equals(t))
      .filter(t -> !STATUS_TO_REVIEW.equals(t))
      .map(t -> new Object[] {t})
      .toArray(Object[][]::new);
  }

  @Test
  @UseDataProvider("validStatusesAndResolutions")
  public void fail_with_IAE_if_parameter_status_is_specified_with_hotspots_parameter(String status, @Nullable String notUsed) {
    TestRequest request = actionTester.newRequest()
      .setParam("hotspots", randomAlphabetic(12))
      .setParam("status", status);

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Parameter 'status' can't be used with parameter 'hotspots'");
  }

  @Test
  @UseDataProvider("badResolutions")
  public void fails_with_IAE_if_resolution_parameter_is_neither_FIXED_nor_SAFE(String badResolution) {
    TestRequest request = actionTester.newRequest()
      .setParam("projectKey", randomAlphabetic(13))
      .setParam("status", STATUS_TO_REVIEW)
      .setParam("resolution", badResolution);

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value of parameter 'resolution' (" + badResolution + ") must be one of: [FIXED, SAFE]");
  }

  @DataProvider
  public static Object[][] badResolutions() {
    return Stream.of(
      Issue.RESOLUTIONS.stream(),
      Issue.SECURITY_HOTSPOT_RESOLUTIONS.stream(),
      Stream.of(randomAlphabetic(4)))
      .flatMap(t -> t)
      .filter(t -> !RESOLUTION_FIXED.equals(t))
      .filter(t -> !RESOLUTION_SAFE.equals(t))
      .map(t -> new Object[] {t})
      .toArray(Object[][]::new);
  }

  @Test
  @UseDataProvider("fixedOrSafeResolution")
  public void fails_with_IAE_if_resolution_is_provided_with_status_TO_REVIEW(String resolution) {
    TestRequest request = actionTester.newRequest()
      .setParam("projectKey", randomAlphabetic(13))
      .setParam("status", STATUS_TO_REVIEW)
      .setParam("resolution", resolution);

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value '" + resolution + "' of parameter 'resolution' can only be provided if value of parameter 'status' is 'REVIEWED'");
  }

  @Test
  @UseDataProvider("fixedOrSafeResolution")
  public void fails_with_IAE_if_resolution_is_provided_with_hotspots_parameter(String resolution) {
    TestRequest request = actionTester.newRequest()
      .setParam("hotspots", randomAlphabetic(13))
      .setParam("resolution", resolution);

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Parameter 'resolution' can't be used with parameter 'hotspots'");
  }

  @DataProvider
  public static Object[][] fixedOrSafeResolution() {
    return new Object[][] {
      {RESOLUTION_SAFE},
      {RESOLUTION_FIXED}
    };
  }

  @Test
  public void fails_with_NotFoundException_if_project_does_not_exist() {
    String key = randomAlphabetic(12);
    TestRequest request = actionTester.newRequest()
      .setParam("projectKey", key);

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Project '%s' not found", key);
  }

  @Test
  public void fails_with_NotFoundException_if_project_is_neither_a_project_nor_an_application() {
    ComponentDto project = dbTester.components().insertPrivateProject();
    ComponentDto directory = dbTester.components().insertComponent(ComponentTesting.newDirectory(project, "foo"));
    ComponentDto file = dbTester.components().insertComponent(ComponentTesting.newFileDto(project));
    ComponentDto portfolio = dbTester.components().insertPrivatePortfolio();
    TestRequest request = actionTester.newRequest();

    for (ComponentDto component : Arrays.asList(directory, file, portfolio)) {
      request.setParam("projectKey", component.getKey());

      assertThatThrownBy(request::execute)
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Project '%s' not found", component.getKey());
    }
  }

  @Test
  public void fails_with_ForbiddenException_if_project_is_private_and_not_allowed() {
    ComponentDto project = dbTester.components().insertPrivateProject();
    userSessionRule.registerComponents(project);
    TestRequest request = newRequest(project);

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void fails_with_ForbiddenException_if_application_is_private_and_not_allowed() {
    ComponentDto application = dbTester.components().insertPrivateApplication();
    userSessionRule.registerComponents(application);
    TestRequest request = newRequest(application);

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void succeeds_on_public_project() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);

    SearchWsResponse response = newRequest(project)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList()).isEmpty();
    assertThat(response.getComponentsList()).isEmpty();
  }

  @Test
  public void succeeds_on_public_application() {
    ComponentDto application = dbTester.components().insertPublicApplication();
    userSessionRule.registerComponents(application);

    SearchWsResponse response = newRequest(application)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList()).isEmpty();
    assertThat(response.getComponentsList()).isEmpty();
  }

  @Test
  public void succeeds_on_private_project_with_permission() {
    ComponentDto project = dbTester.components().insertPrivateProject();
    userSessionRule.registerComponents(project);
    userSessionRule.logIn().addProjectPermission(UserRole.USER, project);

    SearchWsResponse response = newRequest(project)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList()).isEmpty();
    assertThat(response.getComponentsList()).isEmpty();
  }

  @Test
  public void succeeds_on_private_application_with_permission() {
    ComponentDto application = dbTester.components().insertPrivateApplication();
    userSessionRule.registerComponents(application);
    userSessionRule.logIn().addProjectPermission(UserRole.USER, application);

    SearchWsResponse response = newRequest(application)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList()).isEmpty();
    assertThat(response.getComponentsList()).isEmpty();
  }

  @Test
  public void does_not_fail_if_rule_of_hotspot_does_not_exist_in_DB() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    indexPermissions();
    IssueDto[] hotspots = IntStream.range(0, 1 + RANDOM.nextInt(10))
      .mapToObj(i -> {
        RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
        return insertHotspot(project, file, rule);
      })
      .toArray(IssueDto[]::new);
    indexIssues();
    IssueDto hotspotWithoutRule = hotspots[RANDOM.nextInt(hotspots.length)];
    dbTester.executeUpdateSql("delete from rules where id=" + hotspotWithoutRule.getRuleId());

    SearchWsResponse response = newRequest(project)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(Hotspots.SearchWsResponse.Hotspot::getKey)
      .containsOnly(Arrays.stream(hotspots)
        .filter(t -> !t.getKey().equals(hotspotWithoutRule.getKey()))
        .map(IssueDto::getKey)
        .toArray(String[]::new));
  }

  @Test
  public void returns_no_hotspot_component_nor_rule_when_project_has_no_hotspot() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    Arrays.stream(RuleType.values())
      .filter(t -> t != SECURITY_HOTSPOT)
      .forEach(ruleType -> {
        RuleDefinitionDto rule = newRule(ruleType);
        dbTester.issues().insert(rule, project, file, t -> t.setType(ruleType));
      });
    indexIssues();

    SearchWsResponse response = newRequest(project)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList()).isEmpty();
  }

  @Test
  public void returns_hotspot_components_when_project_has_hotspots() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    ComponentDto fileWithHotspot = dbTester.components().insertComponent(newFileDto(project));
    Arrays.stream(RuleType.values())
      .filter(t -> t != SECURITY_HOTSPOT)
      .forEach(ruleType -> {
        RuleDefinitionDto rule = newRule(ruleType);
        dbTester.issues().insert(rule, project, file, t -> t.setType(ruleType));
      });
    IssueDto[] hotspots = IntStream.range(0, 1 + RANDOM.nextInt(10))
      .mapToObj(i -> {
        RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
        return insertHotspot(project, fileWithHotspot, rule);
      })
      .toArray(IssueDto[]::new);
    indexIssues();

    SearchWsResponse response = newRequest(project)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsOnly(Arrays.stream(hotspots).map(IssueDto::getKey).toArray(String[]::new));
    assertThat(response.getComponentsList())
      .extracting(Component::getKey)
      .containsOnly(project.getKey(), fileWithHotspot.getKey());
  }

  @Test
  public void returns_single_component_when_all_hotspots_are_on_project() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    indexPermissions();
    IssueDto[] hotspots = IntStream.range(0, 1 + RANDOM.nextInt(10))
      .mapToObj(i -> {
        RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
        return insertHotspot(project, project, rule);
      })
      .toArray(IssueDto[]::new);
    indexIssues();

    SearchWsResponse response = newRequest(project)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(Hotspots.SearchWsResponse.Hotspot::getKey)
      .containsOnly(Arrays.stream(hotspots).map(IssueDto::getKey).toArray(String[]::new));
    assertThat(response.getComponentsList())
      .extracting(Component::getKey)
      .containsOnly(project.getKey());
  }

  @Test
  public void returns_hotspots_of_specified_project() {
    ComponentDto project1 = dbTester.components().insertPublicProject();
    ComponentDto project2 = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project1, project2);
    indexPermissions();
    ComponentDto file1 = dbTester.components().insertComponent(newFileDto(project1));
    ComponentDto file2 = dbTester.components().insertComponent(newFileDto(project2));
    IssueDto[] hotspots2 = IntStream.range(0, 1 + RANDOM.nextInt(10))
      .mapToObj(i -> {
        RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
        insertHotspot(project1, file1, rule);
        return insertHotspot(project2, file2, rule);
      })
      .toArray(IssueDto[]::new);
    indexIssues();

    SearchWsResponse responseProject1 = newRequest(project1)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(responseProject1.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .doesNotContainAnyElementsOf(Arrays.stream(hotspots2).map(IssueDto::getKey).collect(toList()));
    assertThat(responseProject1.getComponentsList())
      .extracting(Component::getKey)
      .containsOnly(project1.getKey(), file1.getKey());

    SearchWsResponse responseProject2 = newRequest(project2)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(responseProject2.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsOnly(Arrays.stream(hotspots2).map(IssueDto::getKey).toArray(String[]::new));
    assertThat(responseProject2.getComponentsList())
      .extracting(Component::getKey)
      .containsOnly(project2.getKey(), file2.getKey());
  }

  @Test
  public void returns_only_hotspots_to_review_or_reviewed_of_project() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    IssueDto[] hotspots = STATUSES.stream()
      .map(status -> {
        RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
        return insertHotspot(rule, project, file, t -> t.setStatus(status));
      })
      .toArray(IssueDto[]::new);
    indexIssues();
    String[] expectedKeys = Arrays.stream(hotspots)
      .filter(t -> STATUS_REVIEWED.equals(t.getStatus()) || STATUS_TO_REVIEW.equals(t.getStatus()))
      .map(IssueDto::getKey)
      .toArray(String[]::new);

    SearchWsResponse response = newRequest(project)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsOnly(expectedKeys);
  }

  @Test
  public void returns_hotspots_of_specified_application() {
    ComponentDto application1 = dbTester.components().insertPublicApplication();
    ComponentDto application2 = dbTester.components().insertPublicApplication();
    ComponentDto project1 = dbTester.components().insertPublicProject();
    ComponentDto project2 = dbTester.components().insertPublicProject();
    dbTester.components().insertComponent(ComponentTesting.newProjectCopy(project1, application1));
    dbTester.components().insertComponent(ComponentTesting.newProjectCopy(project2, application2));
    indexViews();
    userSessionRule.registerComponents(application1, application2, project1, project2);
    indexPermissions();
    ComponentDto file1 = dbTester.components().insertComponent(newFileDto(project1));
    ComponentDto file2 = dbTester.components().insertComponent(newFileDto(project2));
    IssueDto[] hotspots2 = IntStream.range(0, 1 + RANDOM.nextInt(10))
      .mapToObj(i -> {
        RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
        insertHotspot(project1, file1, rule);
        return insertHotspot(project2, file2, rule);
      })
      .toArray(IssueDto[]::new);
    indexIssues();

    SearchWsResponse responseApplication1 = newRequest(application1)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(responseApplication1.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .doesNotContainAnyElementsOf(Arrays.stream(hotspots2).map(IssueDto::getKey).collect(toList()));
    assertThat(responseApplication1.getComponentsList())
      .extracting(Component::getKey)
      .containsOnly(project1.getKey(), file1.getKey());

    SearchWsResponse responseApplication2 = newRequest(application2)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(responseApplication2.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsOnly(Arrays.stream(hotspots2).map(IssueDto::getKey).toArray(String[]::new));
    assertThat(responseApplication2.getComponentsList())
      .extracting(Component::getKey)
      .containsOnly(project2.getKey(), file2.getKey());
  }

  @Test
  public void returns_hotspots_of_specified_application_branch() {
    ComponentDto application = dbTester.components().insertPublicApplication();
    ComponentDto applicationBranch = dbTester.components().insertProjectBranch(application);
    ComponentDto project1 = dbTester.components().insertPublicProject();
    ComponentDto project2 = dbTester.components().insertPublicProject();
    dbTester.components().insertComponent(ComponentTesting.newProjectCopy(project1, application));
    dbTester.components().insertComponent(ComponentTesting.newProjectCopy(project2, applicationBranch));
    indexViews();
    userSessionRule.registerComponents(application, applicationBranch, project1, project2);
    indexPermissions();
    ComponentDto file1 = dbTester.components().insertComponent(newFileDto(project1));
    ComponentDto file2 = dbTester.components().insertComponent(newFileDto(project2));
    IssueDto[] hotspots2 = IntStream.range(0, 1 + RANDOM.nextInt(10))
      .mapToObj(i -> {
        RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
        insertHotspot(project1, file1, rule);
        return insertHotspot(project2, file2, rule);
      })
      .toArray(IssueDto[]::new);
    indexIssues();

    SearchWsResponse responseApplication = newRequest(application)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(responseApplication.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .doesNotContainAnyElementsOf(Arrays.stream(hotspots2).map(IssueDto::getKey).collect(toList()));
    assertThat(responseApplication.getComponentsList())
      .extracting(Component::getKey)
      .containsOnly(project1.getKey(), file1.getKey());

    SearchWsResponse responseApplicationBranch = newRequest(applicationBranch)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(responseApplicationBranch.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsOnly(Arrays.stream(hotspots2).map(IssueDto::getKey).toArray(String[]::new));
    assertThat(responseApplicationBranch.getComponentsList())
      .extracting(Component::getKey)
      .containsOnly(project2.getKey(), file2.getKey());

  }

  @Test
  public void returns_hotspot_of_branch_or_pullRequest() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    indexPermissions();
    ComponentDto branch = dbTester.components().insertProjectBranch(project);
    ComponentDto pullRequest = dbTester.components().insertProjectBranch(project, t -> t.setBranchType(BranchType.PULL_REQUEST));
    ComponentDto fileProject = dbTester.components().insertComponent(newFileDto(project));
    ComponentDto fileBranch = dbTester.components().insertComponent(newFileDto(branch));
    ComponentDto filePR = dbTester.components().insertComponent(newFileDto(pullRequest));
    IssueDto[] hotspotProject = IntStream.range(0, 1 + RANDOM.nextInt(10))
      .mapToObj(i -> {
        RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
        return insertHotspot(project, fileProject, rule);
      })
      .toArray(IssueDto[]::new);
    IssueDto[] hotspotBranch = IntStream.range(0, 1 + RANDOM.nextInt(10))
      .mapToObj(i -> {
        RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
        return insertHotspot(branch, fileBranch, rule);
      })
      .toArray(IssueDto[]::new);
    IssueDto[] hotspotPR = IntStream.range(0, 1 + RANDOM.nextInt(10))
      .mapToObj(i -> {
        RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
        return insertHotspot(pullRequest, filePR, rule);
      })
      .toArray(IssueDto[]::new);
    indexIssues();

    SearchWsResponse responseProject = newRequest(project)
      .executeProtobuf(SearchWsResponse.class);
    SearchWsResponse responseBranch = newRequest(branch)
      .executeProtobuf(SearchWsResponse.class);
    SearchWsResponse responsePR = newRequest(pullRequest)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(responseProject.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactlyInAnyOrder(Arrays.stream(hotspotProject).map(IssueDto::getKey).toArray(String[]::new));
    assertThat(responseBranch.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactlyInAnyOrder(Arrays.stream(hotspotBranch).map(IssueDto::getKey).toArray(String[]::new));
    assertThat(responsePR.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactlyInAnyOrder(Arrays.stream(hotspotPR).map(IssueDto::getKey).toArray(String[]::new));
  }

  @Test
  @UseDataProvider("onlyMineParamValues")
  public void returns_hotspots_of_specified_project_assigned_to_current_user_if_only_mine_is_set(String onlyMineParameter, boolean shouldFilter) {
    ComponentDto project1 = dbTester.components().insertPublicProject();
    String assigneeUuid = this.userSessionRule.logIn().registerComponents(project1).getUuid();

    indexPermissions();
    ComponentDto file1 = dbTester.components().insertComponent(newFileDto(project1));
    IssueDto[] assigneeHotspots = IntStream.range(0, 1 + RANDOM.nextInt(10))
      .mapToObj(i -> {
        RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
        insertHotspot(rule, project1, file1, randomAlphabetic(5));
        return insertHotspot(rule, project1, file1, assigneeUuid);
      })
      .toArray(IssueDto[]::new);

    indexIssues();

    SearchWsResponse allHotspots = newRequest(project1)
      .executeProtobuf(SearchWsResponse.class);

    SearchWsResponse userHotspots = newRequest(project1, r -> r.setParam("onlyMine", onlyMineParameter))
      .executeProtobuf(SearchWsResponse.class);

    assertThat(allHotspots.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .contains(Arrays.stream(assigneeHotspots).map(IssueDto::getKey).toArray(String[]::new))
      .hasSizeGreaterThan(assigneeHotspots.length);

    if (shouldFilter) {
      assertThat(userHotspots.getHotspotsList())
        .extracting(SearchWsResponse.Hotspot::getKey)
        .containsOnly(Arrays.stream(assigneeHotspots).map(IssueDto::getKey).toArray(String[]::new));
    } else {
      assertThat(userHotspots.getHotspotsList())
        .extracting(SearchWsResponse.Hotspot::getKey)
        .containsOnly(allHotspots.getHotspotsList().stream().map(SearchWsResponse.Hotspot::getKey).toArray(String[]::new));
    }
  }

  @DataProvider
  public static Object[][] onlyMineParamValues() {
    return new Object[][] {
      {"yes", true},
      {"true", true},
      {"no", false},
      {"false", false}
    };
  }

  @Test
  public void fail_if_hotspots_provided_with_onlyMine_param() {
    ComponentDto project = dbTester.components().insertPrivateProject();

    userSessionRule.registerComponents(project);
    userSessionRule.logIn().addProjectPermission(UserRole.USER, project);

    assertThatThrownBy(() -> actionTester.newRequest()
      .setParam("hotspots", IntStream.range(2, 10).mapToObj(String::valueOf).collect(joining(",")))
      .setParam("onlyMine", "true")
      .execute())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Parameter 'onlyMine' can be used with parameter 'projectKey' only");
  }

  @Test
  public void fail_if_user_not_authenticated_with_onlyMine_param() {
    ComponentDto project = dbTester.components().insertPublicProject();

    userSessionRule.anonymous();

    assertThatThrownBy(() -> actionTester.newRequest()
      .setParam("projectKey", project.getKey())
      .setParam("onlyMine", "true")
      .execute())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Parameter 'onlyMine' requires user to be logged in");
  }

  @Test
  public void returns_hotpots_with_any_status_if_no_status_nor_resolution_parameter() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    List<IssueDto> hotspots = insertRandomNumberOfHotspotsOfAllSupportedStatusesAndResolutions(project, file)
      .collect(toList());
    indexIssues();

    SearchWsResponse response = newRequest(project, null, null)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactlyInAnyOrder(hotspots.stream().map(IssueDto::getKey).toArray(String[]::new));
  }

  @Test
  public void returns_hotpots_reviewed_as_safe_and_fixed_if_status_is_REVIEWED_and_resolution_is_not_set() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    List<IssueDto> reviewedHotspots = insertRandomNumberOfHotspotsOfAllSupportedStatusesAndResolutions(project, file)
      .filter(t -> STATUS_REVIEWED.equals(t.getStatus()))
      .collect(toList());
    indexIssues();

    SearchWsResponse response = newRequest(project, STATUS_REVIEWED, null)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactlyInAnyOrder(reviewedHotspots.stream().map(IssueDto::getKey).toArray(String[]::new));
  }

  @Test
  public void returns_hotpots_reviewed_as_safe_if_status_is_REVIEWED_and_resolution_is_SAFE() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    List<IssueDto> safeHotspots = insertRandomNumberOfHotspotsOfAllSupportedStatusesAndResolutions(project, file)
      .filter(t -> STATUS_REVIEWED.equals(t.getStatus()) && RESOLUTION_SAFE.equals(t.getResolution()))
      .collect(toList());
    indexIssues();

    SearchWsResponse response = newRequest(project, STATUS_REVIEWED, RESOLUTION_SAFE)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactlyInAnyOrder(safeHotspots.stream().map(IssueDto::getKey).toArray(String[]::new));
  }

  @Test
  public void returns_hotpots_reviewed_as_fixed_if_status_is_REVIEWED_and_resolution_is_FIXED() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    List<IssueDto> fixedHotspots = insertRandomNumberOfHotspotsOfAllSupportedStatusesAndResolutions(project, file)
      .filter(t -> STATUS_REVIEWED.equals(t.getStatus()) && RESOLUTION_FIXED.equals(t.getResolution()))
      .collect(toList());
    indexIssues();

    SearchWsResponse response = newRequest(project, STATUS_REVIEWED, RESOLUTION_FIXED)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactlyInAnyOrder(fixedHotspots.stream().map(IssueDto::getKey).toArray(String[]::new));
  }

  @Test
  public void returns_only_unresolved_hotspots_when_status_is_TO_REVIEW() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
    IssueDto unresolvedHotspot = insertHotspot(rule, project, file, t -> t.setResolution(null));
    // unrealistic case since a resolution must be set, but shows a limit of current implementation (resolution is enough)
    IssueDto badlyResolved = insertHotspot(rule, project, file, t -> t.setStatus(STATUS_TO_REVIEW).setResolution(randomAlphabetic(5)));
    IssueDto badlyReviewed = insertHotspot(rule, project, file, t -> t.setStatus(STATUS_REVIEWED).setResolution(null));
    IssueDto badlyClosedHotspot = insertHotspot(rule, project, file, t -> t.setStatus(STATUS_CLOSED).setResolution(null));
    indexIssues();

    SearchWsResponse response = newRequest(project, STATUS_TO_REVIEW, null)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsOnly(unresolvedHotspot.getKey());
  }

  private Stream<IssueDto> insertRandomNumberOfHotspotsOfAllSupportedStatusesAndResolutions(ComponentDto project, ComponentDto file) {
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
    List<IssueDto> hotspots = Arrays.stream(validStatusesAndResolutions())
      .flatMap(objects -> {
        String status = (String) objects[0];
        String resolution = (String) objects[1];
        return IntStream.range(0, 1 + RANDOM.nextInt(15))
          .mapToObj(i -> newIssue(rule, project, file)
            .setKee("hotspot_" + status + "_" + resolution + "_" + i)
            .setType(SECURITY_HOTSPOT)
            .setStatus(status)
            .setResolution(resolution));
      })
      .collect(toList());
    Collections.shuffle(hotspots);
    hotspots.forEach(t -> dbTester.issues().insertHotspot(t));
    return hotspots.stream();
  }

  @Test
  @UseDataProvider("validStatusesAndResolutions")
  public void returns_fields_of_hotspot(String status, @Nullable String resolution) {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
    IssueDto hotspot = insertHotspot(rule, project, file,
      t -> t
        .setStatus(randomAlphabetic(11))
        .setLine(RANDOM.nextInt(230))
        .setMessage(randomAlphabetic(10))
        .setAssigneeUuid(randomAlphabetic(9))
        .setAuthorLogin(randomAlphabetic(8))
        .setStatus(status)
        .setResolution(resolution));
    indexIssues();

    SearchWsResponse response = newRequest(project)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList()).hasSize(1);
    SearchWsResponse.Hotspot actual = response.getHotspots(0);
    assertThat(actual.getComponent()).isEqualTo(file.getKey());
    assertThat(actual.getProject()).isEqualTo(project.getKey());
    assertThat(actual.getStatus()).isEqualTo(status);
    if (resolution == null) {
      assertThat(actual.hasResolution()).isFalse();
    } else {
      assertThat(actual.getResolution()).isEqualTo(resolution);
    }
    assertThat(actual.getLine()).isEqualTo(hotspot.getLine());
    assertThat(actual.getMessage()).isEqualTo(hotspot.getMessage());
    assertThat(actual.getAssignee()).isEqualTo(hotspot.getAssigneeUuid());
    assertThat(actual.getAuthor()).isEqualTo(hotspot.getAuthorLogin());
    assertThat(actual.getCreationDate()).isEqualTo(formatDateTime(hotspot.getIssueCreationDate()));
    assertThat(actual.getUpdateDate()).isEqualTo(formatDateTime(hotspot.getIssueUpdateDate()));
  }

  @DataProvider
  public static Object[][] validStatusesAndResolutions() {
    return new Object[][] {
      {STATUS_TO_REVIEW, null},
      {STATUS_REVIEWED, RESOLUTION_FIXED},
      {STATUS_REVIEWED, RESOLUTION_SAFE},
    };
  }

  @Test
  @UseDataProvider("allSQCategories")
  public void returns_SQCategory_and_VulnerabilityProbability_of_rule(Set<String> securityStandards, SQCategory expected) {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT, t -> t.setSecurityStandards(securityStandards));
    IssueDto hotspot = insertHotspot(project, file, rule);
    indexIssues();

    SearchWsResponse response = newRequest(project)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList()).hasSize(1);
    Hotspots.SearchWsResponse.Hotspot actual = response.getHotspots(0);
    assertThat(actual.getSecurityCategory()).isEqualTo(expected.getKey());
    assertThat(actual.getVulnerabilityProbability()).isEqualTo(expected.getVulnerability().name());
  }

  @DataProvider
  public static Object[][] allSQCategories() {
    Stream<Object[]> allCategoriesButOTHERS = SecurityStandards.CWES_BY_SQ_CATEGORY.entrySet()
      .stream()
      .map(t -> new Object[] {
        t.getValue().stream().map(c -> "cwe:" + c).collect(toSet()),
        t.getKey()
      });
    Stream<Object[]> sqCategoryOTHERS = Stream.of(
      new Object[] {Collections.emptySet(), SQCategory.OTHERS},
      new Object[] {ImmutableSet.of("foo", "donut", "acme"), SQCategory.OTHERS});
    return Stream.concat(allCategoriesButOTHERS, sqCategoryOTHERS).toArray(Object[][]::new);
  }

  @Test
  public void does_not_fail_when_hotspot_has_none_of_the_nullable_fields() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
    insertHotspot(rule, project, file,
      t -> t.setResolution(null)
        .setLine(null)
        .setMessage(null)
        .setAssigneeUuid(null)
        .setAuthorLogin(null));
    indexIssues();

    SearchWsResponse response = newRequest(project)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .hasSize(1);
    SearchWsResponse.Hotspot actual = response.getHotspots(0);
    assertThat(actual.hasResolution()).isFalse();
    assertThat(actual.hasLine()).isFalse();
    assertThat(actual.getMessage()).isEmpty();
    assertThat(actual.hasAssignee()).isFalse();
    assertThat(actual.getAuthor()).isEmpty();
  }

  @Test
  public void returns_details_of_components() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    indexPermissions();
    ComponentDto directory = dbTester.components().insertComponent(newDirectory(project, "donut/acme"));
    ComponentDto directory2 = dbTester.components().insertComponent(newDirectory(project, "foo/bar"));
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    ComponentDto file2 = dbTester.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
    IssueDto fileHotspot = insertHotspot(project, file, rule);
    IssueDto dirHotspot = insertHotspot(project, directory, rule);
    IssueDto projectHotspot = insertHotspot(project, project, rule);
    indexIssues();

    SearchWsResponse response = newRequest(project)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsOnly(fileHotspot.getKey(), dirHotspot.getKey(), projectHotspot.getKey());
    assertThat(response.getComponentsList()).hasSize(3);
    assertThat(response.getComponentsList())
      .extracting(Component::getOrganization)
      .containsOnly(defaultOrganizationProvider.get().getKey());
    assertThat(response.getComponentsList())
      .extracting(Component::getKey)
      .containsOnly(project.getKey(), directory.getKey(), file.getKey());
    Map<String, Component> componentByKey = response.getComponentsList().stream().collect(uniqueIndex(Component::getKey));
    Component actualProject = componentByKey.get(project.getKey());
    assertThat(actualProject.getQualifier()).isEqualTo(project.qualifier());
    assertThat(actualProject.getName()).isEqualTo(project.name());
    assertThat(actualProject.getLongName()).isEqualTo(project.longName());
    assertThat(actualProject.hasPath()).isFalse();
    assertThat(actualProject.hasBranch()).isFalse();
    assertThat(actualProject.hasPullRequest()).isFalse();
    Component actualDirectory = componentByKey.get(directory.getKey());
    assertThat(actualDirectory.getQualifier()).isEqualTo(directory.qualifier());
    assertThat(actualDirectory.getName()).isEqualTo(directory.name());
    assertThat(actualDirectory.getLongName()).isEqualTo(directory.longName());
    assertThat(actualDirectory.getPath()).isEqualTo(directory.path());
    assertThat(actualDirectory.hasBranch()).isFalse();
    assertThat(actualDirectory.hasPullRequest()).isFalse();
    Component actualFile = componentByKey.get(file.getKey());
    assertThat(actualFile.getQualifier()).isEqualTo(file.qualifier());
    assertThat(actualFile.getName()).isEqualTo(file.name());
    assertThat(actualFile.getLongName()).isEqualTo(file.longName());
    assertThat(actualFile.getPath()).isEqualTo(file.path());
    assertThat(actualFile.hasBranch()).isFalse();
    assertThat(actualFile.hasPullRequest()).isFalse();
  }

  @Test
  public void returns_branch_field_of_components_of_branch() {
    ComponentDto project = dbTester.components().insertPublicProject();
    ComponentDto branch = dbTester.components().insertProjectBranch(project);
    userSessionRule.registerComponents(project, branch);
    indexPermissions();
    ComponentDto directory = dbTester.components().insertComponent(newDirectory(branch, "donut/acme"));
    ComponentDto file = dbTester.components().insertComponent(newFileDto(branch));
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
    IssueDto fileHotspot = insertHotspot(branch, file, rule);
    IssueDto dirHotspot = insertHotspot(branch, directory, rule);
    IssueDto projectHotspot = insertHotspot(branch, branch, rule);
    indexIssues();

    SearchWsResponse response = newRequest(branch)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsOnly(fileHotspot.getKey(), dirHotspot.getKey(), projectHotspot.getKey());
    assertThat(response.getComponentsList())
      .extracting(Component::getKey)
      .containsOnly(project.getKey(), directory.getKey(), file.getKey());
    Map<String, Component> componentByKey = response.getComponentsList().stream().collect(uniqueIndex(Component::getKey));
    Component actualProject = componentByKey.get(project.getKey());
    assertThat(actualProject.getBranch()).isEqualTo(branch.getBranch());
    assertThat(actualProject.hasPullRequest()).isFalse();
    Component actualDirectory = componentByKey.get(directory.getKey());
    assertThat(actualDirectory.getBranch()).isEqualTo(branch.getBranch());
    assertThat(actualDirectory.hasPullRequest()).isFalse();
    Component actualFile = componentByKey.get(file.getKey());
    assertThat(actualFile.getBranch()).isEqualTo(branch.getBranch());
    assertThat(actualFile.hasPullRequest()).isFalse();
  }

  @Test
  public void returns_pullRequest_field_of_components_of_pullRequest() {
    ComponentDto project = dbTester.components().insertPublicProject();
    ComponentDto pullRequest = dbTester.components().insertProjectBranch(project, t -> t.setBranchType(BranchType.PULL_REQUEST));
    userSessionRule.registerComponents(project, pullRequest);
    indexPermissions();
    ComponentDto directory = dbTester.components().insertComponent(newDirectory(pullRequest, "donut/acme"));
    ComponentDto file = dbTester.components().insertComponent(newFileDto(pullRequest));
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
    IssueDto fileHotspot = insertHotspot(pullRequest, file, rule);
    IssueDto dirHotspot = insertHotspot(pullRequest, directory, rule);
    IssueDto projectHotspot = insertHotspot(pullRequest, pullRequest, rule);
    indexIssues();

    SearchWsResponse response = newRequest(pullRequest)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsOnly(fileHotspot.getKey(), dirHotspot.getKey(), projectHotspot.getKey());
    assertThat(response.getComponentsList())
      .extracting(Component::getKey)
      .containsOnly(project.getKey(), directory.getKey(), file.getKey());
    Map<String, Component> componentByKey = response.getComponentsList().stream().collect(uniqueIndex(Component::getKey));
    Component actualProject = componentByKey.get(project.getKey());
    assertThat(actualProject.hasBranch()).isFalse();
    assertThat(actualProject.getPullRequest()).isEqualTo(pullRequest.getPullRequest());
    Component actualDirectory = componentByKey.get(directory.getKey());
    assertThat(actualDirectory.hasBranch()).isFalse();
    assertThat(actualDirectory.getPullRequest()).isEqualTo(pullRequest.getPullRequest());
    Component actualFile = componentByKey.get(file.getKey());
    assertThat(actualFile.hasBranch()).isFalse();
    assertThat(actualFile.getPullRequest()).isEqualTo(pullRequest.getPullRequest());
  }

  @Test
  public void returns_hotspots_ordered_by_vulnerabilityProbability_score_then_rule_id() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    List<IssueDto> hotspots = Arrays.stream(SQCategory.values())
      .sorted(Ordering.from(Comparator.<SQCategory>comparingInt(t1 -> t1.getVulnerability().getScore()).reversed())
        .thenComparing(SQCategory::getKey))
      .flatMap(sqCategory -> {
        Set<String> cwes = SecurityStandards.CWES_BY_SQ_CATEGORY.get(sqCategory);
        Set<String> securityStandards = singleton("cwe:" + (cwes == null ? "unknown" : cwes.iterator().next()));
        RuleDefinitionDto rule1 = newRule(
          SECURITY_HOTSPOT,
          t -> t.setName("rule_" + sqCategory.name() + "_a").setSecurityStandards(securityStandards));
        RuleDefinitionDto rule2 = newRule(
          SECURITY_HOTSPOT,
          t -> t.setName("rule_" + sqCategory.name() + "_b").setSecurityStandards(securityStandards));
        return Stream.of(
          newHotspot(rule1, project, file).setKee(sqCategory + "_a"),
          newHotspot(rule2, project, file).setKee(sqCategory + "_b"));
      })
      .collect(toList());
    String[] expectedHotspotKeys = hotspots.stream().map(IssueDto::getKey).toArray(String[]::new);
    // insert hotspots in random order
    Collections.shuffle(hotspots);
    hotspots.forEach(dbTester.issues()::insertHotspot);
    indexIssues();

    SearchWsResponse response = newRequest(project)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactly(expectedHotspotKeys);
  }

  @Test
  public void returns_hotspots_ordered_by_file_path_then_line_then_key() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    indexPermissions();
    ComponentDto file1 = dbTester.components().insertComponent(newFileDto(project).setPath("b/c/a"));
    ComponentDto file2 = dbTester.components().insertComponent(newFileDto(project).setPath("b/c/b"));
    ComponentDto file3 = dbTester.components().insertComponent(newFileDto(project).setPath("a/a/d"));
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
    List<IssueDto> hotspots = Stream.of(
      newHotspot(rule, project, file3).setLine(8),
      newHotspot(rule, project, file3).setLine(10),
      newHotspot(rule, project, file1).setLine(null),
      newHotspot(rule, project, file1).setLine(9),
      newHotspot(rule, project, file1).setLine(11).setKee("a"),
      newHotspot(rule, project, file1).setLine(11).setKee("b"),
      newHotspot(rule, project, file2).setLine(null),
      newHotspot(rule, project, file2).setLine(2))
      .collect(toList());
    String[] expectedHotspotKeys = hotspots.stream().map(IssueDto::getKey).toArray(String[]::new);
    // insert hotspots in random order
    Collections.shuffle(hotspots);
    hotspots.forEach(dbTester.issues()::insertHotspot);
    indexIssues();

    SearchWsResponse response = newRequest(project)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactly(expectedHotspotKeys);
  }

  @Test
  public void returns_first_page_with_100_results_by_default() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
    int total = 436;
    List<IssueDto> hotspots = IntStream.range(0, total)
      .mapToObj(i -> dbTester.issues().insertHotspot(rule, project, file, t -> t.setLine(i)))
      .collect(toList());
    indexIssues();

    TestRequest request = newRequest(project);

    SearchWsResponse response = request.executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactly(hotspots.stream().limit(100).map(IssueDto::getKey).toArray(String[]::new));
    assertThat(response.getPaging().getTotal()).isEqualTo(hotspots.size());
    assertThat(response.getPaging().getPageIndex()).isEqualTo(1);
    assertThat(response.getPaging().getPageSize()).isEqualTo(100);
  }

  @Test
  public void returns_specified_page_with_100_results_by_default() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);

    verifyPaging(project, file, rule, 336, 100);
  }

  @Test
  public void returns_specified_page_with_specified_number_of_results() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
    int total = 336;
    int pageSize = 1 + new Random().nextInt(100);

    verifyPaging(project, file, rule, total, pageSize);
  }

  private void verifyPaging(ComponentDto project, ComponentDto file, RuleDefinitionDto rule, int total, int pageSize) {
    List<IssueDto> hotspots = IntStream.range(0, total)
      .mapToObj(i -> dbTester.issues().insertHotspot(rule, project, file, t -> t.setLine(i).setKee("issue_" + i)))
      .collect(toList());
    indexIssues();

    SearchWsResponse response = newRequest(project)
      .setParam("p", "3")
      .setParam("ps", String.valueOf(pageSize))
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactly(hotspots.stream().skip(2 * pageSize).limit(pageSize).map(IssueDto::getKey).toArray(String[]::new));
    assertThat(response.getPaging().getTotal()).isEqualTo(hotspots.size());
    assertThat(response.getPaging().getPageIndex()).isEqualTo(3);
    assertThat(response.getPaging().getPageSize()).isEqualTo(pageSize);

    response = newRequest(project)
      .setParam("p", "4")
      .setParam("ps", String.valueOf(pageSize))
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactly(hotspots.stream().skip(3 * pageSize).limit(pageSize).map(IssueDto::getKey).toArray(String[]::new));
    assertThat(response.getPaging().getTotal()).isEqualTo(hotspots.size());
    assertThat(response.getPaging().getPageIndex()).isEqualTo(4);
    assertThat(response.getPaging().getPageSize()).isEqualTo(pageSize);

    int emptyPage = (hotspots.size() / pageSize) + 10;
    response = newRequest(project)
      .setParam("p", String.valueOf(emptyPage))
      .setParam("ps", String.valueOf(pageSize))
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .isEmpty();
    assertThat(response.getPaging().getTotal()).isEqualTo(hotspots.size());
    assertThat(response.getPaging().getPageIndex()).isEqualTo(emptyPage);
    assertThat(response.getPaging().getPageSize()).isEqualTo(pageSize);
  }

  @Test
  public void returns_empty_if_none_of_hotspot_keys_exist() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
    List<IssueDto> hotspots = IntStream.range(0, 1 + RANDOM.nextInt(15))
      .mapToObj(i -> dbTester.issues().insertHotspot(rule, project, file, t -> t.setLine(i)))
      .collect(toList());
    indexIssues();

    SearchWsResponse response = newRequest(IntStream.range(0, 1 + RANDOM.nextInt(30)).mapToObj(i -> "key_" + i).collect(toList()))
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList()).isEmpty();
  }

  @Test
  public void returns_specified_hotspots() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
    int total = 1 + RANDOM.nextInt(20);
    List<IssueDto> hotspots = IntStream.range(0, total)
      .mapToObj(i -> dbTester.issues().insertHotspot(rule, project, file, t -> t.setLine(i)))
      .collect(toList());
    Collections.shuffle(hotspots);
    List<IssueDto> selectedHotspots = hotspots.stream().limit(total == 1 ? 1 : 1 + RANDOM.nextInt(total - 1)).collect(toList());
    indexIssues();

    SearchWsResponse response = newRequest(selectedHotspots.stream().map(IssueDto::getKey).collect(toList()))
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactlyInAnyOrder(selectedHotspots.stream().map(IssueDto::getKey).toArray(String[]::new));
  }

  @Test
  public void returns_hotspots_on_the_leak_period_when_sinceLeakPeriod_is_true() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    long periodDate = 800_996_999_332L;
    dbTester.components().insertSnapshot(project, t -> t.setPeriodDate(periodDate).setLast(false));
    dbTester.components().insertSnapshot(project, t -> t.setPeriodDate(periodDate - 1_500).setLast(true));
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
    List<IssueDto> hotspotsInLeakPeriod = IntStream.range(0, 1 + RANDOM.nextInt(20))
      .mapToObj(i -> {
        long issueCreationDate = periodDate + ONE_MINUTE + (RANDOM.nextInt(300) * ONE_MINUTE);
        return dbTester.issues().insertHotspot(rule, project, file,
          t -> t.setLine(i).setIssueCreationTime(issueCreationDate));
      })
      .collect(toList());
    // included because
    List<IssueDto> atLeakPeriod = IntStream.range(0, 1 + RANDOM.nextInt(20))
      .mapToObj(i -> dbTester.issues().insertHotspot(rule, project, file,
        t -> t.setType(SECURITY_HOTSPOT).setLine(i).setIssueCreationTime(periodDate)))
      .collect(toList());
    List<IssueDto> hotspotsBefore = IntStream.range(0, 1 + RANDOM.nextInt(20))
      .mapToObj(i -> {
        long issueCreationDate = periodDate - ONE_MINUTE - (RANDOM.nextInt(300) * ONE_MINUTE);
        return dbTester.issues().insertHotspot(rule, project, file,
          t -> t.setLine(i).setIssueCreationTime(issueCreationDate));
      })
      .collect(toList());
    indexIssues();

    SearchWsResponse responseAll = newRequest(project)
      .executeProtobuf(SearchWsResponse.class);
    assertThat(responseAll.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactlyInAnyOrder(Stream.of(
        hotspotsInLeakPeriod.stream(),
        atLeakPeriod.stream(),
        hotspotsBefore.stream())
        .flatMap(t -> t)
        .map(IssueDto::getKey)
        .toArray(String[]::new));

    SearchWsResponse responseOnLeak = newRequest(project,
      t -> t.setParam("sinceLeakPeriod", "true"))
        .executeProtobuf(SearchWsResponse.class);
    assertThat(responseOnLeak.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactlyInAnyOrder(Stream.concat(
        hotspotsInLeakPeriod.stream(),
        atLeakPeriod.stream())
        .map(IssueDto::getKey)
        .toArray(String[]::new));
  }

  @Test
  public void verify_response_example() {
    ComponentDto project = dbTester.components().insertPublicProject(componentDto -> componentDto
      .setName("test-project")
      .setLongName("test-project")
      .setDbKey("com.sonarsource:test-project")
    );
    userSessionRule.registerComponents(project);
    indexPermissions();
    ComponentDto fileWithHotspot = dbTester.components().insertComponent(newFileDto(project)
      .setDbKey("com.sonarsource:test-project:src/main/java/com/sonarsource/FourthClass.java")
      .setName("FourthClass.java")
      .setLongName("src/main/java/com/sonarsource/FourthClass.java")
      .setPath("src/main/java/com/sonarsource/FourthClass.java")
    );

    long time = 1577976190000L;

    IssueDto[] hotspots = IntStream.range(0, 3)
      .mapToObj(i -> {
        RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT)
          .setSecurityStandards(Sets.newHashSet(SQCategory.WEAK_CRYPTOGRAPHY.getKey()));
        return insertHotspot(rule, project, fileWithHotspot, issueDto -> issueDto.setKee("hotspot-" + i)
          .setAssigneeUuid("assignee-uuid")
          .setAuthorLogin("joe")
          .setMessage("message-" +i)
          .setLine(10 + i)
          .setIssueCreationTime(time)
          .setIssueUpdateTime(time)
        );
      })
      .toArray(IssueDto[]::new);
    indexIssues();

    newRequest(project)
      .execute()
      .assertJson(actionTester.getDef().responseExampleAsString()
        .replaceAll("default-organization", dbTester.getDefaultOrganization().getKey()));
  }

  private IssueDto insertHotspot(ComponentDto project, ComponentDto file, RuleDefinitionDto rule) {
    return insertHotspot(rule, project, file, t -> {
    });
  }

  private IssueDto insertHotspot(RuleDefinitionDto rule, ComponentDto project, ComponentDto file, @Nullable String assigneeUuid) {
    return insertHotspot(rule, project, file, t -> t.setAssigneeUuid(assigneeUuid));
  }

  private IssueDto insertHotspot(RuleDefinitionDto rule, ComponentDto project, ComponentDto file, Consumer<IssueDto> consumer) {
    return dbTester.issues().insertHotspot(rule, project, file, consumer);
  }

  private static IssueDto newHotspot(RuleDefinitionDto rule, ComponentDto project, ComponentDto component) {
    return newHotspot(rule, project, component, t -> {
    });
  }

  private static IssueDto newHotspot(RuleDefinitionDto rule, ComponentDto project, ComponentDto component, Consumer<IssueDto> consumer) {
    IssueDto res = newIssue(rule, project, component)
      .setStatus(STATUS_TO_REVIEW);
    consumer.accept(res);
    return res.setType(SECURITY_HOTSPOT);
  }

  private TestRequest newRequest(ComponentDto project) {
    return newRequest(project, null, null);
  }

  private TestRequest newRequest(ComponentDto project, Consumer<TestRequest> consumer) {
    return newRequest(project, null, null, consumer);
  }

  private TestRequest newRequest(ComponentDto project, @Nullable String status, @Nullable String resolution) {
    return newRequest(project, status, resolution, t -> {
    });
  }

  private TestRequest newRequest(ComponentDto project, @Nullable String status, @Nullable String resolution, Consumer<TestRequest> consumer) {
    TestRequest res = actionTester.newRequest()
      .setParam("projectKey", project.getKey());
    String branch = project.getBranch();
    if (branch != null) {
      res.setParam("branch", branch);
    }
    String pullRequest = project.getPullRequest();
    if (pullRequest != null) {
      res.setParam("pullRequest", pullRequest);
    }
    if (status != null) {
      res.setParam("status", status);
    }
    if (resolution != null) {
      res.setParam("resolution", resolution);
    }
    consumer.accept(res);
    return res;
  }

  private TestRequest newRequest(Collection<String> hotspotKeys) {
    return actionTester.newRequest()
      .setParam("hotspots", String.join(",", hotspotKeys));
  }

  private void indexPermissions() {
    permissionIndexer.indexOnStartup(permissionIndexer.getIndexTypes());
  }

  private void indexIssues() {
    issueIndexer.indexOnStartup(issueIndexer.getIndexTypes());
  }

  private void indexViews() {
    viewIndexer.indexOnStartup(viewIndexer.getIndexTypes());
  }

  private RuleDefinitionDto newRule(RuleType ruleType) {
    return newRule(ruleType, t -> {
    });
  }

  private RuleDefinitionDto newRule(RuleType ruleType, Consumer<RuleDefinitionDto> populate) {
    RuleDefinitionDto ruleDefinition = RuleTesting.newRule()
      .setType(ruleType);
    populate.accept(ruleDefinition);
    dbTester.rules().insert(ruleDefinition);
    return ruleDefinition;
  }
}
