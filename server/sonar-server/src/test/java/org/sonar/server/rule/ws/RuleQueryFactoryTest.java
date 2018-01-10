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
package org.sonar.server.rule.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsAction;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
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
import static org.sonar.server.rule.ws.SearchAction.defineRuleSearchParameters;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_ACTIVATION;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_ACTIVE_SEVERITIES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_AVAILABLE_SINCE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_COMPARE_TO_PROFILE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_INHERITANCE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_IS_TEMPLATE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_LANGUAGES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_ORGANIZATION;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_QPROFILE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_REPOSITORIES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_RULE_KEY;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_SEVERITIES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_STATUSES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_TAGS;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_TEMPLATE_KEY;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_TYPES;

public class RuleQueryFactoryTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = dbTester.getDbClient();

  private RuleQueryFactory underTest = new RuleQueryFactory(dbClient, new RuleWsSupport(dbClient, null, TestDefaultOrganizationProvider.from(dbTester)));

  private FakeAction fakeAction = new FakeAction(underTest);
  private OrganizationDto organization;

  @Before
  public void before() {
    organization = dbTester.organizations().insert();
  }

  @Test
  public void create_empty_query() {
    RuleQuery result = execute();

    assertThat(result.getKey()).isNull();

    assertThat(result.getActivation()).isNull();
    assertThat(result.getActiveSeverities()).isNull();
    assertThat(result.isAscendingSort()).isTrue();
    assertThat(result.getAvailableSinceLong()).isNull();
    assertThat(result.getInheritance()).isNull();
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
  public void create_query() {
    QProfileDto qualityProfile = dbTester.qualityProfiles().insert(organization);
    QProfileDto compareToQualityProfile = dbTester.qualityProfiles().insert(organization);

    RuleQuery result = execute(
      PARAM_RULE_KEY, "ruleKey",

      PARAM_ACTIVATION, "true",
      PARAM_ACTIVE_SEVERITIES, "MINOR,MAJOR",
      PARAM_AVAILABLE_SINCE, "2016-01-01",
      PARAM_INHERITANCE, "INHERITED,OVERRIDES",
      PARAM_IS_TEMPLATE, "true",
      PARAM_LANGUAGES, "java,js",
      TEXT_QUERY, "S001",
      PARAM_ORGANIZATION, organization.getKey(),
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
    assertThat(result.getOrganization().getUuid()).isEqualTo(organization.getUuid());
    assertThat(result.getRepositories()).containsOnly("pmd", "checkstyle");
    assertThat(result.getRuleKey()).isNull();
    assertThat(result.getSeverities()).containsOnly(MINOR, CRITICAL);
    assertThat(result.getStatuses()).containsOnly(DEPRECATED, READY);
    assertThat(result.getTags()).containsOnly("tag1", "tag2");
    assertThat(result.templateKey()).isEqualTo("architectural");
    assertThat(result.getTypes()).containsOnly(BUG, CODE_SMELL);

    assertThat(result.getSortField()).isEqualTo("updatedAt");
  }

  @Test
  public void use_quality_profiles_language_if_available() {
    QProfileDto qualityProfile = dbTester.qualityProfiles().insert(organization);
    String qualityProfileKey = qualityProfile.getKee();

    RuleQuery result = execute(
      PARAM_LANGUAGES, "specifiedLanguage",
      PARAM_ACTIVATION, "true",
      PARAM_QPROFILE, qualityProfileKey);

    assertThat(result.getLanguages()).containsExactly(qualityProfile.getLanguage());
  }

  @Test
  public void use_specified_languages_if_no_quality_profile_available() {
    RuleQuery result = execute(
      PARAM_LANGUAGES, "specifiedLanguage");

    assertThat(result.getLanguages()).containsExactly("specifiedLanguage");
  }

  @Test
  public void create_query_add_language_from_profile() {
    QProfileDto profile = dbTester.qualityProfiles().insert(organization, p -> p.setName("Sonar way").setLanguage("xoo").setKee("sonar-way"));

    RuleQuery result = execute(
      PARAM_QPROFILE, profile.getKee(),
      PARAM_LANGUAGES, "java,js");

    assertThat(result.getQProfile().getKee()).isEqualTo(profile.getKee());
    assertThat(result.getLanguages()).containsOnly("xoo");
  }

  @Test
  public void filter_on_quality_profiles_organization_if_searching_for_actives_with_no_organization_specified() {
    QProfileDto profile = dbTester.qualityProfiles().insert(organization, p -> p.setName("Sonar way").setLanguage("xoo").setKee("sonar-way"));

    RuleQuery result = execute(
      PARAM_ACTIVATION, "true",
      PARAM_QPROFILE, profile.getKee());

    assertThat(result.getOrganization().getUuid()).isEqualTo(organization.getUuid());
  }

  @Test
  public void filter_on_compare_to() {
    QProfileDto compareToProfile = dbTester.qualityProfiles().insert(organization);

    RuleQuery result = execute(
      PARAM_ORGANIZATION, organization.getKey(),
      PARAM_COMPARE_TO_PROFILE, compareToProfile.getKee());

    assertThat(result.getCompareToQProfile().getKee()).isEqualTo(compareToProfile.getKee());
  }

  @Test
  public void fail_if_organization_and_quality_profile_are_contradictory() {
    OrganizationDto organization1 = dbTester.organizations().insert();
    OrganizationDto organization2 = dbTester.organizations().insert();

    QProfileDto qualityProfile = dbTester.qualityProfiles().insert(organization1);

    String qualityProfileKey = qualityProfile.getKee();
    String organization2Key = organization2.getKey();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The specified quality profile '" + qualityProfileKey + "' is not part of the specified organization '" + organization2Key + "'");

    execute(PARAM_QPROFILE, qualityProfileKey,
      PARAM_ORGANIZATION, organization2Key);
  }

  @Test
  public void fail_if_organization_and_compare_to_quality_profile_are_contradictory() {
    OrganizationDto organization = dbTester.organizations().insert();
    QProfileDto qualityProfile = dbTester.qualityProfiles().insert(organization);

    OrganizationDto otherOrganization = dbTester.organizations().insert();
    QProfileDto compareToQualityProfile = dbTester.qualityProfiles().insert(otherOrganization);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The specified quality profile '" + compareToQualityProfile.getKee() + "' is not part of the specified organization '" + organization.getKey() + "'");

    execute(PARAM_QPROFILE, qualityProfile.getKee(),
      PARAM_COMPARE_TO_PROFILE, compareToQualityProfile.getKee(),
      PARAM_ORGANIZATION, organization.getKey());
  }

  @Test
  public void fail_when_organization_does_not_exist() {
    QProfileDto qualityProfile = dbTester.qualityProfiles().insert(organization);
    String qualityProfileKey = qualityProfile.getKee();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No organization with key 'unknown'");

    execute(PARAM_QPROFILE, qualityProfileKey,
      PARAM_ORGANIZATION, "unknown");
  }

  @Test
  public void fail_when_profile_does_not_exist() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("The specified qualityProfile 'unknown' does not exist");

    execute(PARAM_QPROFILE, "unknown");
  }

  @Test
  public void fail_when_compare_to_profile_does_not_exist() {
    QProfileDto qualityProfile = dbTester.qualityProfiles().insert(organization);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("The specified qualityProfile 'unknown' does not exist");

    execute(PARAM_QPROFILE, qualityProfile.getKee(),
      PARAM_COMPARE_TO_PROFILE, "unknown");
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
      defineRuleSearchParameters(action);
    }

    @Override
    public void handle(Request request, Response response) {
      ruleQuery = ruleQueryFactory.createRuleQuery(dbTester.getSession(), request);
    }

    RuleQuery getRuleQuery() {
      return ruleQuery;
    }
  }
}
