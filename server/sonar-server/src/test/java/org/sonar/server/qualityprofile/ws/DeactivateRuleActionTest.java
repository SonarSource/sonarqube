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

public class DeactivateRuleActionTest {

//  @Rule
//  public DbTester db = DbTester.create();
//  @Rule
//  public UserSessionRule userSession = UserSessionRule.standalone();
//  @Rule
//  public ExpectedException thrown = ExpectedException.none();
//
//  private DbClient dbClient = db.getDbClient();
//  private RuleActivator ruleActivator = mock(RuleActivator.class);
//  private QProfileWsSupport wsSupport = new QProfileWsSupport(dbClient, userSession, TestDefaultOrganizationProvider.from(db));
//  private DeactivateRuleAction underTest = new DeactivateRuleAction(dbClient, ruleActivator, userSession, wsSupport);
//  private WsActionTester wsActionTester = new WsActionTester(underTest);
//  private OrganizationDto defaultOrganization;
//  private OrganizationDto organization;
//
//  @Before
//  public void before() {
//    defaultOrganization = db.getDefaultOrganization();
//    organization = db.organizations().insert();
//  }
//
//  @Test
//  public void define_deactivate_rule_action() {
//    WebService.Action definition = wsActionTester.getDef();
//    assertThat(definition).isNotNull();
//    assertThat(definition.isPost()).isTrue();
//    assertThat(definition.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder("profile_key", "rule_key");
//  }
//
//  @Test
//  public void should_fail_if_not_logged_in() {
//    TestRequest request = wsActionTester.newRequest()
//      .setMethod("POST")
//      .setParam("rule_key", RuleTesting.newRuleDto().getKey().toString())
//      .setParam("profile_key", randomAlphanumeric(UUID_SIZE));
//
//    thrown.expect(UnauthorizedException.class);
//    request.execute();
//  }
//
//  @Test
//  public void should_fail_if_not_organization_quality_profile_administrator() {
//    userSession.logIn().addPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, defaultOrganization);
//    QProfileDto qualityProfile = db.qualityProfiles().insert(organization);
//    TestRequest request = wsActionTester.newRequest()
//      .setMethod("POST")
//      .setParam("rule_key", RuleTesting.newRuleDto().getKey().toString())
//      .setParam("profile_key", qualityProfile.getKee());
//
//    thrown.expect(ForbiddenException.class);
//    request.execute();
//  }
//
//  @Test
//  public void fail_deactivate_if_built_in_profile() {
//    userSession.logIn().addPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, defaultOrganization);
//
//    QProfileDto qualityProfile = db.qualityProfiles().insert(defaultOrganization, profile -> profile.setIsBuiltIn(true));
//    TestRequest request = wsActionTester.newRequest()
//      .setMethod("POST")
//      .setParam("rule_key", RuleTesting.newRuleDto().getKey().toString())
//      .setParam("profile_key", qualityProfile.getKee());
//
//    thrown.expect(BadRequestException.class);
//
//    request.execute();
//  }
//
//  @Test
//  public void deactivate_rule_in_default_organization() {
//    userSession.logIn().addPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, defaultOrganization);
//    QProfileDto qualityProfile = db.qualityProfiles().insert(defaultOrganization);
//    RuleKey ruleKey = RuleTesting.randomRuleKey();
//    TestRequest request = wsActionTester.newRequest()
//      .setMethod("POST")
//      .setParam("rule_key", ruleKey.toString())
//      .setParam("profile_key", qualityProfile.getKee());
//
//    TestResponse response = request.execute();
//
//    assertThat(response.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);
//    ArgumentCaptor<ActiveRuleKey> captor = ArgumentCaptor.forClass(ActiveRuleKey.class);
//    Mockito.verify(ruleActivator).deactivateAndUpdateIndex(any(DbSession.class), captor.capture());
//    assertThat(captor.getValue().getRuleKey()).isEqualTo(ruleKey);
//    assertThat(captor.getValue().getRuleProfileUuid()).isEqualTo(qualityProfile.getKee());
//  }
//
//  @Test
//  public void deactivate_rule_in_specific_organization() {
//    userSession.logIn().addPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, organization);
//    QProfileDto qualityProfile = db.qualityProfiles().insert(organization);
//    RuleKey ruleKey = RuleTesting.randomRuleKey();
//    TestRequest request = wsActionTester.newRequest()
//      .setMethod("POST")
//      .setParam("organization", organization.getKey())
//      .setParam("rule_key", ruleKey.toString())
//      .setParam("profile_key", qualityProfile.getKee());
//
//    TestResponse response = request.execute();
//
//    assertThat(response.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);
//    ArgumentCaptor<ActiveRuleKey> captor = ArgumentCaptor.forClass(ActiveRuleKey.class);
//    Mockito.verify(ruleActivator).deactivateAndUpdateIndex(any(DbSession.class), captor.capture());
//    assertThat(captor.getValue().getRuleKey()).isEqualTo(ruleKey);
//    assertThat(captor.getValue().getRuleProfileUuid()).isEqualTo(qualityProfile.getKee());
//  }
}
