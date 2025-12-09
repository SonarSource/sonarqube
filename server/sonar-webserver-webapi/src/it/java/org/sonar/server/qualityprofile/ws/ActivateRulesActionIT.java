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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_TARGET_KEY;

class ActivateRulesActionIT {

  @RegisterExtension
  public DbTester db = DbTester.create();
  @RegisterExtension
  public UserSessionRule userSession = UserSessionRule.standalone();

  private DbClient dbClient = db.getDbClient();
  private final QProfileWsSupport wsSupport = new QProfileWsSupport(dbClient, userSession);
  private RuleQueryFactory ruleQueryFactory = mock(RuleQueryFactory.class, Mockito.RETURNS_MOCKS);

  private QProfileRules qProfileRules = mock(QProfileRules.class, Mockito.RETURNS_DEEP_STUBS);
  private WsActionTester ws = new WsActionTester(new ActivateRulesAction(ruleQueryFactory, userSession, qProfileRules, wsSupport,
    dbClient));

  @Test
  void define_bulk_activate_rule_action() {
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
      "targetSeverity",
      "tags",
      "asc",
      "q",
      "active_severities",
      "prioritizedRule",
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
      "owaspMobileTop10-2024",
      "sansTop25",
      "sonarsourceSecurity",
      "cleanCodeAttributeCategories",
      "impactSoftwareQualities",
      "impactSeverities",
      "active_impactSeverities");
  }

  @Test
  void as_global_qprofile_admin() {
    userSession.logIn(db.users().insertUser()).addPermission(ADMINISTER_QUALITY_PROFILES);
    QProfileDto qualityProfile = db.qualityProfiles().insert();

    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_TARGET_KEY, qualityProfile.getKee())
      .execute();

    verify(qProfileRules).bulkActivateAndCommit(any(), any(), any(), any(), any());
  }

  @Test
  void as_qprofile_editor() {
    UserDto user = db.users().insertUser();
    GroupDto group = db.users().insertGroup();
    QProfileDto qualityProfile = db.qualityProfiles().insert();
    db.qualityProfiles().addGroupPermission(qualityProfile, group);
    userSession.logIn(user).setGroups(group);

    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_TARGET_KEY, qualityProfile.getKee())
      .execute();

    verify(qProfileRules).bulkActivateAndCommit(any(), any(), any(), any(), any());
  }

  @Test
  void fail_if_not_logged_in() {
    TestRequest request = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_TARGET_KEY, secure().nextAlphanumeric(UUID_SIZE));

    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  void fail_if_built_in_profile() {
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES);
    QProfileDto qualityProfile = db.qualityProfiles().insert(p -> p.setIsBuiltIn(true));
    TestRequest request = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_TARGET_KEY, qualityProfile.getKee());

    assertThatThrownBy(request::execute)
      .isInstanceOf(BadRequestException.class);
  }

  @Test
  void fail_if_not_enough_permission() {
    userSession.logIn(db.users().insertUser());
    QProfileDto qualityProfile = db.qualityProfiles().insert();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setMethod("POST")
        .setParam(PARAM_TARGET_KEY, qualityProfile.getKee())
        .execute();
    })
      .isInstanceOf(ForbiddenException.class);
  }
}
