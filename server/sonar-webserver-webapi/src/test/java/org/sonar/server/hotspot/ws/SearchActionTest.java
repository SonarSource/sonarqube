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
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
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
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.StartupIndexer;
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
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Hotspots;
import org.sonarqube.ws.Hotspots.Component;
import org.sonarqube.ws.Hotspots.SearchWsResponse;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_SAFE;
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
  private StartupIndexer permissionIndexer = new PermissionIndexer(dbClient, es.client(), issueIndexer);
  private HotspotWsResponseFormatter responseFormatter = new HotspotWsResponseFormatter(defaultOrganizationProvider);

  private SearchAction underTest = new SearchAction(dbClient, userSessionRule, issueIndex, responseFormatter);
  private WsActionTester actionTester = new WsActionTester(underTest);

  @Test
  public void ws_is_internal() {
    assertThat(actionTester.getDef().isInternal()).isTrue();
  }

  @Test
  public void fails_with_IAE_if_parameter_projectKey_is_missing() {
    TestRequest request = actionTester.newRequest();

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'projectKey' parameter is missing");
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
  public void fails_with_NotFoundException_if_project_is_not_a_project() {
    ComponentDto project = dbTester.components().insertPrivateProject();
    ComponentDto directory = dbTester.components().insertComponent(ComponentTesting.newDirectory(project, "foo"));
    ComponentDto file = dbTester.components().insertComponent(ComponentTesting.newFileDto(project));
    ComponentDto portfolio = dbTester.components().insertPrivatePortfolio(dbTester.getDefaultOrganization());
    ComponentDto application = dbTester.components().insertPrivateApplication(dbTester.getDefaultOrganization());
    TestRequest request = actionTester.newRequest();

    for (ComponentDto component : Arrays.asList(directory, file, portfolio, application)) {
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
  public void succeeds_on_public_project() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);

    SearchWsResponse response = newRequest(project)
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
  public void does_not_fail_if_rule_of_hotspot_does_not_exist_in_DB() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    indexPermissions();
    IssueDto[] hotspots = IntStream.range(0, 1 + RANDOM.nextInt(10))
      .mapToObj(i -> {
        RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
        return dbTester.issues().insert(rule, project, file, t -> t.setType(SECURITY_HOTSPOT));
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
        return dbTester.issues().insert(rule, project, fileWithHotspot, t -> t.setType(SECURITY_HOTSPOT));
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
        return dbTester.issues().insert(rule, project, project, t -> t.setType(SECURITY_HOTSPOT));
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
        dbTester.issues().insert(rule, project1, file1, t -> t.setType(SECURITY_HOTSPOT));
        return dbTester.issues().insert(rule, project2, file2, t -> t.setType(SECURITY_HOTSPOT));
      })
      .toArray(IssueDto[]::new);
    indexIssues();

    SearchWsResponse responseProject1 = newRequest(project1)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(responseProject1.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .doesNotContainAnyElementsOf(Arrays.stream(hotspots2).map(IssueDto::getKey).collect(Collectors.toList()));
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
  public void returns_hotpots_with_any_status_if_no_status_nor_resolution_parameter() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    List<IssueDto> hotspots = insertRandomNumberOfHotspotsOfAllSupportedStatusesAndResolutions(project, file)
      .collect(Collectors.toList());
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
      .collect(Collectors.toList());
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
      .collect(Collectors.toList());
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
      .collect(Collectors.toList());
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
    IssueDto unresolvedHotspot = dbTester.issues().insert(rule, project, file,
      t -> t.setType(SECURITY_HOTSPOT).setResolution(null));
    // unrealistic case since a resolution must be set, but shows a limit of current implementation
    IssueDto badlyClosedHotspot = dbTester.issues().insert(rule, project, file,
      t -> t.setType(SECURITY_HOTSPOT).setStatus(Issue.STATUS_CLOSED).setResolution(null));
    IssueDto resolvedHotspot = dbTester.issues().insert(rule, project, file,
      t -> t.setType(SECURITY_HOTSPOT).setResolution(randomAlphabetic(5)));
    indexIssues();

    SearchWsResponse response = newRequest(project, STATUS_TO_REVIEW, null)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsOnly(unresolvedHotspot.getKey(), badlyClosedHotspot.getKey());
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
      .collect(Collectors.toList());
    Collections.shuffle(hotspots);
    hotspots.forEach(t -> dbTester.issues().insertIssue(t));
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
    IssueDto hotspot = dbTester.issues().insert(rule, project, file,
      t -> t.setType(SECURITY_HOTSPOT)
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
    IssueDto hotspot = dbTester.issues().insert(rule, project, file, t -> t.setType(SECURITY_HOTSPOT));
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
    dbTester.issues().insert(rule, project, file,
      t -> t.setType(SECURITY_HOTSPOT)
        .setStatus(null)
        .setResolution(null)
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
    assertThat(actual.hasStatus()).isFalse();
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
    IssueDto fileHotspot = dbTester.issues().insert(rule, project, file, t -> t.setType(SECURITY_HOTSPOT));
    IssueDto dirHotspot = dbTester.issues().insert(rule, project, directory, t -> t.setType(SECURITY_HOTSPOT));
    IssueDto projectHotspot = dbTester.issues().insert(rule, project, project, t -> t.setType(SECURITY_HOTSPOT));
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
    Component actualDirectory = componentByKey.get(directory.getKey());
    assertThat(actualDirectory.getQualifier()).isEqualTo(directory.qualifier());
    assertThat(actualDirectory.getName()).isEqualTo(directory.name());
    assertThat(actualDirectory.getLongName()).isEqualTo(directory.longName());
    assertThat(actualDirectory.getPath()).isEqualTo(directory.path());
    Component actualFile = componentByKey.get(file.getKey());
    assertThat(actualFile.getQualifier()).isEqualTo(file.qualifier());
    assertThat(actualFile.getName()).isEqualTo(file.name());
    assertThat(actualFile.getLongName()).isEqualTo(file.longName());
    assertThat(actualFile.getPath()).isEqualTo(file.path());
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
          newIssue(rule1, project, file).setKee(sqCategory + "_a").setType(SECURITY_HOTSPOT),
          newIssue(rule2, project, file).setKee(sqCategory + "_b").setType(SECURITY_HOTSPOT));
      })
      .collect(Collectors.toList());
    String[] expectedHotspotKeys = hotspots.stream().map(IssueDto::getKey).toArray(String[]::new);
    // insert hotspots in random order
    Collections.shuffle(hotspots);
    hotspots.forEach(dbTester.issues()::insertIssue);
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
      newIssue(rule, project, file3).setType(SECURITY_HOTSPOT).setLine(8),
      newIssue(rule, project, file3).setType(SECURITY_HOTSPOT).setLine(10),
      newIssue(rule, project, file1).setType(SECURITY_HOTSPOT).setLine(null),
      newIssue(rule, project, file1).setType(SECURITY_HOTSPOT).setLine(9),
      newIssue(rule, project, file1).setType(SECURITY_HOTSPOT).setLine(11).setKee("a"),
      newIssue(rule, project, file1).setType(SECURITY_HOTSPOT).setLine(11).setKee("b"),
      newIssue(rule, project, file2).setType(SECURITY_HOTSPOT).setLine(null),
      newIssue(rule, project, file2).setType(SECURITY_HOTSPOT).setLine(2))
      .collect(Collectors.toList());
    String[] expectedHotspotKeys = hotspots.stream().map(IssueDto::getKey).toArray(String[]::new);
    // insert hotspots in random order
    Collections.shuffle(hotspots);
    hotspots.forEach(dbTester.issues()::insertIssue);
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
      .mapToObj(i -> newIssue(rule, project, file).setType(SECURITY_HOTSPOT).setLine(i))
      .map(i -> dbTester.issues().insertIssue(i))
      .collect(Collectors.toList());
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
      .mapToObj(i -> newIssue(rule, project, file).setType(SECURITY_HOTSPOT).setLine(i).setKee("issue_" + i))
      .map(i -> dbTester.issues().insertIssue(i))
      .collect(Collectors.toList());
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

  private TestRequest newRequest(ComponentDto project) {
    return newRequest(project, null, null);
  }

  private TestRequest newRequest(ComponentDto project, @Nullable String status, @Nullable String resolution) {
    TestRequest res = actionTester.newRequest()
      .setParam("projectKey", project.getKey());
    if (status != null) {
      res.setParam("status", status);
    }
    if (resolution != null) {
      res.setParam("resolution", resolution);
    }
    return res;
  }

  private void indexPermissions() {
    permissionIndexer.indexOnStartup(permissionIndexer.getIndexTypes());
  }

  private void indexIssues() {
    issueIndexer.indexOnStartup(issueIndexer.getIndexTypes());
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
