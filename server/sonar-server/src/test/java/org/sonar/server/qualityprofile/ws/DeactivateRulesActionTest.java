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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.rule.ws.RuleQueryFactory;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_TARGET_PROFILE;

public class DeactivateRulesActionTest {

  @Rule
  public DbTester dbTester = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private DbClient dbClient = dbTester.getDbClient();
  private RuleActivator ruleActivator = mock(RuleActivator.class);
  private QProfileWsSupport wsSupport = new QProfileWsSupport(dbClient, userSession, TestDefaultOrganizationProvider.from(dbTester));
  private RuleQueryFactory ruleQueryFactory = mock(RuleQueryFactory.class);
  private DeactivateRulesAction underTest = new DeactivateRulesAction(ruleQueryFactory, userSession, ruleActivator, wsSupport, dbClient);
  private WsActionTester wsActionTester = new WsActionTester(underTest);
  private OrganizationDto defaultOrganization;
  private OrganizationDto organization;

  @Before
  public void before() {
    defaultOrganization = dbTester.getDefaultOrganization();
    organization = dbTester.organizations().insert();
  }

  @Test
  public void define_bulk_deactivate_rule_action() {
    WebService.Action definition = wsActionTester.getDef();
    assertThat(definition).isNotNull();
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder(
      "types",
      "template_key",
      "languages",
      "is_template",
      "inheritance",
      "qprofile",
      "compareToProfile",
      "tags",
      "asc",
      "q",
      "active_severities",
      "s",
      "repositories",
      "targetProfile",
      "statuses",
      "rule_key",
      "available_since",
      "activation",
      "severities",
      "organization");
    WebService.Param targetProfile = definition.param("targetProfile");
    assertThat(targetProfile.deprecatedKey()).isEqualTo("profile_key");
  }

  @Test
  public void should_fail_if_not_logged_in() {
    TestRequest request = wsActionTester.newRequest()
      .setMethod("POST")
      .setParam(PARAM_TARGET_PROFILE, randomAlphanumeric(UUID_SIZE));

    thrown.expect(UnauthorizedException.class);
    request.execute();
  }

  @Test
  public void fail_if_built_in_profile() {
    userSession.logIn().addPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, defaultOrganization);
    QProfileDto qualityProfile = dbTester.qualityProfiles().insert(defaultOrganization, p -> p.setIsBuiltIn(true));
    TestRequest request = wsActionTester.newRequest()
      .setMethod("POST")
      .setParam(PARAM_TARGET_PROFILE, qualityProfile.getKee());

    thrown.expect(BadRequestException.class);

    request.execute();
  }

  @Test
  public void should_fail_if_not_organization_quality_profile_administrator() {
    userSession.logIn().addPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, defaultOrganization);
    QProfileDto qualityProfile = dbTester.qualityProfiles().insert(organization);
    TestRequest request = wsActionTester.newRequest()
      .setMethod("POST")
      .setParam(PARAM_TARGET_PROFILE, qualityProfile.getKee());

    thrown.expect(ForbiddenException.class);
    request.execute();
  }
}
