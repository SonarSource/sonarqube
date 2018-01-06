/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.qualityprofile.ws;

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.QualityProfileDbTester;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.Qualityprofiles.SearchWsResponse;
import org.sonarqube.ws.Qualityprofiles.SearchWsResponse.QualityProfile;

import static java.lang.String.format;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.rule.RuleStatus.DEPRECATED;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.qualityprofile.QualityProfileTesting.newQualityProfileDto;
import static org.sonar.server.language.LanguageTesting.newLanguage;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_DEFAULTS;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_QUALITY_PROFILE;

public class SearchActionTest {

  private static Language XOO1 = newLanguage("xoo1");
  private static Language XOO2 = newLanguage("xoo2");
  private static Languages LANGUAGES = new Languages(XOO1, XOO2);

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  private QualityProfileDbTester qualityProfileDb = db.qualityProfiles();
  private DbClient dbClient = db.getDbClient();
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private QProfileWsSupport qProfileWsSupport = new QProfileWsSupport(dbClient, userSession, defaultOrganizationProvider);

  private SearchAction underTest = new SearchAction(userSession, LANGUAGES, dbClient, qProfileWsSupport, new ComponentFinder(dbClient, null));
  private WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("search");
    assertThat(definition.responseExampleAsString()).isNotEmpty();
    assertThat(definition.isPost()).isFalse();

    assertThat(definition.changelog())
      .extracting(Change::getVersion, Change::getDescription)
      .containsExactlyInAnyOrder(
        tuple("6.5", "The parameters 'defaults', 'project' and 'language' can be combined without any constraint"),
        tuple("6.6", "Add available actions 'edit', 'copy' and 'setAsDefault' and global action 'create'"),
        tuple("7.0", "Add available actions 'delete' and 'associateProjects'"));

    WebService.Param organization = definition.param("organization");
    assertThat(organization).isNotNull();
    assertThat(organization.isRequired()).isFalse();
    assertThat(organization.isInternal()).isTrue();
    assertThat(organization.description()).isNotEmpty();
    assertThat(organization.since()).isEqualTo("6.4");

    WebService.Param defaults = definition.param("defaults");
    assertThat(defaults.defaultValue()).isEqualTo("false");
    assertThat(defaults.description()).isEqualTo("If set to true, return only the quality profiles marked as default for each language");

    WebService.Param projectKey = definition.param("project");
    assertThat(projectKey.description()).isEqualTo("Project key");
    assertThat(projectKey.deprecatedKey()).isEqualTo("projectKey");

    WebService.Param language = definition.param("language");
    assertThat(language.possibleValues()).containsExactly("xoo1", "xoo2");
    assertThat(language.deprecatedSince()).isNull();
    assertThat(language.description()).isEqualTo("Language key. If provided, only profiles for the given language are returned.");

    WebService.Param profileName = definition.param("qualityProfile");
    assertThat(profileName.deprecatedSince()).isNull();
    assertThat(profileName.description()).isEqualTo("Quality profile name");
  }

  @Test
  public void no_profile() {
    SearchWsResponse result = call(ws.newRequest());

    assertThat(result.getProfilesList()).isEmpty();
  }

  @Test
  public void empty_when_no_language_installed() {
    WsActionTester ws = new WsActionTester(new SearchAction(userSession, new Languages(), dbClient, qProfileWsSupport, new ComponentFinder(dbClient, null)));
    db.qualityProfiles().insert(db.getDefaultOrganization());

    SearchWsResponse result = call(ws.newRequest());

    assertThat(result.getProfilesList()).isEmpty();
  }

  @Test
  public void default_organization() {
    OrganizationDto defaultOrganization = db.getDefaultOrganization();
    OrganizationDto anotherOrganization = db.organizations().insert();
    QProfileDto profile1OnDefaultOrg = db.qualityProfiles().insert(defaultOrganization, p -> p.setLanguage(XOO1.getKey()));
    QProfileDto profile2OnDefaultOrg = db.qualityProfiles().insert(defaultOrganization, p -> p.setLanguage(XOO2.getKey()));
    QProfileDto profileOnAnotherOrg = db.qualityProfiles().insert(anotherOrganization, p -> p.setLanguage(XOO1.getKey()));

    SearchWsResponse result = call(ws.newRequest());

    assertThat(result.getProfilesList()).extracting(QualityProfile::getKey)
      .containsExactlyInAnyOrder(profile1OnDefaultOrg.getKee(), profile2OnDefaultOrg.getKee())
      .doesNotContain(profileOnAnotherOrg.getKee());
  }

  @Test
  public void specific_organization() {
    OrganizationDto defaultOrganization = db.getDefaultOrganization();
    OrganizationDto specificOrganization = db.organizations().insert();
    QProfileDto profile1OnSpecificOrg = db.qualityProfiles().insert(specificOrganization, p -> p.setLanguage(XOO1.getKey()));
    QProfileDto profile2OnSpecificOrg = db.qualityProfiles().insert(specificOrganization, p -> p.setLanguage(XOO2.getKey()));
    QProfileDto profileOnDefaultOrg = db.qualityProfiles().insert(defaultOrganization, p -> p.setLanguage(XOO1.getKey()));

    SearchWsResponse result = call(ws.newRequest().setParam(PARAM_ORGANIZATION, specificOrganization.getKey()));

    assertThat(result.getProfilesList()).extracting(QualityProfile::getKey)
      .containsExactlyInAnyOrder(profile1OnSpecificOrg.getKee(), profile2OnSpecificOrg.getKee())
      .doesNotContain(profileOnDefaultOrg.getKee());
  }

  @Test
  public void filter_on_default_profile() {
    QProfileDto defaultProfile1 = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(XOO1.getKey()));
    QProfileDto defaultProfile2 = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(XOO2.getKey()));
    QProfileDto nonDefaultProfile = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(XOO1.getKey()));
    db.qualityProfiles().setAsDefault(defaultProfile1, defaultProfile2);

    SearchWsResponse result = call(ws.newRequest().setParam(PARAM_DEFAULTS, "true"));

    assertThat(result.getProfilesList()).extracting(QualityProfile::getKey)
      .containsExactlyInAnyOrder(defaultProfile1.getKee(), defaultProfile2.getKee())
      .doesNotContain(nonDefaultProfile.getKee());
  }

  @Test
  public void does_not_filter_when_defaults_is_false() {
    QProfileDto defaultProfile1 = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(XOO1.getKey()));
    QProfileDto defaultProfile2 = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(XOO2.getKey()));
    QProfileDto nonDefaultProfile = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(XOO1.getKey()));
    db.qualityProfiles().setAsDefault(defaultProfile1, defaultProfile2);

    SearchWsResponse result = call(ws.newRequest().setParam(PARAM_DEFAULTS, "false"));

    assertThat(result.getProfilesList()).extracting(QualityProfile::getKey)
      .containsExactlyInAnyOrder(defaultProfile1.getKee(), defaultProfile2.getKee(), nonDefaultProfile.getKee());
  }

  @Test
  public void filter_on_language() {
    QProfileDto profile1OnXoo1 = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(XOO1.getKey()));
    QProfileDto profile2OnXoo1 = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(XOO1.getKey()));
    QProfileDto profileOnXoo2 = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(XOO2.getKey()));

    SearchWsResponse result = call(ws.newRequest().setParam(PARAM_LANGUAGE, XOO1.getKey()));

    assertThat(result.getProfilesList()).extracting(QualityProfile::getKey)
      .containsExactlyInAnyOrder(profile1OnXoo1.getKee(), profile2OnXoo1.getKee())
      .doesNotContain(profileOnXoo2.getKee());
  }

  @Test
  public void ignore_profiles_on_unknown_language() {
    QProfileDto profile1OnXoo1 = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(XOO1.getKey()));
    QProfileDto profile2OnXoo1 = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(XOO2.getKey()));
    QProfileDto profileOnUnknownLanguage = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage("unknown"));

    SearchWsResponse result = call(ws.newRequest());

    assertThat(result.getProfilesList()).extracting(QualityProfile::getKey)
      .containsExactlyInAnyOrder(profile1OnXoo1.getKee(), profile2OnXoo1.getKee())
      .doesNotContain(profileOnUnknownLanguage.getKee());
  }

  @Test
  public void filter_on_profile_name() {
    QProfileDto sonarWayOnXoo1 = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setName("Sonar way").setLanguage(XOO1.getKey()));
    QProfileDto sonarWayOnXoo2 = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setName("Sonar way").setLanguage(XOO1.getKey()));
    QProfileDto sonarWayInCamelCase = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setName("Sonar Way").setLanguage(XOO2.getKey()));
    QProfileDto anotherProfile = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setName("Another").setLanguage(XOO2.getKey()));

    SearchWsResponse result = call(ws.newRequest().setParam(PARAM_QUALITY_PROFILE, "Sonar way"));

    assertThat(result.getProfilesList()).extracting(QualityProfile::getKey)
      .containsExactlyInAnyOrder(sonarWayOnXoo1.getKee(), sonarWayOnXoo2.getKee())
      .doesNotContain(anotherProfile.getKee(), sonarWayInCamelCase.getKee());
  }

  @Test
  public void filter_on_defaults_and_name() {
    QProfileDto sonarWayOnXoo1 = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setName("Sonar way").setLanguage(XOO1.getKey()));
    QProfileDto sonarWayOnXoo2 = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setName("Sonar way").setLanguage(XOO2.getKey()));
    QProfileDto anotherProfile = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setName("Another").setLanguage(XOO2.getKey()));
    db.qualityProfiles().setAsDefault(sonarWayOnXoo1, anotherProfile);

    SearchWsResponse result = call(ws.newRequest()
      .setParam(PARAM_DEFAULTS, "true")
      .setParam(PARAM_QUALITY_PROFILE, "Sonar way"));

    assertThat(result.getProfilesList()).extracting(QualityProfile::getKey)
      .containsExactlyInAnyOrder(sonarWayOnXoo1.getKee())
      .doesNotContain(sonarWayOnXoo2.getKee(), anotherProfile.getKee());
  }

  @Test
  public void filter_on_project_key() {
    ComponentDto project = db.components().insertPrivateProject();
    QProfileDto profileOnXoo1 = db.qualityProfiles().insert(db.getDefaultOrganization(), q -> q.setLanguage(XOO1.getKey()));
    QProfileDto defaultProfileOnXoo1 = db.qualityProfiles().insert(db.getDefaultOrganization(), q -> q.setLanguage(XOO1.getKey()));
    QProfileDto defaultProfileOnXoo2 = db.qualityProfiles().insert(db.getDefaultOrganization(), q -> q.setLanguage(XOO2.getKey()));
    db.qualityProfiles().associateWithProject(project, profileOnXoo1);
    db.qualityProfiles().setAsDefault(defaultProfileOnXoo1, defaultProfileOnXoo2);

    SearchWsResponse result = call(ws.newRequest().setParam(PARAM_PROJECT_KEY, project.getDbKey()));

    assertThat(result.getProfilesList())
      .extracting(QualityProfile::getKey)
      .containsExactlyInAnyOrder(profileOnXoo1.getKee(), defaultProfileOnXoo2.getKee())
      .doesNotContain(defaultProfileOnXoo1.getKee());
  }

  @Test
  public void filter_on_module_key() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto module = db.components().insertComponent(newModuleDto(project));
    QProfileDto profileOnXoo1 = db.qualityProfiles().insert(db.getDefaultOrganization(), q -> q.setLanguage(XOO1.getKey()));
    QProfileDto defaultProfileOnXoo1 = db.qualityProfiles().insert(db.getDefaultOrganization(), q -> q.setLanguage(XOO1.getKey()));
    QProfileDto defaultProfileOnXoo2 = db.qualityProfiles().insert(db.getDefaultOrganization(), q -> q.setLanguage(XOO2.getKey()));
    db.qualityProfiles().associateWithProject(project, profileOnXoo1);
    db.qualityProfiles().setAsDefault(defaultProfileOnXoo1, defaultProfileOnXoo2);

    SearchWsResponse result = call(ws.newRequest().setParam(PARAM_PROJECT_KEY, module.getDbKey()));

    assertThat(result.getProfilesList())
      .extracting(QualityProfile::getKey)
      .containsExactlyInAnyOrder(profileOnXoo1.getKee(), defaultProfileOnXoo2.getKee())
      .doesNotContain(defaultProfileOnXoo1.getKee());
  }

  @Test
  public void filter_on_project_key_and_default() {
    ComponentDto project = db.components().insertPrivateProject();
    QProfileDto profileOnXoo1 = db.qualityProfiles().insert(db.getDefaultOrganization(), q -> q.setLanguage(XOO1.getKey()));
    QProfileDto defaultProfileOnXoo1 = db.qualityProfiles().insert(db.getDefaultOrganization(), q -> q.setLanguage(XOO1.getKey()));
    QProfileDto defaultProfileOnXoo2 = db.qualityProfiles().insert(db.getDefaultOrganization(), q -> q.setLanguage(XOO2.getKey()));
    db.qualityProfiles().associateWithProject(project, profileOnXoo1);
    db.qualityProfiles().setAsDefault(defaultProfileOnXoo1, defaultProfileOnXoo2);

    SearchWsResponse result = call(ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, project.getDbKey())
      .setParam(PARAM_DEFAULTS, "true"));

    assertThat(result.getProfilesList())
      .extracting(QualityProfile::getKey)
      .containsExactlyInAnyOrder(defaultProfileOnXoo2.getKee())
      .doesNotContain(defaultProfileOnXoo1.getKee(), profileOnXoo1.getKee());
  }

  @Test
  public void empty_when_filtering_on_project_and_no_language_installed() {
    WsActionTester ws = new WsActionTester(new SearchAction(userSession, new Languages(), dbClient, qProfileWsSupport, new ComponentFinder(dbClient, null)));
    db.qualityProfiles().insert(db.getDefaultOrganization());
    ComponentDto project = db.components().insertPrivateProject();
    QProfileDto profileOnXoo1 = db.qualityProfiles().insert(db.getDefaultOrganization(), q -> q.setLanguage(XOO1.getKey()));
    db.qualityProfiles().associateWithProject(project, profileOnXoo1);

    SearchWsResponse result = call(ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, project.getDbKey())
      .setParam(PARAM_DEFAULTS, "true"));

    assertThat(result.getProfilesList()).isEmpty();
  }

  @Test
  public void actions_when_user_is_global_qprofile_administer() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto customProfile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO1.getKey()));
    QProfileDto builtInProfile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO1.getKey()).setIsBuiltIn(true));
    QProfileDto defaultProfile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO1.getKey()));
    db.qualityProfiles().setAsDefault(defaultProfile);
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, organization);

    SearchWsResponse result = call(ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey()));

    assertThat(result.getProfilesList())
      .extracting(QualityProfile::getKey, qp -> qp.getActions().getEdit(), qp -> qp.getActions().getCopy(), qp -> qp.getActions().getSetAsDefault(),
        qp -> qp.getActions().getDelete(), qp -> qp.getActions().getAssociateProjects())
      .containsExactlyInAnyOrder(
        tuple(customProfile.getKee(), true, true, true, true, true),
        tuple(builtInProfile.getKee(), false, true, true, false, true),
        tuple(defaultProfile.getKee(), true, true, false, false, false));
    assertThat(result.getActions().getCreate()).isTrue();
  }

  @Test
  public void actions_when_user_can_edit_profile() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile1 = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO1.getKey()));
    QProfileDto profile2 = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO2.getKey()));
    QProfileDto profile3 = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO2.getKey()));
    QProfileDto builtInProfile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO2.getKey()).setIsBuiltIn(true));
    UserDto user = db.users().insertUser();
    GroupDto group = db.users().insertGroup(organization);
    db.qualityProfiles().addUserPermission(profile1, user);
    db.qualityProfiles().addGroupPermission(profile3, group);
    userSession.logIn(user).setGroups(group);

    SearchWsResponse result = call(ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey()));

    assertThat(result.getProfilesList())
      .extracting(QualityProfile::getKey, qp -> qp.getActions().getEdit(), qp -> qp.getActions().getCopy(), qp -> qp.getActions().getSetAsDefault(),
        qp -> qp.getActions().getDelete(), qp -> qp.getActions().getAssociateProjects())
      .containsExactlyInAnyOrder(
        tuple(profile1.getKee(), true, false, false, true, true),
        tuple(profile2.getKee(), false, false, false, false, false),
        tuple(profile3.getKee(), true, false, false, true, true),
        tuple(builtInProfile.getKee(), false, false, false, false, false));
    assertThat(result.getActions().getCreate()).isFalse();
  }

  @Test
  public void actions_when_not_logged_in() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO1.getKey()));
    userSession.anonymous();

    SearchWsResponse result = call(ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey()));

    assertThat(result.getProfilesList())
      .extracting(QualityProfile::getKey, qp -> qp.getActions().getEdit(), qp -> qp.getActions().getCopy(), qp -> qp.getActions().getSetAsDefault(),
        qp -> qp.getActions().getDelete(), qp -> qp.getActions().getAssociateProjects())
      .containsExactlyInAnyOrder(tuple(profile.getKee(), false, false, false, false, false));
    assertThat(result.getActions().getCreate()).isFalse();
  }

  @Test
  public void fail_if_project_does_not_exist() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component key 'unknown-project' not found");

    call(ws.newRequest().setParam(PARAM_PROJECT_KEY, "unknown-project"));
  }

  @Test
  public void fail_if_project_of_module_does_not_exist() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto module = db.components().insertComponent(newModuleDto(project).setProjectUuid("unknown"));

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(format("Project uuid of component uuid '%s' does not exist", module.uuid()));

    call(ws.newRequest().setParam(PARAM_PROJECT_KEY, module.getDbKey()));
  }

  @Test
  public void fail_if_project_is_on_another_organization() {
    OrganizationDto organization = db.organizations().insert();
    OrganizationDto anotherOrganization = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(anotherOrganization);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Component key '%s' not found", project.getDbKey()));

    call(ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_PROJECT_KEY, project.getDbKey()));
  }

  @Test
  public void statistics_on_active_rules() {
    QProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(XOO1.getKey()));
    RuleDefinitionDto rule = db.rules().insertRule(r -> r.setLanguage(XOO1.getKey())).getDefinition();
    RuleDefinitionDto deprecatedRule1 = db.rules().insertRule(r -> r.setStatus(DEPRECATED)).getDefinition();
    RuleDefinitionDto deprecatedRule2 = db.rules().insertRule(r -> r.setStatus(DEPRECATED)).getDefinition();
    RuleDefinitionDto inactiveRule = db.rules().insertRule(r -> r.setLanguage(XOO1.getKey())).getDefinition();
    db.qualityProfiles().activateRule(profile, rule);
    db.qualityProfiles().activateRule(profile, deprecatedRule1);
    db.qualityProfiles().activateRule(profile, deprecatedRule2);

    SearchWsResponse result = call(ws.newRequest());

    assertThat(result.getProfilesList())
      .extracting(QualityProfile::getActiveRuleCount, QualityProfile::getActiveDeprecatedRuleCount)
      .containsExactlyInAnyOrder(tuple(3L, 2L));
  }

  @Test
  public void statistics_on_projects() {
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    QProfileDto profileOnXoo1 = db.qualityProfiles().insert(db.getDefaultOrganization(), q -> q.setLanguage(XOO1.getKey()));
    QProfileDto defaultProfileOnXoo1 = db.qualityProfiles().insert(db.getDefaultOrganization(), q -> q.setLanguage(XOO1.getKey()));
    db.qualityProfiles().associateWithProject(project1, profileOnXoo1);
    db.qualityProfiles().associateWithProject(project2, profileOnXoo1);
    db.qualityProfiles().setAsDefault(defaultProfileOnXoo1);

    SearchWsResponse result = call(ws.newRequest());

    assertThat(result.getProfilesList())
      .extracting(QualityProfile::hasProjectCount, QualityProfile::getProjectCount)
      .containsExactlyInAnyOrder(tuple(true, 2L), tuple(false, 0L));
  }

  @Test
  public void map_dates() {
    long time = DateUtils.parseDateTime("2016-12-22T19:10:03+0100").getTime();
    qualityProfileDb.insert(newQualityProfileDto()
      .setOrganizationUuid(defaultOrganizationProvider.get().getUuid())
      .setLanguage(XOO1.getKey())
      .setRulesUpdatedAt("2016-12-21T19:10:03+0100")
      .setLastUsed(time)
      .setUserUpdatedAt(time));

    SearchWsResponse result = call(ws.newRequest());

    assertThat(result.getProfilesCount()).isEqualTo(1);
    assertThat(result.getProfiles(0).getRulesUpdatedAt()).isEqualTo("2016-12-21T19:10:03+0100");
    assertThat(parseDateTime(result.getProfiles(0).getLastUsed()).getTime()).isEqualTo(time);
    assertThat(parseDateTime(result.getProfiles(0).getUserUpdatedAt()).getTime()).isEqualTo(time);
  }

  @Test
  public void json_example() {
    OrganizationDto organization = db.organizations().insertForKey("My Organization");
    // languages
    Language cs = newLanguage("cs", "C#");
    Language java = newLanguage("java", "Java");
    Language python = newLanguage("py", "Python");
    // profiles
    QProfileDto sonarWayCs = db.qualityProfiles().insert(organization,
      p -> p.setName("Sonar way").setKee("AU-TpxcA-iU5OvuD2FL3").setIsBuiltIn(true).setLanguage(cs.getKey()));
    QProfileDto myCompanyProfile = db.qualityProfiles().insert(organization,
      p -> p.setName("My Company Profile").setKee("iU5OvuD2FLz").setLanguage(java.getKey()));
    QProfileDto myBuProfile = db.qualityProfiles().insert(organization,
      p -> p.setName("My BU Profile").setKee("AU-TpxcA-iU5OvuD2FL1").setParentKee(myCompanyProfile.getKee()).setLanguage(java.getKey()));
    QProfileDto sonarWayPython = db.qualityProfiles().insert(organization,
      p -> p.setName("Sonar way").setKee("AU-TpxcB-iU5OvuD2FL7").setIsBuiltIn(true).setLanguage(python.getKey()));
    db.qualityProfiles().setAsDefault(sonarWayCs, myCompanyProfile, sonarWayPython);
    // rules
    List<RuleDefinitionDto> javaRules = range(0, 10).mapToObj(i -> db.rules().insertRule(r -> r.setLanguage(java.getKey())).getDefinition())
      .collect(MoreCollectors.toList());
    List<RuleDefinitionDto> deprecatedJavaRules = range(0, 5)
      .mapToObj(i -> db.rules().insertRule(r -> r.setLanguage(java.getKey()).setStatus(DEPRECATED)).getDefinition())
      .collect(MoreCollectors.toList());
    range(0, 7).forEach(i -> db.qualityProfiles().activateRule(myCompanyProfile, javaRules.get(i)));
    range(0, 2).forEach(i -> db.qualityProfiles().activateRule(myCompanyProfile, deprecatedJavaRules.get(i)));
    range(0, 10).forEach(i -> db.qualityProfiles().activateRule(myBuProfile, javaRules.get(i)));
    range(0, 5).forEach(i -> db.qualityProfiles().activateRule(myBuProfile, deprecatedJavaRules.get(i)));
    range(0, 2)
      .mapToObj(i -> db.rules().insertRule(r -> r.setLanguage(python.getKey())).getDefinition())
      .forEach(rule -> db.qualityProfiles().activateRule(sonarWayPython, rule));
    range(0, 3)
      .mapToObj(i -> db.rules().insertRule(r -> r.setLanguage(cs.getKey())).getDefinition())
      .forEach(rule -> db.qualityProfiles().activateRule(sonarWayCs, rule));
    // project
    range(0, 7)
      .mapToObj(i -> db.components().insertPrivateProject(organization))
      .forEach(project -> db.qualityProfiles().associateWithProject(project, myBuProfile));
    // User
    UserDto user = db.users().insertUser();
    db.qualityProfiles().addUserPermission(myCompanyProfile, user);
    db.qualityProfiles().addUserPermission(myBuProfile, user);
    userSession.logIn(user);

    underTest = new SearchAction(userSession, new Languages(cs, java, python), dbClient, qProfileWsSupport, new ComponentFinder(dbClient, null));
    ws = new WsActionTester(underTest);
    String result = ws.newRequest().setParam(PARAM_ORGANIZATION, organization.getKey()).execute().getInput();
    assertJson(result).ignoreFields("ruleUpdatedAt", "lastUsed", "userUpdatedAt")
      .isSimilarTo(ws.getDef().responseExampleAsString());
  }

  private SearchWsResponse call(TestRequest request) {
    TestRequest wsRequest = request.setMediaType(MediaTypes.PROTOBUF);

    return wsRequest.executeProtobuf(SearchWsResponse.class);
  }
}
