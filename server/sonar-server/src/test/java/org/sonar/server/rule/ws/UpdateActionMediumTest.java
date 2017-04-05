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
package org.sonar.server.rule.ws;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.organization.DefaultOrganization;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.rule.NewCustomRule;
import org.sonar.server.rule.RuleCreator;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;

public class UpdateActionMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester().withEsIndexes();

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  private WsTester wsTester;
  private RuleDao ruleDao;
  private DbSession session;
  private OrganizationDto defaultOrganization;

  @Before
  public void setUp() {
    tester.clearDbAndIndexes();
    wsTester = tester.get(WsTester.class);
    ruleDao = tester.get(RuleDao.class);
    DbClient dbClient = tester.get(DbClient.class);
    session = dbClient.openSession(false);
    logInAsQProfileAdministrator();
    DefaultOrganization defaultOrganization = tester.get(DefaultOrganizationProvider.class).get();
    this.defaultOrganization = dbClient.organizationDao().selectByUuid(session, defaultOrganization.getUuid()).get();
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void update_custom_rule() throws Exception {
    // Template rule
    RuleDto templateRule = RuleTesting.newTemplateRule(RuleKey.of("java", "S001"), defaultOrganization);
    RuleDefinitionDto definition = templateRule.getDefinition();
    ruleDao.insert(session, definition);
    ruleDao.insertOrUpdate(session, templateRule.getMetadata().setRuleId(templateRule.getId()));
    RuleParamDto param = RuleParamDto.createFor(definition).setName("regex").setType("STRING").setDescription("Reg ex").setDefaultValue(".*");
    ruleDao.insertRuleParam(session, definition, param);
    session.commit();

    // Custom rule
    NewCustomRule newRule = NewCustomRule.createForCustomRule("MY_CUSTOM", templateRule.getKey())
      .setName("Old custom")
      .setHtmlDescription("Old description")
      .setSeverity(Severity.MINOR)
      .setStatus(RuleStatus.BETA)
      .setParameters(ImmutableMap.of("regex", "a"));

    RuleKey customRuleKey = tester.get(RuleCreator.class).create(session, newRule);
    session.clearCache();

    WsTester.TestRequest request = wsTester.newPostRequest("api/rules", "update")
      .setParam("key", customRuleKey.toString())
      .setParam("name", "My custom rule")
      .setParam("markdown_description", "Description")
      .setParam("severity", "MAJOR")
      .setParam("status", "BETA")
      .setParam("params", "regex=a.*");
    request.execute().assertJson(getClass(), "update_custom_rule.json");
  }

  @Test
  public void fail_to_update_custom_when_description_is_empty() {
    // Template rule
    RuleDto templateRule = RuleTesting.newTemplateRule(RuleKey.of("java", "S001"));
    ruleDao.insert(session, templateRule.getDefinition());

    // Custom rule
    RuleDto customRule = RuleTesting.newCustomRule(templateRule);
    ruleDao.insert(session, customRule.getDefinition());
    session.commit();
    session.clearCache();

    WsTester.TestRequest request = wsTester.newPostRequest("api/rules", "update")
      .setParam("key", customRule.getKey().toString())
      .setParam("name", "My custom rule")
      .setParam("markdown_description", "");

    try {
      request.execute();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("The description is missing");
    }
  }

  private void logInAsQProfileAdministrator() {
    userSessionRule
      .logIn()
      .addPermission(ADMINISTER_QUALITY_PROFILES, tester.get(DefaultOrganizationProvider.class).get().getUuid());
  }
}
