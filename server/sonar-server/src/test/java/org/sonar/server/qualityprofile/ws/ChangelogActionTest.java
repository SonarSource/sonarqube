/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QProfileChangeDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.qualityprofile.ActiveRuleInheritance;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.organization.OrganizationDto.Subscription.PAID;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_QUALITY_PROFILE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_SINCE;

public class ChangelogActionTest {

  private static final String DATE = "2011-04-25T01:15:42+0100";

  private TestSystem2 system2 = new TestSystem2().setNow(DateUtils.parseDateTime(DATE).getTime());

  @Rule
  public DbTester db = DbTester.create(system2);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private QProfileWsSupport wsSupport = new QProfileWsSupport(db.getDbClient(), userSession, TestDefaultOrganizationProvider.from(db));
  private WsActionTester ws = new WsActionTester(
    new ChangelogAction(wsSupport, new Languages(), db.getDbClient()));

  @Test
  public void return_change_with_all_fields() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization);
    UserDto user = db.users().insertUser();
    RuleDefinitionDto rule = db.rules().insert(RuleKey.of("java", "S001"));
    insertChange(profile, ActiveRuleChange.Type.ACTIVATED, user, ImmutableMap.of(
      "ruleId", valueOf(rule.getId()),
      "severity", "MINOR",
      "inheritance", ActiveRuleInheritance.INHERITED.name(),
      "param_foo", "foo_value",
      "param_bar", "bar_value"));

    String response = ws.newRequest()
      .setParam(PARAM_KEY, profile.getKee())
      .execute()
      .getInput();

    assertJson(response).isSimilarTo("{\n" +
      "  \"total\": 1,\n" +
      "  \"p\": 1,\n" +
      "  \"ps\": 50,\n" +
      "  \"events\": [\n" +
      "    {\n" +
      "      \"date\": \"" + DATE + "\",\n" +
      "      \"authorLogin\": \"" + user.getLogin() + "\",\n" +
      "      \"authorName\": \"" + user.getName() + "\",\n" +
      "      \"action\": \"ACTIVATED\",\n" +
      "      \"ruleKey\": \"" + rule.getKey() + "\",\n" +
      "      \"ruleName\": \"" + rule.getName() + "\",\n" +
      "      \"params\": {\n" +
      "        \"severity\": \"MINOR\",\n" +
      "        \"bar\": \"bar_value\",\n" +
      "        \"foo\": \"foo_value\"\n" +
      "      }\n" +
      "    }\n" +
      "  ]\n" +
      "}");
  }

  @Test
  public void find_changelog_by_profile_key() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization);
    RuleDefinitionDto rule = db.rules().insert();
    UserDto user = db.users().insertUser();
    insertChange(profile, ActiveRuleChange.Type.ACTIVATED, user,
      ImmutableMap.of(
        "ruleId", valueOf(rule.getId()),
        "severity", "MINOR"));

    String response = ws.newRequest()
      .setParam(PARAM_KEY, profile.getKee())
      .execute()
      .getInput();

    assertJson(response).isSimilarTo("{\n" +
      "  \"events\": [\n" +
      "    {\n" +
      "      \"date\": \"" + DATE + "\",\n" +
      "      \"authorLogin\": \"" + user.getLogin() + "\",\n" +
      "      \"action\": \"ACTIVATED\",\n" +
      "      \"ruleKey\": \"" + rule.getKey() + "\",\n" +
      "      \"ruleName\": \"" + rule.getName() + "\",\n" +
      "      \"params\": {\n" +
      "        \"severity\": \"MINOR\"\n" +
      "      }\n" +
      "    }\n" +
      "  ]\n" +
      "}");
  }

  @Test
  public void find_changelog_by_language_and_name() {
    QProfileDto qualityProfile = db.qualityProfiles().insert(db.getDefaultOrganization());
    RuleDefinitionDto rule = db.rules().insert();
    UserDto user = db.users().insertUser();
    insertChange(qualityProfile, ActiveRuleChange.Type.ACTIVATED, user,
      ImmutableMap.of(
        "ruleId", valueOf(rule.getId()),
        "severity", "MINOR"));

    String response = ws.newRequest()
      .setParam(PARAM_LANGUAGE, qualityProfile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, qualityProfile.getName())
      .execute()
      .getInput();

    assertJson(response).isSimilarTo("{\n" +
      "  \"events\": [\n" +
      "    {\n" +
      "      \"date\": \"" + DATE + "\",\n" +
      "      \"authorLogin\": \"" + user.getLogin() + "\",\n" +
      "      \"action\": \"ACTIVATED\",\n" +
      "      \"ruleKey\": \"" + rule.getKey() + "\",\n" +
      "      \"ruleName\": \"" + rule.getName() + "\",\n" +
      "      \"params\": {\n" +
      "        \"severity\": \"MINOR\"\n" +
      "      }\n" +
      "    }\n" +
      "  ]\n" +
      "}");
  }

  @Test
  public void find_changelog_by_organization_and_language_and_name() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto qualityProfile = db.qualityProfiles().insert(organization);
    RuleDefinitionDto rule = db.rules().insert();
    UserDto user = db.users().insertUser();
    insertChange(qualityProfile, ActiveRuleChange.Type.ACTIVATED, user,
      ImmutableMap.of(
        "ruleId", valueOf(rule.getId()),
        "severity", "MINOR"));

    String response = ws.newRequest()
      .setParam(PARAM_LANGUAGE, qualityProfile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, qualityProfile.getName())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute()
      .getInput();

    assertJson(response).isSimilarTo("{\n" +
      "  \"events\": [\n" +
      "    {\n" +
      "      \"date\": \"" + DATE + "\",\n" +
      "      \"authorLogin\": \"" + user.getLogin() + "\",\n" +
      "      \"action\": \"ACTIVATED\",\n" +
      "      \"ruleKey\": \"" + rule.getKey() + "\",\n" +
      "      \"ruleName\": \"" + rule.getName() + "\",\n" +
      "      \"params\": {\n" +
      "        \"severity\": \"MINOR\"\n" +
      "      }\n" +
      "    }\n" +
      "  ]\n" +
      "}");
  }

  @Test
  public void changelog_empty() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto qualityProfile = db.qualityProfiles().insert(organization);

    String response = ws.newRequest()
      .setParam(PARAM_KEY, qualityProfile.getKee())
      .execute()
      .getInput();

    assertJson(response).isSimilarTo("{\"total\":0,\"p\":1,\"ps\":50,\"events\":[]}");
  }

  @Test
  public void changelog_filter_by_since() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto qualityProfile = db.qualityProfiles().insert(organization);
    system2.setNow(DateUtils.parseDateTime("2011-04-25T01:15:42+0100").getTime());
    RuleDefinitionDto rule = db.rules().insert();
    UserDto user = db.users().insertUser();
    insertChange(qualityProfile, ActiveRuleChange.Type.ACTIVATED, user,
      ImmutableMap.of(
        "ruleId", valueOf(rule.getId()),
        "severity", "MINOR"));

    assertJson(ws.newRequest()
      .setParam(PARAM_KEY, qualityProfile.getKee())
      .setParam(PARAM_SINCE, "2011-04-25T01:15:42+0100")
      .execute()
      .getInput()).isSimilarTo("{\n" +
        "  \"events\": [\n" +
        "    {\n" +
        "      \"date\": \"2011-04-25T01:15:42+0100\",\n" +
        "      \"authorLogin\": \"" + user.getLogin() + "\",\n" +
        "      \"action\": \"ACTIVATED\",\n" +
        "      \"ruleKey\": \"" + rule.getKey() + "\",\n" +
        "      \"ruleName\": \"" + rule.getName() + "\",\n" +
        "    }\n" +
        "  ]\n" +
        "}");

    assertJson(ws.newRequest()
      .setParam(PARAM_KEY, qualityProfile.getKee())
      .setParam(PARAM_SINCE, "2011-04-25T01:15:43+0100")
      .execute()
      .getInput()).isSimilarTo("{\n" +
        "  \"events\": []\n" +
        "}");
  }

  @Test
  public void sort_changelog_events_in_reverse_chronological_order() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization);
    system2.setNow(DateUtils.parseDateTime("2011-04-25T01:15:42+0100").getTime());
    RuleDefinitionDto rule1 = db.rules().insert();
    insertChange(profile, ActiveRuleChange.Type.ACTIVATED, null,
      ImmutableMap.of(
        "ruleId", valueOf(rule1.getId()),
        "severity", "MINOR"));
    system2.setNow(DateUtils.parseDateTime("2011-04-25T01:15:43+0100").getTime());
    UserDto user = db.users().insertUser();
    RuleDefinitionDto rule2 = db.rules().insert();
    insertChange(profile, ActiveRuleChange.Type.DEACTIVATED, user,
      ImmutableMap.of("ruleId", valueOf(rule2.getId())));

    String response = ws.newRequest()
      .setParam(PARAM_KEY, profile.getKee())
      .execute()
      .getInput();

    assertJson(response).isSimilarTo("{\n" +
      "\"events\": [\n" +
      "    {\n" +
      "      \"date\": \"2011-04-25T02:15:43+0200\",\n" +
      "      \"action\": \"DEACTIVATED\",\n" +
      "      \"authorLogin\": \"" + user.getLogin() + "\",\n" +
      "      \"ruleKey\": \"" + rule2.getKey() + "\",\n" +
      "      \"ruleName\": \"" + rule2.getName() + "\",\n" +
      "      \"params\": {}\n" +
      "    },\n" +
      "    {\n" +
      "      \"date\": \"2011-04-25T02:15:42+0200\",\n" +
      "      \"action\": \"ACTIVATED\",\n" +
      "      \"ruleKey\": \"" + rule1.getKey() + "\",\n" +
      "      \"ruleName\": \"" + rule1.getName() + "\",\n" +
      "      \"params\": {\n" +
      "        \"severity\": \"MINOR\"\n" +
      "      }\n" +
      "    }\n" +
      "  ]" +
      "}");
  }

  @Test
  public void changelog_on_no_more_existing_rule() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto qualityProfile = db.qualityProfiles().insert(organization);
    UserDto user = db.users().insertUser();
    insertChange(qualityProfile, ActiveRuleChange.Type.ACTIVATED, user,
      ImmutableMap.of("ruleId", "123"));

    String response = ws.newRequest()
      .setParam(PARAM_LANGUAGE, qualityProfile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, qualityProfile.getName())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute()
      .getInput();

    assertJson(response).isSimilarTo("{\n" +
      "  \"events\": [\n" +
      "    {\n" +
      "      \"date\": \"" + DATE + "\",\n" +
      "      \"action\": \"ACTIVATED\",\n" +
      "      \"params\": {}\n" +
      "    }\n" +
      "  ]\n" +
      "}");
    assertThat(response).doesNotContain("ruleKey", "ruleName");
  }

  @Test
  public void changelog_on_no_more_existing_user() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto qualityProfile = db.qualityProfiles().insert(organization);
    RuleDefinitionDto rule = db.rules().insert();
    insertChange(c -> c.setRulesProfileUuid(qualityProfile.getRulesProfileUuid())
      .setUserUuid("UNKNOWN")
      .setChangeType(ActiveRuleChange.Type.ACTIVATED.name())
      .setData(ImmutableMap.of("ruleId", rule.getId())));

    String response = ws.newRequest()
      .setParam(PARAM_LANGUAGE, qualityProfile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, qualityProfile.getName())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute()
      .getInput();

    assertJson(response).isSimilarTo("{\n" +
      "  \"events\": [\n" +
      "    {\n" +
      "      \"date\": \"" + DATE + "\",\n" +
      "      \"ruleKey\": \"" + rule.getKey() + "\",\n" +
      "      \"ruleName\": \"" + rule.getName() + "\",\n" +
      "      \"action\": \"ACTIVATED\",\n" +
      "      \"params\": {}\n" +
      "    }\n" +
      "  ]\n" +
      "}");
    assertThat(response).doesNotContain("authorLogin", "authorName");
  }

  @Test
  public void changelog_on_paid_organization() {
    OrganizationDto organization = db.organizations().insert(o -> o.setSubscription(PAID));
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addMembership(organization);
    QProfileDto qualityProfile = db.qualityProfiles().insert(organization);
    RuleDefinitionDto rule = db.rules().insert();
    insertChange(qualityProfile, ActiveRuleChange.Type.ACTIVATED, db.users().insertUser(),
      ImmutableMap.of(
        "ruleId", valueOf(rule.getId()),
        "severity", "MINOR"));

    String response = ws.newRequest()
      .setParam(PARAM_LANGUAGE, qualityProfile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, qualityProfile.getName())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute()
      .getInput();

    assertJson(response).isSimilarTo("{\n" +
      "  \"events\": [\n" +
      "    {\n" +
      "      \"ruleKey\": \"" + rule.getKey() + "\",\n" +
      "    }\n" +
      "  ]\n" +
      "}");
  }

  @Test
  public void do_not_find_changelog_by_wrong_organization_and_language_and_name() {
    OrganizationDto organization1 = db.organizations().insert();
    OrganizationDto organization2 = db.organizations().insert();
    QProfileDto qualityProfile = db.qualityProfiles().insert(organization1);
    RuleDefinitionDto rule = db.rules().insert();
    UserDto user = db.users().insertUser();
    insertChange(qualityProfile, ActiveRuleChange.Type.ACTIVATED, user,
      ImmutableMap.of(
        "ruleId", valueOf(rule.getId()),
        "severity", "MINOR"));

    expectedException.expect(NotFoundException.class);

    ws.newRequest()
      .setParam(PARAM_LANGUAGE, qualityProfile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, qualityProfile.getName())
      .setParam(PARAM_ORGANIZATION, organization2.getKey())
      .execute();
  }

  @Test
  public void fail_on_paid_organization_when_not_member() {
    OrganizationDto organization = db.organizations().insert(o -> o.setSubscription(PAID));
    QProfileDto qualityProfile = db.qualityProfiles().insert(organization);

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage(format("You're not member of organization '%s'", organization.getKey()));

    ws.newRequest()
      .setParam(PARAM_LANGUAGE, qualityProfile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, qualityProfile.getName())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }

  @Test
  public void example() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization);
    String profileUuid = profile.getRulesProfileUuid();

    system2.setNow(DateUtils.parseDateTime("2015-02-23T17:58:39+0100").getTime());
    RuleDefinitionDto rule1 = db.rules().insert(RuleKey.of("squid", "S2438"), r -> r.setName("\"Threads\" should not be used where \"Runnables\" are expected"));
    UserDto user1 = db.users().insertUser(u -> u.setLogin("anakin.skywalker").setName("Anakin Skywalker"));
    insertChange(c -> c.setRulesProfileUuid(profileUuid)
      .setUserUuid(user1.getUuid())
      .setChangeType(ActiveRuleChange.Type.ACTIVATED.name())
      .setData(ImmutableMap.of("severity", "CRITICAL", "ruleId", valueOf(rule1.getId()))));

    system2.setNow(DateUtils.parseDateTime("2015-02-23T17:58:18+0100").getTime());
    RuleDefinitionDto rule2 = db.rules().insert(RuleKey.of("squid", "S2162"), r -> r.setName("\"equals\" methods should be symmetric and work for subclasses"));
    UserDto user2 = db.users().insertUser(u -> u.setLogin("padme.amidala").setName("Padme Amidala"));
    QProfileChangeDto change2 = insertChange(c -> c.setRulesProfileUuid(profileUuid)
      .setUserUuid(user2.getUuid())
      .setChangeType(ActiveRuleChange.Type.DEACTIVATED.name())
      .setData(ImmutableMap.of("ruleId", valueOf(rule2.getId()))));

    system2.setNow(DateUtils.parseDateTime("2014-09-12T15:20:46+0200").getTime());
    RuleDefinitionDto rule3 = db.rules().insert(RuleKey.of("squid", "S00101"), r -> r.setName("Class names should comply with a naming convention"));
    UserDto user3 = db.users().insertUser(u -> u.setLogin("obiwan.kenobi").setName("Obiwan Kenobi"));
    QProfileChangeDto change3 = insertChange(c -> c.setRulesProfileUuid(profileUuid)
      .setUserUuid(user3.getUuid())
      .setChangeType(ActiveRuleChange.Type.ACTIVATED.name())
      .setData(ImmutableMap.of("severity", "MAJOR", "param_format", "^[A-Z][a-zA-Z0-9]*$", "ruleId", valueOf(rule3.getId()))));

    String response = ws.newRequest()
      .setMethod("GET")
      .setParam(PARAM_KEY, profile.getKee())
      .setParam("ps", "10")
      .execute()
      .getInput();

    assertJson(response).isSimilarTo(getClass().getResource("changelog-example.json"));
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.isPost()).isFalse();
    assertThat(definition.responseExampleAsString()).isNotEmpty();
    assertThat(definition.params()).extracting(WebService.Param::key)
      .containsExactlyInAnyOrder("key", "qualityProfile", "language", "organization", "since", "to", "p", "ps");
    WebService.Param profileName = definition.param("qualityProfile");
    assertThat(profileName.deprecatedSince()).isNullOrEmpty();
    WebService.Param language = definition.param("language");
    assertThat(language.deprecatedSince()).isNullOrEmpty();
  }

  private QProfileChangeDto insertChange(QProfileDto profile, ActiveRuleChange.Type type, @Nullable UserDto user, @Nullable Map<String, Object> data) {
    return insertChange(c -> c.setRulesProfileUuid(profile.getRulesProfileUuid())
      .setUserUuid(user == null ? null : user.getUuid())
      .setChangeType(type.name())
      .setData(data));
  }

  @SafeVarargs
  private final QProfileChangeDto insertChange(Consumer<QProfileChangeDto>... consumers) {
    QProfileChangeDto dto = new QProfileChangeDto();
    Arrays.stream(consumers).forEach(c -> c.accept(dto));
    db.getDbClient().qProfileChangeDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }
}
