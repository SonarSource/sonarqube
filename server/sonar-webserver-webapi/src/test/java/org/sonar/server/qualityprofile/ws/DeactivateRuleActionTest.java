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

import java.net.HttpURLConnection;
import java.util.Collection;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.QProfileRules;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_RULE;

public class DeactivateRuleActionTest {
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Captor
  public ArgumentCaptor<Collection<Integer>> ruleIdsCaptor;

  private DbClient dbClient = db.getDbClient();
  private QProfileRules qProfileRules = mock(QProfileRules.class);
  private QProfileWsSupport wsSupport = new QProfileWsSupport(dbClient, userSession, TestDefaultOrganizationProvider.from(db));
  private DeactivateRuleAction underTest = new DeactivateRuleAction(dbClient, qProfileRules, userSession, wsSupport);
  private WsActionTester ws = new WsActionTester(underTest);
  private OrganizationDto defaultOrganization;
  private OrganizationDto organization;

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);
    defaultOrganization = db.getDefaultOrganization();
    organization = db.organizations().insert();
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();
    assertThat(definition).isNotNull();
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder("key", "rule");
    WebService.Param profileKey = definition.param("key");
    assertThat(profileKey.deprecatedKey()).isEqualTo("profile_key");
    WebService.Param ruleKey = definition.param("rule");
    assertThat(ruleKey.deprecatedKey()).isEqualTo("rule_key");
  }

  @Test
  public void deactivate_rule_in_default_organization() {
    userSession.logIn(db.users().insertUser()).addPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, defaultOrganization);
    QProfileDto qualityProfile = db.qualityProfiles().insert(defaultOrganization);
    RuleDefinitionDto rule = db.rules().insert(RuleTesting.randomRuleKey());
    TestRequest request = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_RULE, rule.getKey().toString())
      .setParam(PARAM_KEY, qualityProfile.getKee());

    TestResponse response = request.execute();

    assertThat(response.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);
    ArgumentCaptor<QProfileDto> qProfileDtoCaptor = ArgumentCaptor.forClass(QProfileDto.class);
    verify(qProfileRules).deactivateAndCommit(any(DbSession.class), qProfileDtoCaptor.capture(), ruleIdsCaptor.capture());
    assertThat(ruleIdsCaptor.getValue()).containsExactly(rule.getId());
    assertThat(qProfileDtoCaptor.getValue().getKee()).isEqualTo(qualityProfile.getKee());
  }

  @Test
  public void deactivate_rule_in_specific_organization() {
    userSession.logIn(db.users().insertUser()).addPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, organization);
    QProfileDto qualityProfile = db.qualityProfiles().insert(organization);
    RuleDefinitionDto rule = db.rules().insert(RuleTesting.randomRuleKey());
    TestRequest request = ws.newRequest()
      .setMethod("POST")
      .setParam("organization", organization.getKey())
      .setParam(PARAM_RULE, rule.getKey().toString())
      .setParam(PARAM_KEY, qualityProfile.getKee());

    TestResponse response = request.execute();

    assertThat(response.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);
    ArgumentCaptor<QProfileDto> qProfileDtoCaptor = ArgumentCaptor.forClass(QProfileDto.class);
    verify(qProfileRules).deactivateAndCommit(any(DbSession.class), qProfileDtoCaptor.capture(), ruleIdsCaptor.capture());
    assertThat(ruleIdsCaptor.getValue()).containsExactly(rule.getId());
    assertThat(qProfileDtoCaptor.getValue().getKee()).isEqualTo(qualityProfile.getKee());
  }

  @Test
  public void as_qprofile_editor() {
    UserDto user = db.users().insertUser();
    QProfileDto qualityProfile = db.qualityProfiles().insert(organization);
    db.qualityProfiles().addUserPermission(qualityProfile, user);
    userSession.logIn(user);
    RuleKey ruleKey = RuleTesting.randomRuleKey();
    db.rules().insert(ruleKey);

    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_RULE, ruleKey.toString())
      .setParam(PARAM_KEY, qualityProfile.getKee())
      .execute();

    verify(qProfileRules).deactivateAndCommit(any(DbSession.class), any(QProfileDto.class), anyCollection());
  }

  @Test
  public void fail_if_not_logged_in() {
    TestRequest request = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_RULE, RuleTesting.newRule().getKey().toString())
      .setParam(PARAM_KEY, randomAlphanumeric(UUID_SIZE));

    expectedException.expect(UnauthorizedException.class);

    request.execute();
  }

  @Test
  public void fail_if_not_organization_quality_profile_administrator() {
    RuleDefinitionDto rule = db.rules().insert();
    userSession.logIn(db.users().insertUser()).addPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, defaultOrganization);
    QProfileDto qualityProfile = db.qualityProfiles().insert(organization);
    TestRequest request = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_RULE, rule.getKey().toString())
      .setParam(PARAM_KEY, qualityProfile.getKee());

    expectedException.expect(ForbiddenException.class);

    request.execute();
  }

  @Test
  public void fail_activate_external_rule() {
    userSession.logIn(db.users().insertUser()).addPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, defaultOrganization);
    QProfileDto qualityProfile = db.qualityProfiles().insert(defaultOrganization);
    RuleDefinitionDto rule = db.rules().insert(r -> r.setIsExternal(true));

    TestRequest request = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_RULE, rule.getKey().toString())
      .setParam(PARAM_KEY, qualityProfile.getKee());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(String.format("Operation forbidden for rule '%s' imported from an external rule engine.", rule.getKey()));

    request.execute();
  }

  @Test
  public void fail_deactivate_if_built_in_profile() {
    RuleDefinitionDto rule = db.rules().insert();
    userSession.logIn(db.users().insertUser()).addPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, defaultOrganization);

    QProfileDto qualityProfile = db.qualityProfiles().insert(defaultOrganization, profile -> profile.setIsBuiltIn(true));
    TestRequest request = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_RULE, rule.getKey().toString())
      .setParam(PARAM_KEY, qualityProfile.getKee());

    expectedException.expect(BadRequestException.class);

    request.execute();
  }
}
