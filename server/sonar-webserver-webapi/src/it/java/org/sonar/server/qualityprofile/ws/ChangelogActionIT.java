/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbTester;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.qualityprofile.QProfileChangeDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleChangeDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleImpactChangeDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.issue.impact.Severity.HIGH;
import static org.sonar.api.issue.impact.Severity.LOW;
import static org.sonar.api.issue.impact.Severity.MEDIUM;
import static org.sonar.api.issue.impact.SoftwareQuality.MAINTAINABILITY;
import static org.sonar.api.issue.impact.SoftwareQuality.RELIABILITY;
import static org.sonar.api.issue.impact.SoftwareQuality.SECURITY;
import static org.sonar.api.rules.CleanCodeAttribute.CLEAR;
import static org.sonar.api.rules.CleanCodeAttribute.COMPLETE;
import static org.sonar.api.rules.CleanCodeAttribute.FOCUSED;
import static org.sonar.api.rules.CleanCodeAttribute.TESTED;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_FILTER_MODE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_QUALITY_PROFILE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_SINCE;

class ChangelogActionIT {

  private static final String DATE = "2011-04-25T01:15:42+0100";

  private final TestSystem2 system2 = new TestSystem2().setNow(DateUtils.parseDateTime(DATE).getTime());

  @RegisterExtension
  private final DbTester db = DbTester.create(system2);
  @RegisterExtension
  private final UserSessionRule userSession = UserSessionRule.standalone();

  private final QProfileWsSupport wsSupport = new QProfileWsSupport(db.getDbClient(), userSession);
  private final WsActionTester ws = new WsActionTester(new ChangelogAction(wsSupport, new Languages(), db.getDbClient()));

  @Test
  void return_change_with_all_fields() {
    QProfileDto profile = db.qualityProfiles().insert();
    UserDto user = db.users().insertUser();
    RuleDto rule = db.rules().insert(RuleKey.of("java", "S001"));
    RuleChangeDto ruleChange = insertRuleChange(CLEAR, TESTED, rule.getUuid(),
      Set.of(new RuleImpactChangeDto(MAINTAINABILITY, SECURITY, HIGH, MEDIUM)));
    insertChange(profile, ActiveRuleChange.Type.ACTIVATED, user, ImmutableMap.of(
      "ruleUuid", rule.getUuid(),
      "severity", "MINOR",
      "prioritizedRule", "true",
      "param_foo", "foo_value",
      "param_bar", "bar_value"),
      ruleChange);

    String response = ws.newRequest()
      .setParam(PARAM_LANGUAGE, profile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .execute()
      .getInput();

    assertJson(response).isSimilarTo("""
      {
        "total": 1,
        "p": 1,
        "ps": 50,
        "paging": {
          "pageIndex": 1,
          "pageSize": 50,
          "total": 1
        },
        "events": [
          {
            "date": "%s",
            "action": "ACTIVATED",
            "authorLogin": "%s",
            "authorName": "%s",
            "ruleKey": "%s",
            "ruleName": "%s",
            "cleanCodeAttributeCategory": "INTENTIONAL",
            "impacts": [
              {
                "softwareQuality": "MAINTAINABILITY",
                "severity": "HIGH"
              }
            ],
            "params": {
              "severity": "MINOR",
              "prioritizedRule": "true",
              "foo": "foo_value",
              "bar": "bar_value",
              "oldCleanCodeAttribute": "CLEAR",
              "newCleanCodeAttribute": "TESTED",
              "oldCleanCodeAttributeCategory": "INTENTIONAL",
              "newCleanCodeAttributeCategory": "ADAPTABLE",
              "impactChanges": [
                {
                  "oldSoftwareQuality": "SECURITY",
                  "newSoftwareQuality": "MAINTAINABILITY",
                  "oldSeverity": "MEDIUM",
                  "newSeverity": "HIGH"
                }
              ]
            }
          }
        ]
      }
      """.formatted(DATE, user.getLogin(), user.getName(), rule.getKey(), rule.getName()));
  }

  @Test
  void call_shouldReturnRemovedOrAddedImpacts() {
    QProfileDto profile = db.qualityProfiles().insert();
    UserDto user = db.users().insertUser();
    RuleDto rule = db.rules().insert(RuleKey.of("java", "S001"));
    RuleChangeDto ruleChange = insertRuleChange(COMPLETE, FOCUSED, rule.getUuid(),
      Set.of(new RuleImpactChangeDto(MAINTAINABILITY, null, HIGH, null), new RuleImpactChangeDto(null, RELIABILITY, null, LOW)));
    insertChange(profile, ActiveRuleChange.Type.DEACTIVATED, user, ImmutableMap.of(
      "ruleUuid", rule.getUuid(),
      "severity", "MINOR",
      "param_foo", "foo_value",
      "param_bar", "bar_value"),
      ruleChange);

    String response = ws.newRequest()
      .setParam(PARAM_LANGUAGE, profile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .execute()
      .getInput();

    assertJson(response).isSimilarTo("""
      {
        "total": 1,
        "p": 1,
        "ps": 50,
        "paging": {
          "pageIndex": 1,
          "pageSize": 50,
          "total": 1
        },
        "events": [
          {
            "date": "%s",
            "action": "DEACTIVATED",
            "authorLogin": "%s",
            "authorName": "%s",
            "ruleKey": "%s",
            "ruleName": "%s",
            "cleanCodeAttributeCategory": "INTENTIONAL",
            "impacts": [
              {
                "softwareQuality": "MAINTAINABILITY",
                "severity": "HIGH"
              }
            ],
            "params": {
              "severity": "MINOR",
              "foo": "foo_value",
              "bar": "bar_value",
              "oldCleanCodeAttribute": "COMPLETE",
              "newCleanCodeAttribute": "FOCUSED",
              "oldCleanCodeAttributeCategory": "INTENTIONAL",
              "newCleanCodeAttributeCategory": "ADAPTABLE",
              "impactChanges": [
               {
                 "newSoftwareQuality": "MAINTAINABILITY",
                 "newSeverity": "HIGH"
               },
               {
                 "oldSoftwareQuality": "RELIABILITY",
                 "oldSeverity": "LOW"
               }
             ]
            }
          }
        ]
      }
      """.formatted(DATE, user.getLogin(), user.getName(), rule.getKey(), rule.getName()));
  }

  @Test
  void call_whenNoChangeData_shouldIncludeRuleUuid() {
    String ruleUuid = "ruleUuid";
    QProfileDto profile = db.qualityProfiles().insert();
    UserDto user = db.users().insertUser();
    RuleDto rule = db.rules().insert(RuleKey.of("java", ruleUuid));
    RuleChangeDto ruleChange = insertRuleChange(COMPLETE, FOCUSED, rule.getUuid(), Set.of());
    insertChange(profile, ActiveRuleChange.Type.DEACTIVATED, user, Map.of(), ruleChange);

    String response = ws.newRequest()
      .setParam(PARAM_LANGUAGE, profile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .execute()
      .getInput();

    assertThat(response).contains(ruleUuid);
  }

  @Test
  void find_changelog_by_profile_key() {
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
  void find_changelog_by_language_and_name() {
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
  void changelog_empty() {
    QProfileDto qualityProfile = db.qualityProfiles().insert();
    String response = ws.newRequest()
      .setParam(PARAM_LANGUAGE, qualityProfile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, qualityProfile.getName())
      .execute()
      .getInput();

    assertJson(response).isSimilarTo("{\"total\":0,\"p\":1,\"ps\":50,\"paging\":{\"pageIndex\":1,\"pageSize\":50,\"total\":0},\"events\":[]}");
  }

  @Test
  void changelog_filter_by_since() {
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
      .getInput()).isSimilarTo("""
        {
          "events": []
        }""");
  }

  @Test
  void sort_changelog_events_in_reverse_chronological_order() {
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
  void changelog_on_no_more_existing_rule() {
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
  void changelog_on_no_more_existing_user() {
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
  void changelog_filter_by_STANDARD_mode() {
    QProfileDto qualityProfile = db.qualityProfiles().insert();
    system2.setNow(DateUtils.parseDateTime(DATE).getTime());
    RuleDto rule = db.rules().insert();
    UserDto user = db.users().insertUser();
    // ACTIVATED and DEACTIVATED rules must always appear
    insertChange(qualityProfile, ActiveRuleChange.Type.ACTIVATED, user, Map.of("ruleUuid", rule.getUuid()));
    insertChange(qualityProfile, ActiveRuleChange.Type.DEACTIVATED, user, Map.of("ruleUuid", rule.getUuid()));
    // Changes with data must appear in STANDARD mode
    insertChange(qualityProfile, ActiveRuleChange.Type.UPDATED, user, Map.of("severity", "BLOCKER", "ruleUuid", rule.getUuid()));
    // Changes without data must not appear in STANDARD mode
    RuleChangeDto ruleChange = insertRuleChange(TESTED, CLEAR, rule.getUuid(),
      Set.of(new RuleImpactChangeDto(MAINTAINABILITY, SECURITY, HIGH, MEDIUM), new RuleImpactChangeDto(null, RELIABILITY, null, LOW)));
    insertChange(qualityProfile, ActiveRuleChange.Type.UPDATED, user, null, ruleChange);

    String response = ws.newRequest()
      .setParam(PARAM_LANGUAGE, qualityProfile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, qualityProfile.getName())
      .setParam(PARAM_FILTER_MODE, QProfileChangelogFilterMode.STANDARD.name())
      .execute()
      .getInput();

    assertJson(response).isSimilarTo("""
      {
        "total": 3,
        "p": 1,
        "ps": 50,
        "paging": {
          "pageIndex": 1,
          "pageSize": 50,
          "total": 3
        },
        "events": [
          {
            "date": "%s",
            "sonarQubeVersion": "7.6",
            "action": "ACTIVATED",
            "authorLogin": "%s",
            "authorName": "%s",
            "ruleKey": "%s",
            "ruleName": "%s",
            "cleanCodeAttributeCategory": "INTENTIONAL",
            "impacts": [
              {
                "softwareQuality": "MAINTAINABILITY",
                "severity": "HIGH"
              }
            ],
            "params": {}
          },
          {
            "date": "%s",
            "sonarQubeVersion": "7.6",
            "action": "DEACTIVATED",
            "authorLogin": "%s",
            "authorName": "%s",
            "ruleKey": "%s",
            "ruleName": "%s",
            "cleanCodeAttributeCategory": "INTENTIONAL",
            "impacts": [
              {
                "softwareQuality": "MAINTAINABILITY",
                "severity": "HIGH"
              }
            ],
            "params": {}
          },
          {
            "date": "%s",
            "sonarQubeVersion": "7.6",
            "action": "UPDATED",
            "authorLogin": "%s",
            "authorName": "%s",
            "ruleKey": "%s",
            "ruleName": "%s",
            "cleanCodeAttributeCategory": "INTENTIONAL",
            "impacts": [
              {
                "softwareQuality": "MAINTAINABILITY",
                "severity": "HIGH"
              }
            ],
            "params": {
              "severity": "BLOCKER"
            }
          }
        ]
      }
      """.formatted(
      DATE, user.getLogin(), user.getName(), rule.getKey(), rule.getName(),
      DATE, user.getLogin(), user.getName(), rule.getKey(), rule.getName(),
      DATE, user.getLogin(), user.getName(), rule.getKey(), rule.getName()));
  }

  @Test
  void changelog_filter_by_MQR_mode() {
    QProfileDto qualityProfile = db.qualityProfiles().insert();
    system2.setNow(DateUtils.parseDateTime(DATE).getTime());
    RuleDto rule = db.rules().insert();
    UserDto user = db.users().insertUser();
    // ACTIVATED and DEACTIVATED rules must always appear
    insertChange(qualityProfile, ActiveRuleChange.Type.ACTIVATED, user, Map.of("ruleUuid", rule.getUuid()));
    insertChange(qualityProfile, ActiveRuleChange.Type.DEACTIVATED, user, Map.of("ruleUuid", rule.getUuid()));
    // Changes without rule_change must not appear in MQR mode
    insertChange(qualityProfile, ActiveRuleChange.Type.UPDATED, user, Map.of("severity", "BLOCKER", "ruleUuid", rule.getUuid()));
    // Changes with param changes must appear in MQR mode
    insertChange(qualityProfile, ActiveRuleChange.Type.UPDATED, user, Map.of("param_format", "^[A-Z][a-zA-Z0-9]*", "ruleUuid",
      rule.getUuid()));
    // Changes with rule_change must appear in MQR mode
    RuleChangeDto ruleChange = insertRuleChange(TESTED, CLEAR, rule.getUuid(),
      Set.of(new RuleImpactChangeDto(MAINTAINABILITY, SECURITY, HIGH, MEDIUM), new RuleImpactChangeDto(null, RELIABILITY, null, LOW)));
    insertChange(qualityProfile, ActiveRuleChange.Type.UPDATED, user, null, ruleChange);

    String response = ws.newRequest()
      .setParam(PARAM_LANGUAGE, qualityProfile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, qualityProfile.getName())
      .setParam(PARAM_FILTER_MODE, QProfileChangelogFilterMode.MQR.name())
      .execute()
      .getInput();

    assertJson(response).isSimilarTo("""
      {
        "total": 4,
        "p": 1,
        "ps": 50,
        "paging": {
          "pageIndex": 1,
          "pageSize": 50,
          "total": 4
        },
        "events": [
          {
            "date": "%s",
            "sonarQubeVersion": "7.6",
            "action": "ACTIVATED",
            "authorLogin": "%s",
            "authorName": "%s",
            "ruleKey": "%s",
            "ruleName": "%s",
            "cleanCodeAttributeCategory": "INTENTIONAL",
            "impacts": [
              {
                "softwareQuality": "MAINTAINABILITY",
                "severity": "HIGH"
              }
            ],
            "params": {}
          },
          {
            "date": "%s",
            "sonarQubeVersion": "7.6",
            "action": "DEACTIVATED",
            "authorLogin": "%s",
            "authorName": "%s",
            "ruleKey": "%s",
            "ruleName": "%s",
            "cleanCodeAttributeCategory": "INTENTIONAL",
            "impacts": [
              {
                "softwareQuality": "MAINTAINABILITY",
                "severity": "HIGH"
              }
            ],
            "params": {}
          },
          {
            "date": "%s",
            "sonarQubeVersion": "7.6",
            "action": "UPDATED",
            "authorLogin": "%s",
            "authorName": "%s",
            "ruleKey": "%s",
            "ruleName": "%s",
            "cleanCodeAttributeCategory": "INTENTIONAL",
            "impacts": [
              {
                "softwareQuality": "MAINTAINABILITY",
                "severity": "HIGH"
              }
            ],
            "params": {
              "format": "^[A-Z][a-zA-Z0-9]*"
            }
          },
          {
            "date": "%s",
            "sonarQubeVersion": "7.6",
            "action": "UPDATED",
            "authorLogin": "%s",
            "authorName": "%s",
            "ruleKey": "%s",
            "ruleName": "%s",
            "cleanCodeAttributeCategory": "INTENTIONAL",
            "impacts": [
              {
                "softwareQuality": "MAINTAINABILITY",
                "severity": "HIGH"
              }
            ],
            "params": {
              "oldCleanCodeAttribute": "TESTED",
              "newCleanCodeAttribute": "CLEAR",
              "oldCleanCodeAttributeCategory": "ADAPTABLE",
              "newCleanCodeAttributeCategory": "INTENTIONAL",
              "impactChanges": [
                {
                  "oldSoftwareQuality": "SECURITY",
                  "newSoftwareQuality": "MAINTAINABILITY",
                  "oldSeverity": "MEDIUM",
                  "newSeverity": "HIGH"
                },
                {
                  "oldSoftwareQuality": "RELIABILITY",
                  "oldSeverity": "LOW"
                }
              ]
            }
          }
        ]
      }
      """.formatted(DATE, user.getLogin(), user.getName(), rule.getKey(), rule.getName(),
      DATE, user.getLogin(), user.getName(), rule.getKey(), rule.getName(),
      DATE, user.getLogin(), user.getName(), rule.getKey(), rule.getName(),
      DATE, user.getLogin(), user.getName(), rule.getKey(), rule.getName()));
  }

  @Test
  void changelog_example() {
    QProfileDto profile = db.qualityProfiles().insert();
    String profileUuid = profile.getRulesProfileUuid();

    system2.setNow(DateUtils.parseDateTime("2015-02-23T17:58:39+0100").getTime());
    RuleDto rule1 = db.rules().insert(RuleKey.of("java", "S2438"), r -> r.setName("\"Threads\" should not be used where \"Runnables\" are expected")
      .addDefaultImpact(new ImpactDto(SECURITY, LOW)));
    UserDto user1 = db.users().insertUser(u -> u.setLogin("anakin.skywalker").setName("Anakin Skywalker"));
    insertChange(c -> c.setRulesProfileUuid(profileUuid)
      .setUserUuid(user1.getUuid())
      .setSqVersion("8.3.1")
      .setChangeType(ActiveRuleChange.Type.ACTIVATED.name())
      .setData(ImmutableMap.of("severity", "CRITICAL", "prioritizedRule", "true", "ruleUuid", rule1.getUuid())));

    system2.setNow(DateUtils.parseDateTime("2015-02-23T17:58:18+0100").getTime());
    RuleDto rule2 = db.rules().insert(RuleKey.of("java", "S2162"), r -> r.setName("\"equals\" methods should be symmetric and work for subclasses"));
    UserDto user2 = db.users().insertUser(u -> u.setLogin("padme.amidala").setName("Padme Amidala"));
    insertChange(c -> c.setRulesProfileUuid(profileUuid)
      .setUserUuid(user2.getUuid())
      .setSqVersion("8.3.1")
      .setChangeType(ActiveRuleChange.Type.DEACTIVATED.name())
      .setData(ImmutableMap.of("ruleUuid", rule2.getUuid())));

    system2.setNow(DateUtils.parseDateTime("2014-09-12T15:20:46+0200").getTime());
    RuleDto rule3 = db.rules().insert(RuleKey.of("java", "S00101"), r -> r.setName("Class names should comply with a naming convention"));
    UserDto user3 = db.users().insertUser(u -> u.setLogin("obiwan.kenobi").setName("Obiwan Kenobi"));
    RuleChangeDto ruleChange = insertRuleChange(TESTED, CLEAR, rule3.getUuid(),
      Set.of(new RuleImpactChangeDto(MAINTAINABILITY, SECURITY, HIGH, MEDIUM), new RuleImpactChangeDto(null, RELIABILITY, null, LOW)));
    insertChange(profile, ActiveRuleChange.Type.ACTIVATED, user3,
      ImmutableMap.of("severity", "MAJOR", "prioritizedRule", "false", "param_format", "^[A-Z][a-zA-Z0-9]*", "ruleUuid", rule3.getUuid())
      , ruleChange);

    ws.newRequest()
      .setMethod("GET")
      .setParam(PARAM_LANGUAGE, profile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam("ps", "10")
      .execute()
      .assertJson(Objects.requireNonNull(ws.getDef().responseExampleAsString()));
  }

  @Test
  void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.isPost()).isFalse();
    assertThat(definition.responseExampleAsString()).isNotEmpty();
    assertThat(definition.params()).extracting(WebService.Param::key)
      .containsExactlyInAnyOrder("qualityProfile", "language", "filterMode", "since", "to", "p", "ps");
    WebService.Param profileName = definition.param("qualityProfile");
    assertThat(profileName.deprecatedSince()).isNullOrEmpty();
    WebService.Param language = definition.param("language");
    assertThat(language.deprecatedSince()).isNullOrEmpty();
  }

  private void insertChange(QProfileDto profile, ActiveRuleChange.Type type, @Nullable UserDto user, @Nullable Map<String, Object> data) {
    insertChange(profile, type, user, data, null);
  }

  private void insertChange(QProfileDto profile, ActiveRuleChange.Type type, @Nullable UserDto user, @Nullable Map<String, Object> data,
    @Nullable RuleChangeDto ruleChange) {
    insertChange(c -> c.setRulesProfileUuid(profile.getRulesProfileUuid())
      .setUserUuid(user == null ? null : user.getUuid())
      .setSqVersion("7.6")
      .setChangeType(type.name())
      .setData(data)
      .setRuleChange(ruleChange));
  }

  private RuleChangeDto insertRuleChange(CleanCodeAttribute oldAttribute, CleanCodeAttribute newAttribute, String ruleUuid, Set<RuleImpactChangeDto> impactChanges) {
    RuleChangeDto ruleChange = new RuleChangeDto();
    ruleChange.setUuid(Uuids.createFast());
    ruleChange.setOldCleanCodeAttribute(oldAttribute);
    ruleChange.setNewCleanCodeAttribute(newAttribute);
    ruleChange.setRuleUuid(ruleUuid);

    impactChanges.forEach(impact -> impact.setRuleChangeUuid(ruleChange.getUuid()));
    ruleChange.setRuleImpactChanges(impactChanges);

    db.getDbClient().ruleChangeDao().insert(db.getSession(), ruleChange);
    return ruleChange;
  }

  @SafeVarargs
  private void insertChange(Consumer<QProfileChangeDto>... consumers) {
    QProfileChangeDto dto = new QProfileChangeDto();
    Arrays.stream(consumers).forEach(c -> c.accept(dto));
    db.getDbClient().qProfileChangeDao().insert(db.getSession(), dto);
    db.commit();
  }
}
