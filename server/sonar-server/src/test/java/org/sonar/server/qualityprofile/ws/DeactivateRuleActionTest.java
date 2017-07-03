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

import java.net.HttpURLConnection;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROFILE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_RULE;

public class DeactivateRuleActionTest {
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = db.getDbClient();
  private RuleActivator ruleActivator = mock(RuleActivator.class);
  private QProfileWsSupport wsSupport = new QProfileWsSupport(dbClient, userSession, TestDefaultOrganizationProvider.from(db));
  private DeactivateRuleAction underTest = new DeactivateRuleAction(dbClient, ruleActivator, userSession, wsSupport);
  private WsActionTester wsActionTester = new WsActionTester(underTest);
  private OrganizationDto defaultOrganization;
  private OrganizationDto organization;

  @Before
  public void before() {
    defaultOrganization = db.getDefaultOrganization();
    organization = db.organizations().insert();
  }

  @Test
  public void define_deactivate_rule_action() {
    WebService.Action definition = wsActionTester.getDef();
    assertThat(definition).isNotNull();
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder("profile", "rule");
    WebService.Param profileKey = definition.param("profile");
    assertThat(profileKey.deprecatedKey()).isEqualTo("profile_key");
    WebService.Param ruleKey = definition.param("rule");
    assertThat(ruleKey.deprecatedKey()).isEqualTo("rule_key");
  }

  @Test
  public void should_fail_if_not_logged_in() {
    TestRequest request = wsActionTester.newRequest()
      .setMethod("POST")
      .setParam(PARAM_RULE, RuleTesting.newRuleDto().getKey().toString())
      .setParam(PARAM_PROFILE, randomAlphanumeric(UUID_SIZE));

    expectedException.expect(UnauthorizedException.class);

    request.execute();
  }

  @Test
  public void should_fail_if_not_organization_quality_profile_administrator() {
    userSession.logIn().addPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, defaultOrganization);
    QProfileDto qualityProfile = db.qualityProfiles().insert(organization);
    TestRequest request = wsActionTester.newRequest()
      .setMethod("POST")
      .setParam(PARAM_RULE, RuleTesting.newRuleDto().getKey().toString())
      .setParam(PARAM_PROFILE, qualityProfile.getKee());

    expectedException.expect(ForbiddenException.class);

    request.execute();
  }

  @Test
  public void fail_deactivate_if_built_in_profile() {
    userSession.logIn().addPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, defaultOrganization);

    QProfileDto qualityProfile = db.qualityProfiles().insert(defaultOrganization, profile -> profile.setIsBuiltIn(true));
    TestRequest request = wsActionTester.newRequest()
      .setMethod("POST")
      .setParam(PARAM_RULE, RuleTesting.newRuleDto().getKey().toString())
      .setParam(PARAM_PROFILE, qualityProfile.getKee());

    expectedException.expect(BadRequestException.class);

    request.execute();
  }

  @Test
  public void deactivate_rule_in_default_organization() {
    userSession.logIn().addPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, defaultOrganization);
    QProfileDto qualityProfile = db.qualityProfiles().insert(defaultOrganization);
    RuleKey ruleKey = RuleTesting.randomRuleKey();
    TestRequest request = wsActionTester.newRequest()
      .setMethod("POST")
      .setParam(PARAM_RULE, ruleKey.toString())
      .setParam(PARAM_PROFILE, qualityProfile.getKee());

    TestResponse response = request.execute();

    assertThat(response.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);
    ArgumentCaptor<RuleKey> ruleKeyCaptor = ArgumentCaptor.forClass(RuleKey.class);
    ArgumentCaptor<QProfileDto> qProfileDtoCaptor = ArgumentCaptor.forClass(QProfileDto.class);
    verify(ruleActivator).deactivateAndCommit(any(DbSession.class), qProfileDtoCaptor.capture(), ruleKeyCaptor.capture());
    assertThat(ruleKeyCaptor.getValue()).isEqualTo(ruleKey);
    assertThat(qProfileDtoCaptor.getValue().getKee()).isEqualTo(qualityProfile.getKee());
  }

  @Test
  public void deactivate_rule_in_specific_organization() {
    userSession.logIn().addPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, organization);
    QProfileDto qualityProfile = db.qualityProfiles().insert(organization);
    RuleKey ruleKey = RuleTesting.randomRuleKey();
    TestRequest request = wsActionTester.newRequest()
      .setMethod("POST")
      .setParam("organization", organization.getKey())
      .setParam(PARAM_RULE, ruleKey.toString())
      .setParam(PARAM_PROFILE, qualityProfile.getKee());

    TestResponse response = request.execute();

    assertThat(response.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);
    ArgumentCaptor<RuleKey> captor = ArgumentCaptor.forClass(RuleKey.class);
    ArgumentCaptor<QProfileDto> qProfileDtoCaptor = ArgumentCaptor.forClass(QProfileDto.class);
    verify(ruleActivator).deactivateAndCommit(any(DbSession.class), qProfileDtoCaptor.capture(), captor.capture());
    assertThat(captor.getValue()).isEqualTo(ruleKey);
    assertThat(qProfileDtoCaptor.getValue().getKee()).isEqualTo(qualityProfile.getKee());
  }
}
