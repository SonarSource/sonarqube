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

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QProfileChangeDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.QualityProfileTesting;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.qualityprofile.ActiveRuleInheritance;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_QUALITY_PROFILE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_SINCE;

public class ChangelogActionTest {

  private TestSystem2 system2 = new TestSystem2().setNow(1_500_000_000_000L);

  @Rule
  public DbTester dbTester = DbTester.create(system2);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private WsActionTester ws;
  private QProfileWsSupport wsSupport;
  private OrganizationDto organization;
  private DefaultOrganizationProvider defaultOrganizationProvider;

  @Before
  public void before() {
    system2.setNow(DateUtils.parseDateTime("2011-04-25T01:15:42+0100").getTime());
    defaultOrganizationProvider = TestDefaultOrganizationProvider.from(dbTester);
    wsSupport = new QProfileWsSupport(dbTester.getDbClient(), userSession, defaultOrganizationProvider);
    ws = new WsActionTester(
      new ChangelogAction(wsSupport, new Languages(), dbTester.getDbClient()));
    organization = dbTester.organizations().insert();
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.responseExampleAsString()).isNotEmpty();
    assertThat(definition.params()).extracting(WebService.Param::key)
      .containsExactlyInAnyOrder("key", "qualityProfile", "language", "organization", "since", "to", "p", "ps");
    WebService.Param profileName = definition.param("qualityProfile");
    assertThat(profileName.deprecatedSince()).isNullOrEmpty();
    WebService.Param language = definition.param("language");
    assertThat(language.deprecatedSince()).isNullOrEmpty();
  }

  @Test
  public void example() {
    QProfileDto profile = dbTester.qualityProfiles().insert(organization);
    String profileUuid = profile.getRulesProfileUuid();

    system2.setNow(DateUtils.parseDateTime("2015-02-23T17:58:39+0100").getTime());
    RuleDefinitionDto rule1 = dbTester.rules().insert(RuleKey.of("squid", "S2438"), r -> r.setName("\"Threads\" should not be used where \"Runnables\" are expected"));
    UserDto user1 = dbTester.users().insertUser(u -> u.setLogin("anakin.skywalker").setName("Anakin Skywalker"));
    insertChange(profile, c -> c.setRulesProfileUuid(profileUuid)
      .setLogin(user1.getLogin())
      .setChangeType(ActiveRuleChange.Type.ACTIVATED.name())
      .setData(ImmutableMap.of("severity", "CRITICAL", "ruleId", valueOf(rule1.getId()))));

    system2.setNow(DateUtils.parseDateTime("2015-02-23T17:58:18+0100").getTime());
    RuleDefinitionDto rule2 = dbTester.rules().insert(RuleKey.of("squid", "S2162"), r -> r.setName("\"equals\" methods should be symmetric and work for subclasses"));
    UserDto user2 = dbTester.users().insertUser(u -> u.setLogin("padme.amidala").setName("Padme Amidala"));
    QProfileChangeDto change2 = insertChange(profile, c -> c.setRulesProfileUuid(profileUuid)
      .setLogin(user2.getLogin())
      .setChangeType(ActiveRuleChange.Type.DEACTIVATED.name())
      .setData(ImmutableMap.of("ruleId", valueOf(rule2.getId()))));

    system2.setNow(DateUtils.parseDateTime("2014-09-12T15:20:46+0200").getTime());
    RuleDefinitionDto rule3 = dbTester.rules().insert(RuleKey.of("squid", "S00101"), r -> r.setName("Class names should comply with a naming convention"));
    UserDto user3 = dbTester.users().insertUser(u -> u.setLogin("obiwan.kenobi").setName("Obiwan Kenobi"));
    QProfileChangeDto change3 = insertChange(profile, c -> c.setRulesProfileUuid(profileUuid)
      .setLogin(user3.getLogin())
      .setChangeType(ActiveRuleChange.Type.ACTIVATED.name())
      .setData(ImmutableMap.of("severity", "MAJOR", "param_format", "^[A-Z][a-zA-Z0-9]*$", "ruleId", valueOf(rule3.getId()))));

    dbTester.commit();

    String response = ws.newRequest()
      .setMethod("GET")
      .setParam(PARAM_KEY, profile.getKee())
      .setParam("ps", "10")
      .execute()
      .getInput();

    assertJson(response).isSimilarTo(getClass().getResource("changelog-example.json"));
  }

  @Test
  public void find_changelog_by_profile_key() {
    QProfileDto profile = dbTester.qualityProfiles().insert(organization);

    String response = ws.newRequest()
      .setMethod("GET")
      .setParam(PARAM_KEY, profile.getKee())
      .execute()
      .getInput();

    assertThat(response).isNotEmpty();
  }

  @Test
  public void find_changelog_by_language_and_name() {
    QProfileDto qualityProfile = dbTester.qualityProfiles().insert(dbTester.getDefaultOrganization());

    String response = ws.newRequest()
      .setMethod("GET")
      .setParam(PARAM_LANGUAGE, qualityProfile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, qualityProfile.getName())
      .execute()
      .getInput();

    assertThat(response).isNotEmpty();
  }

  @Test
  public void find_changelog_by_organization_and_language_and_name() {
    QProfileDto qualityProfile = dbTester.qualityProfiles().insert(organization);

    String response = ws.newRequest()
      .setMethod("GET")
      .setParam(PARAM_LANGUAGE, qualityProfile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, qualityProfile.getName())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute()
      .getInput();

    assertThat(response).isNotEmpty();
  }

  @Test
  public void do_not_find_changelog_by_wrong_organization_and_language_and_name() {
    OrganizationDto organization1 = dbTester.organizations().insert();
    OrganizationDto organization2 = dbTester.organizations().insert();

    QProfileDto qualityProfile = dbTester.qualityProfiles().insert(organization1);

    TestRequest request = ws.newRequest()
      .setMethod("GET")
      .setParam(PARAM_LANGUAGE, qualityProfile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, qualityProfile.getName())
      .setParam(PARAM_ORGANIZATION, organization2.getKey());

    thrown.expect(NotFoundException.class);

    request.execute();
  }

  @Test
  public void changelog_empty() {
    QProfileDto qualityProfile = dbTester.qualityProfiles().insert(organization);

    String response = ws.newRequest()
      .setMethod("GET")
      .setParam(PARAM_KEY, qualityProfile.getKee())
      .execute()
      .getInput();

    assertThat(response).contains("\"total\":0");
    assertThat(response).contains("\"events\":[]");
  }

  @Test
  public void changelog_not_empty() {
    QProfileDto qualityProfile = dbTester.qualityProfiles().insert(organization);
    QProfileChangeDto change = QualityProfileTesting.newQProfileChangeDto()
      .setUuid(null)
      .setCreatedAt(0)
      .setRulesProfileUuid(qualityProfile.getRulesProfileUuid());
    DbSession session = dbTester.getSession();
    dbTester.getDbClient().qProfileChangeDao().insert(session, change);
    session.commit();

    String response = ws.newRequest()
      .setMethod("GET")
      .setParam(PARAM_KEY, qualityProfile.getKee())
      .execute()
      .getInput();

    assertThat(response).contains("\"total\":1");
  }

  @Test
  public void changelog_filter_by_since() {
    QProfileDto qualityProfile = dbTester.qualityProfiles().insert(organization);
    system2.setNow(DateUtils.parseDateTime("2011-04-25T01:15:42+0100").getTime());
    QProfileChangeDto change = QualityProfileTesting.newQProfileChangeDto()
      .setUuid(null)
      .setCreatedAt(0)
      .setRulesProfileUuid(qualityProfile.getRulesProfileUuid());
    DbSession session = dbTester.getSession();
    dbTester.getDbClient().qProfileChangeDao().insert(session, change);
    session.commit();

    String response = ws.newRequest()
      .setMethod("GET")
      .setParam(PARAM_KEY, qualityProfile.getKee())
      .setParam(PARAM_SINCE, "2011-04-25T01:15:42+0100")
      .execute()
      .getInput();

    assertThat(response).contains("\"total\":1");

    String response2 = ws.newRequest()
      .setMethod("GET")
      .setParam(PARAM_KEY, qualityProfile.getKee())
      .setParam(PARAM_SINCE, "2011-04-25T01:15:43+0100")
      .execute()
      .getInput();

    assertThat(response2).contains("\"total\":0");
  }

  @Test
  public void sort_changelog_events_in_reverse_chronological_order() {
    QProfileDto profile = dbTester.qualityProfiles().insert(organization);
    system2.setNow(DateUtils.parseDateTime("2011-04-25T01:15:42+0100").getTime());
    QProfileChangeDto change1 = insertChange(profile, ActiveRuleChange.Type.ACTIVATED, null, null);
    system2.setNow(DateUtils.parseDateTime("2011-04-25T01:15:43+0100").getTime());
    QProfileChangeDto change2 = insertChange(profile, ActiveRuleChange.Type.DEACTIVATED, "mazout", null);
    dbTester.commit();

    String response = ws.newRequest()
      .setMethod("GET")
      .setParam(PARAM_KEY, profile.getKee())
      .execute()
      .getInput();

    assertThat(response).containsSubsequence("15:43", "15:42");
  }

  @Test
  public void return_change_with_all_fields() {
    QProfileDto profile = dbTester.qualityProfiles().insert(organization);
    RuleDefinitionDto rule1 = dbTester.rules().insert(RuleKey.of("java", "S001"));

    Map<String, Object> data = ImmutableMap.of(
      "ruleId", valueOf(rule1.getId()),
      "severity", "MINOR",
      "inheritance", ActiveRuleInheritance.INHERITED.name(),
      "param_foo", "foo_value",
      "param_bar", "bar_value");
    QProfileChangeDto change = insertChange(profile, ActiveRuleChange.Type.ACTIVATED, "theLogin", data);
    dbTester.commit();

    String response = ws.newRequest()
      .setMethod("GET")
      .setParam(PARAM_KEY, profile.getKee())
      .execute()
      .getInput();

    JsonAssert.assertJson(response).isSimilarTo("{\n" +
      "  \"total\": 1,\n" +
      "  \"p\": 1,\n" +
      "  \"ps\": 50,\n" +
      "  \"events\": [\n" +
      "    {\n" +
      "      \"date\": \"2011-04-25T02:15:42+0200\",\n" +
      "      \"authorLogin\": \"theLogin\",\n" +
      "      \"action\": \"ACTIVATED\",\n" +
      "      \"ruleKey\": \"java:S001\",\n" +
      "      \"params\": {\n" +
      "        \"severity\": \"MINOR\",\n" +
      "        \"bar\": \"bar_value\",\n" +
      "        \"foo\": \"foo_value\"\n" +
      "      }\n" +
      "    }\n" +
      "  ]\n" +
      "}");
  }

  private QProfileChangeDto insertChange(QProfileDto profile, ActiveRuleChange.Type type, @Nullable String login, @Nullable Map<String, Object> data) {
    return insertChange(profile, c -> c.setRulesProfileUuid(profile.getRulesProfileUuid())
      .setLogin(login)
      .setChangeType(type.name())
      .setData(data));
  }

  private QProfileChangeDto insertChange(QProfileDto profile, Consumer<QProfileChangeDto>... consumers) {
    QProfileChangeDto dto = new QProfileChangeDto();
    Arrays.stream(consumers).forEach(c -> c.accept(dto));
    dbTester.getDbClient().qProfileChangeDao().insert(dbTester.getSession(), dto);
    return dto;
  }
}
