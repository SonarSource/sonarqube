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
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.qualityprofile.QProfileRules;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;

public class DeleteActionIT {
  private static final long PAST = 10000L;

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester dbTester = DbTester.create();
  @Rule
  public EsTester es = EsTester.create();
  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();
  private RuleIndexer ruleIndexer = spy(new RuleIndexer(es.client(), dbClient));
  private QProfileRules qProfileRules = mock(QProfileRules.class);
  private RuleWsSupport ruleWsSupport = new RuleWsSupport(mock(DbClient.class), userSession);
  private DeleteAction underTest = new DeleteAction(System2.INSTANCE, ruleIndexer, dbClient, qProfileRules, ruleWsSupport);
  private WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void delete_custom_rule() {
    logInAsQProfileAdministrator();

    RuleDto templateRule = dbTester.rules().insert(
      r -> r.setIsTemplate(true),
      r -> r.setCreatedAt(PAST),
      r -> r.setUpdatedAt(PAST));
    RuleDto customRule = dbTester.rules().insert(
      r -> r.setTemplateUuid(templateRule.getUuid()),
      r -> r.setCreatedAt(PAST),
      r -> r.setUpdatedAt(PAST));

    tester.newRequest()
      .setMethod("POST")
      .setParam("key", customRule.getKey().toString())
      .execute();

    verify(ruleIndexer).commitAndIndex(any(), eq(customRule.getUuid()));

    // Verify custom rule has status REMOVED
    RuleDto customRuleReloaded = dbClient.ruleDao().selectOrFailByKey(dbSession, customRule.getKey());
    assertThat(customRuleReloaded).isNotNull();
    assertThat(customRuleReloaded.getStatus()).isEqualTo(RuleStatus.REMOVED);
    assertThat(customRuleReloaded.getUpdatedAt()).isNotEqualTo(PAST);
  }

  @Test
  public void throw_ForbiddenException_if_not_profile_administrator() {
    userSession.logIn();

    assertThatThrownBy(() -> {
      tester.newRequest()
        .setMethod("POST")
        .setParam("key", "anyRuleKey")
        .execute();
    })
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() {
    assertThatThrownBy(() -> {
      tester.newRequest()
        .setMethod("POST")
        .setParam("key", "anyRuleKey")
        .execute();
    })
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_to_delete_if_not_custom() {
    logInAsQProfileAdministrator();
    RuleDto rule = dbTester.rules().insert();

    assertThatThrownBy(() -> {
      tester.newRequest()
        .setMethod("POST")
        .setParam("key", rule.getKey().toString())
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Rule '" + rule.getKey().toString() + "' cannot be deleted because it is not a custom rule");
  }

  private void logInAsQProfileAdministrator() {
    userSession
      .logIn()
      .addPermission(ADMINISTER_QUALITY_PROFILES);
  }
}
