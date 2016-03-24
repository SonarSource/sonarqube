/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.debt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.debt.DebtModelXMLExporter.RuleDebt;
import org.sonar.server.rule.RuleDefinitionsLoader;
import org.sonar.server.rule.RuleOperations;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.tester.UserSessionRule;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DebtModelBackupTest {

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  @Mock
  DbClient dbClient;
  @Mock
  DbSession session;
  @Mock
  RuleDao ruleDao;
  @Mock
  RuleOperations ruleOperations;
  @Mock
  DebtRulesXMLImporter rulesXMLImporter;
  @Mock
  DebtModelXMLExporter debtModelXMLExporter;
  @Mock
  RuleDefinitionsLoader defLoader;
  @Mock
  System2 system2;
  @Mock
  RuleIndexer ruleIndexer;
  @Captor
  ArgumentCaptor<RuleDto> ruleCaptor;
  @Captor
  ArgumentCaptor<ArrayList<RuleDebt>> ruleDebtListCaptor;

  Date now = DateUtils.parseDate("2014-03-19");

  int currentId;

  DebtModelBackup underTest;

  @Before
  public void setUp() {
    userSessionRule.setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    when(system2.now()).thenReturn(now.getTime());

    currentId = 10;
    when(dbClient.openSession(false)).thenReturn(session);
    when(dbClient.ruleDao()).thenReturn(ruleDao);

    underTest = new DebtModelBackup(dbClient, ruleOperations, rulesXMLImporter,
      debtModelXMLExporter, defLoader, system2, userSessionRule, ruleIndexer);
  }

  @Test
  public void backup() {
    when(ruleDao.selectEnabled(session)).thenReturn(
      newArrayList(
        // Rule with overridden debt values
        new RuleDto().setRepositoryKey("squid").setRuleKey("UselessImportCheck")
          .setRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.toString())
          .setRemediationGapMultiplier("2h")
          .setRemediationBaseEffort("15min"),

        // Rule with default debt values
        new RuleDto().setRepositoryKey("squid").setRuleKey("AvoidNPE")
          .setDefaultRemediationFunction("LINEAR").setDefaultRemediationGapMultiplier("2h")
      ));

    underTest.backup();

    verify(debtModelXMLExporter).export(ruleDebtListCaptor.capture());
    List<RuleDebt> rules = ruleDebtListCaptor.getValue();
    assertThat(rules).hasSize(2);

    RuleDebt rule = rules.get(0);
    assertThat(rule.ruleKey().repository()).isEqualTo("squid");
    assertThat(rule.ruleKey().rule()).isEqualTo("UselessImportCheck");
    assertThat(rule.function()).isEqualTo("LINEAR_OFFSET");
    assertThat(rule.coefficient()).isEqualTo("2h");
    assertThat(rule.offset()).isEqualTo("15min");

    rule = rules.get(1);
    assertThat(rule.ruleKey().repository()).isEqualTo("squid");
    assertThat(rule.ruleKey().rule()).isEqualTo("AvoidNPE");
    assertThat(rule.function()).isEqualTo("LINEAR");
    assertThat(rule.coefficient()).isEqualTo("2h");
    assertThat(rule.offset()).isNull();
  }

  @Test
  public void backup_with_disabled_rules() {
    when(ruleDao.selectEnabled(session)).thenReturn(newArrayList(
      // Debt disabled
      new RuleDto().setRepositoryKey("squid").setRuleKey("UselessImportCheck"),

      // Not debt
      new RuleDto().setRepositoryKey("squid").setRuleKey("AvoidNPE")
      ));

    underTest.backup();

    verify(debtModelXMLExporter).export(ruleDebtListCaptor.capture());

    assertThat(ruleDebtListCaptor.getValue()).isEmpty();
  }

  @Test
  public void backup_with_rule_having_default_linear_and_overridden_offset() {
    when(ruleDao.selectEnabled(session)).thenReturn(newArrayList(
      // Rule with default debt values : default value is linear (only coefficient is set) and overridden value is constant per issue (only
      // offset is set)
      // -> Ony offset should be set
      new RuleDto().setRepositoryKey("squid").setRuleKey("AvoidNPE")
        .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR.toString())
        .setDefaultRemediationGapMultiplier("2h")
        .setRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE.toString())
        .setRemediationBaseEffort("15min")
      ));

    underTest.backup();

    verify(debtModelXMLExporter).export(ruleDebtListCaptor.capture());

    List<RuleDebt> rules = ruleDebtListCaptor.getValue();
    assertThat(rules).hasSize(1);

    RuleDebt rule = rules.get(0);
    assertThat(rule.ruleKey().repository()).isEqualTo("squid");
    assertThat(rule.ruleKey().rule()).isEqualTo("AvoidNPE");
    assertThat(rule.function()).isEqualTo("CONSTANT_ISSUE");
    assertThat(rule.offset()).isEqualTo("15min");
    assertThat(rule.coefficient()).isNull();
  }

  @Test
  public void backup_from_language() {
    when(ruleDao.selectEnabled(session)).thenReturn(newArrayList(
      new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck").setLanguage("java")
        .setRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE.toString())
        .setRemediationBaseEffort("15min"),
      // Should be ignored
      new RuleDto().setId(2).setRepositoryKey("checkstyle")
        .setLanguage("java2")
        .setRemediationFunction(DebtRemediationFunction.Type.LINEAR.toString())
        .setRemediationGapMultiplier("2h")
      ));

    underTest.backup("java");

    verify(debtModelXMLExporter).export(ruleDebtListCaptor.capture());

    List<RuleDebt> rules = ruleDebtListCaptor.getValue();
    assertThat(rules).hasSize(1);

    RuleDebt rule = rules.get(0);
    assertThat(rule.ruleKey().repository()).isEqualTo("squid");
    assertThat(rule.ruleKey().rule()).isEqualTo("UselessImportCheck");
    assertThat(rule.function()).isEqualTo("CONSTANT_ISSUE");
    assertThat(rule.coefficient()).isNull();
    assertThat(rule.offset()).isEqualTo("15min");
  }

  @Test
  public void reset_model() {
    when(ruleDao.selectEnabled(session)).thenReturn(newArrayList(
      new RuleDto().setRepositoryKey("squid").setRuleKey("NPE")
        .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR.toString())
        .setDefaultRemediationGapMultiplier("2h")
        .setRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.toString())
        .setRemediationGapMultiplier("2h")
        .setRemediationBaseEffort("15min")
      ));

    RulesDefinition.Context context = new RulesDefinition.Context();
    RulesDefinition.NewRepository repo = context.createRepository("squid", "java").setName("Squid");
    RulesDefinition.NewRule newRule = repo.createRule("NPE")
      .setName("Detect NPE")
      .setHtmlDescription("Detect <code>java.lang.NullPointerException</code>")
      .setSeverity(Severity.BLOCKER)
      .setStatus(RuleStatus.BETA);
    newRule.setDebtRemediationFunction(newRule.debtRemediationFunctions().linearWithOffset("4h", "20min"));
    repo.done();
    when(defLoader.load()).thenReturn(context);

    underTest.reset();

    verify(ruleDao).selectEnabled(session);
    verify(ruleDao).update(eq(session), ruleCaptor.capture());
    verifyNoMoreInteractions(ruleDao);

    verify(session).commit();
    verify(ruleIndexer).index();

    RuleDto rule = ruleCaptor.getValue();

    assertThat(rule.getDefaultRemediationFunction()).isEqualTo("LINEAR_OFFSET");
    assertThat(rule.getDefaultRemediationGapMultiplier()).isEqualTo("4h");
    assertThat(rule.getDefaultRemediationBaseEffort()).isEqualTo("20min");
    assertThat(rule.getUpdatedAt()).isEqualTo(now.getTime());

    assertThat(rule.getRemediationFunction()).isNull();
    assertThat(rule.getRemediationGapMultiplier()).isNull();
    assertThat(rule.getRemediationBaseEffort()).isNull();
    assertThat(rule.getUpdatedAt()).isEqualTo(now.getTime());
  }

  @Test
  public void reset_model_when_no_default_value() {
    when(ruleDao.selectEnabled(session)).thenReturn(newArrayList(
      new RuleDto().setRepositoryKey("squid").setRuleKey("NPE")
        .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR.toString())
        .setDefaultRemediationGapMultiplier("2h")
        .setRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.toString())
        .setRemediationGapMultiplier("2h")
        .setRemediationBaseEffort("15min")
      ));

    RulesDefinition.Context context = new RulesDefinition.Context();
    RulesDefinition.NewRepository repo = context.createRepository("squid", "java").setName("Squid");
    repo.createRule("NPE")
      .setName("Detect NPE")
      .setHtmlDescription("Detect <code>java.lang.NullPointerException</code>")
      .setSeverity(Severity.BLOCKER)
      .setStatus(RuleStatus.BETA);
    repo.done();
    when(defLoader.load()).thenReturn(context);

    underTest.reset();

    verify(ruleDao).selectEnabled(session);
    verify(ruleDao).update(eq(session), ruleCaptor.capture());
    verifyNoMoreInteractions(ruleDao);

    verify(session).commit();
    verify(ruleIndexer).index();

    RuleDto rule = ruleCaptor.getValue();
    assertThat(rule.getDefaultRemediationFunction()).isNull();
    assertThat(rule.getDefaultRemediationGapMultiplier()).isNull();
    assertThat(rule.getDefaultRemediationBaseEffort()).isNull();
    assertThat(rule.getUpdatedAt()).isEqualTo(now.getTime());
  }

  @Test
  public void reset_model_on_custom_rules() {
    when(ruleDao.selectEnabled(session)).thenReturn(newArrayList(
      // Template rule
      new RuleDto().setId(5).setRepositoryKey("squid").setRuleKey("XPath")
        .setRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.toString())
        .setRemediationGapMultiplier("2h")
        .setRemediationBaseEffort("15min"),
      // Custom rule
      new RuleDto().setId(6).setRepositoryKey("squid").setRuleKey("XPath_1369910135").setTemplateId(5)
        .setRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.toString())
        .setRemediationGapMultiplier("2h")
        .setRemediationBaseEffort("15min")
      ));

    RulesDefinition.Context context = new RulesDefinition.Context();
    // Template rule
    RulesDefinition.NewRepository repo = context.createRepository("squid", "java").setName("XPath");
    RulesDefinition.NewRule newRule = repo.createRule("XPath")
      .setName("XPath")
      .setHtmlDescription("XPath")
      .setSeverity(Severity.BLOCKER)
      .setStatus(RuleStatus.BETA);
    newRule.setDebtRemediationFunction(newRule.debtRemediationFunctions().linearWithOffset("4h", "20min"));
    repo.done();
    when(defLoader.load()).thenReturn(context);

    underTest.reset();

    verify(ruleDao).selectEnabled(session);
    verify(ruleDao, times(2)).update(eq(session), ruleCaptor.capture());
    verifyNoMoreInteractions(ruleDao);
    verify(session).commit();
    verify(ruleIndexer).index();

    RuleDto rule = ruleCaptor.getAllValues().get(1);

    assertThat(rule.getId()).isEqualTo(6);
    assertThat(rule.getDefaultRemediationFunction()).isEqualTo("LINEAR_OFFSET");
    assertThat(rule.getDefaultRemediationGapMultiplier()).isEqualTo("4h");
    assertThat(rule.getDefaultRemediationBaseEffort()).isEqualTo("20min");
    assertThat(rule.getUpdatedAt()).isEqualTo(now.getTime());

    assertThat(rule.getRemediationFunction()).isNull();
    assertThat(rule.getRemediationGapMultiplier()).isNull();
    assertThat(rule.getRemediationBaseEffort()).isNull();
    assertThat(rule.getUpdatedAt()).isEqualTo(now.getTime());
  }

  @Test
  public void reset_model_do_not_load_rule_definitions_if_no_rule() {
    when(ruleDao.selectEnabled(session)).thenReturn(Collections.<RuleDto>emptyList());

    underTest.reset();

    verify(ruleDao).selectEnabled(session);
    verify(ruleDao, never()).update(eq(session), any(RuleDto.class));
    verifyZeroInteractions(defLoader);

    verify(session).commit();
    verify(ruleIndexer).index();
  }

  @Test
  public void restore_from_xml() {
    when(rulesXMLImporter.importXML(anyString(), any(ValidationMessages.class))).thenReturn(newArrayList(new RuleDebt()
      .setRuleKey(RuleKey.of("squid", "UselessImportCheck"))
      .setFunction(DebtRemediationFunction.Type.LINEAR.name()).setCoefficient("2h")));

    when(ruleDao.selectEnabled(session)).thenReturn(newArrayList(
      new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck")
        .setDefaultRemediationFunction("LINEAR").setDefaultRemediationGapMultiplier("2h")
      ));

    underTest.restoreFromXml("<xml/>");

    verify(ruleOperations).updateRule(ruleCaptor.capture(), eq("LINEAR"), eq("2h"), isNull(String.class), eq(session));

    verify(ruleDao).selectEnabled(session);
    verify(session).commit();
    verify(ruleIndexer).index();
  }

  @Test
  public void restore_from_xml_disable_rule_debt_when_not_in_xml_and_rule_have_default_debt_values() {
    when(ruleDao.selectEnabled(session)).thenReturn(newArrayList(
      new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck")
        .setDefaultRemediationFunction("LINEAR_OFFSET").setDefaultRemediationGapMultiplier("2h").setDefaultRemediationBaseEffort("15min")
      ));

    underTest.restoreFromXml("<xml/>");

    verify(ruleOperations).updateRule(ruleCaptor.capture(), isNull(String.class), isNull(String.class), isNull(String.class), eq(session));

    verify(ruleDao).selectEnabled(session);
    verify(session).commit();
    verify(ruleIndexer).index();
  }

  @Test
  public void restore_from_xml_and_language() {
    when(rulesXMLImporter.importXML(anyString(), any(ValidationMessages.class))).thenReturn(newArrayList(new RuleDebt()
      .setRuleKey(RuleKey.of("squid", "UselessImportCheck")).setFunction(DebtRemediationFunction.Type.LINEAR.name()).setCoefficient("2h")));

    when(ruleDao.selectEnabled(session)).thenReturn(newArrayList(
      new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck").setLanguage("java")
        .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR.toString())
        .setDefaultRemediationGapMultiplier("2h"),
      // Should be ignored
      new RuleDto().setId(2).setRepositoryKey("checkstyle").setLanguage("java2")
        .setRemediationFunction(DebtRemediationFunction.Type.LINEAR.toString())
        .setRemediationGapMultiplier("2h")
      ));

    underTest.restoreFromXml("<xml/>", "java");

    verify(ruleOperations).updateRule(ruleCaptor.capture(), eq("LINEAR"), eq("2h"), isNull(String.class), eq(session));

    verify(ruleDao).selectEnabled(session);
    verify(session).commit();
    verify(ruleIndexer).index();
  }

  @Test
  public void restore_from_xml_and_language_with_rule_not_in_xml() {
    when(rulesXMLImporter.importXML(anyString(), any(ValidationMessages.class))).thenReturn(Collections.<RuleDebt>emptyList());
    when(ruleDao.selectEnabled(session)).thenReturn(newArrayList(
      // Rule does not exits in XML -> debt will be disabled
      new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck").setLanguage("java")
        .setDefaultRemediationFunction("LINEAR").setDefaultRemediationGapMultiplier("2h")
        .setRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.toString())
        .setRemediationGapMultiplier("2h")
        .setRemediationBaseEffort("15min")
      ));

    underTest.restoreFromXml("<xml/>", "java");

    verify(ruleOperations).updateRule(ruleCaptor.capture(), isNull(String.class), isNull(String.class), isNull(String.class), eq(session));

    verify(ruleDao).selectEnabled(session);
    verify(session).commit();
    verify(ruleIndexer).index();
  }

  @Test
  public void restore_from_xml_add_warning_message_when_rule_from_xml_is_not_found() {
    when(rulesXMLImporter.importXML(anyString(), any(ValidationMessages.class))).thenReturn(newArrayList(new RuleDebt()
      .setRuleKey(RuleKey.of("squid", "UselessImportCheck")).setFunction(DebtRemediationFunction.Type.LINEAR.name()).setCoefficient("2h")));

    when(ruleDao.selectEnabled(session)).thenReturn(Collections.<RuleDto>emptyList());

    assertThat(underTest.restoreFromXml("<xml/>").getWarnings()).hasSize(1);

    verifyZeroInteractions(ruleOperations);

    verify(ruleDao).selectEnabled(session);
    verify(session).commit();
    verify(ruleIndexer).index();
  }

  @Test
  public void restore_from_xml_add_error_message_when_illegal_argument_exception() {
    when(rulesXMLImporter.importXML(anyString(), any(ValidationMessages.class))).thenReturn(newArrayList(new RuleDebt()
      .setRuleKey(RuleKey.of("squid", "UselessImportCheck")).setFunction(DebtRemediationFunction.Type.LINEAR.name()).setCoefficient("2h")));

    when(ruleDao.selectEnabled(session)).thenReturn(newArrayList(
      new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck")
        .setDefaultRemediationFunction("LINEAR").setDefaultRemediationGapMultiplier("2h")
      ));

    when(ruleOperations.updateRule(any(RuleDto.class), anyString(), anyString(), anyString(), eq(session))).thenThrow(IllegalArgumentException.class);

    assertThat(underTest.restoreFromXml("<xml/>").getErrors()).hasSize(1);

    verify(ruleDao).selectEnabled(session);
    verify(session, never()).commit();
    verify(ruleIndexer, never()).index();
  }

}
