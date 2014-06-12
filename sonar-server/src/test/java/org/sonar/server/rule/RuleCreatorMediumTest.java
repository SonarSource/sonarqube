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

package org.sonar.server.rule;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.tester.ServerTester;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class RuleCreatorMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbSession dbSession;
  DbClient db = tester.get(DbClient.class);
  RuleDao dao = tester.get(RuleDao.class);
  RuleCreator creator = tester.get(RuleCreator.class);

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    dbSession = tester.get(DbClient.class).openSession(false);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void create_custom_rule() throws Exception {
    // insert template rule
    RuleDto templateRule = createTemplateRule();

    // Create custom rule
    NewRule newRule = new NewRule()
      .setRuleKey("CUSTOM_RULE")
      .setTemplateKey(templateRule.getKey())
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("regex", "a.*"));
    RuleKey customRuleKey = creator.create(newRule);

    dbSession.clearCache();

    RuleDto rule = db.ruleDao().getNullableByKey(dbSession, customRuleKey);
    assertThat(rule).isNotNull();
    assertThat(rule.getKey()).isEqualTo(RuleKey.of("java", "CUSTOM_RULE"));
    assertThat(rule.getTemplateId()).isEqualTo(templateRule.getId());
    assertThat(rule.getName()).isEqualTo("My custom");
    assertThat(rule.getDescription()).isEqualTo("Some description");
    assertThat(rule.getSeverityString()).isEqualTo("MAJOR");
    assertThat(rule.getStatus()).isEqualTo(RuleStatus.READY);
    assertThat(rule.getLanguage()).isEqualTo("java");
    assertThat(rule.getConfigKey()).isEqualTo("S001");
    assertThat(rule.getDefaultSubCharacteristicId()).isEqualTo(1);
    assertThat(rule.getDefaultRemediationFunction()).isEqualTo("LINEAR_OFFSET");
    assertThat(rule.getDefaultRemediationCoefficient()).isEqualTo("1h");
    assertThat(rule.getDefaultRemediationOffset()).isEqualTo("5min");
    assertThat(rule.getEffortToFixDescription()).isEqualTo("desc");
    assertThat(rule.getTags()).containsOnly("usertag1", "usertag2");
    assertThat(rule.getSystemTags()).containsOnly("tag1", "tag4");

    List<RuleParamDto> params = db.ruleDao().findRuleParamsByRuleKey(dbSession, customRuleKey);
    assertThat(params).hasSize(1);

    RuleParamDto param = params.get(0);
    // From template rule
    assertThat(param.getName()).isEqualTo("regex");
    assertThat(param.getDescription()).isEqualTo("Reg ex");
    assertThat(param.getType()).isEqualTo("STRING");
    // From user
    assertThat(param.getDefaultValue()).isEqualTo("a.*");
  }

  @Test
  public void fail_to_create_custom_rule_when_missing_key() throws Exception {
    // insert template rule
    RuleDto templateRule = createTemplateRule();

    NewRule newRule = new NewRule()
      .setName("My custom")
      .setTemplateKey(templateRule.getKey())
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("regex", "a.*"));

    try {
      creator.create(newRule);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("The rule key is missing");
    }
  }

  @Test
  public void fail_to_create_custom_rule_when_invalid_key() throws Exception {
    // insert template rule
    RuleDto templateRule = createTemplateRule();

    NewRule newRule = new NewRule()
      .setRuleKey("*INVALID*")
      .setName("My custom")
      .setTemplateKey(templateRule.getKey())
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("regex", "a.*"));

    try {
      creator.create(newRule);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("The rule key '*INVALID*' is invalid, it should only contains : a-z, 0-9, '_'");
    }
  }

  @Test
  public void fail_to_create_custom_rule_when_rule_key_already_exits() throws Exception {
    // insert template rule
    RuleDto templateRule = createTemplateRule();

    // Create a custom rule
    NewRule newRule = new NewRule()
      .setRuleKey("CUSTOM_RULE")
      .setName("My custom")
      .setTemplateKey(templateRule.getKey())
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("regex", "a.*"));
    creator.create(newRule);

    try {
      // Create another custom rule having same key
      newRule = new NewRule()
        .setRuleKey("CUSTOM_RULE")
        .setName("My another custom")
        .setTemplateKey(templateRule.getKey())
        .setHtmlDescription("Some description")
        .setSeverity(Severity.MAJOR)
        .setStatus(RuleStatus.READY)
        .setParameters(ImmutableMap.of("regex", "a.*"));
      creator.create(newRule);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("A rule with the key 'CUSTOM_RULE' already exits");
    }
  }

  @Test
  public void fail_to_create_custom_rule_when_missing_name() throws Exception {
    // insert template rule
    RuleDto templateRule = createTemplateRule();

    NewRule newRule = new NewRule()
      .setRuleKey("CUSTOM_RULE")
      .setTemplateKey(templateRule.getKey())
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("regex", "a.*"));

    try {
      creator.create(newRule);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("The name is missing");
    }
  }

  @Test
  public void fail_to_create_custom_rule_when_missing_description() throws Exception {
    // insert template rule
    RuleDto templateRule = createTemplateRule();

    NewRule newRule = new NewRule()
      .setRuleKey("CUSTOM_RULE")
      .setTemplateKey(templateRule.getKey())
      .setName("My custom")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("regex", "a.*"));

    try {
      creator.create(newRule);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("The description is missing");
    }
  }

  @Test
  public void fail_to_create_custom_rule_when_missing_severity() throws Exception {
    // insert template rule
    RuleDto templateRule = createTemplateRule();

    NewRule newRule = new NewRule()
      .setRuleKey("CUSTOM_RULE")
      .setTemplateKey(templateRule.getKey())
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("regex", "a.*"));

    try {
      creator.create(newRule);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("The severity is missing");
    }
  }

  @Test
  public void fail_to_create_custom_rule_when_invalid_severity() throws Exception {
    // insert template rule
    RuleDto templateRule = createTemplateRule();

    NewRule newRule = new NewRule()
      .setRuleKey("CUSTOM_RULE")
      .setTemplateKey(templateRule.getKey())
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setSeverity("INVALID")
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("regex", "a.*"));

    try {
      creator.create(newRule);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("This severity is invalid : INVALID");
    }
  }

  @Test
  public void fail_to_create_custom_rule_when_missing_status() throws Exception {
    // insert template rule
    RuleDto templateRule = createTemplateRule();

    NewRule newRule = new NewRule()
      .setRuleKey("CUSTOM_RULE")
      .setTemplateKey(templateRule.getKey())
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setParameters(ImmutableMap.of("regex", "a.*"));

    try {
      creator.create(newRule);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("The status is missing");
    }
  }

  @Test
  public void fail_to_create_custom_rule_when_wrong_rule_template() throws Exception {
    // insert rule
    RuleDto rule = dao.insert(dbSession,
      RuleTesting.newDto(RuleKey.of("java", "S001")).setIsTemplate(false));
    dbSession.commit();

    // Create custom rule with unknown template rule
    NewRule newRule = new NewRule()
      .setRuleKey("CUSTOM_RULE")
      .setTemplateKey(rule.getKey())
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("regex", "a.*"));

    try {
      creator.create(newRule);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("This rule is not a template rule: java:S001");
    }
  }

  @Test
  public void fail_to_create_custom_rule_when_a_param_is_missing() throws Exception {
    // insert template rule
    RuleDto templateRule = createTemplateRule();

    NewRule newRule = new NewRule()
      .setRuleKey("CUSTOM_RULE")
      .setTemplateKey(templateRule.getKey())
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY);

    try {
      creator.create(newRule);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("The parameter 'regex' has not been set");
    }
  }

  private RuleDto createTemplateRule(){
    RuleDto templateRule = dao.insert(dbSession,
      RuleTesting.newDto(RuleKey.of("java", "S001"))
        .setIsTemplate(true)
        .setLanguage("java")
        .setConfigKey("S001")
        .setDefaultSubCharacteristicId(1)
        .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
        .setDefaultRemediationCoefficient("1h")
        .setDefaultRemediationOffset("5min")
        .setEffortToFixDescription("desc")
        .setTags(Sets.newHashSet("usertag1", "usertag2"))
        .setSystemTags(Sets.newHashSet("tag1", "tag4"))
    );
    RuleParamDto ruleParamDto = RuleParamDto.createFor(templateRule).setName("regex").setType("STRING").setDescription("Reg ex").setDefaultValue(".*");
    dao.addRuleParam(dbSession, templateRule, ruleParamDto);
    dbSession.commit();
    return templateRule;
  }

}
