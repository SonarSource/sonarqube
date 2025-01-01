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

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.qualityprofile.QProfileRules;
import org.sonar.server.rule.ws.RuleQueryFactory;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.db.permission.GlobalPermission.SCAN;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_TARGET_KEY;

public class DeactivateRulesActionIT {

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private DbClient dbClient = db.getDbClient();
  private QProfileRules qProfileRules = mock(QProfileRules.class, RETURNS_DEEP_STUBS);
  private final QProfileWsSupport wsSupport = new QProfileWsSupport(dbClient, userSession);
  private RuleQueryFactory ruleQueryFactory = mock(RuleQueryFactory.class, RETURNS_DEEP_STUBS);
  private DeactivateRulesAction underTest = new DeactivateRulesAction(ruleQueryFactory, userSession, qProfileRules, wsSupport, dbClient);
  private WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();
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
      "targetKey",
      "statuses",
      "rule_key",
      "available_since",
      "activation",
      "severities",
      "cwe",
      "owaspTop10",
      "owaspTop10-2021",
      "sansTop25",
      "sonarsourceSecurity",
      "cleanCodeAttributeCategories",
      "impactSoftwareQualities",
      "impactSeverities");
  }

  @Test
  public void as_global_admin() {
    UserDto user = db.users().insertUser();
    QProfileDto qualityProfile = db.qualityProfiles().insert();
    userSession.logIn(user).addPermission(ADMINISTER_QUALITY_PROFILES);

    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_TARGET_KEY, qualityProfile.getKee())
      .execute();

    verify(qProfileRules).bulkDeactivateAndCommit(any(), any(), any());
  }

  @Test
  public void as_qprofile_editor() {
    UserDto user = db.users().insertUser();
    GroupDto group = db.users().insertGroup();
    QProfileDto qualityProfile = db.qualityProfiles().insert();
    db.qualityProfiles().addGroupPermission(qualityProfile, group);
    userSession.logIn(user).setGroups(group);

    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_TARGET_KEY, qualityProfile.getKee())
      .execute();

    verify(qProfileRules).bulkDeactivateAndCommit(any(), any(), any());
  }

  @Test
  public void fail_if_not_logged_in() {
    TestRequest request = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_TARGET_KEY, secure().nextAlphanumeric(UUID_SIZE));

    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_if_built_in_profile() {
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES);
    QProfileDto qualityProfile = db.qualityProfiles().insert(p -> p.setIsBuiltIn(true));
    TestRequest request = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_TARGET_KEY, qualityProfile.getKee());

    assertThatThrownBy(request::execute)
      .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void fail_if_not_quality_profile_administrator() {
    userSession.logIn(db.users().insertUser()).addPermission(SCAN);
    QProfileDto qualityProfile = db.qualityProfiles().insert();
    TestRequest request = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_TARGET_KEY, qualityProfile.getKee());

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class);
  }
}
