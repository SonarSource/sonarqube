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
package org.sonar.server.qualityprofile.ws;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.QProfileChangeDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.qualityprofile.ActiveRuleInheritance;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_QUALITY_PROFILE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_SINCE;

public class ChangelogActionTest {

  private static final String DATE = "2011-04-25T01:15:42+0100";

  private final TestSystem2 system2 = new TestSystem2().setNow(DateUtils.parseDateTime(DATE).getTime());

  @Rule
  public DbTester db = DbTester.create(system2);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final QProfileWsSupport wsSupport = new QProfileWsSupport(db.getDbClient(), userSession);
  private final WsActionTester ws = new WsActionTester(new ChangelogAction(wsSupport, new Languages(), db.getDbClient()));

  @Test
  public void return_change_with_all_fields() {
    QProfileDto profile = db.qualityProfiles().insert();
    UserDto user = db.users().insertUser();
    RuleDto rule = db.rules().insert(RuleKey.of("java", "S001"));
    insertChange(profile, ActiveRuleChange.Type.ACTIVATED, user, ImmutableMap.of(
      "ruleUuid", rule.getUuid(),
      "severity", "MINOR",
      "inheritance", ActiveRuleInheritance.INHERITED.name(),
      "param_foo", "foo_value",
      "param_bar", "bar_value"));

    String response = ws.newRequest()
      .setParam(PARAM_LANGUAGE, profile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .execute()
      .getInput();

    assertJson(response).isSimilarTo("{\n" +
      "  \"total\": 1,\n" +
      "  \"p\": 1,\n" +
      "  \"ps\": 50,\n" +
      "  \"paging\": {\n" +
      "     \"pageIndex\": 1,\n" +
      "     \"pageSize\": 50,\n" +
      "     \"total\": 1\n" +
      "  }," +
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
    QProfileDto profile = db.qualityProfiles().insert();
    RuleDto rule = db.rules().insert();
    UserDto user = db.users().insertUser();
    insertChange(profile, ActiveRuleChange.Type.ACTIVATED, user,
      ImmutableMap.of(
        "ruleUuid", rule.getUuid(),
        "severity", "MINOR"));

    String response = ws.newRequest()
      .setParam(PARAM_LANGUAGE, profile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
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
    QProfileDto qualityProfile = db.qualityProfiles().insert();
    RuleDto rule = db.rules().insert();
    UserDto user = db.users().insertUser();
    insertChange(qualityProfile, ActiveRuleChange.Type.ACTIVATED, user,
      ImmutableMap.of(
        "ruleUuid", rule.getUuid(),
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
  public void changelog_empty() {
    QProfileDto qualityProfile = db.qualityProfiles().insert();
    String response = ws.newRequest()
      .setParam(PARAM_LANGUAGE, qualityProfile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, qualityProfile.getName())
      .execute()
      .getInput();

    assertJson(response).isSimilarTo("{\"total\":0,\"p\":1,\"ps\":50,\"paging\":{\"pageIndex\":1,\"pageSize\":50,\"total\":0},\"events\":[]}");
  }

  @Test
  public void changelog_filter_by_since() {
    QProfileDto qualityProfile = db.qualityProfiles().insert();
    system2.setNow(DateUtils.parseDateTime("2011-04-25T01:15:42+0100").getTime());
    RuleDto rule = db.rules().insert();
    UserDto user = db.users().insertUser();
    insertChange(qualityProfile, ActiveRuleChange.Type.ACTIVATED, user,
      ImmutableMap.of(
        "ruleUuid", rule.getUuid(),
        "severity", "MINOR"));

    assertJson(ws.newRequest()
      .setParam(PARAM_LANGUAGE, qualityProfile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, qualityProfile.getName())
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
      .setParam(PARAM_LANGUAGE, qualityProfile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, qualityProfile.getName())
      .setParam(PARAM_SINCE, "2011-04-25T01:15:43+0100")
      .execute()
      .getInput()).isSimilarTo("{\n" +
        "  \"events\": []\n" +
        "}");
  }

  @Test
  public void sort_changelog_events_in_reverse_chronological_order() {
    QProfileDto profile = db.qualityProfiles().insert();
    system2.setNow(DateUtils.parseDateTime("2011-04-25T01:15:42+0100").getTime());
    RuleDto rule1 = db.rules().insert();
    insertChange(profile, ActiveRuleChange.Type.ACTIVATED, null,
      ImmutableMap.of(
        "ruleUuid", rule1.getUuid(),
        "severity", "MINOR"));
    system2.setNow(DateUtils.parseDateTime("2011-04-25T01:15:43+0100").getTime());
    UserDto user = db.users().insertUser();
    RuleDto rule2 = db.rules().insert();
    insertChange(profile, ActiveRuleChange.Type.DEACTIVATED, user,
      ImmutableMap.of("ruleUuid", rule2.getUuid()));

    String response = ws.newRequest()
      .setParam(PARAM_LANGUAGE, profile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
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
    QProfileDto qualityProfile = db.qualityProfiles().insert();
    UserDto user = db.users().insertUser();
    insertChange(qualityProfile, ActiveRuleChange.Type.ACTIVATED, user,
      ImmutableMap.of("ruleUuid", "123"));

    String response = ws.newRequest()
      .setParam(PARAM_LANGUAGE, qualityProfile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, qualityProfile.getName())
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
    QProfileDto qualityProfile = db.qualityProfiles().insert();
    RuleDto rule = db.rules().insert();
    insertChange(c -> c.setRulesProfileUuid(qualityProfile.getRulesProfileUuid())
      .setUserUuid("UNKNOWN")
      .setChangeType(ActiveRuleChange.Type.ACTIVATED.name())
      .setData(ImmutableMap.of("ruleUuid", rule.getUuid())));

    String response = ws.newRequest()
      .setParam(PARAM_LANGUAGE, qualityProfile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, qualityProfile.getName())
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
  public void changelog_example() {
    QProfileDto profile = db.qualityProfiles().insert();
    String profileUuid = profile.getRulesProfileUuid();

    system2.setNow(DateUtils.parseDateTime("2015-02-23T17:58:39+0100").getTime());
    RuleDto rule1 = db.rules().insert(RuleKey.of("java", "S2438"), r -> r.setName("\"Threads\" should not be used where \"Runnables\" are expected"));
    UserDto user1 = db.users().insertUser(u -> u.setLogin("anakin.skywalker").setName("Anakin Skywalker"));
    insertChange(c -> c.setRulesProfileUuid(profileUuid)
      .setUserUuid(user1.getUuid())
      .setChangeType(ActiveRuleChange.Type.ACTIVATED.name())
      .setData(ImmutableMap.of("severity", "CRITICAL", "ruleUuid", rule1.getUuid())));

    system2.setNow(DateUtils.parseDateTime("2015-02-23T17:58:18+0100").getTime());
    RuleDto rule2 = db.rules().insert(RuleKey.of("java", "S2162"), r -> r.setName("\"equals\" methods should be symmetric and work for subclasses"));
    UserDto user2 = db.users().insertUser(u -> u.setLogin("padme.amidala").setName("Padme Amidala"));
    insertChange(c -> c.setRulesProfileUuid(profileUuid)
      .setUserUuid(user2.getUuid())
      .setChangeType(ActiveRuleChange.Type.DEACTIVATED.name())
      .setData(ImmutableMap.of("ruleUuid", rule2.getUuid())));

    system2.setNow(DateUtils.parseDateTime("2014-09-12T15:20:46+0200").getTime());
    RuleDto rule3 = db.rules().insert(RuleKey.of("java", "S00101"), r -> r.setName("Class names should comply with a naming convention"));
    UserDto user3 = db.users().insertUser(u -> u.setLogin("obiwan.kenobi").setName("Obiwan Kenobi"));
    insertChange(c -> c.setRulesProfileUuid(profileUuid)
      .setUserUuid(user3.getUuid())
      .setChangeType(ActiveRuleChange.Type.ACTIVATED.name())
      .setData(ImmutableMap.of("severity", "MAJOR", "param_format", "^[A-Z][a-zA-Z0-9]*$", "ruleUuid", rule3.getUuid())));

    ws.newRequest()
      .setMethod("GET")
      .setParam(PARAM_LANGUAGE, profile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam("ps", "10")
      .execute()
      .assertJson(this.getClass(), "changelog_example.json");
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.isPost()).isFalse();
    assertThat(definition.responseExampleAsString()).isNotEmpty();
    assertThat(definition.params()).extracting(WebService.Param::key)
      .containsExactlyInAnyOrder("qualityProfile", "language", "since", "to", "p", "ps");
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
