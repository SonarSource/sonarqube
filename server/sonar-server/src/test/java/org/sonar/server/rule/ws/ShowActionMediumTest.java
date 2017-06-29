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

import java.util.Date;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleDao;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QualityProfileDao;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleDto.Format;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.NewCustomRule;
import org.sonar.server.rule.RuleCreator;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static com.google.common.collect.Sets.newHashSet;
import static org.sonar.api.rule.Severity.MINOR;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;

public class ShowActionMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester().withEsIndexes();

  DefaultOrganizationProvider defaultOrganizationProvider = tester.get(DefaultOrganizationProvider.class);

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester).logIn()
    .addPermission(ADMINISTER_QUALITY_PROFILES, defaultOrganizationProvider.get().getUuid());

  private WsTester wsTester;
  private RuleIndexer ruleIndexer;
  private RuleDao ruleDao;
  private DbSession session;
  private OrganizationDto defaultOrganization;

  @Before
  public void setUp() {
    tester.clearDbAndIndexes();
    wsTester = tester.get(WsTester.class);
    ruleIndexer = tester.get(RuleIndexer.class);
    ruleDao = tester.get(RuleDao.class);
    session = tester.get(DbClient.class).openSession(false);
    defaultOrganization = tester.get(DbClient.class).organizationDao().selectByUuid(session, defaultOrganizationProvider.get().getUuid()).get();
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void show_rule() throws Exception {
    RuleDto ruleDto = RuleTesting.newDto(RuleKey.of("java", "S001"), defaultOrganization)
      .setName("Rule S001")
      .setDescription("Rule S001 <b>description</b>")
      .setDescriptionFormat(Format.HTML)
      .setSeverity(MINOR)
      .setStatus(RuleStatus.BETA)
      .setConfigKey("InternalKeyS001")
      .setLanguage("xoo")
      .setTags(newHashSet("tag1", "tag2"))
      .setSystemTags(newHashSet("systag1", "systag2"))
      .setType(RuleType.BUG);
    RuleDefinitionDto definition = ruleDto.getDefinition();
    ruleDao.insert(session, definition);
    ruleDao.insertOrUpdate(session, ruleDto.getMetadata().setRuleId(ruleDto.getId()));
    RuleParamDto param = RuleParamDto.createFor(definition).setName("regex").setType("STRING").setDescription("Reg *exp*").setDefaultValue(".*");
    ruleDao.insertRuleParam(session, definition, param);
    session.commit();
    session.clearCache();

    WsTester.TestRequest request = wsTester.newGetRequest("api/rules", "show")
      .setParam("key", ruleDto.getKey().toString());
    request.execute().assertJson(getClass(), "show_rule.json");
  }

  @Test
  public void show_rule_with_default_debt_infos() throws Exception {
    RuleDto ruleDto = RuleTesting.newDto(RuleKey.of("java", "S001"), defaultOrganization)
      .setName("Rule S001")
      .setDescription("Rule S001 <b>description</b>")
      .setSeverity(MINOR)
      .setStatus(RuleStatus.BETA)
      .setConfigKey("InternalKeyS001")
      .setLanguage("xoo")
      .setDefRemediationFunction("LINEAR_OFFSET")
      .setDefRemediationGapMultiplier("5d")
      .setDefRemediationBaseEffort("10h")
      .setRemediationFunction(null)
      .setRemediationGapMultiplier(null)
      .setRemediationBaseEffort(null);
    ruleDao.insert(session, ruleDto.getDefinition());
    ruleDao.insertOrUpdate(session, ruleDto.getMetadata());
    session.commit();
    session.clearCache();

    WsTester.TestRequest request = wsTester.newGetRequest("api/rules", "show")
      .setParam("key", ruleDto.getKey().toString());
    WsTester.Result response = request.execute();

    response.assertJson(getClass(), "show_rule_with_default_debt_infos.json");
  }

  @Test
  public void show_rule_with_overridden_debt() throws Exception {
    RuleDto ruleDto = RuleTesting.newDto(RuleKey.of("java", "S001"), defaultOrganization)
      .setName("Rule S001")
      .setDescription("Rule S001 <b>description</b>")
      .setSeverity(MINOR)
      .setStatus(RuleStatus.BETA)
      .setConfigKey("InternalKeyS001")
      .setLanguage("xoo")
      .setDefRemediationFunction(null)
      .setDefRemediationGapMultiplier(null)
      .setDefRemediationBaseEffort(null)
      .setRemediationFunction("LINEAR_OFFSET")
      .setRemediationGapMultiplier("5d")
      .setRemediationBaseEffort("10h");
    ruleDao.insert(session, ruleDto.getDefinition());
    ruleDao.insertOrUpdate(session, ruleDto.getMetadata().setRuleId(ruleDto.getId()));
    session.commit();
    session.clearCache();

    WsTester.TestRequest request = wsTester.newGetRequest("api/rules", "show")
      .setParam("key", ruleDto.getKey().toString());
    request.execute().assertJson(getClass(), "show_rule_with_overridden_debt_infos.json");
  }

  @Test
  public void show_rule_with_default_and_overridden_debt_infos() throws Exception {
    RuleDto ruleDto = RuleTesting.newDto(RuleKey.of("java", "S001"), defaultOrganization)
      .setName("Rule S001")
      .setDescription("Rule S001 <b>description</b>")
      .setSeverity(MINOR)
      .setStatus(RuleStatus.BETA)
      .setConfigKey("InternalKeyS001")
      .setLanguage("xoo")
      .setDefRemediationFunction("LINEAR")
      .setDefRemediationGapMultiplier("5min")
      .setDefRemediationBaseEffort(null)
      .setRemediationFunction("LINEAR_OFFSET")
      .setRemediationGapMultiplier("5d")
      .setRemediationBaseEffort("10h");
    ruleDao.insert(session, ruleDto.getDefinition());
    ruleDao.insertOrUpdate(session, ruleDto.getMetadata().setRuleId(ruleDto.getId()));
    session.commit();
    session.clearCache();

    WsTester.TestRequest request = wsTester.newGetRequest("api/rules", "show")
      .setParam("key", ruleDto.getKey().toString());
    request.execute().assertJson(getClass(), "show_rule_with_default_and_overridden_debt_infos.json");
  }

  @Test
  public void show_rule_with_no_default_and_no_overridden_debt() throws Exception {
    RuleDefinitionDto ruleDto = RuleTesting.newRule(RuleKey.of("java", "S001"))
      .setName("Rule S001")
      .setDescription("Rule S001 <b>description</b>")
      .setDescriptionFormat(Format.HTML)
      .setSeverity(MINOR)
      .setStatus(RuleStatus.BETA)
      .setConfigKey("InternalKeyS001")
      .setLanguage("xoo")
      .setDefRemediationFunction(null)
      .setDefRemediationGapMultiplier(null)
      .setDefRemediationBaseEffort(null);
    ruleDao.insert(session, ruleDto);
    session.commit();
    session.clearCache();

    WsTester.TestRequest request = wsTester.newGetRequest("api/rules", "show")
      .setParam("key", ruleDto.getKey().toString());
    request.execute().assertJson(getClass(), "show_rule_with_no_default_and_no_overridden_debt.json");
  }

  @Test
  public void encode_html_description_of_custom_rule() throws Exception {
    // Template rule
    RuleDto templateRule = RuleTesting.newTemplateRule(RuleKey.of("java", "S001"));
    ruleDao.insert(session, templateRule.getDefinition());
    session.commit();

    // Custom rule
    NewCustomRule customRule = NewCustomRule.createForCustomRule("MY_CUSTOM", templateRule.getKey())
      .setName("My custom")
      .setSeverity(MINOR)
      .setStatus(RuleStatus.READY)
      .setMarkdownDescription("<div>line1\nline2</div>");
    RuleKey customRuleKey = tester.get(RuleCreator.class).create(session, customRule);
    session.clearCache();

    WsTester.TestRequest request = wsTester.newGetRequest("api/rules", "show")
      .setParam("key", customRuleKey.toString());
    request.execute().assertJson(getClass(), "encode_html_description_of_custom_rule.json");
  }

  @Test
  public void show_deprecated_rule_rem_function_fields() throws Exception {
    RuleDto ruleDto = RuleTesting.newDto(RuleKey.of("java", "S001"), defaultOrganization)
      .setName("Rule S001")
      .setDescription("Rule S001 <b>description</b>")
      .setSeverity(MINOR)
      .setStatus(RuleStatus.BETA)
      .setConfigKey("InternalKeyS001")
      .setLanguage("xoo")
      .setDefRemediationFunction("LINEAR_OFFSET")
      .setDefRemediationGapMultiplier("6d")
      .setDefRemediationBaseEffort("11h")
      .setRemediationFunction("LINEAR_OFFSET")
      .setRemediationGapMultiplier("5d")
      .setRemediationBaseEffort("10h");
    ruleDao.insert(session, ruleDto.getDefinition());
    ruleDao.insertOrUpdate(session, ruleDto.getMetadata().setRuleId(ruleDto.getId()));
    session.commit();

    WsTester.TestRequest request = wsTester.newGetRequest("api/rules", "show")
      .setParam("key", ruleDto.getKey().toString());
    request.execute().assertJson(getClass(), "show_deprecated_rule_rem_function_fields.json");
  }

  @Test
  public void show_rule_when_activated() throws Exception {
    RuleDefinitionDto ruleDto = RuleTesting.newRule(RuleKey.of("java", "S001"))
      .setName("Rule S001")
      .setDescription("Rule S001 <b>description</b>")
      .setDescriptionFormat(Format.HTML)
      .setSeverity(MINOR)
      .setStatus(RuleStatus.BETA)
      .setLanguage("xoo")
      .setType(RuleType.BUG)
      .setCreatedAt(new Date().getTime())
      .setUpdatedAt(new Date().getTime());
    ruleDao.insert(session, ruleDto);
    session.commit();
    ruleIndexer.commitAndIndex(session, ruleDto.getKey());
    RuleParamDto regexParam = RuleParamDto.createFor(ruleDto).setName("regex").setType("STRING").setDescription("Reg *exp*").setDefaultValue(".*");
    ruleDao.insertRuleParam(session, ruleDto, regexParam);

    QProfileDto profile = new QProfileDto()
      .setRulesProfileUuid("profile")
      .setKee("profile")
      .setOrganizationUuid(defaultOrganizationProvider.get().getUuid())
      .setName("Profile")
      .setLanguage("xoo");
    tester.get(QualityProfileDao.class).insert(session, profile);
    ActiveRuleDto activeRuleDto = new ActiveRuleDto()
      .setProfileId(profile.getId())
      .setRuleId(ruleDto.getId())
      .setSeverity(MINOR)
      .setCreatedAt(new Date().getTime())
      .setUpdatedAt(new Date().getTime());
    tester.get(ActiveRuleDao.class).insert(session, activeRuleDto);
    tester.get(ActiveRuleDao.class).insertParam(session, activeRuleDto, new ActiveRuleParamDto()
      .setRulesParameterId(regexParam.getId())
      .setKey(regexParam.getName())
      .setValue(".*?"));
    session.commit();

    ActiveRuleIndexer activeRuleIndexer = tester.get(ActiveRuleIndexer.class);
    activeRuleIndexer.indexOnStartup(activeRuleIndexer.getIndexTypes());

    WsTester.TestRequest request = wsTester.newGetRequest("api/rules", "show")
      .setParam("key", ruleDto.getKey().toString())
      .setParam("actives", "true");
    request.execute().assertJson(getClass(), "show_rule_when_activated.json");
  }

}
