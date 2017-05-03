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
package org.sonar.server.qualityprofile.ws;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.db.qualityprofile.QualityProfileDao;
import org.sonar.db.qualityprofile.QualityProfileDbTester;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.qualityprofile.QualityProfileTesting;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.QProfileLookup;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.QualityProfiles;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse.QualityProfile;
import org.sonarqube.ws.client.qualityprofile.SearchWsRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.db.qualityprofile.QualityProfileTesting.newQualityProfileDto;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_DEFAULTS;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROFILE_NAME;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROJECT_KEY;

public class SearchActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  private QualityProfileDbTester qualityProfileDb = db.qualityProfiles();
  private ComponentDbTester componentDb = new ComponentDbTester(db);
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private QualityProfileDao qualityProfileDao = dbClient.qualityProfileDao();
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private QProfileWsSupport qProfileWsSupport = new QProfileWsSupport(dbClient, userSession, defaultOrganizationProvider);

  private Language xoo1;
  private Language xoo2;

  private WsActionTester ws;
  private SearchAction underTest;

  @Before
  public void setUp() {
    xoo1 = LanguageTesting.newLanguage("xoo1");
    xoo2 = LanguageTesting.newLanguage("xoo2");

    Languages languages = new Languages(xoo1, xoo2);
    underTest = new SearchAction(
      new SearchDataLoader(
        languages,
        new QProfileLookup(dbClient),
        dbClient
      ),
      languages,
      dbClient,
      qProfileWsSupport);
    ws = new WsActionTester(underTest);
  }

  @Test
  public void define_search() {
    WebService.Action search = ws.getDef();
    assertThat(search).isNotNull();
    assertThat(search.isPost()).isFalse();
    assertThat(search.param("language").possibleValues()).containsExactly("xoo1", "xoo2");
    assertThat(search.param("language").deprecatedSince()).isEqualTo("6.4");
    assertThat(search.param("profileName").deprecatedSince()).isEqualTo("6.4");
    assertThat(search.param("projectKey")).isNotNull();
    assertThat(search.param("defaults")).isNotNull();
    assertThat(search.param("organization")).isNotNull();
    assertThat(search.param("organization").isRequired()).isFalse();
    assertThat(search.param("organization").isInternal()).isTrue();
    assertThat(search.param("organization").description()).isNotEmpty();
    assertThat(search.param("organization").since()).isEqualTo("6.4");
  }

  @Test
  public void ws_returns_the_profiles_of_default_organization() throws Exception {
    OrganizationDto organization = getDefaultOrganization();

    QualityProfileDto defaultProfile = QualityProfileDto.createFor("sonar-way-xoo1-12345")
      .setOrganizationUuid(organization.getUuid())
      .setLanguage(xoo1.getKey())
      .setName("Sonar way")
      .setDefault(true);
    QualityProfileDto parentProfile = QualityProfileDto
      .createFor("sonar-way-xoo2-23456")
      .setOrganizationUuid(organization.getUuid())
      .setLanguage(xoo2.getKey())
      .setName("Sonar way");
    QualityProfileDto childProfile = QualityProfileDto
      .createFor("my-sonar-way-xoo2-34567")
      .setOrganizationUuid(organization.getUuid())
      .setLanguage(xoo2.getKey())
      .setName("My Sonar way")
      .setParentKee(parentProfile.getKey());
    QualityProfileDto profileOnUnknownLang = QualityProfileDto.createFor("sonar-way-other-666")
      .setOrganizationUuid(organization.getUuid())
      .setLanguage("other").setName("Sonar way")
      .setDefault(true);
    qualityProfileDao.insert(dbSession, defaultProfile, parentProfile, childProfile, profileOnUnknownLang);

    ComponentDto project1 = db.components().insertPrivateProject(organization);
    ComponentDto project2 = db.components().insertPrivateProject(organization);
    db.qualityProfiles().associateProjectWithQualityProfile(project1, parentProfile);
    db.qualityProfiles().associateProjectWithQualityProfile(project2, parentProfile);

    String result = ws.newRequest().execute().getInput();

    assertJson(result).isSimilarTo("{" +
      "  \"profiles\": [" +
      "    {" +
      "      \"key\": \"sonar-way-xoo1-12345\"," +
      "      \"name\": \"Sonar way\"," +
      "      \"language\": \"xoo1\"," +
      "      \"languageName\": \"Xoo1\"," +
      "      \"isInherited\": false," +
      "      \"isDefault\": true," +
      "      \"activeRuleCount\": 0," +
      "      \"activeDeprecatedRuleCount\": 0," +
      "      \"organization\": \"" + organization.getKey() + "\"" +
      "    }," +
      "    {" +
      "      \"key\": \"my-sonar-way-xoo2-34567\"," +
      "      \"name\": \"My Sonar way\"," +
      "      \"language\": \"xoo2\"," +
      "      \"languageName\": \"Xoo2\"," +
      "      \"isInherited\": true," +
      "      \"isDefault\": false," +
      "      \"parentKey\": \"sonar-way-xoo2-23456\"," +
      "      \"parentName\": \"Sonar way\"," +
      "      \"activeRuleCount\": 0," +
      "      \"activeDeprecatedRuleCount\": 0," +
      "      \"projectCount\": 0," +
      "      \"organization\": \"" + organization.getKey() + "\"" +
      "    }," +
      "    {" +
      "      \"key\": \"sonar-way-xoo2-23456\"," +
      "      \"name\": \"Sonar way\"," +
      "      \"language\": \"xoo2\"," +
      "      \"languageName\": \"Xoo2\"," +
      "      \"isInherited\": false," +
      "      \"isDefault\": false," +
      "      \"activeRuleCount\": 0," +
      "      \"activeDeprecatedRuleCount\": 0," +
      "      \"projectCount\": 2," +
      "      \"organization\": \"" + organization.getKey() + "\"" +
      "    }" +
      "  ]" +
      "}");
  }

  @Test
  public void response_contains_statistics_on_active_rules() {
    QualityProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(xoo1.getKey()));
    RuleDefinitionDto rule = db.rules().insertRule(r -> r.setLanguage(xoo1.getKey())).getDefinition();
    RuleDefinitionDto deprecatedRule1 = db.rules().insertRule(r -> r.setStatus(RuleStatus.DEPRECATED)).getDefinition();
    RuleDefinitionDto deprecatedRule2 = db.rules().insertRule(r -> r.setStatus(RuleStatus.DEPRECATED)).getDefinition();
    db.qualityProfiles().activateRule(profile, rule);
    db.qualityProfiles().activateRule(profile, deprecatedRule1);
    db.qualityProfiles().activateRule(profile, deprecatedRule2);

    String result = ws.newRequest().execute().getInput();

    assertJson(result).isSimilarTo("{\"profiles\":[" +
      "{" +
      "     \"key\":\"" + profile.getKey() + "\"," +
      "     \"activeRuleCount\":3," +
      "     \"activeDeprecatedRuleCount\":2" +
      "}]}");

  }

  @Test
  public void search_map_dates() {
    long time = DateUtils.parseDateTime("2016-12-22T19:10:03+0100").getTime();
    qualityProfileDb.insertQualityProfiles(newQualityProfileDto()
      .setOrganizationUuid(defaultOrganizationProvider.get().getUuid())
      .setLanguage(xoo1.getKey())
      .setRulesUpdatedAt("2016-12-21T19:10:03+0100")
      .setLastUsed(time)
      .setUserUpdatedAt(time));

    SearchWsResponse result = ws.newRequest()
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getProfilesCount()).isEqualTo(1);
    assertThat(result.getProfiles(0).getRulesUpdatedAt()).isEqualTo("2016-12-21T19:10:03+0100");
    assertThat(parseDateTime(result.getProfiles(0).getLastUsed()).getTime()).isEqualTo(time);
    assertThat(parseDateTime(result.getProfiles(0).getUserUpdatedAt()).getTime()).isEqualTo(time);
  }

  @Test
  public void search_for_language() throws Exception {
    qualityProfileDb.insertQualityProfiles(
      QualityProfileDto.createFor("sonar-way-xoo1-12345")
        .setOrganizationUuid(defaultOrganizationProvider.get().getUuid())
        .setLanguage(xoo1.getKey())
        .setName("Sonar way"));

    String result = ws.newRequest().setParam("language", xoo1.getKey()).execute().getInput();

    assertJson(result).isSimilarTo(getClass().getResource("SearchActionTest/search_xoo1.json"));
  }

  @Test
  public void search_for_project_qp() {
    OrganizationDto org = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(org);
    QualityProfileDto qualityProfileOnXoo1 = db.qualityProfiles().insert(org, q -> q.setLanguage(xoo1.getKey()));
    db.qualityProfiles().associateProjectWithQualityProfile(project, qualityProfileOnXoo1);
    QualityProfileDto qualityProfileOnXoo2 = db.qualityProfiles().insert(org, q -> q.setLanguage(xoo2.getKey()).setDefault(true));
    db.qualityProfiles().associateProjectWithQualityProfile(project, qualityProfileOnXoo2);
    db.qualityProfiles().insert(org, q -> q.setLanguage(xoo1.getKey()).setDefault(true));

    SearchWsResponse result = ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, project.key())
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getProfilesList())
      .extracting(QualityProfile::getKey)
      .containsExactlyInAnyOrder(qualityProfileOnXoo1.getKey(), qualityProfileOnXoo2.getKey());
  }

  @Test
  public void search_for_project_qp_with_wrong_organization() {
    OrganizationDto org1 = db.organizations().insert();
    OrganizationDto org2 = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(org1);

    TestRequest request = ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, project.key())
      .setParam(PARAM_ORGANIZATION, org2.getKey());

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(
      "The provided organization key '" + org2.getKey() + "' does not match the organization key '" + org1.getKey() + "' of the component '" + project.getKey() + "'");

    request.execute();
  }

  @Test
  public void search_for_default_qp_with_profile_name() {
    String orgUuid = defaultOrganizationProvider.get().getUuid();
    QualityProfileDto qualityProfileOnXoo1 = QualityProfileDto.createFor("sonar-way-xoo1-12345")
      .setOrganizationUuid(orgUuid)
      .setLanguage(xoo1.getKey())
      .setName("Sonar way")
      .setDefault(false);
    QualityProfileDto qualityProfileOnXoo2 = QualityProfileDto.createFor("sonar-way-xoo2-12345")
      .setOrganizationUuid(orgUuid)
      .setLanguage(xoo2.getKey())
      .setName("Sonar way")
      .setDefault(true);
    QualityProfileDto anotherQualityProfileOnXoo1 = QualityProfileDto.createFor("sonar-way-xoo1-45678")
      .setOrganizationUuid(orgUuid)
      .setLanguage(xoo1.getKey())
      .setName("Another way")
      .setDefault(true);
    qualityProfileDb.insertQualityProfiles(qualityProfileOnXoo1, qualityProfileOnXoo2, anotherQualityProfileOnXoo1);

    String result = ws.newRequest()
      .setParam(PARAM_DEFAULTS, Boolean.TRUE.toString())
      .setParam(PARAM_PROFILE_NAME, "Sonar way")
      .execute().getInput();

    assertThat(result)
      .contains("sonar-way-xoo1-12345", "sonar-way-xoo2-12345")
      .doesNotContain("sonar-way-xoo1-45678");
  }

  @Test
  public void search_by_profile_name() {
    OrganizationDto org = db.organizations().insert();
    QualityProfileDto qualityProfileOnXoo1 = db.qualityProfiles().insert(org, q -> q.setLanguage(xoo1.getKey()).setName("Sonar way"));
    QualityProfileDto qualityProfileOnXoo2 = db.qualityProfiles().insert(org, q -> q.setLanguage(xoo2.getKey()).setName("Sonar way").setDefault(true));
    QualityProfileDto anotherQualityProfileOnXoo1 = db.qualityProfiles().insert(org, q -> q.setLanguage(xoo1.getKey()).setName("Another way").setDefault(true));
    ComponentDto project = componentDb.insertPrivateProject(org);

    SearchWsResponse result = ws.newRequest()
      .setParam(PARAM_ORGANIZATION, org.getKey())
      .setParam(PARAM_PROJECT_KEY, project.key())
      .setParam(PARAM_PROFILE_NAME, "Sonar way")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getProfilesList())
      .extracting(QualityProfile::getKey)
      .contains(qualityProfileOnXoo1.getKey(), qualityProfileOnXoo2.getKey())
      .doesNotContain(anotherQualityProfileOnXoo1.getKey());
  }

  @Test
  public void search_default_profile_by_profile_name_and_org() {
    List<QualityProfileDto> profiles = new ArrayList<>();
    for (String orgKey : Arrays.asList("ORG1", "ORG2")) {
      OrganizationDto org = db.organizations().insert(OrganizationTesting.newOrganizationDto().setKey(orgKey));
      profiles.add(createProfile("A", xoo1, org, "MATCH", false));
      profiles.add(createProfile("B", xoo2, org, "NOMATCH", false));
      profiles.add(createProfile("C", xoo1, org, "NOMATCH", true));
      profiles.add(createProfile("D", xoo2, org, "NOMATCH", true));
    }

    profiles.forEach(db.qualityProfiles()::insertQualityProfile);

    SearchWsRequest request = new SearchWsRequest()
      .setDefaults(true)
      .setProfileName("MATCH")
      .setOrganizationKey("ORG1");
    QualityProfiles.SearchWsResponse response = underTest.doHandle(request);

    assertThat(response.getProfilesList())
      .extracting(QualityProfiles.SearchWsResponse.QualityProfile::getKey)
      .containsExactlyInAnyOrder(

        // name match for xoo1
        "ORG1-A",

        // default for xoo2
        "ORG1-D");
  }

  @Test
  public void name_and_default_query_is_valid() throws Exception {
    minimalValidSetup();

    SearchWsRequest request = new SearchWsRequest()
      .setProfileName("bla")
      .setDefaults(true);

    assertThat(findProfiles(request).getProfilesList()).isNotNull();
  }

  @Test
  public void name_and_component_query_is_valid() throws Exception {
    minimalValidSetup();
    ComponentDto project = db.components().insertPrivateProject();

    SearchWsRequest request = new SearchWsRequest()
      .setProfileName("bla")
      .setProjectKey(project.key());

    assertThat(findProfiles(request).getProfilesList()).isNotNull();
  }

  @Test
  public void name_requires_either_component_or_defaults() throws Exception {
    SearchWsRequest request = new SearchWsRequest()
      .setProfileName("bla");

    thrown.expect(BadRequestException.class);
    thrown.expectMessage("The name parameter requires either projectKey or defaults to be set.");

    findProfiles(request);
  }

  @Test
  public void default_and_component_cannot_be_set_at_same_time() throws Exception {
    SearchWsRequest request = new SearchWsRequest()
      .setDefaults(true)
      .setProjectKey("blubb");

    thrown.expect(BadRequestException.class);
    thrown.expectMessage("The default parameter cannot be provided at the same time than the component key.");

    findProfiles(request);
  }

  @Test
  public void language_and_component_cannot_be_set_at_same_time() throws Exception {
    SearchWsRequest request = new SearchWsRequest()
      .setLanguage("xoo")
      .setProjectKey("bla");

    thrown.expect(BadRequestException.class);
    thrown.expectMessage("The language parameter cannot be provided at the same time than the component key or profile name.");

    findProfiles(request);
  }

  @Test
  public void language_and_name_cannot_be_set_at_same_time() throws Exception {
    SearchWsRequest request = new SearchWsRequest()
      .setLanguage("xoo")
      .setProfileName("bla");

    thrown.expect(BadRequestException.class);
    thrown.expectMessage("The language parameter cannot be provided at the same time than the component key or profile name.");

    findProfiles(request);
  }

  private void minimalValidSetup() {
    for (Language language : Arrays.asList(xoo1, xoo2)) {
      db.qualityProfiles().insertQualityProfile(
        QualityProfileTesting.newQualityProfileDto()
          .setOrganizationUuid(getDefaultOrganization().getUuid())
          .setLanguage(language.getKey())
          .setDefault(true));
    }
  }

  private QualityProfileDto createProfile(String keySuffix, Language language, OrganizationDto org, String name, boolean isDefault) {
    return QualityProfileDto.createFor(org.getKey() + "-" + keySuffix)
      .setOrganizationUuid(org.getUuid())
      .setLanguage(language.getKey())
      .setName(name)
      .setDefault(isDefault);
  }

  private SearchWsResponse findProfiles(SearchWsRequest request) {
    return underTest.doHandle(request);
  }

  private OrganizationDto getDefaultOrganization() {
    return db.getDefaultOrganization();
  }
}
