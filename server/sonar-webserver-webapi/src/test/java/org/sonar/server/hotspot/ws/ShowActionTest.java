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
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.sonar.api.issue.Issue;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueTesting;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.IssueChangelog;
import org.sonar.server.issue.IssueChangelog.ChangelogLoadingContext;
import org.sonar.server.issue.TextRangeResponseFormatter;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.security.SecurityStandards;
import org.sonar.server.security.SecurityStandards.SQCategory;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Hotspots;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.api.rules.RuleType.SECURITY_HOTSPOT;
import static org.sonar.db.component.ComponentTesting.newFileDto;

@RunWith(DataProviderRunner.class)
public class ShowActionTest {
  private static final Random RANDOM = new Random();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private DbClient dbClient = dbTester.getDbClient();
  private TestDefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(dbTester);

  private TextRangeResponseFormatter textRangeFormatter = new TextRangeResponseFormatter();
  private HotspotWsResponseFormatter responseFormatter = new HotspotWsResponseFormatter(defaultOrganizationProvider);
  private IssueChangelog issueChangelog = Mockito.mock(IssueChangelog.class);

  private ShowAction underTest = new ShowAction(dbClient, userSessionRule, responseFormatter, textRangeFormatter, issueChangelog);
  private WsActionTester actionTester = new WsActionTester(underTest);

  @Test
  public void ws_is_internal() {
    assertThat(actionTester.getDef().isInternal()).isTrue();
  }

  @Test
  public void fails_with_IAE_if_parameter_hotspot_is_missing() {
    TestRequest request = actionTester.newRequest();

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'hotspot' parameter is missing");
  }

  @Test
  public void fails_with_NotFoundException_if_hotspot_does_not_exist() {
    String key = randomAlphabetic(12);
    TestRequest request = actionTester.newRequest()
      .setParam("hotspot", key);

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Hotspot '%s' does not exist", key);
  }

  @Test
  @UseDataProvider("ruleTypesButHotspot")
  public void fails_with_NotFoundException_if_issue_is_not_a_hotspot(RuleType ruleType) {
    ComponentDto project = dbTester.components().insertPublicProject();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule(ruleType);
    IssueDto notAHotspot = dbTester.issues().insertIssue(IssueTesting.newIssue(rule, project, file).setType(ruleType));
    TestRequest request = newRequest(notAHotspot);

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Hotspot '%s' does not exist", notAHotspot.getKey());
  }

  @DataProvider
  public static Object[][] ruleTypesButHotspot() {
    return Arrays.stream(RuleType.values())
      .filter(t -> t != SECURITY_HOTSPOT)
      .map(t -> new Object[] {t})
      .toArray(Object[][]::new);
  }

  @Test
  public void fails_with_ForbiddenException_if_project_is_private_and_not_allowed() {
    ComponentDto project = dbTester.components().insertPrivateProject();
    userSessionRule.registerComponents(project);
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
    IssueDto hotspot = dbTester.issues().insertIssue(newHotspot(project, file, rule));
    TestRequest request = newRequest(hotspot);

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void succeeds_on_public_project() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
    IssueDto hotspot = dbTester.issues().insertIssue(newHotspot(project, file, rule));

    Hotspots.ShowWsResponse response = newRequest(hotspot)
      .executeProtobuf(Hotspots.ShowWsResponse.class);

    assertThat(response.getKey()).isEqualTo(hotspot.getKey());
  }

  @Test
  public void succeeds_on_private_project_with_permission() {
    ComponentDto project = dbTester.components().insertPrivateProject();
    userSessionRule.registerComponents(project);
    userSessionRule.logIn().addProjectPermission(UserRole.USER, project);
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
    IssueDto hotspot = dbTester.issues().insertIssue(newHotspot(project, file, rule));

    Hotspots.ShowWsResponse response = newRequest(hotspot)
      .executeProtobuf(Hotspots.ShowWsResponse.class);

    assertThat(response.getKey()).isEqualTo(hotspot.getKey());
  }

  @Test
  @UseDataProvider("statusAndResolutionCombinations")
  public void returns_status_and_resolution(String status, @Nullable String resolution) {
    ComponentDto project = dbTester.components().insertPrivateProject();
    userSessionRule.registerComponents(project);
    userSessionRule.logIn().addProjectPermission(UserRole.USER, project);
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
    IssueDto hotspot = dbTester.issues().insertIssue(newHotspot(project, file, rule).setStatus(status).setResolution(resolution));

    Hotspots.ShowWsResponse response = newRequest(hotspot)
      .executeProtobuf(Hotspots.ShowWsResponse.class);

    assertThat(response.getStatus()).isEqualTo(status);
    if (resolution == null) {
      assertThat(response.hasResolution()).isFalse();
    } else {
      assertThat(response.getResolution()).isEqualTo(resolution);
    }
  }

  @DataProvider
  public static Object[][] statusAndResolutionCombinations() {
    return new Object[][] {
      {Issue.STATUS_TO_REVIEW, null},
      {Issue.STATUS_REVIEWED, Issue.RESOLUTION_FIXED},
      {Issue.STATUS_REVIEWED, Issue.RESOLUTION_SAFE},
      {Issue.STATUS_CLOSED, Issue.RESOLUTION_REMOVED}
    };
  }

  @Test
  public void returns_hotspot_component_and_rule() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
    IssueDto hotspot = dbTester.issues().insertIssue(newHotspot(project, file, rule));

    Hotspots.ShowWsResponse response = newRequest(hotspot)
      .executeProtobuf(Hotspots.ShowWsResponse.class);

    assertThat(response.getKey()).isEqualTo(hotspot.getKey());
    verifyComponent(response.getComponent(), file);
    verifyComponent(response.getProject(), project);
    verifyRule(response.getRule(), rule);
    assertThat(response.hasTextRange()).isFalse();
  }

  @Test
  public void returns_no_textRange_when_locations_have_none() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
    IssueDto hotspot = dbTester.issues().insertIssue(newHotspot(project, file, rule)
      .setLocations(DbIssues.Locations.newBuilder().build()));

    Hotspots.ShowWsResponse response = newRequest(hotspot)
      .executeProtobuf(Hotspots.ShowWsResponse.class);

    assertThat(response.hasTextRange()).isFalse();
  }

  @Test
  @UseDataProvider("randomTextRangeValues")
  public void returns_textRange(int startLine, int endLine, int startOffset, int endOffset) {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
    IssueDto hotspot = dbTester.issues().insertIssue(newHotspot(project, file, rule)
      .setLocations(DbIssues.Locations.newBuilder()
        .setTextRange(DbCommons.TextRange.newBuilder()
          .setStartLine(startLine)
          .setEndLine(endLine)
          .setStartOffset(startOffset)
          .setEndOffset(endOffset)
          .build())
        .build()));

    Hotspots.ShowWsResponse response = newRequest(hotspot)
      .executeProtobuf(Hotspots.ShowWsResponse.class);
    assertThat(response.hasTextRange()).isTrue();
    Common.TextRange textRange = response.getTextRange();
    assertThat(textRange.getStartLine()).isEqualTo(startLine);
    assertThat(textRange.getEndLine()).isEqualTo(endLine);
    assertThat(textRange.getStartOffset()).isEqualTo(startOffset);
    assertThat(textRange.getEndOffset()).isEqualTo(endOffset);
  }

  @DataProvider
  public static Object[][] randomTextRangeValues() {
    int startLine = RANDOM.nextInt(200);
    int endLine = RANDOM.nextInt(200);
    int startOffset = RANDOM.nextInt(200);
    int endOffset = RANDOM.nextInt(200);
    return new Object[][] {
      {startLine, endLine, startOffset, endOffset}
    };
  }

  @Test
  public void returns_textRange_missing_fields() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
    IssueDto hotspot = dbTester.issues().insertIssue(newHotspot(project, file, rule)
      .setLocations(DbIssues.Locations.newBuilder()
        .setTextRange(DbCommons.TextRange.newBuilder().build())
        .build()));

    Hotspots.ShowWsResponse response = newRequest(hotspot)
      .executeProtobuf(Hotspots.ShowWsResponse.class);

    assertThat(response.hasTextRange()).isTrue();
    Common.TextRange textRange = response.getTextRange();
    assertThat(textRange.hasStartLine()).isFalse();
    assertThat(textRange.hasEndLine()).isFalse();
    assertThat(textRange.hasStartOffset()).isFalse();
    assertThat(textRange.hasEndOffset()).isFalse();
  }

  @Test
  @UseDataProvider("allSQCategoryAndVulnerabilityProbability")
  public void returns_securityCategory_and_vulnerabilityProbability_of_rule(Set<String> standards,
    SQCategory expected) {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT, t -> t.setSecurityStandards(standards));
    IssueDto hotspot = dbTester.issues().insertIssue(newHotspot(project, file, rule)
      .setLocations(DbIssues.Locations.newBuilder()
        .setTextRange(DbCommons.TextRange.newBuilder().build())
        .build()));

    Hotspots.ShowWsResponse response = newRequest(hotspot)
      .executeProtobuf(Hotspots.ShowWsResponse.class);
    Hotspots.Rule wsRule = response.getRule();
    assertThat(wsRule.getSecurityCategory()).isEqualTo(expected.getKey());
    assertThat(wsRule.getVulnerabilityProbability()).isEqualTo(expected.getVulnerability().name());
  }

  @DataProvider
  public static Object[][] allSQCategoryAndVulnerabilityProbability() {
    Stream<Object[]> allButOthers = SecurityStandards.CWES_BY_SQ_CATEGORY
      .entrySet()
      .stream()
      .map(t -> new Object[] {
        t.getValue().stream().map(s -> "cwe:" + s).collect(Collectors.toSet()),
        t.getKey()
      });
    Stream<Object[]> others = Stream.of(
      new Object[] {Collections.emptySet(), SQCategory.OTHERS},
      new Object[] {ImmutableSet.of("foo", "bar", "acme"), SQCategory.OTHERS});
    return Stream.concat(allButOthers, others)
      .toArray(Object[][]::new);
  }

  @Test
  public void returns_project_twice_when_hotspot_on_project() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
    IssueDto hotspot = dbTester.issues().insertIssue(newHotspot(project, project, rule)
      .setLocations(DbIssues.Locations.newBuilder()
        .setTextRange(DbCommons.TextRange.newBuilder().build())
        .build()));

    Hotspots.ShowWsResponse response = newRequest(hotspot)
      .executeProtobuf(Hotspots.ShowWsResponse.class);

    verifyComponent(response.getProject(), project);
    verifyComponent(response.getComponent(), project);
  }

  @Test
  public void returns_hotspot_changelog() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    RuleDefinitionDto rule = newRule(SECURITY_HOTSPOT);
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    IssueDto hotspot = dbTester.issues().insertIssue(newHotspot(project, file, rule)
      .setLocations(DbIssues.Locations.newBuilder()
        .setTextRange(DbCommons.TextRange.newBuilder().build())
        .build()));
    ChangelogLoadingContext changelogLoadingContext = Mockito.mock(ChangelogLoadingContext.class);
    List<Common.Changelog> changelog = IntStream.range(0, 1 + new Random().nextInt(12))
      .mapToObj(i -> Common.Changelog.newBuilder().setUser("u" + i).build())
      .collect(Collectors.toList());
    when(issueChangelog.newChangelogLoadingContext(any(), any(), anySet(), anySet())).thenReturn(changelogLoadingContext);
    when(issueChangelog.formatChangelog(any(), eq(changelogLoadingContext)))
      .thenReturn(changelog.stream());

    Hotspots.ShowWsResponse response = newRequest(hotspot)
      .executeProtobuf(Hotspots.ShowWsResponse.class);

    assertThat(response.getChangelogList())
      .extracting(Common.Changelog::getUser)
      .containsExactly(changelog.stream().map(Common.Changelog::getUser).toArray(String[]::new));
    verify(issueChangelog).newChangelogLoadingContext(any(DbSession.class),
      argThat(new IssueDtoArgumentMatcher(hotspot)),
      eq(Collections.emptySet()), eq(ImmutableSet.of(project, file)));
    verify(issueChangelog).formatChangelog(any(DbSession.class), eq(changelogLoadingContext));
  }

  public void verifyRule(Hotspots.Rule wsRule, RuleDefinitionDto dto) {
    assertThat(wsRule.getKey()).isEqualTo(dto.getKey().toString());
    assertThat(wsRule.getName()).isEqualTo(dto.getName());
    assertThat(wsRule.getSecurityCategory()).isEqualTo(SQCategory.OTHERS.getKey());
    assertThat(wsRule.getVulnerabilityProbability()).isEqualTo(SQCategory.OTHERS.getVulnerability().name());
  }

  private static void verifyComponent(Hotspots.Component wsComponent, ComponentDto dto) {
    assertThat(wsComponent.getKey()).isEqualTo(dto.getKey());
    if (dto.path() == null) {
      assertThat(wsComponent.hasPath()).isFalse();
    } else {
      assertThat(wsComponent.getPath()).isEqualTo(dto.path());
    }
    assertThat(wsComponent.getQualifier()).isEqualTo(dto.qualifier());
    assertThat(wsComponent.getName()).isEqualTo(dto.name());
    assertThat(wsComponent.getLongName()).isEqualTo(dto.longName());
  }

  private static IssueDto newHotspot(ComponentDto project, ComponentDto file, RuleDefinitionDto rule) {
    return IssueTesting.newIssue(rule, project, file).setType(SECURITY_HOTSPOT);
  }

  private TestRequest newRequest(IssueDto hotspot) {
    return actionTester.newRequest()
      .setParam("hotspot", hotspot.getKey());
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

  private static class IssueDtoArgumentMatcher implements ArgumentMatcher<IssueDto> {
    private final IssueDto expected;

    private IssueDtoArgumentMatcher(IssueDto expected) {
      this.expected = expected;
    }

    @Override
    public boolean matches(IssueDto argument) {
      return argument != null && argument.getKey().equals(expected.getKey());
    }
  }

}
