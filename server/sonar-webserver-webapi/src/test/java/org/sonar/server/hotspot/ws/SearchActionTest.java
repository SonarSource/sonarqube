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

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.AbstractLanguage;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
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
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Common.RuleStatus;
import org.sonarqube.ws.Hotspots;
import org.sonarqube.ws.Hotspots.Component;
import org.sonarqube.ws.Hotspots.SearchWsResponse;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.rules.RuleType.SECURITY_HOTSPOT;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;

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

  private Languages languages = mock(Languages.class);
  private SearchAction underTest = new SearchAction(dbClient, userSessionRule, issueIndex, defaultOrganizationProvider, languages);
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
    TestRequest request = actionTester.newRequest()
      .setParam("projectKey", project.getKey());

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void succeeds_on_public_project() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);

    SearchWsResponse response = actionTester.newRequest()
      .setParam("projectKey", project.getKey())
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList()).isEmpty();
    assertThat(response.getComponentsList()).isEmpty();
    assertThat(response.getRules().getRulesList()).isEmpty();
  }

  @Test
  public void succeeds_on_private_project_with_permission() {
    ComponentDto project = dbTester.components().insertPrivateProject();
    userSessionRule.registerComponents(project);
    userSessionRule.logIn().addProjectPermission(UserRole.USER, project);

    SearchWsResponse response = actionTester.newRequest()
      .setParam("projectKey", project.getKey())
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList()).isEmpty();
    assertThat(response.getComponentsList()).isEmpty();
    assertThat(response.getRules().getRulesList()).isEmpty();
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

    SearchWsResponse response = actionTester.newRequest()
      .setParam("projectKey", project.getKey())
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList()).isEmpty();
    assertThat(response.getComponentsList()).isEmpty();
    assertThat(response.getRules().getRulesList()).isEmpty();
  }

  @Test
  public void returns_hotspot_component_and_rule_when_project_has_hotspots() {
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

    SearchWsResponse response = actionTester.newRequest()
      .setParam("projectKey", project.getKey())
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(Hotspots.Hotspot::getKey)
      .containsOnly(Arrays.stream(hotspots).map(IssueDto::getKey).toArray(String[]::new));
    assertThat(response.getComponentsList())
      .extracting(Component::getKey)
      .containsOnly(project.getKey(), fileWithHotspot.getKey());
    assertThat(response.getRules().getRulesList())
      .extracting(Common.Rule::getKey)
      .containsOnly(Arrays.stream(hotspots).map(t -> t.getRuleKey().toString()).toArray(String[]::new));
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

    SearchWsResponse responseProject1 = actionTester.newRequest()
      .setParam("projectKey", project1.getKey())
      .executeProtobuf(SearchWsResponse.class);

    assertThat(responseProject1.getHotspotsList())
      .extracting(Hotspots.Hotspot::getKey)
      .doesNotContainAnyElementsOf(Arrays.stream(hotspots2).map(IssueDto::getKey).collect(Collectors.toList()));
    assertThat(responseProject1.getComponentsList())
      .extracting(Component::getKey)
      .containsOnly(project1.getKey(), file1.getKey());
    assertThat(responseProject1.getRules().getRulesList())
      .extracting(Common.Rule::getKey)
      .containsOnly(Arrays.stream(hotspots2).map(t -> t.getRuleKey().toString()).toArray(String[]::new));

    SearchWsResponse responseProject2 = actionTester.newRequest()
      .setParam("projectKey", project2.getKey())
      .executeProtobuf(SearchWsResponse.class);

    assertThat(responseProject2.getHotspotsList())
      .extracting(Hotspots.Hotspot::getKey)
      .containsOnly(Arrays.stream(hotspots2).map(IssueDto::getKey).toArray(String[]::new));
    assertThat(responseProject2.getComponentsList())
      .extracting(Component::getKey)
      .containsOnly(project2.getKey(), file2.getKey());
    assertThat(responseProject2.getRules().getRulesList())
      .extracting(Common.Rule::getKey)
      .containsOnly(Arrays.stream(hotspots2).map(t -> t.getRuleKey().toString()).toArray(String[]::new));
  }

  @Test
  public void returns_only_unresolved_hotspots() {
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

    SearchWsResponse response = actionTester.newRequest()
      .setParam("projectKey", project.getKey())
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(Hotspots.Hotspot::getKey)
      .containsOnly(unresolvedHotspot.getKey(), badlyClosedHotspot.getKey());
  }

  @Test
  public void returns_fields_of_hotspot() {
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
        .setAuthorLogin(randomAlphabetic(8)));
    indexIssues();

    SearchWsResponse response = actionTester.newRequest()
      .setParam("projectKey", project.getKey())
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList()).hasSize(1);
    Hotspots.Hotspot actual = response.getHotspots(0);
    assertThat(actual.getComponent()).isEqualTo(file.getKey());
    assertThat(actual.getProject()).isEqualTo(project.getKey());
    assertThat(actual.getRule()).isEqualTo(hotspot.getRuleKey().toString());
    assertThat(actual.getStatus()).isEqualTo(hotspot.getStatus());
    // FIXME resolution field will be added later
    // assertThat(actual.getResolution()).isEqualTo(hotspot.getResolution());
    assertThat(actual.getLine()).isEqualTo(hotspot.getLine());
    assertThat(actual.getMessage()).isEqualTo(hotspot.getMessage());
    assertThat(actual.getAssignee()).isEqualTo(hotspot.getAssigneeUuid());
    assertThat(actual.getAuthor()).isEqualTo(hotspot.getAuthorLogin());
    assertThat(actual.getCreationDate()).isEqualTo(formatDateTime(hotspot.getIssueCreationDate()));
    assertThat(actual.getUpdateDate()).isEqualTo(formatDateTime(hotspot.getIssueUpdateDate()));
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
        // FIXME resolution field will be added later
        // .setResolution(null)
        .setLine(null)
        .setMessage(null)
        .setAssigneeUuid(null)
        .setAuthorLogin(null));
    indexIssues();

    SearchWsResponse response = actionTester.newRequest()
      .setParam("projectKey", project.getKey())
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .hasSize(1);
    Hotspots.Hotspot actual = response.getHotspots(0);
    assertThat(actual.hasStatus()).isFalse();
    // FIXME resolution field will be added later
    // assertThat(actual.hasResolution()).isFalse();
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

    SearchWsResponse response = actionTester.newRequest()
      .setParam("projectKey", project.getKey())
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(Hotspots.Hotspot::getKey)
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
  public void returns_details_of_rule_with_language_name_when_available() {
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.registerComponents(project);
    indexPermissions();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    String language1 = randomAlphabetic(3);
    String language2 = randomAlphabetic(2);
    RuleDefinitionDto rule1a = newRule(SECURITY_HOTSPOT, t -> t.setLanguage(language1));
    RuleDefinitionDto rule1b = newRule(SECURITY_HOTSPOT, t -> t.setLanguage(language1));
    RuleDefinitionDto rule2 = newRule(SECURITY_HOTSPOT, t -> t.setLanguage(language2));
    when(languages.get(language1)).thenReturn(new AbstractLanguage(language1, language1 + "_name") {
      @Override
      public String[] getFileSuffixes() {
        return new String[0];
      }
    });
    IssueDto hotspot1a = dbTester.issues().insert(rule1a, project, file, t -> t.setType(SECURITY_HOTSPOT));
    IssueDto hotspot1b = dbTester.issues().insert(rule1b, project, file, t -> t.setType(SECURITY_HOTSPOT));
    IssueDto hotspot2 = dbTester.issues().insert(rule2, project, file, t -> t.setType(SECURITY_HOTSPOT));
    indexIssues();

    SearchWsResponse response = actionTester.newRequest()
      .setParam("projectKey", project.getKey())
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getHotspotsList()).hasSize(3);
    assertThat(response.getRules().getRulesList()).hasSize(3);
    Map<RuleKey, Common.Rule> rulesByKey = response.getRules().getRulesList()
      .stream()
      .collect(uniqueIndex(t -> RuleKey.parse(t.getKey())));
    Common.Rule actualRule1a = rulesByKey.get(hotspot1a.getRuleKey());
    assertThat(actualRule1a.getName()).isEqualTo(rule1a.getName());
    assertThat(actualRule1a.getLang()).isEqualTo(rule1a.getLanguage());
    assertThat(actualRule1a.getLangName()).isEqualTo(rule1a.getLanguage() + "_name");
    assertThat(actualRule1a.getStatus()).isEqualTo(RuleStatus.valueOf(rule1a.getStatus().name()));
    Common.Rule actualRule1b = rulesByKey.get(hotspot1b.getRuleKey());
    assertThat(actualRule1b.getName()).isEqualTo(rule1b.getName());
    assertThat(actualRule1b.getLang()).isEqualTo(rule1b.getLanguage());
    assertThat(actualRule1b.getLangName()).isEqualTo(rule1b.getLanguage() + "_name");
    assertThat(actualRule1b.getStatus()).isEqualTo(RuleStatus.valueOf(rule1b.getStatus().name()));
    Common.Rule actualRule2 = rulesByKey.get(hotspot2.getRuleKey());
    assertThat(actualRule2.getName()).isEqualTo(rule2.getName());
    assertThat(actualRule2.getLang()).isEqualTo(rule2.getLanguage());
    assertThat(actualRule2.hasLangName()).isFalse();
    assertThat(actualRule2.getStatus()).isEqualTo(RuleStatus.valueOf(rule2.getStatus().name()));
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
