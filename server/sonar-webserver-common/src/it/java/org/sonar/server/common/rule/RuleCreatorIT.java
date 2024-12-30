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
package org.sonar.server.common.rule;

import com.google.common.collect.Sets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.assertj.core.api.Fail;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.config.Configuration;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.utils.System2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleDto.Format;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.common.rule.service.NewCustomRule;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.rule.index.RuleQuery;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.sonar.api.issue.impact.Severity.HIGH;
import static org.sonar.api.issue.impact.Severity.LOW;
import static org.sonar.api.issue.impact.Severity.MEDIUM;
import static org.sonar.api.issue.impact.SoftwareQuality.MAINTAINABILITY;
import static org.sonar.api.issue.impact.SoftwareQuality.RELIABILITY;
import static org.sonar.api.issue.impact.SoftwareQuality.SECURITY;
import static org.sonar.api.server.rule.internal.ImpactMapper.convertToImpactSeverity;
import static org.sonar.api.server.rule.internal.ImpactMapper.convertToSoftwareQuality;
import static org.sonar.db.rule.RuleTesting.newCustomRule;
import static org.sonar.db.rule.RuleTesting.newRule;
import static org.sonar.server.util.TypeValidationsTesting.newFullTypeValidations;

public class RuleCreatorIT {

  private static final RuleKey CUSTOM_RULE_KEY = RuleKey.parse("java:CUSTOM_RULE");
  private final System2 system2 = new TestSystem2().setNow(Instant.now().toEpochMilli());

  @Rule
  public DbTester dbTester = DbTester.create(system2);

  @Rule
  public EsTester es = EsTester.create();

  private final Configuration config = mock(Configuration.class);
  private final RuleIndex ruleIndex = new RuleIndex(es.client(), system2, config);
  private final RuleIndexer ruleIndexer = new RuleIndexer(es.client(), dbTester.getDbClient());
  private final DbSession dbSession = dbTester.getSession();
  private final UuidFactory uuidFactory = new SequenceUuidFactory();

  private final RuleCreator underTest = new RuleCreator(system2, new RuleIndexer(es.client(), dbTester.getDbClient()), dbTester.getDbClient(), newFullTypeValidations(),
    uuidFactory);

  @Test
  public void create_custom_rule() {
    // insert template rule
    RuleDto templateRule = createTemplateRule();
    // Create custom rule
    NewCustomRule newRule = NewCustomRule.createForCustomRule(CUSTOM_RULE_KEY, templateRule.getKey())
      .setName("My custom")
      .setMarkdownDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(Map.of("regex", "a.*"));
    RuleKey customRuleKey = underTest.create(dbSession, newRule).getKey();

    RuleDto rule = dbTester.getDbClient().ruleDao().selectOrFailByKey(dbSession, customRuleKey);
    assertThat(rule).isNotNull();
    assertThat(rule.getKey()).isEqualTo(CUSTOM_RULE_KEY);
    assertThat(rule.getPluginKey()).isEqualTo("sonarjava");
    assertThat(rule.getTemplateUuid()).isEqualTo(templateRule.getUuid());
    assertThat(rule.getName()).isEqualTo("My custom");
    assertThat(rule.getDefaultRuleDescriptionSection().getContent()).isEqualTo("Some description");
    assertThat(rule.getEnumType()).isEqualTo(RuleType.CODE_SMELL);
    assertCleanCodeInformation(rule);
    assertThat(rule.getSeverityString()).isEqualTo("MAJOR");
    assertThat(rule.getStatus()).isEqualTo(RuleStatus.READY);
    assertThat(rule.getLanguage()).isEqualTo("java");
    assertThat(rule.getConfigKey()).isEqualTo("S001");
    assertDefRemediation(rule);
    assertThat(rule.getGapDescription()).isEqualTo("desc");
    assertThat(rule.getTags()).containsOnly("usertag1", "usertag2");
    assertThat(rule.getSystemTags()).containsOnly("tag1", "tag4");
    assertThat(rule.getSecurityStandards()).containsOnly("owaspTop10:a1", "cwe:123");
    assertThat(rule.isExternal()).isFalse();
    assertThat(rule.isAdHoc()).isFalse();

    List<RuleParamDto> params = dbTester.getDbClient().ruleDao().selectRuleParamsByRuleKey(dbSession, customRuleKey);
    assertThat(params).hasSize(1);

    RuleParamDto param = params.get(0);
    // From template rule
    assertThat(param.getName()).isEqualTo("regex");
    assertThat(param.getDescription()).isEqualTo("Reg ex");
    assertThat(param.getType()).isEqualTo("STRING");
    // From user
    assertThat(param.getDefaultValue()).isEqualTo("a.*");

    assertThat(ruleIndex.search(new RuleQuery(), new SearchOptions()).getUuids()).containsOnly(rule.getUuid(), templateRule.getUuid());
  }

  private static void assertCleanCodeInformation(RuleDto rule) {
    assertThat(rule.getCleanCodeAttribute()).isEqualTo(CleanCodeAttribute.CONVENTIONAL);
    assertThat(rule.getDefaultImpacts()).extracting(ImpactDto::getSoftwareQuality, ImpactDto::getSeverity)
      .containsExactly(tuple(MAINTAINABILITY, MEDIUM));
  }

  private static void assertDefRemediation(RuleDto rule) {
    assertThat(rule.getDefRemediationFunction()).isEqualTo("LINEAR_OFFSET");
    assertThat(rule.getDefRemediationGapMultiplier()).isEqualTo("1h");
    assertThat(rule.getDefRemediationBaseEffort()).isEqualTo("5min");
  }

  @Test
  public void create_shouldSetCleanCodeAttributeAndImpacts() {
    // insert template rule
    RuleDto templateRule = createTemplateRule();
    // Create custom rule
    NewCustomRule newRule = NewCustomRule.createForCustomRule(CUSTOM_RULE_KEY, templateRule.getKey())
      .setName("My custom")
      .setMarkdownDescription("Some description")
      .setStatus(RuleStatus.READY)
      .setCleanCodeAttribute(CleanCodeAttribute.MODULAR)
      .setImpacts(List.of(
        new NewCustomRule.Impact(RELIABILITY, HIGH),
        new NewCustomRule.Impact(SECURITY, LOW)));
    RuleKey customRuleKey = underTest.create(dbSession, newRule).getKey();

    RuleDto rule = dbTester.getDbClient().ruleDao().selectOrFailByKey(dbSession, customRuleKey);
    assertThat(rule).isNotNull();
    assertThat(rule.getKey()).isEqualTo(CUSTOM_RULE_KEY);
    assertThat(rule.getCleanCodeAttribute()).isEqualTo(CleanCodeAttribute.MODULAR);
    assertThat(rule.getDefaultImpacts()).extracting(ImpactDto::getSoftwareQuality, ImpactDto::getSeverity)
      .containsExactlyInAnyOrder(tuple(RELIABILITY, HIGH), tuple(SECURITY, LOW));
    // Back-mapped from the impact
    assertThat(rule.getType()).isEqualTo(RuleType.VULNERABILITY.getDbConstant());
    assertThat(rule.getSeverityString()).isEqualTo(Severity.MINOR);
  }

  @Test
  public void create_whenFieldsNotSpecified_shouldSetDefaultValues() {
    // insert template rule
    RuleDto templateRule = createTemplateRule();
    // Create custom rule
    NewCustomRule newRule = NewCustomRule.createForCustomRule(CUSTOM_RULE_KEY, templateRule.getKey())
      .setName("My custom")
      .setMarkdownDescription("Some description");
    RuleKey customRuleKey = underTest.create(dbSession, newRule).getKey();

    RuleDto rule = dbTester.getDbClient().ruleDao().selectOrFailByKey(dbSession, customRuleKey);
    assertThat(rule).isNotNull();
    assertThat(rule.getStatus()).isEqualTo(RuleStatus.READY);
    assertThat(rule.getSeverityString()).isEqualTo(Severity.MAJOR);
    assertThat(rule.getCleanCodeAttribute()).isEqualTo(CleanCodeAttribute.CONVENTIONAL);
    assertThat(rule.getDefaultImpacts()).extracting(ImpactDto::getSoftwareQuality, ImpactDto::getSeverity).containsExactly(tuple(
      convertToSoftwareQuality(RuleType.valueOf(templateRule.getType())),
      convertToImpactSeverity(requireNonNull(Severity.MAJOR))));
  }

  @Test
  public void create_whenImpactsAndTypeAreSet_shouldFail() {
    // insert template rule
    RuleDto templateRule = createTemplateRule();
    // Create custom rule
    NewCustomRule newRule = NewCustomRule.createForCustomRule(CUSTOM_RULE_KEY, templateRule.getKey())
      .setName("My custom")
      .setMarkdownDescription("Some description")
      .setStatus(RuleStatus.READY)
      .setType(RuleType.BUG)
      .setImpacts(List.of(
        new NewCustomRule.Impact(RELIABILITY, HIGH),
        new NewCustomRule.Impact(SECURITY, LOW)));

    assertThatThrownBy(() -> underTest.create(dbSession, newRule))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("The rule cannot have both impacts and type/severity specified");
  }

  @Test
  public void create_whenImpactsAndSeverityAreSet_shouldFail() {
    // insert template rule
    RuleDto templateRule = createTemplateRule();
    // Create custom rule
    NewCustomRule newRule = NewCustomRule.createForCustomRule(CUSTOM_RULE_KEY, templateRule.getKey())
      .setName("My custom")
      .setMarkdownDescription("Some description")
      .setStatus(RuleStatus.READY)
      .setSeverity(Severity.CRITICAL)
      .setImpacts(List.of(
        new NewCustomRule.Impact(RELIABILITY, HIGH),
        new NewCustomRule.Impact(SECURITY, LOW)));

    assertThatThrownBy(() -> underTest.create(dbSession, newRule))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("The rule cannot have both impacts and type/severity specified");
  }

  @Test
  public void create_whenRuleAndTemplateHaveDifferentRepo_shouldFail() {
    // insert template rule
    RuleDto templateRule = createTemplateRule();
    // Create custom rule
    NewCustomRule newRule = NewCustomRule.createForCustomRule(RuleKey.parse("web:CUSTOM_RULE"), templateRule.getKey())
      .setName("My custom")
      .setMarkdownDescription("Some description")
      .setStatus(RuleStatus.READY);

    assertThatThrownBy(() -> underTest.create(dbSession, newRule))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Custom and template keys must be in the same repository");
  }

  @Test
  public void create_whenRuleStatusRemoved_shouldFail() {
    // insert template rule
    RuleDto templateRule = createTemplateRule();
    // Create custom rule
    NewCustomRule newRule = NewCustomRule.createForCustomRule(CUSTOM_RULE_KEY, templateRule.getKey())
      .setName("My custom")
      .setMarkdownDescription("Some description")
      .setStatus(RuleStatus.REMOVED);

    assertThatThrownBy(() -> underTest.create(dbSession, newRule))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Rule status 'REMOVED' is not allowed");
  }

  @Test
  public void create_custom_rule_with_empty_parameter_value() {
    // insert template rule
    RuleDto templateRule = createTemplateRule();
    NewCustomRule newRule = NewCustomRule.createForCustomRule(CUSTOM_RULE_KEY, templateRule.getKey())
      .setName("My custom")
      .setMarkdownDescription("some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(Map.of("regex", ""));

    RuleKey customRuleKey = underTest.create(dbSession, newRule).getKey();

    List<RuleParamDto> params = dbTester.getDbClient().ruleDao().selectRuleParamsByRuleKey(dbSession, customRuleKey);
    assertThat(params).hasSize(1);
    RuleParamDto param = params.get(0);
    assertThat(param.getName()).isEqualTo("regex");
    assertThat(param.getDescription()).isEqualTo("Reg ex");
    assertThat(param.getType()).isEqualTo("STRING");
    assertThat(param.getDefaultValue()).isNull();
  }

  @Test
  public void create_whenTypeIsHotspot_shouldNotComputeDefaultImpact() {
    // insert template rule
    RuleDto templateRule = createTemplateRule();
    NewCustomRule newRule = NewCustomRule.createForCustomRule(CUSTOM_RULE_KEY, templateRule.getKey())
      .setName("My custom")
      .setMarkdownDescription("some description")
      .setSeverity(Severity.MAJOR)
      .setType(RuleType.SECURITY_HOTSPOT)
      .setStatus(RuleStatus.READY)
      .setParameters(Map.of("regex", ""));

    RuleKey customRuleKey = underTest.create(dbSession, newRule).getKey();

    RuleDto rule = dbTester.getDbClient().ruleDao().selectOrFailByKey(dbSession, customRuleKey);
    assertThat(rule.getDefaultImpacts()).isEmpty();
  }

  @Test
  public void create_custom_rule_with_no_parameter_value() {
    // insert template rule
    RuleDto templateRule = createTemplateRuleWithIntArrayParam();
    NewCustomRule newRule = NewCustomRule.createForCustomRule(CUSTOM_RULE_KEY, templateRule.getKey())
      .setName("My custom")
      .setMarkdownDescription("some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY);

    RuleKey customRuleKey = underTest.create(dbSession, newRule).getKey();

    List<RuleParamDto> params = dbTester.getDbClient().ruleDao().selectRuleParamsByRuleKey(dbSession, customRuleKey);
    assertThat(params).hasSize(1);
    RuleParamDto param = params.get(0);
    assertThat(param.getName()).isEqualTo("myIntegers");
    assertThat(param.getDescription()).isEqualTo("My Integers");
    assertThat(param.getType()).isEqualTo("INTEGER,multiple=true,values=1;2;3");
    assertThat(param.getDefaultValue()).isNull();
  }

  @Test
  public void create_custom_rule_with_multiple_parameter_values() {
    // insert template rule
    RuleDto templateRule = createTemplateRuleWithIntArrayParam();
    NewCustomRule newRule = NewCustomRule.createForCustomRule(CUSTOM_RULE_KEY, templateRule.getKey())
      .setName("My custom")
      .setMarkdownDescription("some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(Map.of("myIntegers", "1,3"));

    RuleKey customRuleKey = underTest.create(dbSession, newRule).getKey();

    List<RuleParamDto> params = dbTester.getDbClient().ruleDao().selectRuleParamsByRuleKey(dbSession, customRuleKey);
    assertThat(params).hasSize(1);
    RuleParamDto param = params.get(0);
    assertThat(param.getName()).isEqualTo("myIntegers");
    assertThat(param.getDescription()).isEqualTo("My Integers");
    assertThat(param.getType()).isEqualTo("INTEGER,multiple=true,values=1;2;3");
    assertThat(param.getDefaultValue()).isEqualTo("1,3");
  }

  @Test
  public void batch_create_custom_rules() {
    // insert template rule
    RuleDto templateRule = createTemplateRuleWithIntArrayParam();

    NewCustomRule firstRule = NewCustomRule.createForCustomRule(RuleKey.parse("java:CUSTOM_RULE_1"), templateRule.getKey())
      .setName("My custom")
      .setMarkdownDescription("some description")
      .setSeverity(Severity.MAJOR)
      .setImpacts(List.of(new NewCustomRule.Impact(RELIABILITY, MEDIUM)))
      .setStatus(RuleStatus.READY);

    NewCustomRule secondRule = NewCustomRule.createForCustomRule(RuleKey.parse("java:CUSTOM_RULE_2"), templateRule.getKey())
      .setName("My custom")
      .setMarkdownDescription("some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY);

    List<RuleKey> customRuleKeys = underTest.restore(dbSession, Arrays.asList(firstRule, secondRule))
      .stream()
      .map(RuleDto::getKey)
      .toList();

    List<RuleDto> rules = dbTester.getDbClient().ruleDao().selectByKeys(dbSession, customRuleKeys);

    assertThat(rules).hasSize(2);
    assertThat(rules).asList()
      .extracting("ruleKey")
      .containsOnly("CUSTOM_RULE_1", "CUSTOM_RULE_2");

    RuleDto customRule1 = rules.stream().filter(e -> e.getRuleKey().equals("CUSTOM_RULE_1")).findFirst().orElseThrow();
    assertThat(customRule1.getSeverityString()).isEqualTo(Severity.MAJOR);
    assertThat(customRule1.getDefaultImpactsMap()).containsExactlyInAnyOrderEntriesOf(Map.of(RELIABILITY, MEDIUM));

  }

  @Test
  public void fail_to_create_custom_rules_when_wrong_rule_template() {
    // insert rule
    RuleDto rule = newRule(RuleKey.of("java", "S001")).setIsTemplate(false);
    dbTester.rules().insert(rule);
    dbSession.commit();

    NewCustomRule newRule = NewCustomRule.createForCustomRule(CUSTOM_RULE_KEY, rule.getKey())
      .setName("My custom")
      .setMarkdownDescription("some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(Map.of("regex", "a.*"));
    List<NewCustomRule> newRules = singletonList(newRule);
    assertThatThrownBy(() -> underTest.restore(dbSession, newRules))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("This rule is not a template rule: java:S001");
  }

  @Test
  public void fail_to_create_custom_rules_when_removed_rule_template() {
    // insert rule
    RuleDto rule = createTemplateRule();
    newRule(RuleKey.of("java", "S001")).setIsTemplate(false);
    rule.setStatus(RuleStatus.REMOVED);
    dbTester.rules().update(rule);
    dbSession.commit();

    NewCustomRule newRule = NewCustomRule.createForCustomRule(CUSTOM_RULE_KEY, rule.getKey())
      .setName("My custom")
      .setMarkdownDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY);

    List<NewCustomRule> newRules = singletonList(newRule);
    assertThatThrownBy(() -> underTest.restore(dbSession, newRules))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The template key doesn't exist: java:S001");
  }

  @Test
  public void fail_to_create_custom_rule_with_invalid_parameter() {
    // insert template rule
    RuleDto templateRule = createTemplateRuleWithIntArrayParam();

    // Create custom rule
    NewCustomRule newRule = NewCustomRule.createForCustomRule(CUSTOM_RULE_KEY, templateRule.getKey())
      .setName("My custom")
      .setMarkdownDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(Map.of("myIntegers", "1,polop,2"));

    assertThatThrownBy(() -> underTest.create(dbSession, newRule))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Value 'polop' must be an integer.");
  }

  @Test
  public void fail_to_create_custom_rule_with_invalid_parameters() {
    // insert template rule
    RuleDto templateRule = createTemplateRuleWithTwoIntParams();

    // Create custom rule
    NewCustomRule newRule = NewCustomRule.createForCustomRule(CUSTOM_RULE_KEY, templateRule.getKey())
      .setName("My custom")
      .setMarkdownDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(Map.of("first", "polop", "second", "palap"));
    try {
      underTest.create(dbSession, newRule);
      Fail.failBecauseExceptionWasNotThrown(BadRequestException.class);
    } catch (BadRequestException badRequest) {
      assertThat(badRequest.errors().toString()).contains("palap").contains("polop");
    }
  }

  @Test
  public void fail_to_create_custom_rule_with_empty_description() {
    // insert template rule
    RuleDto templateRule = createTemplateRuleWithTwoIntParams();

    // Create custom rule
    NewCustomRule newRule = NewCustomRule.createForCustomRule(CUSTOM_RULE_KEY, templateRule.getKey())
      .setName("My custom")
      .setMarkdownDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setMarkdownDescription("");
    assertThatExceptionOfType(BadRequestException.class)
      .isThrownBy(() -> underTest.create(dbSession, newRule))
      .withMessage("The description is missing");
  }

  @Test
  public void reactivate_custom_rule_if_already_exists_in_removed_status() {
    RuleDto templateRule = createTemplateRule();

    RuleDto rule = newCustomRule(templateRule, "Old description")
      .setRuleKey(CUSTOM_RULE_KEY)
      .setStatus(RuleStatus.REMOVED)
      .setName("Old name")
      .setDescriptionFormat(Format.MARKDOWN)
      .setSeverity(Severity.INFO);
    dbTester.rules().insert(rule);
    dbTester.rules().insertRuleParam(rule, param -> param.setDefaultValue("a.*"));
    dbSession.commit();

    // Create custom rule with same key, but with different values
    NewCustomRule newRule = NewCustomRule.createForCustomRule(CUSTOM_RULE_KEY, templateRule.getKey())
      .setName("New name")
      .setMarkdownDescription("New description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(Map.of("regex", "c.*"));
    RuleKey customRuleKey = underTest.create(dbSession, newRule).getKey();

    RuleDto result = dbTester.getDbClient().ruleDao().selectOrFailByKey(dbSession, customRuleKey);
    assertThat(result.getKey()).isEqualTo(CUSTOM_RULE_KEY);
    assertThat(result.getStatus()).isEqualTo(RuleStatus.READY);

    // These values should be the same than before
    assertThat(result.getName()).isEqualTo("Old name");
    assertThat(result.getDefaultRuleDescriptionSection().getContent()).isEqualTo("Old description");
    assertThat(result.getSeverityString()).isEqualTo(Severity.INFO);

    List<RuleParamDto> params = dbTester.getDbClient().ruleDao().selectRuleParamsByRuleKey(dbSession, customRuleKey);
    assertThat(params).hasSize(1);
    assertThat(params.get(0).getDefaultValue()).isEqualTo("a.*");
  }

  @Test
  public void generate_reactivation_exception_when_rule_exists_in_removed_status_and_prevent_reactivation_parameter_is_true() {
    RuleDto templateRule = createTemplateRule();

    RuleDto rule = newCustomRule(templateRule, "Old description")
      .setRuleKey(CUSTOM_RULE_KEY)
      .setStatus(RuleStatus.REMOVED)
      .setName("Old name")
      .setSeverity(Severity.INFO);
    dbTester.rules().insert(rule);
    dbTester.rules().insertRuleParam(rule, param -> param.setDefaultValue("a.*"));
    dbSession.commit();

    // Create custom rule with same key, but with different values
    NewCustomRule newRule = NewCustomRule.createForCustomRule(CUSTOM_RULE_KEY, templateRule.getKey())
      .setName("New name")
      .setMarkdownDescription("some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(Map.of("regex", "c.*"))
      .setPreventReactivation(true);

    try {
      underTest.create(dbSession, newRule);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(ReactivationException.class);
      ReactivationException reactivationException = (ReactivationException) e;
      assertThat(reactivationException.ruleKey()).isEqualTo(rule.getKey());
    }
  }

  @Test
  public void fail_to_create_custom_rule_when_rule_key_already_exists() {
    // insert template rule
    RuleDto templateRule = createTemplateRule();
    // Create a custom rule
    AtomicReference<NewCustomRule> newRule = new AtomicReference<>(NewCustomRule.createForCustomRule(CUSTOM_RULE_KEY, templateRule.getKey())
      .setName("My custom")
      .setMarkdownDescription("some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(Map.of("regex", "a.*")));
    underTest.create(dbSession, newRule.get());

    // Create another custom rule having same key
    newRule.set(NewCustomRule.createForCustomRule(CUSTOM_RULE_KEY, templateRule.getKey())
      .setName("My another custom")
      .setMarkdownDescription("some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(Map.of("regex", "a.*")));

    assertThatThrownBy(() -> underTest.create(dbSession, newRule.get()))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("A rule with the key 'CUSTOM_RULE' already exists");
  }

  @Test
  public void fail_to_create_custom_rule_when_missing_name() {
    // insert template rule
    RuleDto templateRule = createTemplateRule();

    NewCustomRule newRule = NewCustomRule.createForCustomRule(CUSTOM_RULE_KEY, templateRule.getKey())
      .setMarkdownDescription("some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(Map.of("regex", "a.*"));

    assertThatThrownBy(() -> underTest.create(dbSession, newRule))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("The name is missing");
  }

  @Test
  public void fail_to_create_custom_rule_when_missing_description() {
    // insert template rule
    RuleDto templateRule = createTemplateRule();

    NewCustomRule newRule = NewCustomRule.createForCustomRule(CUSTOM_RULE_KEY, templateRule.getKey())
      .setName("My custom")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(Map.of("regex", "a.*"));

    assertThatThrownBy(() -> underTest.create(dbSession, newRule))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("The description is missing");
  }

  @Test
  public void fail_to_create_custom_rule_when_invalid_severity() {
    // insert template rule
    RuleDto templateRule = createTemplateRule();

    NewCustomRule newRule = NewCustomRule.createForCustomRule(CUSTOM_RULE_KEY, templateRule.getKey())
      .setName("My custom")
      .setMarkdownDescription("some description")
      .setSeverity("INVALID")
      .setStatus(RuleStatus.READY)
      .setParameters(Map.of("regex", "a.*"));

    assertThatThrownBy(() -> underTest.create(dbSession, newRule))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Severity \"INVALID\" is invalid");
  }

  @Test
  public void fail_to_create_custom_rule_when_wrong_rule_template() {
    // insert rule
    RuleDto rule = newRule(RuleKey.of("java", "S001")).setIsTemplate(false);
    dbTester.rules().insert(rule);
    dbSession.commit();

    // Create custom rule with unknown template rule
    NewCustomRule newRule = NewCustomRule.createForCustomRule(CUSTOM_RULE_KEY, rule.getKey())
      .setName("My custom")
      .setMarkdownDescription("some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(Map.of("regex", "a.*"));

    assertThatThrownBy(() -> underTest.create(dbSession, newRule))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("This rule is not a template rule: java:S001");
  }

  @Test
  public void fail_to_create_custom_rule_when_null_custom_key() {
    assertThatThrownBy(() -> NewCustomRule.createForCustomRule(null, CUSTOM_RULE_KEY))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Custom key should be set");
  }

  @Test
  public void fail_to_create_custom_rule_when_null_template() {
    assertThatThrownBy(() -> NewCustomRule.createForCustomRule(CUSTOM_RULE_KEY, null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Template key should be set");
  }

  @Test
  public void fail_to_create_custom_rule_when_unknown_template() {
    // Create custom rule
    NewCustomRule newRule = NewCustomRule.createForCustomRule(CUSTOM_RULE_KEY, RuleKey.of("java", "S001"))
      .setName("My custom")
      .setMarkdownDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY);

    assertThatThrownBy(() -> underTest.create(dbSession, newRule))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The template key doesn't exist: java:S001");
  }

  @Test
  public void create_givenSecurityHotspotRule_doNotSetCleanCodeAttribute() {
    RuleDto templateRule = createTemplateRule();

    NewCustomRule newRule = NewCustomRule.createForCustomRule(RuleKey.parse("java:security_hotspots_rule"), templateRule.getKey())
      .setName("My custom")
      .setMarkdownDescription("some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setType(RuleType.SECURITY_HOTSPOT);

    RuleKey customRuleKey = underTest.create(dbSession, newRule).getKey();

    RuleDto result = dbTester.getDbClient().ruleDao().selectOrFailByKey(dbSession, customRuleKey);

    assertThat(result.getCleanCodeAttribute()).isNull();
  }

  private RuleDto createTemplateRule() {
    RuleDto templateRule = RuleTesting.newRule(RuleKey.of("java", "S001"))
      .setIsTemplate(true)
      .setLanguage("java")
      .setPluginKey("sonarjava")
      .setConfigKey("S001")
      .setDefRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
      .setDefRemediationGapMultiplier("1h")
      .setDefRemediationBaseEffort("5min")
      .setGapDescription("desc")
      .setTags(Sets.newHashSet("usertag1", "usertag2"))
      .setSystemTags(Sets.newHashSet("tag1", "tag4"))
      .setSecurityStandards(Sets.newHashSet("owaspTop10:a1", "cwe:123"))
      .setCreatedAt(new Date().getTime())
      .setUpdatedAt(new Date().getTime());
    dbTester.rules().insert(templateRule);
    dbTester.rules().insertRuleParam(templateRule, param -> param.setName("regex").setType("STRING").setDescription("Reg ex").setDefaultValue(".*"));
    ruleIndexer.commitAndIndex(dbTester.getSession(), templateRule.getUuid());
    return templateRule;
  }

  private RuleDto createTemplateRuleWithIntArrayParam() {
    RuleDto templateRule = newRule(RuleKey.of("java", "S002"))
      .setIsTemplate(true)
      .setLanguage("java")
      .setConfigKey("S002")
      .setDefRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
      .setDefRemediationGapMultiplier("1h")
      .setDefRemediationBaseEffort("5min")
      .setGapDescription("desc")
      .setCreatedAt(new Date().getTime())
      .setUpdatedAt(new Date().getTime());
    dbTester.rules().insert(templateRule);
    dbTester.rules().insertRuleParam(templateRule,
      param -> param.setName("myIntegers").setType("INTEGER,multiple=true,values=1;2;3").setDescription("My Integers").setDefaultValue("1"));
    ruleIndexer.commitAndIndex(dbTester.getSession(), templateRule.getUuid());
    return templateRule;
  }

  private RuleDto createTemplateRuleWithTwoIntParams() {
    RuleDto templateRule = newRule(RuleKey.of("java", "S003"))
      .setIsTemplate(true)
      .setLanguage("java")
      .setConfigKey("S003")
      .setDefRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
      .setDefRemediationGapMultiplier("1h")
      .setDefRemediationBaseEffort("5min")
      .setGapDescription("desc")
      .setCreatedAt(new Date().getTime())
      .setUpdatedAt(new Date().getTime());
    dbTester.rules().insert(templateRule);
    dbTester.rules().insertRuleParam(templateRule, param -> param.setName("first").setType("INTEGER").setDescription("First integer").setDefaultValue("0"));
    dbTester.rules().insertRuleParam(templateRule, param -> param.setName("second").setType("INTEGER").setDescription("Second integer").setDefaultValue("0"));
    return templateRule;
  }

}
