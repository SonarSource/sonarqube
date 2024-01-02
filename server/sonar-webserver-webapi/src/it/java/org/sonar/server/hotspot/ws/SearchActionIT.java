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
package org.sonar.server.hotspot.ws;

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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ProjectData;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.TextRangeResponseFormatter;
import org.sonar.server.issue.index.AsyncIssueIndexing;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexSyncProgressChecker;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.permission.index.PermissionIndexer;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.security.SecurityStandards;
import org.sonar.server.security.SecurityStandards.SQCategory;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.view.index.ViewIndexer;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Hotspots.Component;
import org.sonarqube.ws.Hotspots.SearchWsResponse;

import static com.google.common.collect.ImmutableSet.of;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.sonar.api.issue.Issue.RESOLUTION_ACKNOWLEDGED;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_SAFE;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;
import static org.sonar.api.issue.Issue.STATUS_REVIEWED;
import static org.sonar.api.issue.Issue.STATUS_TO_REVIEW;
import static org.sonar.api.rules.RuleType.SECURITY_HOTSPOT;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newDirectoryOnBranch;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.issue.IssueTesting.newCodeReferenceIssue;
import static org.sonar.db.issue.IssueTesting.newIssue;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.REFERENCE_BRANCH;

@SuppressWarnings("ALL")
@RunWith(DataProviderRunner.class)
public class SearchActionIT {

  private static final String PARAM_PROJECT = "project";
  private static final String PARAM_STATUS = "status";
  private static final String PARAM_RESOLUTION = "resolution";
  private static final String PARAM_HOTSPOTS = "hotspots";
  private static final String PARAM_BRANCH = "branch";
  private static final String PARAM_PULL_REQUEST = "pullRequest";
  private static final String PARAM_IN_NEW_CODE_PERIOD = "inNewCodePeriod";
  private static final String PARAM_ONLY_MINE = "onlyMine";
  private static final String PARAM_PCI_DSS_32 = "pciDss-3.2";
  private static final String PARAM_PCI_DSS_40 = "pciDss-4.0";
  private static final String PARAM_OWASP_ASVS_40 = "owaspAsvs-4.0";
  private static final String PARAM_OWASP_ASVS_LEVEL = "owaspAsvsLevel";
  private static final String PARAM_OWASP_TOP_10_2017 = "owaspTop10";
  private static final String PARAM_OWASP_TOP_10_2021 = "owaspTop10-2021";
  private static final String PARAM_SANS_TOP_25 = "sansTop25";
  private static final String PARAM_SONARSOURCE_SECURITY = "sonarsourceSecurity";
  private static final String PARAM_CWE = "cwe";
  private static final String PARAM_FILES = "files";

  private static final Random RANDOM = new Random();
  private static final int ONE_MINUTE = 60_000;
  private static final List<String> RESOLUTION_TYPES = List.of(RESOLUTION_FIXED, RESOLUTION_SAFE, RESOLUTION_ACKNOWLEDGED);

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private final TestSystem2 system2 = new TestSystem2();
  private final DbClient dbClient = dbTester.getDbClient();
  private final IssueIndex issueIndex = new IssueIndex(es.client(), System2.INSTANCE, userSessionRule, new WebAuthorizationTypeSupport(userSessionRule));
  private final IssueIndexer issueIndexer = new IssueIndexer(es.client(), dbClient, new IssueIteratorFactory(dbClient), mock(AsyncIssueIndexing.class));
  private final ViewIndexer viewIndexer = new ViewIndexer(dbClient, es.client());
  private final PermissionIndexer permissionIndexer = new PermissionIndexer(dbClient, es.client(), issueIndexer);
  private final HotspotWsResponseFormatter responseFormatter = new HotspotWsResponseFormatter(new TextRangeResponseFormatter());
  private final IssueIndexSyncProgressChecker issueIndexSyncProgressChecker = mock(IssueIndexSyncProgressChecker.class);
  private final ComponentFinder componentFinder = TestComponentFinder.from(dbTester);
  private final SearchAction underTest = new SearchAction(dbClient, userSessionRule, issueIndex,
    issueIndexSyncProgressChecker, responseFormatter, system2, componentFinder);
  private final WsActionTester actionTester = new WsActionTester(underTest);

  @Test
  public void verify_ws_def() {
    WebService.Param onlyMineParam = actionTester.getDef().param(PARAM_ONLY_MINE);
    WebService.Param pciDss32Param = actionTester.getDef().param(PARAM_PCI_DSS_32);
    WebService.Param pciDss40Param = actionTester.getDef().param(PARAM_PCI_DSS_40);
    WebService.Param owasAsvs40Param = actionTester.getDef().param(PARAM_OWASP_ASVS_40);
    WebService.Param owaspTop10Param = actionTester.getDef().param(PARAM_OWASP_TOP_10_2017);
    WebService.Param sansTop25Param = actionTester.getDef().param(PARAM_SANS_TOP_25);
    WebService.Param sonarsourceSecurityParam = actionTester.getDef().param(PARAM_SONARSOURCE_SECURITY);
    WebService.Param filesParam = actionTester.getDef().param(PARAM_FILES);

    assertThat(actionTester.getDef().isInternal()).isFalse();
    assertThat(onlyMineParam).isNotNull();
    assertThat(onlyMineParam.isRequired()).isFalse();
    assertThat(actionTester.getDef().param(PARAM_ONLY_MINE).possibleValues())
      .containsExactlyInAnyOrder("yes", "no", "true", "false");

    assertThat(pciDss32Param).isNotNull();
    assertThat(pciDss32Param.isRequired()).isFalse();
    assertThat(pciDss40Param).isNotNull();
    assertThat(pciDss40Param.isRequired()).isFalse();
    assertThat(owasAsvs40Param).isNotNull();
    assertThat(owasAsvs40Param.isRequired()).isFalse();
    assertThat(owaspTop10Param).isNotNull();
    assertThat(owaspTop10Param.isRequired()).isFalse();
    assertThat(sansTop25Param).isNotNull();
    assertThat(sansTop25Param.isRequired()).isFalse();
    assertThat(sonarsourceSecurityParam).isNotNull();
    assertThat(sonarsourceSecurityParam.isRequired()).isFalse();
    assertThat(filesParam).isNotNull();
  }

  @Test
  public void fails_with_IAE_if_parameters_project_and_hotspots_are_missing() {
    TestRequest request = actionTester.newRequest();

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("A value must be provided for either parameter 'project' or parameter 'hotspots'");
  }

  @Test
  public void fail_with_IAE_if_parameter_branch_is_used_without_parameter_project() {
    TestRequest request = actionTester.newRequest()
      .setParam(PARAM_HOTSPOTS, randomAlphabetic(2))
      .setParam(PARAM_BRANCH, randomAlphabetic(1));

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Parameter 'branch' must be used with parameter 'project'");
  }

  @Test
  public void fail_with_IAE_if_parameter_pullRequest_is_used_without_parameter_project() {
    TestRequest request = actionTester.newRequest()
      .setParam(PARAM_HOTSPOTS, randomAlphabetic(2))
      .setParam(PARAM_PULL_REQUEST, randomAlphabetic(1));

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Parameter 'pullRequest' must be used with parameter 'project'");
  }

  @Test
  public void fail_with_IAE_if_both_parameters_pullRequest_and_branch_are_provided() {
    TestRequest request = actionTester.newRequest()
      .setParam(PARAM_PROJECT, randomAlphabetic(2))
      .setParam(PARAM_BRANCH, randomAlphabetic(1))
      .setParam(PARAM_PULL_REQUEST, randomAlphabetic(1));

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Only one of parameters 'branch' and 'pullRequest' can be provided");
  }

  @Test
  @UseDataProvider("badStatuses")
  public void fails_with_IAE_if_status_parameter_is_neither_TO_REVIEW_or_REVIEWED(String badStatus) {
    TestRequest request = actionTester.newRequest()
      .setParam(PARAM_PROJECT, randomAlphabetic(13))
      .setParam(PARAM_STATUS, badStatus);

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
      .setParam(PARAM_HOTSPOTS, randomAlphabetic(12))
      .setParam(PARAM_STATUS, status);

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Parameter 'status' can't be used with parameter 'hotspots'");
  }

  @Test
  @UseDataProvider("badResolutions")
  public void fails_with_IAE_if_resolution_parameter_is_neither_FIXED_nor_SAFE(String badResolution) {
    TestRequest request = actionTester.newRequest()
      .setParam(PARAM_PROJECT, randomAlphabetic(13))
      .setParam(PARAM_STATUS, STATUS_TO_REVIEW)
      .setParam(PARAM_RESOLUTION, badResolution);

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value of parameter 'resolution' (" + badResolution + ") must be one of: [FIXED, SAFE, ACKNOWLEDGED]");
  }

  @DataProvider
  public static Object[][] badResolutions() {
    return Stream.of(
      Issue.RESOLUTIONS.stream(),
      Issue.SECURITY_HOTSPOT_RESOLUTIONS.stream(),
      Stream.of(randomAlphabetic(4)))
      .flatMap(t -> t)
      .filter(t -> !RESOLUTION_TYPES.contains(t))
      .map(t -> new Object[] {t})
      .toArray(Object[][]::new);
  }

  @Test
  @UseDataProvider("fixedOrSafeResolution")
  public void fails_with_IAE_if_resolution_is_provided_with_status_TO_REVIEW(String resolution) {
    TestRequest request = actionTester.newRequest()
      .setParam(PARAM_PROJECT, randomAlphabetic(13))
      .setParam(PARAM_STATUS, STATUS_TO_REVIEW)
      .setParam(PARAM_RESOLUTION, resolution);

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value '" + resolution + "' of parameter 'resolution' can only be provided if value of parameter 'status' is 'REVIEWED'");
  }

  @Test
  @UseDataProvider("fixedOrSafeResolution")
  public void fails_with_IAE_if_resolution_is_provided_with_hotspots_parameter(String resolution) {
    TestRequest request = actionTester.newRequest()
      .setParam(PARAM_HOTSPOTS, randomAlphabetic(13))
      .setParam(PARAM_RESOLUTION, resolution);

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
      .setParam(PARAM_PROJECT, key);

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Project '%s' not found", key);
  }

  @Test
  public void fails_with_NotFoundException_if_project_is_neither_a_project_nor_an_application() {
    ComponentDto project = dbTester.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto directory = dbTester.components().insertComponent(ComponentTesting.newDirectory(project, "foo"));
    ComponentDto file = dbTester.components().insertComponent(ComponentTesting.newFileDto(project));
    ComponentDto portfolio = dbTester.components().insertPrivatePortfolio();
    TestRequest request = actionTester.newRequest();

    for (ComponentDto component : Arrays.asList(directory, file, portfolio)) {
      request.setParam(PARAM_PROJECT, component.getKey());

      assertThatThrownBy(request::execute)
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Project '%s' not found", component.getKey());
    }
  }

  @Test
  public void fails_with_ForbiddenException_if_project_is_private_and_not_allowed() {
    ProjectData projectData = dbTester.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    TestRequest request = newRequest(project);

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void fails_with_ForbiddenException_if_application_is_private_and_not_allowed() {
    ProjectData projectData = dbTester.components().insertPrivateApplication();
    ComponentDto application = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    TestRequest request = newRequest(application);

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void succeeds_on_public_project() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());

    SearchWsResponse response = newRequest(project)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList()).isEmpty();
    assertThat(response.getComponentsList()).isEmpty();
  }

  @Test
  public void succeeds_on_public_application() {
    ProjectData applicationData = dbTester.components().insertPublicApplication();
    ComponentDto application = applicationData.getMainBranchComponent();
    userSessionRule.registerApplication(applicationData.getProjectDto());

    SearchWsResponse response = newRequest(application)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList()).isEmpty();
    assertThat(response.getComponentsList()).isEmpty();
  }

  @Test
  public void succeeds_on_private_project_with_permission() {
    ProjectData projectData = dbTester.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    userSessionRule.logIn().addProjectPermission(USER, projectData.getProjectDto());

    SearchWsResponse response = newRequest(project).executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList()).isEmpty();
    assertThat(response.getComponentsList()).isEmpty();
  }

  @Test
  public void succeeds_on_private_application_with_permission() {
    ProjectData applicationData = dbTester.components().insertPrivateApplication();
    ComponentDto application = applicationData.getMainBranchComponent();
    userSessionRule.logIn().registerApplication(applicationData.getProjectDto()).addProjectPermission(USER, applicationData.getProjectDto());

    SearchWsResponse response = newRequest(application).executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList()).isEmpty();
    assertThat(response.getComponentsList()).isEmpty();
  }

  @Test
  public void does_not_fail_if_rule_of_hotspot_does_not_exist_in_DB() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    indexPermissions();
    IssueDto[] hotspots = IntStream.range(0, 1 + RANDOM.nextInt(10))
      .mapToObj(i -> {
        RuleDto rule = newRule(SECURITY_HOTSPOT);
        return insertHotspot(project, file, rule);
      })
      .toArray(IssueDto[]::new);
    indexIssues();
    IssueDto hotspotWithoutRule = hotspots[RANDOM.nextInt(hotspots.length)];
    dbTester.executeUpdateSql("delete from rules where uuid=?", hotspotWithoutRule.getRuleUuid());

    SearchWsResponse response = newRequest(project)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsOnly(Arrays.stream(hotspots)
        .map(IssueDto::getKey)
        .filter(key -> !key.equals(hotspotWithoutRule.getKey()))
        .toArray(String[]::new));
  }

  @Test
  public void returns_no_hotspot_component_nor_rule_when_project_has_no_hotspot() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    Arrays.stream(RuleType.values())
      .filter(t -> t != SECURITY_HOTSPOT)
      .forEach(ruleType -> {
        RuleDto rule = newRule(ruleType);
        dbTester.issues().insert(rule, project, file, t -> t.setType(ruleType));
      });
    indexIssues();

    SearchWsResponse response = newRequest(project)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList()).isEmpty();
  }

  @Test
  public void returns_hotspot_components_when_project_has_hotspots() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    ComponentDto fileWithHotspot = dbTester.components().insertComponent(newFileDto(project));
    Arrays.stream(RuleType.values())
      .filter(t -> t != SECURITY_HOTSPOT)
      .forEach(ruleType -> {
        RuleDto rule = newRule(ruleType);
        dbTester.issues().insert(rule, project, file, t -> t.setType(ruleType));
      });
    IssueDto[] hotspots = IntStream.range(0, 1 + RANDOM.nextInt(10))
      .mapToObj(i -> {
        RuleDto rule = newRule(SECURITY_HOTSPOT);
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
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    IssueDto[] hotspots = IntStream.range(0, 1 + RANDOM.nextInt(10))
      .mapToObj(i -> {
        RuleDto rule = newRule(SECURITY_HOTSPOT);
        return insertHotspot(project, project, rule);
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
      .containsOnly(project.getKey());
  }

  @Test
  public void returns_hotspots_of_specified_project() {
    ProjectData projectData1 = dbTester.components().insertPublicProject();
    ComponentDto project1 = projectData1.getMainBranchComponent();
    ProjectData projectData2 = dbTester.components().insertPublicProject();
    ComponentDto project2 = projectData2.getMainBranchComponent();
    userSessionRule.registerProjects(projectData1.getProjectDto(), projectData2.getProjectDto());
    indexPermissions();
    ComponentDto file1 = dbTester.components().insertComponent(newFileDto(project1));
    ComponentDto file2 = dbTester.components().insertComponent(newFileDto(project2));
    IssueDto[] hotspots2 = IntStream.range(0, 1 + RANDOM.nextInt(10))
      .mapToObj(i -> {
        RuleDto rule = newRule(SECURITY_HOTSPOT);
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
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(mainBranch));

    RuleDto rule = newRule(SECURITY_HOTSPOT);
    IssueDto toReviewHotspot = insertHotspot(rule, mainBranch, file, i -> i.setStatus(STATUS_TO_REVIEW));
    IssueDto reviewedHotspot = insertHotspot(rule, mainBranch, file, i -> i.setStatus(STATUS_REVIEWED).setResolution(RESOLUTION_ACKNOWLEDGED));
    IssueDto wrongStatusHotspot = insertHotspot(rule, mainBranch, file, i -> i.setStatus(STATUS_RESOLVED).setResolution(RESOLUTION_FIXED));

    indexIssues();

    SearchWsResponse response = newRequest(mainBranch)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsOnly(toReviewHotspot.getKey(), reviewedHotspot.getKey());
  }

  @Test
  public void returns_hotspots_of_specified_application() {
    ProjectData application1 = dbTester.components().insertPublicApplication();
    ProjectData application2 = dbTester.components().insertPublicApplication();
    ProjectData project1 = dbTester.components().insertPublicProject();
    ProjectData project2 = dbTester.components().insertPublicProject();
    dbTester.components().insertComponent(ComponentTesting.newProjectCopy(project1, application1));
    dbTester.components().insertComponent(ComponentTesting.newProjectCopy(project2, application2));
    indexViews();
    userSessionRule.registerApplication(application1.getProjectDto(), project1.getProjectDto())
      .registerApplication(application2.getProjectDto(), project2.getProjectDto());
    indexPermissions();
    ComponentDto file1 = dbTester.components().insertComponent(newFileDto(project1.getMainBranchComponent()));
    ComponentDto file2 = dbTester.components().insertComponent(newFileDto(project2.getMainBranchComponent()));
    IssueDto[] hotspots2 = IntStream.range(0, 1 + RANDOM.nextInt(10))
      .mapToObj(i -> {
        RuleDto rule = newRule(SECURITY_HOTSPOT);
        insertHotspot(project1.getMainBranchComponent(), file1, rule);
        return insertHotspot(project2.getMainBranchComponent(), file2, rule);
      })
      .toArray(IssueDto[]::new);
    indexIssues();

    SearchWsResponse responseApplication1 = newRequest(application1.getMainBranchComponent())
      .executeProtobuf(SearchWsResponse.class);

    assertThat(responseApplication1.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .doesNotContainAnyElementsOf(Arrays.stream(hotspots2).map(IssueDto::getKey).collect(toList()));
    assertThat(responseApplication1.getComponentsList())
      .extracting(Component::getKey)
      .containsOnly(project1.projectKey(), file1.getKey());

    SearchWsResponse responseApplication2 = newRequest(application2.getMainBranchComponent())
      .executeProtobuf(SearchWsResponse.class);

    assertThat(responseApplication2.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsOnly(Arrays.stream(hotspots2).map(IssueDto::getKey).toArray(String[]::new));
    assertThat(responseApplication2.getComponentsList())
      .extracting(Component::getKey)
      .containsOnly(project2.projectKey(), file2.getKey());
  }

  @Test
  public void returns_hotspots_of_specified_application_branch() {
    ProjectData applicationData = dbTester.components().insertPublicApplication();
    ComponentDto application = applicationData.getMainBranchComponent();
    ComponentDto applicationBranch = dbTester.components().insertProjectBranch(application, b -> b.setKey("appBranch"));
    ProjectData projectData1 = dbTester.components().insertPublicProject();
    ComponentDto project1 = projectData1.getMainBranchComponent();
    ProjectData projectData2 = dbTester.components().insertPublicProject();
    ComponentDto project2 = projectData2.getMainBranchComponent();
    dbTester.components().insertComponent(ComponentTesting.newProjectCopy(project1, application));
    dbTester.components().insertComponent(ComponentTesting.newProjectCopy(project2, applicationBranch));
    indexViews();
    userSessionRule.registerApplication(applicationData.getProjectDto(), projectData1.getProjectDto(), projectData2.getProjectDto());
    indexPermissions();
    ComponentDto file1 = dbTester.components().insertComponent(newFileDto(project1));
    ComponentDto file2 = dbTester.components().insertComponent(newFileDto(project2));
    IssueDto[] hotspots2 = IntStream.range(0, 1 + RANDOM.nextInt(10))
      .mapToObj(i -> {
        RuleDto rule = newRule(SECURITY_HOTSPOT);
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

    SearchWsResponse responseApplicationBranch = newRequest(application, null, null, "appBranch", null)
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
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto branch = dbTester.components().insertProjectBranch(project, b -> b.setKey("branch"));
    ComponentDto pullRequest = dbTester.components().insertProjectBranch(project, t -> t.setBranchType(BranchType.PULL_REQUEST).setKey("prKey"));
    ComponentDto fileProject = dbTester.components().insertComponent(newFileDto(project));
    ComponentDto fileBranch = dbTester.components().insertComponent(newFileDto(branch, project.uuid()));
    ComponentDto filePR = dbTester.components().insertComponent(newFileDto(pullRequest, project.uuid()));
    IssueDto[] hotspotProject = IntStream.range(0, 1 + RANDOM.nextInt(10))
      .mapToObj(i -> {
        RuleDto rule = newRule(SECURITY_HOTSPOT);
        return insertHotspot(project, fileProject, rule);
      })
      .toArray(IssueDto[]::new);
    IssueDto[] hotspotBranch = IntStream.range(0, 1 + RANDOM.nextInt(10))
      .mapToObj(i -> {
        RuleDto rule = newRule(SECURITY_HOTSPOT);
        return insertHotspot(branch, fileBranch, rule);
      })
      .toArray(IssueDto[]::new);
    IssueDto[] hotspotPR = IntStream.range(0, 1 + RANDOM.nextInt(10))
      .mapToObj(i -> {
        RuleDto rule = newRule(SECURITY_HOTSPOT);
        return insertHotspot(pullRequest, filePR, rule);
      })
      .toArray(IssueDto[]::new);
    indexIssues();

    SearchWsResponse responseProject = newRequest(project)
      .executeProtobuf(SearchWsResponse.class);
    SearchWsResponse responseBranch = newRequest(branch, res -> res.setParam(PARAM_BRANCH, "branch"))
      .executeProtobuf(SearchWsResponse.class);
    SearchWsResponse responsePR = newRequest(pullRequest, res -> res.setParam(PARAM_PULL_REQUEST, "prKey"))
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

    verify(issueIndexSyncProgressChecker, times(3)).checkIfComponentNeedIssueSync(any(), eq(project.getKey()));
  }

  @Test
  @UseDataProvider("onlyMineParamValues")
  public void returns_hotspots_of_specified_project_assigned_to_current_user_if_only_mine_is_set(String onlyMineParameter, boolean shouldFilter) {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project1 = projectData.getMainBranchComponent();
    String assigneeUuid = this.userSessionRule.logIn().registerProjects(projectData.getProjectDto()).getUuid();

    indexPermissions();
    ComponentDto file1 = dbTester.components().insertComponent(newFileDto(project1));
    IssueDto[] assigneeHotspots = IntStream.range(0, 1 + RANDOM.nextInt(10))
      .mapToObj(i -> {
        RuleDto rule = newRule(SECURITY_HOTSPOT);
        insertHotspot(rule, project1, file1, randomAlphabetic(5));
        return insertHotspot(rule, project1, file1, assigneeUuid);
      })
      .toArray(IssueDto[]::new);

    indexIssues();

    SearchWsResponse allHotspots = newRequest(project1)
      .executeProtobuf(SearchWsResponse.class);

    SearchWsResponse userHotspots = newRequest(project1, r -> r.setParam(PARAM_ONLY_MINE, onlyMineParameter))
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
    ProjectData projectData = dbTester.components().insertPrivateProject();

    userSessionRule.registerProjects(projectData.getProjectDto());
    userSessionRule.logIn().addProjectPermission(USER, projectData.getProjectDto());

    TestRequest request = actionTester.newRequest()
      .setParam(PARAM_HOTSPOTS, IntStream.range(2, 10).mapToObj(String::valueOf).collect(joining(",")))
      .setParam(PARAM_ONLY_MINE, "true");
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Parameter 'onlyMine' can be used with parameter 'project' only");
  }

  @Test
  public void fail_if_user_not_authenticated_with_onlyMine_param() {
    ComponentDto project = dbTester.components().insertPublicProject().getMainBranchComponent();

    userSessionRule.anonymous();

    TestRequest request = actionTester.newRequest()
      .setParam(PARAM_PROJECT, project.getKey())
      .setParam(PARAM_ONLY_MINE, "true");
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Parameter 'onlyMine' requires user to be logged in");
  }

  @Test
  public void returns_hotpots_with_any_status_if_no_status_nor_resolution_parameter() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    List<IssueDto> hotspots = insertRandomNumberOfHotspotsOfAllSupportedStatusesAndResolutions(project, file)
      .collect(toList());
    indexIssues();

    SearchWsResponse response = newRequest(project)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactlyInAnyOrder(hotspots.stream().map(IssueDto::getKey).toArray(String[]::new));
  }

  @Test
  public void returns_hotpots_reviewed_as_safe_and_fixed_if_status_is_REVIEWED_and_resolution_is_not_set() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    List<IssueDto> reviewedHotspots = insertRandomNumberOfHotspotsOfAllSupportedStatusesAndResolutions(project, file)
      .filter(t -> STATUS_REVIEWED.equals(t.getStatus()))
      .collect(toList());
    indexIssues();

    SearchWsResponse response = newRequest(project, STATUS_REVIEWED, null, null, null)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactlyInAnyOrder(reviewedHotspots.stream().map(IssueDto::getKey).toArray(String[]::new));
  }

  @Test
  public void returns_hotpots_reviewed_as_safe_if_status_is_REVIEWED_and_resolution_is_SAFE() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    List<IssueDto> safeHotspots = insertRandomNumberOfHotspotsOfAllSupportedStatusesAndResolutions(project, file)
      .filter(t -> STATUS_REVIEWED.equals(t.getStatus()) && RESOLUTION_SAFE.equals(t.getResolution()))
      .collect(toList());
    indexIssues();

    SearchWsResponse response = newRequest(project, STATUS_REVIEWED, RESOLUTION_SAFE, null, null)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactlyInAnyOrder(safeHotspots.stream().map(IssueDto::getKey).toArray(String[]::new));
  }

  @Test
  public void returns_hotpots_reviewed_as_fixed_if_status_is_REVIEWED_and_resolution_is_FIXED() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    List<IssueDto> fixedHotspots = insertRandomNumberOfHotspotsOfAllSupportedStatusesAndResolutions(project, file)
      .filter(t -> STATUS_REVIEWED.equals(t.getStatus()) && RESOLUTION_FIXED.equals(t.getResolution()))
      .collect(toList());
    indexIssues();

    SearchWsResponse response = newRequest(project, STATUS_REVIEWED, RESOLUTION_FIXED, null, null)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactlyInAnyOrder(fixedHotspots.stream().map(IssueDto::getKey).toArray(String[]::new));
  }

  @Test
  public void returns_only_unresolved_hotspots_when_status_is_TO_REVIEW() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDto rule = newRule(SECURITY_HOTSPOT);
    IssueDto unresolvedHotspot = insertHotspot(rule, project, file, t -> t.setResolution(null));
    // unrealistic case since a resolution must be set, but shows a limit of current implementation (resolution is enough)
    IssueDto badlyResolved = insertHotspot(rule, project, file, t -> t.setStatus(STATUS_TO_REVIEW).setResolution(randomAlphabetic(5)));
    IssueDto badlyReviewed = insertHotspot(rule, project, file, t -> t.setStatus(STATUS_REVIEWED).setResolution(null));
    IssueDto badlyClosedHotspot = insertHotspot(rule, project, file, t -> t.setStatus(STATUS_CLOSED).setResolution(null));
    indexIssues();

    SearchWsResponse response = newRequest(project, STATUS_TO_REVIEW, null, null, null)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsOnly(unresolvedHotspot.getKey());
  }

  private Stream<IssueDto> insertRandomNumberOfHotspotsOfAllSupportedStatusesAndResolutions(ComponentDto project, ComponentDto file) {
    RuleDto rule = newRule(SECURITY_HOTSPOT);
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
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDto rule = newRule(SECURITY_HOTSPOT);
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
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDto rule = newRule(SECURITY_HOTSPOT, t -> t.setSecurityStandards(securityStandards));
    IssueDto hotspot = insertHotspot(project, file, rule);
    indexIssues();

    SearchWsResponse response = newRequest(project)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList()).hasSize(1);
    SearchWsResponse.Hotspot actual = response.getHotspots(0);
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
      new Object[] {of("foo", "donut", "acme"), SQCategory.OTHERS});
    return Stream.concat(allCategoriesButOTHERS, sqCategoryOTHERS).toArray(Object[][]::new);
  }

  @Test
  public void does_not_fail_when_hotspot_has_none_of_the_nullable_fields() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDto rule = newRule(SECURITY_HOTSPOT);
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
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto directory = dbTester.components().insertComponent(newDirectory(project, "donut/acme"));
    ComponentDto directory2 = dbTester.components().insertComponent(newDirectory(project, "foo/bar"));
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    ComponentDto file2 = dbTester.components().insertComponent(newFileDto(project));
    RuleDto rule = newRule(SECURITY_HOTSPOT);
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
      .extracting(Component::getKey)
      .containsOnly(project.getKey(), directory.getKey(), file.getKey());
    Map<String, Component> componentByKey = response.getComponentsList().stream().collect(Collectors.toMap(Component::getKey, Function.identity()));
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
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    String branchName = randomAlphanumeric(248);
    ComponentDto branch = dbTester.components().insertProjectBranch(project, b -> b.setKey(branchName));
    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto directory = dbTester.components().insertComponent(newDirectoryOnBranch(branch, "donut/acme", project.uuid()));
    ComponentDto file = dbTester.components().insertComponent(newFileDto(branch, project.uuid()));
    RuleDto rule = newRule(SECURITY_HOTSPOT);
    IssueDto fileHotspot = insertHotspot(branch, file, rule);
    IssueDto dirHotspot = insertHotspot(branch, directory, rule);
    IssueDto projectHotspot = insertHotspot(branch, branch, rule);
    indexIssues();

    SearchWsResponse response = newRequest(branch, r -> r.setParam(PARAM_BRANCH, branchName))
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsOnly(fileHotspot.getKey(), dirHotspot.getKey(), projectHotspot.getKey());
    assertThat(response.getComponentsList())
      .extracting(Component::getKey)
      .containsOnly(project.getKey(), directory.getKey(), file.getKey());
    Map<String, Component> componentByKey = response.getComponentsList().stream().collect(Collectors.toMap(Component::getKey, Function.identity()));
    Component actualProject = componentByKey.get(project.getKey());
    assertThat(actualProject.getBranch()).isEqualTo(branchName);
    assertThat(actualProject.hasPullRequest()).isFalse();
    Component actualDirectory = componentByKey.get(directory.getKey());
    assertThat(actualDirectory.getBranch()).isEqualTo(branchName);
    assertThat(actualDirectory.hasPullRequest()).isFalse();
    Component actualFile = componentByKey.get(file.getKey());
    assertThat(actualFile.getBranch()).isEqualTo(branchName);
    assertThat(actualFile.hasPullRequest()).isFalse();
  }

  @Test
  public void returns_pullRequest_field_of_components_of_pullRequest() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    String pullRequestKey = randomAlphanumeric(100);
    ComponentDto pullRequest = dbTester.components().insertProjectBranch(project, t -> t.setBranchType(BranchType.PULL_REQUEST)
      .setKey(pullRequestKey));
    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto directory = dbTester.components().insertComponent(newDirectoryOnBranch(pullRequest, "donut/acme", project.uuid()));
    ComponentDto file = dbTester.components().insertComponent(newFileDto(pullRequest, project.uuid()));
    RuleDto rule = newRule(SECURITY_HOTSPOT);
    IssueDto fileHotspot = insertHotspot(pullRequest, file, rule);
    IssueDto dirHotspot = insertHotspot(pullRequest, directory, rule);
    IssueDto projectHotspot = insertHotspot(pullRequest, pullRequest, rule);
    indexIssues();

    SearchWsResponse response = newRequest(pullRequest, r -> r.setParam(PARAM_PULL_REQUEST, pullRequestKey))
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsOnly(fileHotspot.getKey(), dirHotspot.getKey(), projectHotspot.getKey());
    assertThat(response.getComponentsList())
      .extracting(Component::getKey)
      .containsOnly(project.getKey(), directory.getKey(), file.getKey());
    Map<String, Component> componentByKey = response.getComponentsList().stream().collect(Collectors.toMap(Component::getKey, Function.identity()));
    Component actualProject = componentByKey.get(project.getKey());
    assertThat(actualProject.hasBranch()).isFalse();
    assertThat(actualProject.getPullRequest()).isEqualTo(pullRequestKey);
    Component actualDirectory = componentByKey.get(directory.getKey());
    assertThat(actualDirectory.hasBranch()).isFalse();
    assertThat(actualDirectory.getPullRequest()).isEqualTo(pullRequestKey);
    Component actualFile = componentByKey.get(file.getKey());
    assertThat(actualFile.hasBranch()).isFalse();
    assertThat(actualFile.getPullRequest()).isEqualTo(pullRequestKey);
  }

  @Test
  public void returns_hotspots_ordered_by_vulnerabilityProbability_score_then_rule_uuid() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    List<IssueDto> hotspots = Arrays.stream(SQCategory.values())
      .sorted(Ordering.from(Comparator.<SQCategory>comparingInt(t1 -> t1.getVulnerability().getScore()).reversed())
        .thenComparing(SQCategory::getKey))
      .flatMap(sqCategory -> {
        Set<String> cwes = SecurityStandards.CWES_BY_SQ_CATEGORY.get(sqCategory);
        Set<String> securityStandards = singleton("cwe:" + (cwes == null ? "unknown" : cwes.iterator().next()));
        RuleDto rule1 = newRule(
          SECURITY_HOTSPOT,
          t -> t.setUuid(sqCategory.name() + "_a").setName("rule_" + sqCategory.name() + "_a").setSecurityStandards(securityStandards));
        RuleDto rule2 = newRule(
          SECURITY_HOTSPOT,
          t -> t.setUuid(sqCategory.name() + "_b").setName("rule_" + sqCategory.name() + "_b").setSecurityStandards(securityStandards));
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
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file1 = dbTester.components().insertComponent(newFileDto(project).setPath("b/c/a"));
    ComponentDto file2 = dbTester.components().insertComponent(newFileDto(project).setPath("b/c/b"));
    ComponentDto file3 = dbTester.components().insertComponent(newFileDto(project).setPath("a/a/d"));
    RuleDto rule = newRule(SECURITY_HOTSPOT);
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
  public void returns_hotspot_with_secondary_locations() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    ComponentDto anotherFile = dbTester.components().insertComponent(newFileDto(project));

    List<DbIssues.Location> hotspotLocations = Stream.of(
      newHotspotLocation(file.uuid(), "security hotspot flow message 0", 1, 1, 0, 12),
      newHotspotLocation(file.uuid(), "security hotspot flow message 1", 3, 3, 0, 10),
      newHotspotLocation(anotherFile.uuid(), "security hotspot flow message 2", 5, 5, 0, 15),
      newHotspotLocation(anotherFile.uuid(), "security hotspot flow message 3", 7, 7, 0, 18),
      newHotspotLocation(null, "security hotspot flow message 4", 12, 12, 2, 8))
      .collect(toList());

    DbIssues.Locations.Builder locations = DbIssues.Locations.newBuilder().addFlow(DbIssues.Flow.newBuilder().addAllLocation(hotspotLocations));

    RuleDto rule = newRule(SECURITY_HOTSPOT);
    dbTester.issues().insertHotspot(rule, project, file, h -> h.setLocations(locations.build()));

    indexIssues();

    SearchWsResponse response = newRequest(project)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsCount()).isOne();
    assertThat(response.getHotspotsList().stream().findFirst().get().getFlowsCount()).isEqualTo(1);
    assertThat(response.getHotspotsList().stream().findFirst().get().getFlowsList().stream().findFirst().get().getLocationsCount()).isEqualTo(5);
    assertThat(response.getHotspotsList().stream().findFirst().get().getFlowsList().stream().findFirst().get().getLocationsList())
      .extracting(
        Common.Location::getComponent,
        Common.Location::getMsg,
        l -> l.getTextRange().getStartLine(),
        l -> l.getTextRange().getEndLine(),
        l -> l.getTextRange().getStartOffset(),
        l -> l.getTextRange().getEndOffset())
      .containsExactlyInAnyOrder(
        tuple(file.getKey(), "security hotspot flow message 0", 1, 1, 0, 12),
        tuple(file.getKey(), "security hotspot flow message 1", 3, 3, 0, 10),
        tuple(anotherFile.getKey(), "security hotspot flow message 2", 5, 5, 0, 15),
        tuple(anotherFile.getKey(), "security hotspot flow message 3", 7, 7, 0, 18),
        tuple(file.getKey(), "security hotspot flow message 4", 12, 12, 2, 8));
  }

  @Test
  public void returns_first_page_with_100_results_by_default() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDto rule = newRule(SECURITY_HOTSPOT);
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
    assertThat(response.getPaging().getPageIndex()).isOne();
    assertThat(response.getPaging().getPageSize()).isEqualTo(100);
  }

  @Test
  public void returns_specified_page_with_100_results_by_default() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDto rule = newRule(SECURITY_HOTSPOT);

    verifyPaging(project, file, rule, 336, 100);
  }

  @Test
  public void returns_specified_page_with_specified_number_of_results() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDto rule = newRule(SECURITY_HOTSPOT);
    int total = 336;
    int pageSize = 1 + new Random().nextInt(100);

    verifyPaging(project, file, rule, total, pageSize);
  }

  private void verifyPaging(ComponentDto project, ComponentDto file, RuleDto rule, int total, int pageSize) {
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
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDto rule = newRule(SECURITY_HOTSPOT);
    List<IssueDto> hotspots = IntStream.range(0, 1 + RANDOM.nextInt(15))
      .mapToObj(i -> dbTester.issues().insertHotspot(rule, project, file, t -> t.setLine(i)))
      .collect(toList());
    indexIssues();

    SearchWsResponse response = newRequest(IntStream.range(0, 1 + RANDOM.nextInt(30)).mapToObj(i -> "key_" + i).collect(toList()))
      .executeProtobuf(SearchWsResponse.class);

    verify(issueIndexSyncProgressChecker).checkIfIssueSyncInProgress(any());
    assertThat(response.getHotspotsList()).isEmpty();
  }

  @Test
  public void returns_specified_hotspots() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDto rule = newRule(SECURITY_HOTSPOT);
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
  public void returns_hotspots_with_specified_sonarsourceSecurity_category() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDto rule1 = newRule(SECURITY_HOTSPOT);
    RuleDto rule2 = newRule(SECURITY_HOTSPOT, r -> r.setSecurityStandards(of("cwe:117", "cwe:190")));
    RuleDto rule3 = newRule(SECURITY_HOTSPOT, r -> r.setSecurityStandards(of("owaspTop10:a1", "cwe:601")));
    insertHotspot(project, file, rule1);
    IssueDto hotspot2 = insertHotspot(project, file, rule2);
    insertHotspot(project, file, rule3);
    indexIssues();

    SearchWsResponse response = newRequest(project).setParam(PARAM_SONARSOURCE_SECURITY, "log-injection")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactly(hotspot2.getKey());
  }

  @Test
  public void returns_hotspots_with_specified_cwes() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDto rule1 = newRule(SECURITY_HOTSPOT);
    RuleDto rule2 = newRule(SECURITY_HOTSPOT, r -> r.setSecurityStandards(of("cwe:117", "cwe:190")));
    RuleDto rule3 = newRule(SECURITY_HOTSPOT, r -> r.setSecurityStandards(of("owaspTop10:a1", "cwe:601")));
    insertHotspot(project, file, rule1);
    IssueDto hotspot2 = insertHotspot(project, file, rule2);
    insertHotspot(project, file, rule3);
    indexIssues();

    SearchWsResponse response = newRequest(project).setParam(PARAM_CWE, "117,190")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactly(hotspot2.getKey());
  }

  @Test
  public void returns_hotspots_with_specified_owaspTop10_category() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDto rule1 = newRule(SECURITY_HOTSPOT);
    RuleDto rule2 = newRule(SECURITY_HOTSPOT, r -> r.setSecurityStandards(of("cwe:117", "cwe:190")));
    RuleDto rule3 = newRule(SECURITY_HOTSPOT, r -> r.setSecurityStandards(of("owaspTop10:a1", "cwe:601")));
    insertHotspot(project, file, rule1);
    insertHotspot(project, file, rule2);
    IssueDto hotspot3 = insertHotspot(project, file, rule3);
    indexIssues();

    SearchWsResponse response = newRequest(project).setParam(PARAM_OWASP_TOP_10_2017, "a1")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactly(hotspot3.getKey());
  }

  @Test
  public void returns_hotspots_with_specified_pciDss_category() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDto rule1 = newRule(SECURITY_HOTSPOT);
    RuleDto rule2 = newRule(SECURITY_HOTSPOT, r -> r.setSecurityStandards(of("cwe:117", "cwe:190")));
    RuleDto rule3 = newRule(SECURITY_HOTSPOT, r -> r.setSecurityStandards(of("pciDss-3.2:1.2.3")));
    RuleDto rule4 = newRule(SECURITY_HOTSPOT, r -> r.setSecurityStandards(of("pciDss-4.0:2.3a")));
    insertHotspot(project, file, rule1);
    insertHotspot(project, file, rule2);
    IssueDto hotspot3 = insertHotspot(project, file, rule3);
    IssueDto hotspot4 = insertHotspot(project, file, rule4);
    indexIssues();

    SearchWsResponse response32 = newRequest(project).setParam(PARAM_PCI_DSS_32, "1")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response32.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactly(hotspot3.getKey());

    SearchWsResponse response40 = newRequest(project).setParam(PARAM_PCI_DSS_40, "2")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response40.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactly(hotspot4.getKey());
  }

  @Test
  public void returns_hotspots_with_specified_owaspAsvs_category() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDto rule1 = newRule(SECURITY_HOTSPOT);
    RuleDto rule2 = newRule(SECURITY_HOTSPOT, r -> r.setSecurityStandards(of("cwe:117", "cwe:190")));
    RuleDto rule3 = newRule(SECURITY_HOTSPOT, r -> r.setSecurityStandards(of("owaspAsvs-4.0:1.2.3")));
    RuleDto rule4 = newRule(SECURITY_HOTSPOT, r -> r.setSecurityStandards(of("owaspAsvs-4.0:1.2.4")));
    insertHotspot(project, file, rule1);
    insertHotspot(project, file, rule2);
    IssueDto hotspot3 = insertHotspot(project, file, rule3);
    IssueDto hotspot4 = insertHotspot(project, file, rule4);
    indexIssues();

    SearchWsResponse responseFor1 = newRequest(project).setParam(PARAM_OWASP_ASVS_40, "1")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(responseFor1.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsOnly(hotspot3.getKey(), hotspot4.getKey());

    SearchWsResponse responseFor124 = newRequest(project).setParam(PARAM_OWASP_ASVS_40, "1.2.4")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(responseFor124.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactly(hotspot4.getKey());
  }

  @Test
  public void returns_hotspots_with_specified_owaspAsvs_level() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDto rule1 = newRule(SECURITY_HOTSPOT);
    RuleDto rule2 = newRule(SECURITY_HOTSPOT, r -> r.setSecurityStandards(of("cwe:117", "cwe:190")));
    RuleDto rule3 = newRule(SECURITY_HOTSPOT, r -> r.setSecurityStandards(of("owaspAsvs-4.0:2.1.1")));
    RuleDto rule4 = newRule(SECURITY_HOTSPOT, r -> r.setSecurityStandards(of("owaspAsvs-4.0:1.1.1")));
    RuleDto rule5 = newRule(SECURITY_HOTSPOT, r -> r.setSecurityStandards(of("owaspAsvs-4.0:3.6.1")));
    insertHotspot(project, file, rule1);
    insertHotspot(project, file, rule2);
    IssueDto hotspot3 = insertHotspot(project, file, rule3);
    IssueDto hotspot4 = insertHotspot(project, file, rule4);
    IssueDto hotspot5 = insertHotspot(project, file, rule5);
    indexIssues();

    SearchWsResponse responseFor1 = newRequest(project)
      .setParam(PARAM_OWASP_ASVS_40, "1,2,3")
      .setParam(PARAM_OWASP_ASVS_LEVEL, "1")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(responseFor1.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactlyInAnyOrder(hotspot3.getKey());

    SearchWsResponse responseFor2 = newRequest(project)
      .setParam(PARAM_OWASP_ASVS_40, "1,2,3")
      .setParam(PARAM_OWASP_ASVS_LEVEL, "2")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(responseFor2.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactlyInAnyOrder(hotspot3.getKey(), hotspot4.getKey());

    SearchWsResponse responseFor3 = newRequest(project)
      .setParam(PARAM_OWASP_ASVS_40, "1.1.1,2,3")
      .setParam(PARAM_OWASP_ASVS_LEVEL, "3")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(responseFor3.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactlyInAnyOrder(hotspot3.getKey(), hotspot4.getKey(), hotspot5.getKey());

    SearchWsResponse responseFor1111 = newRequest(project)
      .setParam(PARAM_OWASP_ASVS_40, "1.1.1")
      .setParam(PARAM_OWASP_ASVS_LEVEL, "1")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(responseFor1111.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .isEmpty();
  }

  @Test
  public void returns_hotspots_with_specified_owasp2021Top10_category() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDto rule1 = newRule(SECURITY_HOTSPOT);
    RuleDto rule2 = newRule(SECURITY_HOTSPOT, r -> r.setSecurityStandards(of("cwe:117", "cwe:190")));
    RuleDto rule3 = newRule(SECURITY_HOTSPOT, r -> r.setSecurityStandards(of("owaspTop10-2021:a5", "cwe:489")));
    insertHotspot(project, file, rule1);
    insertHotspot(project, file, rule2);
    IssueDto hotspot3 = insertHotspot(project, file, rule3);
    indexIssues();

    SearchWsResponse response = newRequest(project).setParam(PARAM_OWASP_TOP_10_2021, "a5")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactly(hotspot3.getKey());
  }

  @Test
  public void returns_hotspots_with_specified_sansTop25_category() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDto rule1 = newRule(SECURITY_HOTSPOT);
    RuleDto rule2 = newRule(SECURITY_HOTSPOT, r -> r.setSecurityStandards(of("cwe:117", "cwe:190")));
    RuleDto rule3 = newRule(SECURITY_HOTSPOT, r -> r.setSecurityStandards(of("owaspTop10:a1", "cwe:601")));
    insertHotspot(project, file, rule1);
    insertHotspot(project, file, rule2);
    IssueDto hotspot3 = insertHotspot(project, file, rule3);
    indexIssues();

    SearchWsResponse response = newRequest(project).setParam(PARAM_SANS_TOP_25, "insecure-interaction")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactly(hotspot3.getKey());
  }

  @Test
  public void returns_hotspots_with_specified_files() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file1 = dbTester.components().insertComponent(newFileDto(project));
    ComponentDto file2 = dbTester.components().insertComponent(newFileDto(project));
    RuleDto rule = newRule(SECURITY_HOTSPOT);

    final IssueDto hotspot = insertHotspot(project, file1, rule);
    insertHotspot(project, file2, rule);

    indexIssues();

    SearchWsResponse response = newRequest(project).setParam(PARAM_FILES, file1.path())
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactly(hotspot.getKey());
  }

  @Test
  public void returns_hotspots_on_the_leak_period_when_inNewCodePeriod_is_true() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    userSessionRule.addProjectBranchMapping(projectData.projectUuid(), project);
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    long periodDate = 800_996_999_332L;
    dbTester.components().insertSnapshot(project, t -> t.setPeriodDate(periodDate).setLast(false));
    dbTester.components().insertSnapshot(project, t -> t.setPeriodDate(periodDate - 1_500).setLast(true));
    RuleDto rule = newRule(SECURITY_HOTSPOT);
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
      t -> t.setParam(PARAM_IN_NEW_CODE_PERIOD, "true"))
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
  public void returns_hotspots_on_the_leak_period_when_inNewCodePeriod_is_true_and_branch_uses_reference_branch() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    dbTester.components().insertSnapshot(project, t -> t.setPeriodMode(REFERENCE_BRANCH.name()).setPeriodParam("master"));
    RuleDto rule = newRule(SECURITY_HOTSPOT);
    List<IssueDto> hotspotsInLeakPeriod = IntStream.range(0, 1 + RANDOM.nextInt(20))
      .mapToObj(i -> dbTester.issues().insertHotspot(rule, project, file, t -> t.setLine(i)))
      .collect(toList());

    hotspotsInLeakPeriod.stream().forEach(i -> dbTester.issues().insertNewCodeReferenceIssue(newCodeReferenceIssue(i)));

    List<IssueDto> hotspotsNotInLeakPeriod = IntStream.range(0, 1 + RANDOM.nextInt(20))
      .mapToObj(i -> dbTester.issues().insertHotspot(rule, project, file, t -> t.setLine(i)))
      .collect(toList());
    indexIssues();

    SearchWsResponse responseAll = newRequest(project)
      .executeProtobuf(SearchWsResponse.class);
    assertThat(responseAll.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactlyInAnyOrder(Stream.of(
        hotspotsInLeakPeriod.stream(),
        hotspotsNotInLeakPeriod.stream())
        .flatMap(t -> t)
        .map(IssueDto::getKey)
        .toArray(String[]::new));

    SearchWsResponse responseOnLeak = newRequest(project,
      t -> t.setParam(PARAM_IN_NEW_CODE_PERIOD, "true"))
        .executeProtobuf(SearchWsResponse.class);
    assertThat(responseOnLeak.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactlyInAnyOrder(hotspotsInLeakPeriod
        .stream()
        .map(IssueDto::getKey)
        .toArray(String[]::new));
  }

  @Test
  public void returns_nothing_when_inNewCodePeriod_is_true_and_no_period_exists() {
    long referenceDate = 800_996_999_332L;

    system2.setNow(referenceDate + 10_000);
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    dbTester.components().insertSnapshot(project, t -> t.setPeriodDate(referenceDate).setLast(false));
    dbTester.components().insertSnapshot(project, t -> t.setPeriodDate(null).setLast(true));
    RuleDto rule = newRule(SECURITY_HOTSPOT);
    IssueDto afterRef = dbTester.issues().insertHotspot(rule, project, file, t -> t.setIssueCreationTime(referenceDate + 1000));
    IssueDto atRef = dbTester.issues().insertHotspot(rule, project, file, t -> t.setType(SECURITY_HOTSPOT).setIssueCreationTime(referenceDate));
    IssueDto beforeRef = dbTester.issues().insertHotspot(rule, project, file, t -> t.setIssueCreationTime(referenceDate - 1000));
    indexIssues();

    SearchWsResponse responseAll = newRequest(project)
      .executeProtobuf(SearchWsResponse.class);
    assertThat(responseAll.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactlyInAnyOrder(Stream.of(afterRef, atRef, beforeRef)
        .map(IssueDto::getKey)
        .toArray(String[]::new));

    SearchWsResponse responseOnLeak = newRequest(project,
      t -> t.setParam(PARAM_IN_NEW_CODE_PERIOD, "true"))
        .executeProtobuf(SearchWsResponse.class);
    assertThat(responseOnLeak.getHotspotsList()).isEmpty();
  }

  @Test
  public void returns_all_issues_when_inNewCodePeriod_is_true_and_is_pr() {
    long referenceDate = 800_996_999_332L;

    system2.setNow(referenceDate + 10_000);
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto pr = dbTester.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.PULL_REQUEST).setKey("pr"));
    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(pr, project.uuid()));
    dbTester.components().insertSnapshot(project, t -> t.setPeriodDate(referenceDate).setLast(true));
    dbTester.components().insertSnapshot(pr, t -> t.setPeriodDate(null).setLast(true));
    RuleDto rule = newRule(SECURITY_HOTSPOT);
    IssueDto afterRef = dbTester.issues().insertHotspot(rule, pr, file, t -> t.setIssueCreationTime(referenceDate + 1000));
    IssueDto atRef = dbTester.issues().insertHotspot(rule, pr, file, t -> t.setType(SECURITY_HOTSPOT).setIssueCreationTime(referenceDate));
    IssueDto beforeRef = dbTester.issues().insertHotspot(rule, pr, file, t -> t.setIssueCreationTime(referenceDate - 1000));
    indexIssues();

    SearchWsResponse responseAll = newRequest(project).setParam(PARAM_PULL_REQUEST, "pr")
      .executeProtobuf(SearchWsResponse.class);
    assertThat(responseAll.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactlyInAnyOrder(Stream.of(afterRef, atRef, beforeRef)
        .map(IssueDto::getKey)
        .toArray(String[]::new));

    SearchWsResponse responseOnLeak = newRequest(project,
      t -> t.setParam(PARAM_IN_NEW_CODE_PERIOD, "true").setParam(PARAM_PULL_REQUEST, "pr"))
        .executeProtobuf(SearchWsResponse.class);
    assertThat(responseOnLeak.getHotspotsList()).hasSize(3);
  }

  @Test
  public void returns_issues_when_inNewCodePeriod_is_true_and_is_application_for_main_branch() {
    long referenceDate = 800_996_999_332L;

    system2.setNow(referenceDate + 10_000);
    ProjectData application = dbTester.components().insertPublicApplication();
    ProjectData project = dbTester.components().insertPublicProject();
    ProjectData project2 = dbTester.components().insertPublicProject();

    dbTester.components().addApplicationProject(application, project);
    dbTester.components().addApplicationProject(application, project2);

    dbTester.components().insertComponent(ComponentTesting.newProjectCopy(project, application));
    dbTester.components().insertComponent(ComponentTesting.newProjectCopy(project2, application));

    indexViews();

    userSessionRule.registerApplication(application.getProjectDto(), project.getProjectDto(), project2.getProjectDto());
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project.getMainBranchComponent()));
    dbTester.components().insertSnapshot(project, t -> t.setPeriodDate(referenceDate).setLast(true));
    RuleDto rule = newRule(SECURITY_HOTSPOT);
    IssueDto afterRef = dbTester.issues().insertHotspot(rule, project, file, t -> t.setIssueCreationTime(referenceDate + 1000));
    IssueDto atRef = dbTester.issues().insertHotspot(rule, project, file, t -> t.setType(SECURITY_HOTSPOT).setIssueCreationTime(referenceDate));
    IssueDto beforeRef = dbTester.issues().insertHotspot(rule, project, file, t -> t.setIssueCreationTime(referenceDate - 1000));

    ComponentDto file2 = dbTester.components().insertComponent(newFileDto(project2.getMainBranchComponent()));
    IssueDto project2Issue = dbTester.issues().insertHotspot(rule, project2, file2, t -> t.setIssueCreationTime(referenceDate - 1000));

    indexIssues();

    SearchWsResponse responseAll = newRequest(application.getMainBranchComponent())
      .executeProtobuf(SearchWsResponse.class);
    assertThat(responseAll.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactlyInAnyOrder(afterRef.getKey(), atRef.getKey(), beforeRef.getKey(), project2Issue.getKey());

    SearchWsResponse responseOnLeak = newRequest(application.getMainBranchComponent(),
      t -> t.setParam(PARAM_IN_NEW_CODE_PERIOD, "true"))
        .executeProtobuf(SearchWsResponse.class);
    assertThat(responseOnLeak.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactlyInAnyOrder(afterRef.getKey());
  }

  @Test
  public void returns_issues_when_inNewCodePeriod_is_true_and_is_application_for_branch_other_than_main() {
    long referenceDate = 800_996_999_332L;

    system2.setNow(referenceDate + 10_000);
    ProjectData applicationData = dbTester.components().insertPublicApplication();
    ProjectDto application = applicationData.getProjectDto();
    BranchDto applicationBranch = dbTester.components().insertProjectBranch(application, branchDto -> branchDto.setKey("application_branch_1"));
    ProjectDto project = dbTester.components().insertPublicProject().getProjectDto();
    BranchDto projectBranch = dbTester.components().insertProjectBranch(project, branchDto -> branchDto.setKey("project_1_branch_1"));

    ProjectDto project2 = dbTester.components().insertPublicProject().getProjectDto();
    BranchDto project2Branch = dbTester.components().insertProjectBranch(project2, branchDto -> branchDto.setKey("project_2_branch_1"));

    dbTester.components().addApplicationProject(application, project);
    dbTester.components().addApplicationProject(application, project2);

    dbTester.components().addProjectBranchToApplicationBranch(applicationBranch, projectBranch, project2Branch);

    ComponentDto applicationBranchComponentDto = dbClient.componentDao().selectByUuid(dbTester.getSession(), applicationBranch.getUuid()).get();
    ComponentDto projectBranchComponentDto = dbClient.componentDao().selectByUuid(dbTester.getSession(), projectBranch.getUuid()).get();
    ComponentDto project2BranchComponentDto = dbClient.componentDao().selectByUuid(dbTester.getSession(), project2Branch.getUuid()).get();

    dbTester.components().insertComponent(ComponentTesting.newProjectCopy(projectBranchComponentDto, applicationBranchComponentDto));
    dbTester.components().insertComponent(ComponentTesting.newProjectCopy(project2BranchComponentDto, applicationBranchComponentDto));

    indexViews();

    userSessionRule.registerApplication(application, project, project2);
    indexPermissions();

    ComponentDto file = dbTester.components().insertComponent(newFileDto(projectBranchComponentDto));
    dbTester.components().insertSnapshot(projectBranch, t -> t.setPeriodDate(referenceDate).setLast(true));

    RuleDto rule = newRule(SECURITY_HOTSPOT);
    IssueDto afterRef = dbTester.issues().insertHotspot(rule, projectBranchComponentDto, file, t -> t.setIssueCreationTime(referenceDate + 1000));
    IssueDto atRef = dbTester.issues().insertHotspot(rule, projectBranchComponentDto, file, t -> t.setType(SECURITY_HOTSPOT).setIssueCreationTime(referenceDate));
    IssueDto beforeRef = dbTester.issues().insertHotspot(rule, projectBranchComponentDto, file, t -> t.setIssueCreationTime(referenceDate - 1000));

    ComponentDto file2 = dbTester.components().insertComponent(newFileDto(project2BranchComponentDto));
    IssueDto project2Issue = dbTester.issues().insertHotspot(rule, project2BranchComponentDto, file2, t -> t.setIssueCreationTime(referenceDate - 1000));

    indexIssues();

    ComponentDto applicationComponentDto = applicationData.getMainBranchComponent();
    SearchWsResponse responseAll = newRequest(applicationComponentDto,
      t -> t.setParam(PARAM_BRANCH, applicationBranch.getKey()))
        .executeProtobuf(SearchWsResponse.class);
    assertThat(responseAll.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactlyInAnyOrder(afterRef.getKey(), atRef.getKey(), beforeRef.getKey(), project2Issue.getKey());

    SearchWsResponse responseOnLeak = newRequest(applicationComponentDto,
      t -> t.setParam(PARAM_IN_NEW_CODE_PERIOD, "true").setParam(PARAM_BRANCH, applicationBranch.getKey()))
        .executeProtobuf(SearchWsResponse.class);
    assertThat(responseOnLeak.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getKey)
      .containsExactlyInAnyOrder(afterRef.getKey());
  }

  @Test
  public void verify_response_example() {
    ProjectData projectData = dbTester.components().insertPublicProject(componentDto -> componentDto
      .setName("test-project")
      .setLongName("test-project")
      .setKey("com.sonarsource:test-project"));
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto fileWithHotspot = dbTester.components().insertComponent(newFileDto(project)
      .setKey("com.sonarsource:test-project:src/main/java/com/sonarsource/FourthClass.java")
      .setName("FourthClass.java")
      .setLongName("src/main/java/com/sonarsource/FourthClass.java")
      .setPath("src/main/java/com/sonarsource/FourthClass.java"));

    long time = 1577976190000L;

    IssueDto[] hotspots = IntStream.range(0, 3)
      .mapToObj(i -> {
        RuleKey ruleKey = RuleKey.of("repository-" + i, "rule-" + i);
        RuleDto rule = newRule(SECURITY_HOTSPOT, ruleKey)
          .setSecurityStandards(Sets.newHashSet(SQCategory.WEAK_CRYPTOGRAPHY.getKey()));
        return insertHotspot(rule, project, fileWithHotspot, issueDto -> issueDto.setKee("hotspot-" + i)
          .setAssigneeUuid("assignee-uuid")
          .setAuthorLogin("joe")
          .setMessage("message-" + i)
          .setLine(10 + i)
          .setIssueCreationTime(time)
          .setIssueUpdateTime(time));
      })
      .toArray(IssueDto[]::new);
    indexIssues();

    assertThat(actionTester.getDef().responseExampleAsString()).isNotNull();
    newRequest(project)
      .execute()
      .assertJson(actionTester.getDef().responseExampleAsString());
  }

  @Test
  public void returns_hotspots_with_ruleKey() {
    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSessionRule.registerProjects(projectData.getProjectDto());
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDto rule1 = newRule(SECURITY_HOTSPOT);
    insertHotspot(project, file, rule1);
    indexIssues();

    SearchWsResponse response = newRequest(project)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(SearchWsResponse.Hotspot::getRuleKey)
      .containsExactly(rule1.getKey().toString());
  }

  private IssueDto insertHotspot(ComponentDto project, ComponentDto file, RuleDto rule) {
    return insertHotspot(rule, project, file, t -> {
    });
  }

  private IssueDto insertHotspot(RuleDto rule, ComponentDto project, ComponentDto file, @Nullable String assigneeUuid) {
    return insertHotspot(rule, project, file, t -> t.setAssigneeUuid(assigneeUuid));
  }

  private IssueDto insertHotspot(RuleDto rule, ComponentDto project, ComponentDto file, Consumer<IssueDto> consumer) {
    return dbTester.issues().insertHotspot(rule, project, file, consumer);
  }

  private static IssueDto newHotspot(RuleDto rule, ComponentDto project, ComponentDto component) {
    return newHotspot(rule, project, component, t -> {
    });
  }

  private static IssueDto newHotspot(RuleDto rule, ComponentDto project, ComponentDto component, Consumer<IssueDto> consumer) {
    IssueDto res = newIssue(rule, project, component)
      .setStatus(STATUS_TO_REVIEW);
    consumer.accept(res);
    return res.setType(SECURITY_HOTSPOT);
  }

  private static DbIssues.Location newHotspotLocation(@Nullable String componentUuid, String message, int startLine, int endLine, int startOffset, int endOffset) {
    DbIssues.Location.Builder builder = DbIssues.Location.newBuilder();

    if (componentUuid != null) {
      builder.setComponentId(componentUuid);
    }

    builder
      .setMsg(message)
      .setTextRange(DbCommons.TextRange.newBuilder()
        .setStartLine(startLine)
        .setEndLine(endLine)
        .setStartOffset(startOffset)
        .setEndOffset(endOffset)
        .build());

    return builder.build();
  }

  private TestRequest newRequest(ComponentDto project) {
    return newRequest(project, null, null, null, null);
  }

  private TestRequest newRequest(ComponentDto project, Consumer<TestRequest> consumer) {
    return newRequest(project, null,
      null, null, null, consumer);
  }

  private TestRequest newRequest(ComponentDto project, @Nullable String status, @Nullable String resolution, @Nullable String branch, @Nullable String pullRequest) {
    return newRequest(project, status, resolution, branch, pullRequest, t -> {
    });
  }

  private TestRequest newRequest(ComponentDto project, @Nullable String status, @Nullable String resolution,
    @Nullable String branch, @Nullable String pullRequest, Consumer<TestRequest> consumer) {
    TestRequest res = actionTester.newRequest()
      .setParam(PARAM_PROJECT, project.getKey());
    if (branch != null) {
      res.setParam(PARAM_BRANCH, branch);
    }
    if (pullRequest != null) {
      res.setParam(PARAM_PULL_REQUEST, pullRequest);
    }
    if (status != null) {
      res.setParam(PARAM_STATUS, status);
    }
    if (resolution != null) {
      res.setParam(PARAM_RESOLUTION, resolution);
    }
    consumer.accept(res);
    return res;
  }

  private TestRequest newRequest(Collection<String> hotspotKeys) {
    return actionTester.newRequest()
      .setParam(PARAM_HOTSPOTS, String.join(",", hotspotKeys));
  }

  private void indexPermissions() {
    permissionIndexer.indexAll(permissionIndexer.getIndexTypes());
  }

  private void indexIssues() {
    issueIndexer.indexAllIssues();
  }

  private void indexViews() {
    viewIndexer.indexAll();
  }

  private RuleDto newRule(RuleType ruleType) {
    return newRule(ruleType, t -> {
    });
  }

  private RuleDto newRule(RuleType ruleType, RuleKey ruleKey) {
    RuleDto ruleDto = RuleTesting.newRule(ruleKey)
      .setType(ruleType);
    dbTester.rules().insert(ruleDto);
    return ruleDto;
  }

  private RuleDto newRule(RuleType ruleType, Consumer<RuleDto> populate) {
    RuleDto ruleDto = RuleTesting.newRule()
      .setType(ruleType);
    populate.accept(ruleDto);
    dbTester.rules().insert(ruleDto);
    return ruleDto;
  }
}
