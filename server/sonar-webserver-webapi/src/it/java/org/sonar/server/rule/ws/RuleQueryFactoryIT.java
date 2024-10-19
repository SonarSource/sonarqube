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
package org.sonar.server.rule.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.ws.SimpleGetRequest;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsAction;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.api.rule.RuleStatus.DEPRECATED;
import static org.sonar.api.rule.RuleStatus.READY;
import static org.sonar.api.rule.Severity.CRITICAL;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rule.Severity.MINOR;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.api.server.ws.WebService.Param.ASCENDING;
import static org.sonar.api.server.ws.WebService.Param.SORT;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.db.qualityprofile.ActiveRuleDto.INHERITED;
import static org.sonar.db.qualityprofile.ActiveRuleDto.OVERRIDES;
import static org.sonar.server.rule.ws.RuleWsSupport.defineGenericRuleSearchParameters;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_ACTIVATION;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_ACTIVE_SEVERITIES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_AVAILABLE_SINCE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_COMPARE_TO_PROFILE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_INCLUDE_EXTERNAL;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_INHERITANCE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_IS_TEMPLATE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_LANGUAGES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_QPROFILE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_REPOSITORIES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_RULE_KEY;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_SEVERITIES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_STATUSES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_TAGS;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_TEMPLATE_KEY;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_TYPES;

public class RuleQueryFactoryIT {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private DbClient dbClient = db.getDbClient();

  private RuleQueryFactory underTest = new RuleQueryFactory(dbClient);

  private FakeAction fakeAction = new FakeAction(underTest);

  @Test
  public void create_empty_query() {
    RuleQuery result = execute();

    assertThat(result.getKey()).isNull();

    assertThat(result.getActivation()).isNull();
    assertThat(result.getActiveSeverities()).isNull();
    assertThat(result.isAscendingSort()).isTrue();
    assertThat(result.getAvailableSinceLong()).isNull();
    assertThat(result.getInheritance()).isNull();
    assertThat(result.includeExternal()).isFalse();
    assertThat(result.isTemplate()).isNull();
    assertThat(result.getLanguages()).isNull();
    assertThat(result.getQueryText()).isNull();
    assertThat(result.getQProfile()).isNull();
    assertThat(result.getRepositories()).isNull();
    assertThat(result.getRuleKey()).isNull();
    assertThat(result.getSeverities()).isNull();
    assertThat(result.getStatuses()).isEmpty();
    assertThat(result.getTags()).isNull();
    assertThat(result.templateKey()).isNull();
    assertThat(result.getTypes()).isEmpty();
    assertThat(result.getSortField()).isNull();
    assertThat(result.getCompareToQProfile()).isNull();
  }

  @Test
  public void create_rule_search_query() {
    QProfileDto qualityProfile = db.qualityProfiles().insert();
    QProfileDto compareToQualityProfile = db.qualityProfiles().insert();

    RuleQuery result = executeRuleSearchQuery(
      PARAM_RULE_KEY, "ruleKey",

      PARAM_ACTIVATION, "true",
      PARAM_ACTIVE_SEVERITIES, "MINOR,MAJOR",
      PARAM_AVAILABLE_SINCE, "2016-01-01",
      PARAM_INHERITANCE, "INHERITED,OVERRIDES",
      PARAM_IS_TEMPLATE, "true",
      PARAM_INCLUDE_EXTERNAL, "false",
      PARAM_LANGUAGES, "java,js",
      TEXT_QUERY, "S001",
      PARAM_QPROFILE, qualityProfile.getKee(),
      PARAM_COMPARE_TO_PROFILE, compareToQualityProfile.getKee(),
      PARAM_REPOSITORIES, "pmd,checkstyle",
      PARAM_SEVERITIES, "MINOR,CRITICAL",
      PARAM_STATUSES, "DEPRECATED,READY",
      PARAM_TAGS, "tag1,tag2",
      PARAM_TEMPLATE_KEY, "architectural",
      PARAM_TYPES, "CODE_SMELL,BUG",

      SORT, "updatedAt",
      ASCENDING, "false");

    assertResult(result, qualityProfile, compareToQualityProfile);
    assertThat(result.includeExternal()).isFalse();
  }

  @Test
  public void include_external_is_mandatory_for_rule_search_query() {
    db.qualityProfiles().insert();
    db.qualityProfiles().insert();
    Request request = new SimpleGetRequest();

    assertThatThrownBy(() -> {
      underTest.createRuleSearchQuery(db.getSession(), request);
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'include_external' parameter is missing");
  }

  @Test
  public void create_query() {
    QProfileDto qualityProfile = db.qualityProfiles().insert();
    QProfileDto compareToQualityProfile = db.qualityProfiles().insert();

    RuleQuery result = execute(
      PARAM_RULE_KEY, "ruleKey",

      PARAM_ACTIVATION, "true",
      PARAM_ACTIVE_SEVERITIES, "MINOR,MAJOR",
      PARAM_AVAILABLE_SINCE, "2016-01-01",
      PARAM_INHERITANCE, "INHERITED,OVERRIDES",
      PARAM_IS_TEMPLATE, "true",
      PARAM_INCLUDE_EXTERNAL, "true",
      PARAM_LANGUAGES, "java,js",
      TEXT_QUERY, "S001",
      PARAM_QPROFILE, qualityProfile.getKee(),
      PARAM_COMPARE_TO_PROFILE, compareToQualityProfile.getKee(),
      PARAM_REPOSITORIES, "pmd,checkstyle",
      PARAM_SEVERITIES, "MINOR,CRITICAL",
      PARAM_STATUSES, "DEPRECATED,READY",
      PARAM_TAGS, "tag1,tag2",
      PARAM_TEMPLATE_KEY, "architectural",
      PARAM_TYPES, "CODE_SMELL,BUG",

      SORT, "updatedAt",
      ASCENDING, "false");

    assertResult(result, qualityProfile, compareToQualityProfile);
    assertThat(result.includeExternal()).isFalse();
  }

  @Test
  public void use_quality_profiles_language_if_available() {
    QProfileDto qualityProfile = db.qualityProfiles().insert();
    String qualityProfileKey = qualityProfile.getKee();

    RuleQuery result = execute(
      PARAM_LANGUAGES, "specifiedLanguage",
      PARAM_ACTIVATION, "true",
      PARAM_QPROFILE, qualityProfileKey);

    assertThat(result.getLanguages()).containsExactly(qualityProfile.getLanguage());
  }

  @Test
  public void use_specified_languages_if_no_quality_profile_available() {
    RuleQuery result = execute(PARAM_LANGUAGES, "specifiedLanguage");

    assertThat(result.getLanguages()).containsExactly("specifiedLanguage");
  }

  @Test
  public void create_query_add_language_from_profile() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setName("Sonar way").setLanguage("xoo").setKee("sonar-way"));

    RuleQuery result = execute(
      PARAM_QPROFILE, profile.getKee(),
      PARAM_LANGUAGES, "java,js");

    assertThat(result.getQProfile().getKee()).isEqualTo(profile.getKee());
    assertThat(result.getLanguages()).containsOnly("xoo");
  }

  @Test
  public void filter_on_compare_to() {
    QProfileDto compareToProfile = db.qualityProfiles().insert();

    RuleQuery result = execute(
      PARAM_COMPARE_TO_PROFILE, compareToProfile.getKee());

    assertThat(result.getCompareToQProfile().getKee()).isEqualTo(compareToProfile.getKee());
  }

  public void fail_when_profile_does_not_exist() {
    assertThatThrownBy(() -> {
      execute(PARAM_QPROFILE, "unknown");
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("The specified qualityProfile 'unknown' does not exist");
  }

  @Test
  public void fail_when_compare_to_profile_does_not_exist() {
    QProfileDto qualityProfile = db.qualityProfiles().insert();

    assertThatThrownBy(() -> {
      execute(PARAM_QPROFILE, qualityProfile.getKee(),
        PARAM_COMPARE_TO_PROFILE, "unknown");
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("The specified qualityProfile 'unknown' does not exist");
  }

  private void assertResult(RuleQuery result, QProfileDto qualityProfile, QProfileDto compareToQualityProfile) {
    assertThat(result.getKey()).isEqualTo("ruleKey");

    assertThat(result.getActivation()).isTrue();
    assertThat(result.getActiveSeverities()).containsOnly(MINOR, MAJOR);
    assertThat(result.isAscendingSort()).isFalse();
    assertThat(result.getAvailableSinceLong()).isNotNull();
    assertThat(result.getInheritance()).containsOnly(INHERITED, OVERRIDES);
    assertThat(result.isTemplate()).isTrue();
    assertThat(result.getLanguages()).containsOnly(qualityProfile.getLanguage());
    assertThat(result.getQueryText()).isEqualTo("S001");
    assertThat(result.getQProfile().getKee()).isEqualTo(qualityProfile.getKee());
    assertThat(result.getCompareToQProfile().getKee()).isEqualTo(compareToQualityProfile.getKee());
    assertThat(result.getRepositories()).containsOnly("pmd", "checkstyle");
    assertThat(result.getRuleKey()).isNull();
    assertThat(result.getSeverities()).containsOnly(MINOR, CRITICAL);
    assertThat(result.getStatuses()).containsOnly(DEPRECATED, READY);
    assertThat(result.getTags()).containsOnly("tag1", "tag2");
    assertThat(result.templateKey()).isEqualTo("architectural");
    assertThat(result.getTypes()).containsOnly(BUG, CODE_SMELL);
    assertThat(result.getSortField()).isEqualTo("updatedAt");
  }

  private RuleQuery execute(String... paramsKeyAndValue) {
    WsActionTester ws = new WsActionTester(fakeAction);
    TestRequest request = ws.newRequest();
    for (int i = 0; i < paramsKeyAndValue.length; i += 2) {
      request.setParam(paramsKeyAndValue[i], paramsKeyAndValue[i + 1]);
    }
    request.execute();
    return fakeAction.getRuleQuery();
  }

  private RuleQuery executeRuleSearchQuery(String... paramsKeyAndValue) {
    SimpleGetRequest request = new SimpleGetRequest();
    for (int i = 0; i < paramsKeyAndValue.length; i += 2) {
      request.setParam(paramsKeyAndValue[i], paramsKeyAndValue[i + 1]);
    }

    return underTest.createRuleSearchQuery(db.getSession(), request);
  }

  private class FakeAction implements WsAction {

    private final RuleQueryFactory ruleQueryFactory;
    private RuleQuery ruleQuery;

    private FakeAction(RuleQueryFactory ruleQueryFactory) {
      this.ruleQueryFactory = ruleQueryFactory;
    }

    @Override
    public void define(WebService.NewController controller) {
      WebService.NewAction action = controller.createAction("fake")
        .setHandler(this);
      defineGenericRuleSearchParameters(action);
    }

    @Override
    public void handle(Request request, Response response) {
      ruleQuery = ruleQueryFactory.createRuleQuery(db.getSession(), request);
    }

    RuleQuery getRuleQuery() {
      return ruleQuery;
    }
  }
}
