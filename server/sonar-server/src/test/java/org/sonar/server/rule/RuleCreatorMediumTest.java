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
import org.assertj.core.api.Fail;
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
import org.sonar.core.rule.RuleDto.Format;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.search.BaseIndex;
import org.sonar.server.tester.ServerTester;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class RuleCreatorMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbSession dbSession;
  DbClient db = tester.get(DbClient.class);
  RuleDao dao = tester.get(RuleDao.class);
  RuleCreator creator = tester.get(RuleCreator.class);
  BaseIndex<Rule, RuleDto, RuleKey> ruleIndex = tester.get(RuleIndex.class);

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
    NewRule newRule = NewRule.createForCustomRule("CUSTOM_RULE", templateRule.getKey())
      .setName("My custom")
      .setMarkdownDescription("Some description")
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
  public void create_custom_rule_with_empty_parameter_value() throws Exception {
    // insert template rule
    RuleDto templateRule = createTemplateRule();

    NewRule newRule = NewRule.createForCustomRule("CUSTOM_RULE", templateRule.getKey())
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("regex", ""));

    RuleKey customRuleKey = creator.create(newRule);
    dbSession.clearCache();

    List<RuleParamDto> params = db.ruleDao().findRuleParamsByRuleKey(dbSession, customRuleKey);
    assertThat(params).hasSize(1);

    RuleParamDto param = params.get(0);
    assertThat(param.getName()).isEqualTo("regex");
    assertThat(param.getDescription()).isEqualTo("Reg ex");
    assertThat(param.getType()).isEqualTo("STRING");
    assertThat(param.getDefaultValue()).isNull();
  }

  @Test
  public void create_custom_rule_with_no_parameter_value() throws Exception {
    // insert template rule
    RuleDto templateRule = createTemplateRuleWithIntArrayParam();

    NewRule newRule = NewRule.createForCustomRule("CUSTOM_RULE", templateRule.getKey())
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY);

    RuleKey customRuleKey = creator.create(newRule);
    dbSession.clearCache();

    List<RuleParamDto> params = db.ruleDao().findRuleParamsByRuleKey(dbSession, customRuleKey);
    assertThat(params).hasSize(1);

    RuleParamDto param = params.get(0);
    assertThat(param.getName()).isEqualTo("myIntegers");
    assertThat(param.getDescription()).isEqualTo("My Integers");
    assertThat(param.getType()).isEqualTo("INTEGER,multiple=true,values=1;2;3");
    assertThat(param.getDefaultValue()).isNull();
  }

  @Test
  public void create_custom_rule_with_multiple_parameter_values() throws Exception {
    // insert template rule
    RuleDto templateRule = createTemplateRuleWithIntArrayParam();

    NewRule newRule = NewRule.createForCustomRule("CUSTOM_RULE", templateRule.getKey())
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("myIntegers", "1,3"));

    RuleKey customRuleKey = creator.create(newRule);
    dbSession.clearCache();

    List<RuleParamDto> params = db.ruleDao().findRuleParamsByRuleKey(dbSession, customRuleKey);
    assertThat(params).hasSize(1);

    RuleParamDto param = params.get(0);
    assertThat(param.getName()).isEqualTo("myIntegers");
    assertThat(param.getDescription()).isEqualTo("My Integers");
    assertThat(param.getType()).isEqualTo("INTEGER,multiple=true,values=1;2;3");
    assertThat(param.getDefaultValue()).isEqualTo("1,3");
  }

  @Test
  public void create_custom_rule_with_invalid_parameter() throws Exception {
    // insert template rule
    RuleDto templateRule = createTemplateRuleWithIntArrayParam();

    // Create custom rule
    NewRule newRule = NewRule.createForCustomRule("CUSTOM_RULE", templateRule.getKey())
      .setName("My custom")
      .setMarkdownDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("myIntegers", "1,polop,2"));
    try {
      creator.create(newRule);
      Fail.failBecauseExceptionWasNotThrown(BadRequestException.class);
    } catch (BadRequestException iae) {
      assertThat(iae).hasMessage("errors.type.notInteger");
    }

    dbSession.clearCache();
  }

  @Test
  public void create_custom_rule_with_invalid_parameters() throws Exception {
    // insert template rule
    RuleDto templateRule = createTemplateRuleWithTwoIntParams();

    // Create custom rule
    NewRule newRule = NewRule.createForCustomRule("CUSTOM_RULE", templateRule.getKey())
      .setName("My custom")
      .setMarkdownDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("first", "polop", "second", "palap"));
    try {
      creator.create(newRule);
      Fail.failBecauseExceptionWasNotThrown(BadRequestException.class);
    } catch (BadRequestException badRequest) {
      assertThat(badRequest.errors().toString()).contains("palap").contains("polop");
    }

    dbSession.clearCache();
  }


  @Test
  public void reactivate_custom_rule_if_already_exists_in_removed_status() throws Exception {
    String key = "CUSTOM_RULE";

    // insert template rule
    RuleDto templateRule = createTemplateRule();

    // insert a removed rule
    RuleDto rule = dao.insert(dbSession, RuleTesting.newCustomRule(templateRule)
      .setRuleKey(key)
      .setStatus(RuleStatus.REMOVED)
      .setName("Old name")
      .setDescription("Old description")
      .setDescriptionFormat(Format.MARKDOWN)
      .setSeverity(Severity.INFO));
    dao.addRuleParam(dbSession, rule, dao.findRuleParamsByRuleKey(dbSession, templateRule.getKey()).get(0).setDefaultValue("a.*"));
    dbSession.commit();
    dbSession.clearCache();

    // Create custom rule with same key, but with different values
    NewRule newRule = NewRule.createForCustomRule(key, templateRule.getKey())
      .setName("New name")
      .setMarkdownDescription("New description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("regex", "c.*"));
    RuleKey customRuleKey = creator.create(newRule);

    dbSession.clearCache();

    Rule result = ruleIndex.getByKey(customRuleKey);
    assertThat(result.key()).isEqualTo(RuleKey.of("java", key));
    assertThat(result.status()).isEqualTo(RuleStatus.READY);

    // These values should be the same than before
    assertThat(result.name()).isEqualTo("Old name");
    assertThat(result.markdownDescription()).isEqualTo("Old description");
    assertThat(result.severity()).isEqualTo(Severity.INFO);
    assertThat(result.param("regex").defaultValue()).isEqualTo("a.*");

    // Check that the id is the same
    assertThat(db.ruleDao().getByKey(dbSession, result.key()).getId()).isEqualTo(rule.getId());
  }

  @Test
  public void generate_reactivation_exception_when_rule_exists_in_removed_status_and_prevent_reactivation_parameter_is_true() throws Exception {
    String key = "CUSTOM_RULE";

    // insert template rule
    RuleDto templateRule = createTemplateRule();

    // insert a removed rule
    RuleDto rule = dao.insert(dbSession, RuleTesting.newCustomRule(templateRule)
      .setRuleKey(key)
      .setStatus(RuleStatus.REMOVED)
      .setName("Old name")
      .setDescription("Old description")
      .setSeverity(Severity.INFO));
    dao.addRuleParam(dbSession, rule, dao.findRuleParamsByRuleKey(dbSession, templateRule.getKey()).get(0).setDefaultValue("a.*"));
    dbSession.commit();
    dbSession.clearCache();

    // Create custom rule with same key, but with different values
    NewRule newRule = NewRule.createForCustomRule(key, templateRule.getKey())
      .setName("New name")
      .setHtmlDescription("New description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("regex", "c.*"))
      .setPreventReactivation(true);

    try {
      creator.create(newRule);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(ReactivationException.class);
      ReactivationException reactivationException = (ReactivationException) e;
      assertThat(reactivationException.ruleKey()).isEqualTo(rule.getKey());
    }
  }

  @Test
  public void fail_to_create_custom_rule_when_invalid_key() throws Exception {
    // insert template rule
    RuleDto templateRule = createTemplateRule();

    NewRule newRule = NewRule.createForCustomRule("*INVALID*", templateRule.getKey())
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("regex", "a.*"));

    try {
      creator.create(newRule);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("coding_rules.validation.invalid_rule_key");
    }
  }

  @Test
  public void fail_to_create_custom_rule_when_rule_key_already_exists() throws Exception {
    // insert template rule
    RuleDto templateRule = createTemplateRule();

    // Create a custom rule
    NewRule newRule = NewRule.createForCustomRule("CUSTOM_RULE", templateRule.getKey())
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("regex", "a.*"));
    creator.create(newRule);

    try {
      // Create another custom rule having same key
      newRule = NewRule.createForCustomRule("CUSTOM_RULE", templateRule.getKey())
        .setName("My another custom")
        .setHtmlDescription("Some description")
        .setSeverity(Severity.MAJOR)
        .setStatus(RuleStatus.READY)
        .setParameters(ImmutableMap.of("regex", "a.*"));
      creator.create(newRule);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("A rule with the key 'CUSTOM_RULE' already exists");
    }
  }

  @Test
  public void fail_to_create_custom_rule_when_missing_name() throws Exception {
    // insert template rule
    RuleDto templateRule = createTemplateRule();

    NewRule newRule = NewRule.createForCustomRule("CUSTOM_RULE", templateRule.getKey())
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("regex", "a.*"));

    try {
      creator.create(newRule);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("coding_rules.validation.missing_name");
    }
  }

  @Test
  public void fail_to_create_custom_rule_when_missing_description() throws Exception {
    // insert template rule
    RuleDto templateRule = createTemplateRule();

    NewRule newRule = NewRule.createForCustomRule("CUSTOM_RULE", templateRule.getKey())
      .setName("My custom")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("regex", "a.*"));

    try {
      creator.create(newRule);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("coding_rules.validation.missing_description");
    }
  }

  @Test
  public void fail_to_create_custom_rule_when_missing_severity() throws Exception {
    // insert template rule
    RuleDto templateRule = createTemplateRule();

    NewRule newRule = NewRule.createForCustomRule("CUSTOM_RULE", templateRule.getKey())
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("regex", "a.*"));

    try {
      creator.create(newRule);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("coding_rules.validation.missing_severity");
    }
  }

  @Test
  public void fail_to_create_custom_rule_when_invalid_severity() throws Exception {
    // insert template rule
    RuleDto templateRule = createTemplateRule();

    NewRule newRule = NewRule.createForCustomRule("CUSTOM_RULE", templateRule.getKey())
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setSeverity("INVALID")
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("regex", "a.*"));

    try {
      creator.create(newRule);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("coding_rules.validation.invalid_severity");
    }
  }

  @Test
  public void fail_to_create_custom_rule_when_missing_status() throws Exception {
    // insert template rule
    RuleDto templateRule = createTemplateRule();

    NewRule newRule = NewRule.createForCustomRule("CUSTOM_RULE", templateRule.getKey())
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setParameters(ImmutableMap.of("regex", "a.*"));

    try {
      creator.create(newRule);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("coding_rules.validation.missing_status");
    }
  }

  @Test
  public void fail_to_create_custom_rule_when_wrong_rule_template() throws Exception {
    // insert rule
    RuleDto rule = dao.insert(dbSession,
      RuleTesting.newDto(RuleKey.of("java", "S001")).setIsTemplate(false));
    dbSession.commit();

    // Create custom rule with unknown template rule
    NewRule newRule = NewRule.createForCustomRule("CUSTOM_RULE", rule.getKey())
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
  public void create_manual_rule() throws Exception {
    NewRule newRule = NewRule.createForManualRule("MANUAL_RULE")
      .setName("My manual")
      .setMarkdownDescription("Some description");
    RuleKey ruleKey = creator.create(newRule);

    dbSession.clearCache();

    Rule rule = ruleIndex.getByKey(ruleKey);
    assertThat(rule).isNotNull();
    assertThat(rule.key()).isEqualTo(RuleKey.of("manual", "MANUAL_RULE"));
    assertThat(rule.name()).isEqualTo("My manual");
    assertThat(rule.markdownDescription()).isEqualTo("Some description");
    assertThat(rule.severity()).isNull();
    assertThat(rule.status()).isEqualTo(RuleStatus.READY);
    assertThat(rule.language()).isNull();
    assertThat(rule.internalKey()).isNull();
    assertThat(rule.debtSubCharacteristicKey()).isNull();
    assertThat(rule.debtRemediationFunction()).isNull();
    assertThat(rule.tags()).isEmpty();
    assertThat(rule.systemTags()).isEmpty();
    assertThat(rule.params()).isEmpty();
  }

  @Test
  public void create_manual_rule_with_severity() throws Exception {
    NewRule newRule = NewRule.createForManualRule("MANUAL_RULE")
      .setName("My manual")
      .setMarkdownDescription("Some description")
      .setSeverity(Severity.BLOCKER);
    RuleKey ruleKey = creator.create(newRule);

    dbSession.clearCache();

    Rule rule = ruleIndex.getByKey(ruleKey);
    assertThat(rule).isNotNull();
    assertThat(rule.key()).isEqualTo(RuleKey.of("manual", "MANUAL_RULE"));
    assertThat(rule.name()).isEqualTo("My manual");
    assertThat(rule.markdownDescription()).isEqualTo("Some description");
    assertThat(rule.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(rule.status()).isEqualTo(RuleStatus.READY);
    assertThat(rule.language()).isNull();
    assertThat(rule.internalKey()).isNull();
    assertThat(rule.debtSubCharacteristicKey()).isNull();
    assertThat(rule.debtRemediationFunction()).isNull();
    assertThat(rule.tags()).isEmpty();
    assertThat(rule.systemTags()).isEmpty();
    assertThat(rule.params()).isEmpty();
  }

  @Test
  public void reactivate_manual_rule_if_already_exists_in_removed_status() throws Exception {
    String key = "MANUAL_RULE";

    // insert a removed rule
    RuleDto rule = dao.insert(dbSession, RuleTesting.newManualRule(key)
      .setStatus(RuleStatus.REMOVED)
      .setName("Old name")
      .setDescription("Old description")
      .setSeverity(Severity.INFO));
    dbSession.commit();
    dbSession.clearCache();

    // Create a rule with the same key and with another name, description and severity
    NewRule newRule = NewRule.createForManualRule(key)
      .setName("New name")
      .setMarkdownDescription("New description");
    RuleKey ruleKey = creator.create(newRule);

    dbSession.clearCache();

    Rule result = ruleIndex.getByKey(ruleKey);
    assertThat(result.key()).isEqualTo(RuleKey.of("manual", key));
    assertThat(result.status()).isEqualTo(RuleStatus.READY);

    // Name, description and severity should be the same than before
    assertThat(result.name()).isEqualTo("Old name");
    assertThat(result.markdownDescription()).isEqualTo("Old description");
    assertThat(result.severity()).isEqualTo(Severity.INFO);

    // Check that the id is the same
    assertThat(db.ruleDao().getByKey(dbSession, result.key()).getId()).isEqualTo(rule.getId());
  }

  @Test
  public void fail_to_create_manual_rule_when_missing_key() throws Exception {
    try {
      NewRule.createForManualRule("")
        .setName("My manual")
        .setHtmlDescription("Some description");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Manual key should be set");
    }
  }

  @Test
  public void fail_to_create_manual_rule_when_invalid_key() throws Exception {
    NewRule newRule = NewRule.createForManualRule("*INVALID*")
      .setName("My custom")
      .setHtmlDescription("Some description");

    try {
      creator.create(newRule);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("coding_rules.validation.invalid_rule_key");
    }
  }

  @Test
  public void fail_to_create_manual_rule_when_rule_key_already_exists() throws Exception {
    NewRule newRule = NewRule.createForManualRule("MANUAL_RULE")
      .setName("My manual")
      .setHtmlDescription("Some description");
    creator.create(newRule);

    try {
      // Create another rule having same key
      newRule = NewRule.createForManualRule("MANUAL_RULE")
        .setName("My other manual")
        .setHtmlDescription("Some description");
      creator.create(newRule);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("A rule with the key 'MANUAL_RULE' already exists");
    }
  }

  @Test
  public void fail_to_create_manual_rule_when_missing_name() throws Exception {
    NewRule newRule = NewRule.createForManualRule("MANUAL_RULE")
      .setHtmlDescription("Some description");

    try {
      creator.create(newRule);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("coding_rules.validation.missing_name");
    }
  }

  @Test
  public void fail_to_create_manual_rule_when_missing_description() throws Exception {
    NewRule newRule = NewRule.createForManualRule("MANUAL_RULE")
      .setName("My manual");

    try {
      creator.create(newRule);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("coding_rules.validation.missing_description");
    }
  }

  @Test
  public void fail_to_create_manual_rule_with_status() throws Exception {
    try {
      NewRule.createForManualRule("MANUAL_RULE")
        .setName("My manual")
        .setHtmlDescription("Some description")
        .setStatus(RuleStatus.BETA);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not a custom rule");
    }
  }

  @Test
  public void fail_to_create_manual_rule_with_parameters() throws Exception {
    try {
      NewRule.createForManualRule("MANUAL_RULE")
        .setName("My manual")
        .setHtmlDescription("Some description")
        .setParameters(ImmutableMap.of("regex", "a.*"));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not a custom rule");
    }
  }

  private RuleDto createTemplateRule() {
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

  private RuleDto createTemplateRuleWithIntArrayParam() {
    RuleDto templateRule = dao.insert(dbSession,
      RuleTesting.newDto(RuleKey.of("java", "S002"))
        .setIsTemplate(true)
        .setLanguage("java")
        .setConfigKey("S002")
        .setDefaultSubCharacteristicId(1)
        .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
        .setDefaultRemediationCoefficient("1h")
        .setDefaultRemediationOffset("5min")
        .setEffortToFixDescription("desc")
      );
    RuleParamDto ruleParamDto = RuleParamDto.createFor(templateRule)
      .setName("myIntegers").setType("INTEGER,multiple=true,values=1;2;3").setDescription("My Integers").setDefaultValue("1");
    dao.addRuleParam(dbSession, templateRule, ruleParamDto);
    dbSession.commit();
    return templateRule;
  }

  private RuleDto createTemplateRuleWithTwoIntParams() {
    RuleDto templateRule = dao.insert(dbSession,
      RuleTesting.newDto(RuleKey.of("java", "S003"))
        .setIsTemplate(true)
        .setLanguage("java")
        .setConfigKey("S003")
        .setDefaultSubCharacteristicId(1)
        .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
        .setDefaultRemediationCoefficient("1h")
        .setDefaultRemediationOffset("5min")
        .setEffortToFixDescription("desc")
      );
    RuleParamDto ruleParam1Dto = RuleParamDto.createFor(templateRule)
      .setName("first").setType("INTEGER").setDescription("First integer").setDefaultValue("0");
    dao.addRuleParam(dbSession, templateRule, ruleParam1Dto);
    RuleParamDto ruleParam2Dto = RuleParamDto.createFor(templateRule)
      .setName("second").setType("INTEGER").setDescription("Second integer").setDefaultValue("0");
    dao.addRuleParam(dbSession, templateRule, ruleParam2Dto);
    dbSession.commit();
    return templateRule;
  }

}
