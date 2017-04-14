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
package org.sonar.server.rule;

import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.organization.OrganizationFlags;
import org.sonar.server.organization.TestOrganizationFlags;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;

public class RuleDeleterTest {

  static final long PAST = 10000L;

  @org.junit.Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @org.junit.Rule
  public DbTester dbTester = DbTester.create();
  @org.junit.Rule
  public ExpectedException thrown = ExpectedException.none();

  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();
  private RuleIndexer ruleIndexer = mock(RuleIndexer.class);
  private OrganizationFlags organizationFlags = TestOrganizationFlags.standalone();
  private RuleActivator ruleActivator = mock(RuleActivator.class);
  private RuleDeleter deleter = new RuleDeleter(System2.INSTANCE, ruleIndexer, dbClient, ruleActivator, organizationFlags);

  @Test
  public void delete_custom_rule() {
    OrganizationDto organization = dbTester.organizations().insert();

    RuleDefinitionDto templateRule = dbTester.rules().insert(
      r -> r.setIsTemplate(true),
      r -> r.setCreatedAt(PAST),
      r -> r.setUpdatedAt(PAST)
    );
    RuleDefinitionDto customRule = dbTester.rules().insert(
      r -> r.setTemplateId(templateRule.getId()),
      r -> r.setCreatedAt(PAST),
      r -> r.setUpdatedAt(PAST)
    );

    Mockito.reset(ruleIndexer);

    // Delete custom rule
    deleter.delete(customRule.getKey());

    Mockito.verify(ruleIndexer).indexRuleDefinition(eq(customRule.getKey()));
    Mockito.verifyNoMoreInteractions(ruleIndexer);

    // Verify custom rule has status REMOVED
    RuleDefinitionDto customRuleReloaded = dbClient.ruleDao().selectOrFailDefinitionByKey(dbSession, customRule.getKey());
    assertThat(customRuleReloaded).isNotNull();
    assertThat(customRuleReloaded.getStatus()).isEqualTo(RuleStatus.REMOVED);
    assertThat(customRuleReloaded.getUpdatedAt()).isNotEqualTo(PAST);
  }

  @Test
  public void fail_to_delete_if_not_custom() {
    RuleDefinitionDto rule = dbTester.rules().insert();

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Only custom rules can be deleted");

    deleter.delete(rule.getKey());
  }

  @Test
  public void fail_if_organizations_enabled() {
    RuleDefinitionDto rule = dbTester.rules().insert();
    organizationFlags.enable(dbSession);

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Organization support is enabled");

    deleter.delete(rule.getKey());
  }
}
