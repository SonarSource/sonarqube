/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.rule.ws;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleDto.Format;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.debt.CharacteristicDao;
import org.sonar.db.debt.CharacteristicDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.rule.NewRule;
import org.sonar.server.rule.RuleService;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static com.google.common.collect.Sets.newHashSet;

public class ShowActionMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester).login()
      .setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

  WsTester wsTester;

  RuleService ruleService;
  RuleDao ruleDao;
  DbSession session;

  @Before
  public void setUp() {
    tester.clearDbAndIndexes();
    wsTester = tester.get(WsTester.class);
    ruleService = tester.get(RuleService.class);
    ruleDao = tester.get(RuleDao.class);
    session = tester.get(DbClient.class).openSession(false);
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void show_rule() throws Exception {
    RuleDto ruleDto = ruleDao.insert(session,
      RuleTesting.newDto(RuleKey.of("java", "S001"))
        .setName("Rule S001")
        .setDescription("Rule S001 <b>description</b>")
        .setDescriptionFormat(Format.HTML)
        .setSeverity(Severity.MINOR)
        .setStatus(RuleStatus.BETA)
        .setConfigKey("InternalKeyS001")
        .setLanguage("xoo")
        .setTags(newHashSet("tag1", "tag2"))
        .setSystemTags(newHashSet("systag1", "systag2"))
    );
    RuleParamDto param = RuleParamDto.createFor(ruleDto).setName("regex").setType("STRING").setDescription("Reg *exp*").setDefaultValue(".*");
    ruleDao.insertRuleParam(session, ruleDto, param);
    session.commit();
    session.clearCache();

    WsTester.TestRequest request = wsTester.newGetRequest("api/rules", "show")
      .setParam("key", ruleDto.getKey().toString());
    request.execute().assertJson(getClass(), "show_rule.json");
  }

  @Test
  public void show_rule_with_default_debt_infos() throws Exception {
    CharacteristicDto characteristicDto = new CharacteristicDto().setKey("API").setName("Api").setEnabled(true);
    tester.get(CharacteristicDao.class).insert(session, characteristicDto);
    CharacteristicDto subCharacteristicDto = new CharacteristicDto().setKey("API_ABUSE").setName("API Abuse").setEnabled(true).setParentId(characteristicDto.getId());
    tester.get(CharacteristicDao.class).insert(session, subCharacteristicDto);

    RuleDto ruleDto = ruleDao.insert(session,
      RuleTesting.newDto(RuleKey.of("java", "S001"))
        .setName("Rule S001")
        .setDescription("Rule S001 <b>description</b>")
        .setSeverity(Severity.MINOR)
        .setStatus(RuleStatus.BETA)
        .setConfigKey("InternalKeyS001")
        .setLanguage("xoo")
        .setDefaultSubCharacteristicId(subCharacteristicDto.getId())
        .setDefaultRemediationFunction("LINEAR_OFFSET")
        .setDefaultRemediationCoefficient("5d")
        .setDefaultRemediationOffset("10h")
        .setSubCharacteristicId(null)
        .setRemediationFunction(null)
        .setRemediationCoefficient(null)
        .setRemediationOffset(null)
    );
    session.commit();
    session.clearCache();

    WsTester.TestRequest request = wsTester.newGetRequest("api/rules", "show")
      .setParam("key", ruleDto.getKey().toString());
    WsTester.Result response = request.execute();

    response.assertJson(getClass(), "show_rule_with_default_debt_infos.json");
  }

  @Test
  public void show_rule_with_overridden_debt() throws Exception {
    CharacteristicDto characteristicDto = new CharacteristicDto().setKey("API").setName("Api").setEnabled(true);
    tester.get(CharacteristicDao.class).insert(session, characteristicDto);
    CharacteristicDto subCharacteristicDto = new CharacteristicDto().setKey("API_ABUSE").setName("API Abuse").setEnabled(true).setParentId(characteristicDto.getId());
    tester.get(CharacteristicDao.class).insert(session, subCharacteristicDto);

    RuleDto ruleDto = ruleDao.insert(session,
      RuleTesting.newDto(RuleKey.of("java", "S001"))
        .setName("Rule S001")
        .setDescription("Rule S001 <b>description</b>")
        .setSeverity(Severity.MINOR)
        .setStatus(RuleStatus.BETA)
        .setConfigKey("InternalKeyS001")
        .setLanguage("xoo")
        .setDefaultSubCharacteristicId(null)
        .setDefaultRemediationFunction(null)
        .setDefaultRemediationCoefficient(null)
        .setDefaultRemediationOffset(null)
        .setSubCharacteristicId(subCharacteristicDto.getId())
        .setRemediationFunction("LINEAR_OFFSET")
        .setRemediationCoefficient("5d")
        .setRemediationOffset("10h")
    );
    session.commit();
    session.clearCache();

    WsTester.TestRequest request = wsTester.newGetRequest("api/rules", "show")
      .setParam("key", ruleDto.getKey().toString());
    request.execute().assertJson(getClass(), "show_rule_with_overridden_debt_infos.json");
  }

  @Test
  public void show_rule_with_default_and_overridden_debt_infos() throws Exception {
    CharacteristicDto defaultCharacteristic = new CharacteristicDto().setKey("API").setName("Api").setEnabled(true);
    tester.get(CharacteristicDao.class).insert(session, defaultCharacteristic);
    CharacteristicDto defaultSubCharacteristic = new CharacteristicDto().setKey("API_ABUSE").setName("API Abuse").setEnabled(true).setParentId(defaultCharacteristic.getId());
    tester.get(CharacteristicDao.class).insert(session, defaultSubCharacteristic);

    CharacteristicDto characteristic = new CharacteristicDto().setKey("OS").setName("Os").setEnabled(true);
    tester.get(CharacteristicDao.class).insert(session, characteristic);
    CharacteristicDto subCharacteristic = new CharacteristicDto().setKey("OS_RELATED_PORTABILITY").setName("Portability").setEnabled(true).setParentId(characteristic.getId());
    tester.get(CharacteristicDao.class).insert(session, subCharacteristic);

    RuleDto ruleDto = ruleDao.insert(session,
      RuleTesting.newDto(RuleKey.of("java", "S001"))
        .setName("Rule S001")
        .setDescription("Rule S001 <b>description</b>")
        .setSeverity(Severity.MINOR)
        .setStatus(RuleStatus.BETA)
        .setConfigKey("InternalKeyS001")
        .setLanguage("xoo")
        .setDefaultSubCharacteristicId(defaultSubCharacteristic.getId())
        .setDefaultRemediationFunction("LINEAR")
        .setDefaultRemediationCoefficient("5min")
        .setDefaultRemediationOffset(null)
        .setSubCharacteristicId(subCharacteristic.getId())
        .setRemediationFunction("LINEAR_OFFSET")
        .setRemediationCoefficient("5d")
        .setRemediationOffset("10h")
    );
    session.commit();
    session.clearCache();

    WsTester.TestRequest request = wsTester.newGetRequest("api/rules", "show")
      .setParam("key", ruleDto.getKey().toString());
    request.execute().assertJson(getClass(), "show_rule_with_default_and_overridden_debt_infos.json");
  }

  @Test
  public void show_rule_with_no_default_and_no_overridden_debt() throws Exception {
    RuleDto ruleDto = ruleDao.insert(session,
      RuleTesting.newDto(RuleKey.of("java", "S001"))
        .setName("Rule S001")
        .setDescription("Rule S001 <b>description</b>")
        .setDescriptionFormat(Format.HTML)
        .setSeverity(Severity.MINOR)
        .setStatus(RuleStatus.BETA)
        .setConfigKey("InternalKeyS001")
        .setLanguage("xoo")
        .setDefaultSubCharacteristicId(null)
        .setDefaultRemediationFunction(null)
        .setDefaultRemediationCoefficient(null)
        .setDefaultRemediationOffset(null)
        .setSubCharacteristicId(null)
        .setRemediationFunction(null)
        .setRemediationCoefficient(null)
        .setRemediationOffset(null)
    );
    session.commit();
    session.clearCache();

    WsTester.TestRequest request = wsTester.newGetRequest("api/rules", "show")
      .setParam("key", ruleDto.getKey().toString());
    request.execute().assertJson(getClass(), "show_rule_with_no_default_and_no_overridden_debt.json");
  }

  @Test
  public void show_rule_with_overridden_disable_debt() throws Exception {
    RuleDto ruleDto = ruleDao.insert(session,
      RuleTesting.newDto(RuleKey.of("java", "S001"))
        .setName("Rule S001")
        .setDescription("Rule S001 <b>description</b>")
        .setSeverity(Severity.MINOR)
        .setStatus(RuleStatus.BETA)
        .setConfigKey("InternalKeyS001")
        .setLanguage("xoo")
        .setDefaultSubCharacteristicId(null)
        .setDefaultRemediationFunction(null)
        .setDefaultRemediationCoefficient(null)
        .setDefaultRemediationOffset(null)
        .setSubCharacteristicId(-1)
        .setRemediationFunction(null)
        .setRemediationCoefficient(null)
        .setRemediationOffset(null)
    );
    session.commit();
    session.clearCache();

    WsTester.TestRequest request = wsTester.newGetRequest("api/rules", "show")
      .setParam("key", ruleDto.getKey().toString());
    request.execute().assertJson(getClass(), "show_rule_with_overridden_disable_debt.json");
  }

  @Test
  public void encode_html_description_of_custom_rule() throws Exception {
    // Template rule
    RuleDto templateRule = ruleDao.insert(session, RuleTesting.newTemplateRule(RuleKey.of("java", "S001")));
    session.commit();

    // Custom rule
    NewRule customRule = NewRule.createForCustomRule("MY_CUSTOM", templateRule.getKey())
      .setName("My custom")
      .setSeverity(Severity.MINOR)
      .setStatus(RuleStatus.READY)
      .setMarkdownDescription("<div>line1\nline2</div>");
    RuleKey customRuleKey = ruleService.create(customRule);
    session.clearCache();

    WsTester.TestRequest request = wsTester.newGetRequest("api/rules", "show")
      .setParam("key", customRuleKey.toString());
    request.execute().assertJson(getClass(), "encode_html_description_of_custom_rule.json");
  }

  @Test
  public void encode_html_description_of_manual_rule() throws Exception {
    // Manual rule
    NewRule manualRule = NewRule.createForManualRule("MY_MANUAL")
      .setName("My manual")
      .setSeverity(Severity.MINOR)
      .setMarkdownDescription("<div>line1\nline2</div>");
    RuleKey customRuleKey = ruleService.create(manualRule);
    session.clearCache();

    WsTester.TestRequest request = wsTester.newGetRequest("api/rules", "show")
      .setParam("key", customRuleKey.toString());
    request.execute().assertJson(getClass(), "encode_html_description_of_manual_rule.json");
  }

}
