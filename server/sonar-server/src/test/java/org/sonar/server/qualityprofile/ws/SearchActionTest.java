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

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Date;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.db.qualityprofile.QualityProfileDao;
import org.sonar.db.qualityprofile.QualityProfileDbTester;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.QProfileLookup;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse.QualityProfile;

import static com.google.common.base.Throwables.propagate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.qualityprofile.QualityProfileTesting.newQualityProfileDto;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.*;

public class SearchActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  QualityProfileDbTester qualityProfileDb = new QualityProfileDbTester(db);
  ComponentDbTester componentDb = new ComponentDbTester(db);
  DbClient dbClient = db.getDbClient();
  DbSession dbSession = db.getSession();
  QualityProfileDao qualityProfileDao = dbClient.qualityProfileDao();
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);

  private ActiveRuleIndex activeRuleIndex = mock(ActiveRuleIndex.class);
  private QProfileWsSupport qProfileWsSupport = new QProfileWsSupport(dbClient, userSession, defaultOrganizationProvider);

  private Language xoo1;
  private Language xoo2;

  private WsActionTester ws;

  @Before
  public void setUp() {
    xoo1 = LanguageTesting.newLanguage("xoo1");
    xoo2 = LanguageTesting.newLanguage("xoo2");

    Languages languages = new Languages(xoo1, xoo2);
    ws = new WsActionTester(new SearchAction(
      new SearchDataLoader(
        languages,
        new QProfileLookup(dbClient),
        new QProfileFactory(dbClient),
        dbClient,
        new ComponentFinder(dbClient), activeRuleIndex, qProfileWsSupport),
      languages));
  }

  @Test
  public void search_nominal() throws Exception {
    when(activeRuleIndex.countAllByQualityProfileKey()).thenReturn(ImmutableMap.of(
      "sonar-way-xoo1-12345", 11L,
      "my-sonar-way-xoo2-34567", 33L));
    when(activeRuleIndex.countAllDeprecatedByQualityProfileKey()).thenReturn(ImmutableMap.of(
      "sonar-way-xoo1-12345", 1L,
      "my-sonar-way-xoo2-34567", 2L));

    OrganizationDto organizationDto = getDefaultOrganization();
    String organizationUuid = organizationDto.getUuid();
    qualityProfileDao.insert(dbSession,
      QualityProfileDto.createFor("sonar-way-xoo1-12345")
        .setOrganizationUuid(organizationUuid)
        .setLanguage(xoo1.getKey())
        .setName("Sonar way")
        .setDefault(true),

      QualityProfileDto
        .createFor("sonar-way-xoo2-23456")
        .setOrganizationUuid(organizationUuid)
        .setLanguage(xoo2.getKey())
        .setName("Sonar way"),

      QualityProfileDto
        .createFor("my-sonar-way-xoo2-34567")
        .setOrganizationUuid(organizationUuid)
        .setLanguage(xoo2.getKey())
        .setName("My Sonar way")
        .setParentKee("sonar-way-xoo2-23456"),

      QualityProfileDto.createFor("sonar-way-other-666")
        .setOrganizationUuid(organizationUuid)
        .setLanguage("other").setName("Sonar way")
        .setDefault(true));
    new ComponentDao().insert(dbSession,
      newProjectDto(organizationDto, "project-uuid1"),
      newProjectDto(organizationDto, "project-uuid2"));
    qualityProfileDao.insertProjectProfileAssociation("project-uuid1", "sonar-way-xoo2-23456", dbSession);
    qualityProfileDao.insertProjectProfileAssociation("project-uuid2", "sonar-way-xoo2-23456", dbSession);
    db.commit();

    String result = ws.newRequest().execute().getInput();

    assertJson(result).isSimilarTo(getClass().getResource("SearchActionTest/search.json"));
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

    SearchWsResponse result = call(ws.newRequest());

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
    long time = DateUtils.parseDateTime("2016-12-22T19:10:03+0100").getTime();
    OrganizationDto org = db.getDbClient().organizationDao().selectByKey(db.getSession(), defaultOrganizationProvider.get().getKey()).get();
    QualityProfileDto qualityProfileOnXoo1 = QualityProfileDto.createFor("sonar-way-xoo1-12345")
      .setOrganizationUuid(org.getUuid())
      .setLanguage(xoo1.getKey())
      .setRulesUpdatedAt("2016-12-21T19:10:03+0100")
      .setLastUsed(time)
      .setName("Sonar way");
    QualityProfileDto qualityProfileOnXoo2 = QualityProfileDto.createFor("sonar-way-xoo2-12345")
      .setOrganizationUuid(org.getUuid())
      .setLanguage(xoo2.getKey())
      .setRulesUpdatedAt("2016-12-21T19:10:03+0100")
      .setLastUsed(time)
      .setName("Sonar way");
    QualityProfileDto anotherQualityProfileOnXoo1 = QualityProfileDto.createFor("sonar-way-xoo1-45678")
      .setOrganizationUuid(org.getUuid())
      .setLanguage(xoo1.getKey())
      .setRulesUpdatedAt("2016-12-21T19:10:03+0100")
      .setLastUsed(time)
      .setName("Another way");
    ComponentDto project = newProjectDto(org, "project-uuid");
    qualityProfileDb.insertQualityProfiles(qualityProfileOnXoo1, qualityProfileOnXoo2, anotherQualityProfileOnXoo1);
    qualityProfileDb.insertProjectWithQualityProfileAssociations(project, qualityProfileOnXoo1, qualityProfileOnXoo2);

    SearchWsResponse result = call(ws.newRequest().setParam(PARAM_PROJECT_KEY, project.key()));

    assertThat(result.getProfilesList())
      .hasSize(2)
      .extracting(QualityProfile::getKey)
      .contains("sonar-way-xoo1-12345", "sonar-way-xoo2-12345")
      .doesNotContain("sonar-way-xoo1-45678");
    assertThat(result.getProfilesList())
      .extracting(QualityProfile::getRulesUpdatedAt, QualityProfile::getLastUsed)
      .contains(tuple("2016-12-21T19:10:03+0100", formatDateTime(time)));
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
    QualityProfileDto qualityProfileOnXoo1 = QualityProfileDto.createFor("sonar-way-xoo1-12345")
      .setOrganizationUuid(org.getUuid())
      .setLanguage(xoo1.getKey())
      .setRulesUpdatedAtAsDate(new Date())
      .setName("Sonar way");
    QualityProfileDto qualityProfileOnXoo2 = QualityProfileDto.createFor("sonar-way-xoo2-12345")
      .setOrganizationUuid(org.getUuid())
      .setLanguage(xoo2.getKey())
      .setRulesUpdatedAtAsDate(new Date())
      .setName("Sonar way")
      .setDefault(true);
    QualityProfileDto anotherQualityProfileOnXoo1 = QualityProfileDto.createFor("sonar-way-xoo1-45678")
      .setOrganizationUuid(org.getUuid())
      .setLanguage(xoo1.getKey())
      .setRulesUpdatedAtAsDate(new Date())
      .setName("Another way")
      .setDefault(true);
    qualityProfileDb.insertQualityProfiles(qualityProfileOnXoo1, qualityProfileOnXoo2, anotherQualityProfileOnXoo1);
    ComponentDto project = componentDb.insertComponent(newProjectDto(org, "project-uuid"));

    String result = ws.newRequest()
      .setParam(PARAM_ORGANIZATION, org.getKey())
      .setParam(PARAM_PROJECT_KEY, project.key())
      .setParam(PARAM_PROFILE_NAME, "Sonar way")
      .execute().getInput();

    assertThat(result)
      .contains("sonar-way-xoo1-12345", "sonar-way-xoo2-12345")
      .doesNotContain("sonar-way-xoo1-45678");
  }

  private SearchWsResponse call(TestRequest request) {
    try {
      return SearchWsResponse.parseFrom(request
        .setMediaType(MediaTypes.PROTOBUF)
        .execute().getInputStream());
    } catch (IOException e) {
      throw propagate(e);
    }
  }

  private OrganizationDto getDefaultOrganization() {
    return qProfileWsSupport.getOrganizationByKey(db.getSession(), defaultOrganizationProvider.get().getKey());
  }
}
