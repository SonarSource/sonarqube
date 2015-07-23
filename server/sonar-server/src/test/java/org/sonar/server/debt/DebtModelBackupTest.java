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

package org.sonar.server.debt;

import java.io.Reader;
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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.debt.internal.DefaultDebtCharacteristic;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbSession;
import org.sonar.db.debt.CharacteristicDao;
import org.sonar.db.debt.CharacteristicDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.debt.DebtModelXMLExporter.DebtModel;
import org.sonar.server.debt.DebtModelXMLExporter.RuleDebt;
import org.sonar.server.rule.RuleDefinitionsLoader;
import org.sonar.server.rule.RuleOperations;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.tester.UserSessionRule;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
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
  DebtModelPluginRepository debtModelPluginRepository;
  @Mock
  CharacteristicDao dao;
  @Mock
  RuleDao ruleDao;
  @Mock
  DebtModelOperations debtModelOperations;
  @Mock
  RuleOperations ruleOperations;
  @Mock
  DebtCharacteristicsXMLImporter characteristicsXMLImporter;
  @Mock
  DebtRulesXMLImporter rulesXMLImporter;
  @Mock
  DebtModelXMLExporter debtModelXMLExporter;
  @Mock
  RuleDefinitionsLoader defLoader;
  @Mock
  System2 system2;

  @Captor
  ArgumentCaptor<CharacteristicDto> characteristicCaptor;
  @Captor
  ArgumentCaptor<RuleDto> ruleCaptor;
  @Captor
  ArgumentCaptor<ArrayList<RuleDebt>> ruleDebtListCaptor;

  Date oldDate = DateUtils.parseDate("2014-01-01");
  Date now = DateUtils.parseDate("2014-03-19");

  int currentId;

  DebtModelBackup underTest;

  @Before
  public void setUp() {
    userSessionRule.setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    when(system2.now()).thenReturn(now.getTime());

    currentId = 10;
    // Associate an id when inserting an object to simulate the db id generator
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        CharacteristicDto dto = (CharacteristicDto) args[1];
        dto.setId(currentId++);
        return null;
      }
    }).when(dao).insert(any(DbSession.class), any(CharacteristicDto.class));

    when(dbClient.openSession(false)).thenReturn(session);
    when(dbClient.deprecatedRuleDao()).thenReturn(ruleDao);
    when(dbClient.debtCharacteristicDao()).thenReturn(dao);

    Reader defaultModelReader = mock(Reader.class);
    when(debtModelPluginRepository.createReaderForXMLFile("technical-debt")).thenReturn(defaultModelReader);

    underTest = new DebtModelBackup(dbClient, debtModelOperations, ruleOperations, debtModelPluginRepository, characteristicsXMLImporter, rulesXMLImporter,
      debtModelXMLExporter, defLoader, system2, userSessionRule);
  }

  @Test
  public void backup() {
    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability updated").setOrder(2),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler updated").setParentId(1)
      ));

    when(ruleDao.selectEnabledAndNonManual(session)).thenReturn(
      newArrayList(
        // Rule with overridden debt values
        new RuleDto().setRepositoryKey("squid").setRuleKey("UselessImportCheck")
          .setSubCharacteristicId(2)
          .setRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.toString())
          .setRemediationCoefficient("2h")
          .setRemediationOffset("15min"),

        // Rule with default debt values
        new RuleDto().setRepositoryKey("squid").setRuleKey("AvoidNPE").setDefaultSubCharacteristicId(2).setDefaultRemediationFunction("LINEAR")
          .setDefaultRemediationCoefficient("2h")
      ));

    underTest.backup();

    ArgumentCaptor<DebtModel> debtModelArgument = ArgumentCaptor.forClass(DebtModel.class);
    verify(debtModelXMLExporter).export(debtModelArgument.capture(), ruleDebtListCaptor.capture());
    assertThat(debtModelArgument.getValue().rootCharacteristics()).hasSize(1);
    assertThat(debtModelArgument.getValue().subCharacteristics("PORTABILITY")).hasSize(1);

    List<RuleDebt> rules = ruleDebtListCaptor.getValue();
    assertThat(rules).hasSize(2);

    RuleDebt rule = rules.get(0);
    assertThat(rule.ruleKey().repository()).isEqualTo("squid");
    assertThat(rule.ruleKey().rule()).isEqualTo("UselessImportCheck");
    assertThat(rule.subCharacteristicKey()).isEqualTo("COMPILER");
    assertThat(rule.function()).isEqualTo("LINEAR_OFFSET");
    assertThat(rule.coefficient()).isEqualTo("2h");
    assertThat(rule.offset()).isEqualTo("15min");

    rule = rules.get(1);
    assertThat(rule.ruleKey().repository()).isEqualTo("squid");
    assertThat(rule.ruleKey().rule()).isEqualTo("AvoidNPE");
    assertThat(rule.subCharacteristicKey()).isEqualTo("COMPILER");
    assertThat(rule.function()).isEqualTo("LINEAR");
    assertThat(rule.coefficient()).isEqualTo("2h");
    assertThat(rule.offset()).isNull();
  }

  @Test
  public void backup_with_disabled_rules() {
    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability updated").setOrder(2),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler updated").setParentId(1)
      ));

    when(ruleDao.selectEnabledAndNonManual(session)).thenReturn(newArrayList(
      // Debt disabled
      new RuleDto().setRepositoryKey("squid").setRuleKey("UselessImportCheck").setSubCharacteristicId(RuleDto.DISABLED_CHARACTERISTIC_ID),

      // Not debt
      new RuleDto().setRepositoryKey("squid").setRuleKey("AvoidNPE")
      ));

    underTest.backup();

    verify(debtModelXMLExporter).export(any(DebtModel.class), ruleDebtListCaptor.capture());

    assertThat(ruleDebtListCaptor.getValue()).isEmpty();
  }

  @Test
  public void backup_with_rule_having_default_linear_and_overridden_offset() {
    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability updated").setOrder(2),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler updated").setParentId(1)
      ));

    when(ruleDao.selectEnabledAndNonManual(session)).thenReturn(newArrayList(
      // Rule with default debt values : default value is linear (only coefficient is set) and overridden value is constant per issue (only
      // offset is set)
      // -> Ony offset should be set
      new RuleDto().setRepositoryKey("squid").setRuleKey("AvoidNPE")
        .setDefaultSubCharacteristicId(2)
        .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR.toString())
        .setDefaultRemediationCoefficient("2h")
        .setSubCharacteristicId(2)
        .setRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE.toString())
        .setRemediationOffset("15min")
      ));

    underTest.backup();

    ArgumentCaptor<DebtModel> debtModelArgument = ArgumentCaptor.forClass(DebtModel.class);
    verify(debtModelXMLExporter).export(debtModelArgument.capture(), ruleDebtListCaptor.capture());
    assertThat(debtModelArgument.getValue().rootCharacteristics()).hasSize(1);
    assertThat(debtModelArgument.getValue().subCharacteristics("PORTABILITY")).hasSize(1);

    List<RuleDebt> rules = ruleDebtListCaptor.getValue();
    assertThat(rules).hasSize(1);

    RuleDebt rule = rules.get(0);
    assertThat(rule.ruleKey().repository()).isEqualTo("squid");
    assertThat(rule.ruleKey().rule()).isEqualTo("AvoidNPE");
    assertThat(rule.subCharacteristicKey()).isEqualTo("COMPILER");
    assertThat(rule.function()).isEqualTo("CONSTANT_ISSUE");
    assertThat(rule.offset()).isEqualTo("15min");
    assertThat(rule.coefficient()).isNull();
  }

  @Test
  public void backup_from_language() {
    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability updated").setOrder(2),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler updated").setParentId(1)
      ));

    when(ruleDao.selectEnabledAndNonManual(session)).thenReturn(newArrayList(
      new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck").setLanguage("java")
        .setSubCharacteristicId(2)
        .setRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE.toString())
        .setRemediationOffset("15min"),
      // .setCreatedAt(oldDate).setUpdatedAt(oldDate),
      // Should be ignored
      new RuleDto().setId(2).setRepositoryKey("checkstyle")
        .setLanguage("java2")
        .setSubCharacteristicId(3)
        .setRemediationFunction(DebtRemediationFunction.Type.LINEAR.toString())
        .setRemediationCoefficient("2h")
      // .setCreatedAt(oldDate).setUpdatedAt(oldDate)
      ));

    underTest.backup("java");

    verify(debtModelXMLExporter).export(any(DebtModel.class), ruleDebtListCaptor.capture());

    List<RuleDebt> rules = ruleDebtListCaptor.getValue();
    assertThat(rules).hasSize(1);

    RuleDebt rule = rules.get(0);
    assertThat(rule.ruleKey().repository()).isEqualTo("squid");
    assertThat(rule.ruleKey().rule()).isEqualTo("UselessImportCheck");
    assertThat(rule.subCharacteristicKey()).isEqualTo("COMPILER");
    assertThat(rule.function()).isEqualTo("CONSTANT_ISSUE");
    assertThat(rule.coefficient()).isNull();
    assertThat(rule.offset()).isEqualTo("15min");
  }

  @Test
  public void create_characteristics_when_restoring_characteristics() {
    when(dao.selectEnabledCharacteristics(session)).thenReturn(Collections.<CharacteristicDto>emptyList());

    underTest.restoreCharacteristics(
      session,
      new DebtModel()
        .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
        .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY"),
      now
    );

    verify(dao, times(2)).insert(eq(session), characteristicCaptor.capture());

    CharacteristicDto dto1 = characteristicCaptor.getAllValues().get(0);
    assertThat(dto1.getId()).isEqualTo(10);
    assertThat(dto1.getKey()).isEqualTo("PORTABILITY");
    assertThat(dto1.getName()).isEqualTo("Portability");
    assertThat(dto1.getParentId()).isNull();
    assertThat(dto1.getOrder()).isEqualTo(1);
    assertThat(dto1.getCreatedAt()).isEqualTo(now);
    assertThat(dto1.getUpdatedAt()).isNull();

    CharacteristicDto dto2 = characteristicCaptor.getAllValues().get(1);
    assertThat(dto2.getId()).isEqualTo(11);
    assertThat(dto2.getKey()).isEqualTo("COMPILER");
    assertThat(dto2.getName()).isEqualTo("Compiler");
    assertThat(dto2.getParentId()).isEqualTo(10);
    assertThat(dto2.getOrder()).isNull();
    assertThat(dto2.getCreatedAt()).isEqualTo(now);
    assertThat(dto2.getUpdatedAt()).isNull();
  }

  @Test
  public void update_characteristics_when_restoring_characteristics() {
    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(
      // Order and name have changed
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability updated").setOrder(2).setCreatedAt(oldDate).setUpdatedAt(oldDate),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler updated").setParentId(1).setCreatedAt(oldDate).setUpdatedAt(oldDate)
      ));

    underTest.restoreCharacteristics(
      session, new DebtModel()
        .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
        .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY"),
      now
    );

    verify(dao, times(2)).update(characteristicCaptor.capture(), eq(session));

    CharacteristicDto dto1 = characteristicCaptor.getAllValues().get(0);
    assertThat(dto1.getId()).isEqualTo(1);
    assertThat(dto1.getKey()).isEqualTo("PORTABILITY");
    assertThat(dto1.getName()).isEqualTo("Portability");
    assertThat(dto1.getParentId()).isNull();
    assertThat(dto1.getOrder()).isEqualTo(1);
    assertThat(dto1.getCreatedAt()).isEqualTo(oldDate);
    assertThat(dto1.getUpdatedAt()).isEqualTo(now);

    CharacteristicDto dto2 = characteristicCaptor.getAllValues().get(1);
    assertThat(dto2.getId()).isEqualTo(2);
    assertThat(dto2.getKey()).isEqualTo("COMPILER");
    assertThat(dto2.getName()).isEqualTo("Compiler");
    assertThat(dto2.getParentId()).isEqualTo(1);
    assertThat(dto2.getOrder()).isNull();
    assertThat(dto2.getCreatedAt()).isEqualTo(oldDate);
    assertThat(dto2.getUpdatedAt()).isEqualTo(now);
  }

  @Test
  public void update_parent_when_restoring_characteristics() {
    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(
      // Parent has changed
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability updated").setParentId(1).setOrder(1).setCreatedAt(oldDate).setUpdatedAt(oldDate),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler updated").setCreatedAt(oldDate).setUpdatedAt(oldDate)
      ));

    underTest.restoreCharacteristics(
      session, new DebtModel()
        .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
        .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY"),
      now
    );

    verify(dao, times(2)).update(characteristicCaptor.capture(), eq(session));

    CharacteristicDto dto1 = characteristicCaptor.getAllValues().get(0);
    assertThat(dto1.getId()).isEqualTo(1);
    assertThat(dto1.getKey()).isEqualTo("PORTABILITY");
    assertThat(dto1.getParentId()).isNull();
    assertThat(dto1.getUpdatedAt()).isEqualTo(now);

    CharacteristicDto dto2 = characteristicCaptor.getAllValues().get(1);
    assertThat(dto2.getId()).isEqualTo(2);
    assertThat(dto2.getKey()).isEqualTo("COMPILER");
    assertThat(dto2.getParentId()).isEqualTo(1);
    assertThat(dto2.getUpdatedAt()).isEqualTo(now);
  }

  @Test
  public void disable_no_more_existing_characteristics_when_restoring_characteristics() {
    CharacteristicDto dto1 = new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability").setOrder(1);
    CharacteristicDto dto2 = new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler").setParentId(1);

    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(dto1, dto2));

    underTest.restoreCharacteristics(session, new DebtModel(), now);

    verify(debtModelOperations).delete(dto1, now, session);
    verify(debtModelOperations).delete(dto2, now, session);
  }

  @Test
  public void reset_model() {
    when(characteristicsXMLImporter.importXML(any(Reader.class))).thenReturn(new DebtModel()
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY"));

    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability updated").setOrder(2),// .setCreatedAt(oldDate),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler updated").setParentId(1)// .setCreatedAt(oldDate)
      ));

    when(ruleDao.selectEnabledAndNonManual(session)).thenReturn(newArrayList(
      new RuleDto().setRepositoryKey("squid").setRuleKey("NPE")
        .setDefaultSubCharacteristicId(10)
        .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR.toString())
        .setDefaultRemediationCoefficient("2h")
        .setSubCharacteristicId(2)
        .setRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.toString())
        .setRemediationCoefficient("2h")
        .setRemediationOffset("15min")
      ));

    RulesDefinition.Context context = new RulesDefinition.Context();
    RulesDefinition.NewRepository repo = context.createRepository("squid", "java").setName("Squid");
    RulesDefinition.NewRule newRule = repo.createRule("NPE")
      .setName("Detect NPE")
      .setHtmlDescription("Detect <code>java.lang.NullPointerException</code>")
      .setSeverity(Severity.BLOCKER)
      .setStatus(RuleStatus.BETA)
      .setDebtSubCharacteristic("COMPILER");
    newRule.setDebtRemediationFunction(newRule.debtRemediationFunctions().linearWithOffset("4h", "20min"));
    repo.done();
    when(defLoader.load()).thenReturn(context);

    underTest.reset();

    verify(dao).selectEnabledCharacteristics(session);
    verify(dao, times(2)).update(any(CharacteristicDto.class), eq(session));
    verifyNoMoreInteractions(dao);

    verify(ruleDao).selectEnabledAndNonManual(session);
    verify(ruleDao).update(eq(session), ruleCaptor.capture());
    verifyNoMoreInteractions(ruleDao);

    verify(session).commit();

    RuleDto rule = ruleCaptor.getValue();

    assertThat(rule.getDefaultSubCharacteristicId()).isEqualTo(2);
    assertThat(rule.getDefaultRemediationFunction()).isEqualTo("LINEAR_OFFSET");
    assertThat(rule.getDefaultRemediationCoefficient()).isEqualTo("4h");
    assertThat(rule.getDefaultRemediationOffset()).isEqualTo("20min");
    assertThat(rule.getUpdatedAt()).isEqualTo(now);

    assertThat(rule.getSubCharacteristicId()).isNull();
    assertThat(rule.getRemediationFunction()).isNull();
    assertThat(rule.getRemediationCoefficient()).isNull();
    assertThat(rule.getRemediationOffset()).isNull();
    assertThat(rule.getUpdatedAt()).isEqualTo(now);
  }

  @Test
  public void reset_model_when_no_default_value() {
    when(characteristicsXMLImporter.importXML(any(Reader.class))).thenReturn(new DebtModel()
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY"));

    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability updated").setOrder(2).setCreatedAt(oldDate),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler updated").setParentId(1).setCreatedAt(oldDate)
      ));

    when(ruleDao.selectEnabledAndNonManual(session)).thenReturn(newArrayList(
      new RuleDto().setRepositoryKey("squid").setRuleKey("NPE")
        .setDefaultSubCharacteristicId(10)
        .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR.toString())
        .setDefaultRemediationCoefficient("2h")
        .setSubCharacteristicId(2)
        .setRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.toString())
        .setRemediationCoefficient("2h")
        .setRemediationOffset("15min")
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

    verify(dao).selectEnabledCharacteristics(session);
    verify(dao, times(2)).update(any(CharacteristicDto.class), eq(session));
    verifyNoMoreInteractions(dao);

    verify(ruleDao).selectEnabledAndNonManual(session);
    verify(ruleDao).update(eq(session), ruleCaptor.capture());
    verifyNoMoreInteractions(ruleDao);

    verify(session).commit();

    RuleDto rule = ruleCaptor.getValue();
    assertThat(rule.getDefaultSubCharacteristicId()).isNull();
    assertThat(rule.getDefaultRemediationFunction()).isNull();
    assertThat(rule.getDefaultRemediationCoefficient()).isNull();
    assertThat(rule.getDefaultRemediationOffset()).isNull();
    assertThat(rule.getUpdatedAt()).isEqualTo(now);
  }

  @Test
  public void reset_model_on_custom_rules() {
    when(characteristicsXMLImporter.importXML(any(Reader.class))).thenReturn(new DebtModel()
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY"));

    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability updated").setOrder(2).setCreatedAt(oldDate),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler updated").setParentId(1).setCreatedAt(oldDate)
      ));

    when(ruleDao.selectEnabledAndNonManual(session)).thenReturn(newArrayList(
      // Template rule
      new RuleDto().setId(5).setRepositoryKey("squid").setRuleKey("XPath")
        .setSubCharacteristicId(2)
        .setRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.toString())
        .setRemediationCoefficient("2h")
        .setRemediationOffset("15min"),
      // Custom rule
      new RuleDto().setId(6).setRepositoryKey("squid").setRuleKey("XPath_1369910135").setTemplateId(5)
        .setSubCharacteristicId(2)
        .setRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.toString())
        .setRemediationCoefficient("2h")
        .setRemediationOffset("15min")
      ));

    RulesDefinition.Context context = new RulesDefinition.Context();
    // Template rule
    RulesDefinition.NewRepository repo = context.createRepository("squid", "java").setName("XPath");
    RulesDefinition.NewRule newRule = repo.createRule("XPath")
      .setName("XPath")
      .setHtmlDescription("XPath")
      .setSeverity(Severity.BLOCKER)
      .setStatus(RuleStatus.BETA)
      .setDebtSubCharacteristic("COMPILER");
    newRule.setDebtRemediationFunction(newRule.debtRemediationFunctions().linearWithOffset("4h", "20min"));
    repo.done();
    when(defLoader.load()).thenReturn(context);

    underTest.reset();

    verify(ruleDao).selectEnabledAndNonManual(session);
    verify(ruleDao, times(2)).update(eq(session), ruleCaptor.capture());
    verifyNoMoreInteractions(ruleDao);
    verify(session).commit();

    RuleDto rule = ruleCaptor.getAllValues().get(1);

    assertThat(rule.getId()).isEqualTo(6);
    assertThat(rule.getDefaultSubCharacteristicId()).isEqualTo(2);
    assertThat(rule.getDefaultRemediationFunction()).isEqualTo("LINEAR_OFFSET");
    assertThat(rule.getDefaultRemediationCoefficient()).isEqualTo("4h");
    assertThat(rule.getDefaultRemediationOffset()).isEqualTo("20min");
    assertThat(rule.getUpdatedAt()).isEqualTo(now);

    assertThat(rule.getSubCharacteristicId()).isNull();
    assertThat(rule.getRemediationFunction()).isNull();
    assertThat(rule.getRemediationCoefficient()).isNull();
    assertThat(rule.getRemediationOffset()).isNull();
    assertThat(rule.getUpdatedAt()).isEqualTo(now);
  }

  @Test
  public void reset_model_do_not_load_rule_definitions_if_no_rule() {
    when(characteristicsXMLImporter.importXML(any(Reader.class))).thenReturn(new DebtModel()
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY"));

    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability updated").setOrder(2).setCreatedAt(oldDate),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler updated").setParentId(1).setCreatedAt(oldDate)
      ));

    when(ruleDao.selectEnabledAndNonManual(session)).thenReturn(Collections.<RuleDto>emptyList());

    underTest.reset();

    verify(dao).selectEnabledCharacteristics(session);
    verify(dao, times(2)).update(any(CharacteristicDto.class), eq(session));
    verifyNoMoreInteractions(dao);

    verify(ruleDao).selectEnabledAndNonManual(session);
    verify(ruleDao, never()).update(eq(session), any(RuleDto.class));
    verifyZeroInteractions(defLoader);

    verify(session).commit();
  }

  @Test
  public void restore_from_xml() {
    when(characteristicsXMLImporter.importXML(anyString())).thenReturn(new DebtModel()
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY"));

    CharacteristicDto compiler = new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler").setParentId(1).setCreatedAt(oldDate);
    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability").setOrder(1).setCreatedAt(oldDate),
      new CharacteristicDto().setId(3).setKey("HARDWARE").setName("Hardware").setParentId(1).setCreatedAt(oldDate),
      compiler));

    when(rulesXMLImporter.importXML(anyString(), any(ValidationMessages.class))).thenReturn(newArrayList(new RuleDebt()
      .setRuleKey(RuleKey.of("squid", "UselessImportCheck"))
      .setSubCharacteristicKey("COMPILER")
      .setFunction(DebtRemediationFunction.Type.LINEAR.name()).setCoefficient("2h")));

    when(ruleDao.selectEnabledAndNonManual(session)).thenReturn(newArrayList(
      new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck")
        .setDefaultSubCharacteristicId(3).setDefaultRemediationFunction("LINEAR").setDefaultRemediationCoefficient("2h")
      // .setCreatedAt(oldDate).setUpdatedAt(oldDate)
      ));

    underTest.restoreFromXml("<xml/>");

    verify(ruleOperations).updateRule(ruleCaptor.capture(), eq(compiler), eq("LINEAR"), eq("2h"), isNull(String.class), eq(session));

    verify(ruleDao).selectEnabledAndNonManual(session);
    verify(session).commit();
  }

  @Test
  public void restore_from_xml_disable_rule_debt_when_not_in_xml_and_rule_have_default_debt_values() {
    when(characteristicsXMLImporter.importXML(anyString())).thenReturn(new DebtModel()
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY"));

    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability").setOrder(1).setCreatedAt(oldDate),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler").setParentId(1).setCreatedAt(oldDate)));

    when(ruleDao.selectEnabledAndNonManual(session)).thenReturn(newArrayList(
      new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck")
        .setDefaultSubCharacteristicId(2).setDefaultRemediationFunction("LINEAR_OFFSET").setDefaultRemediationCoefficient("2h").setDefaultRemediationOffset("15min")
      // .setCreatedAt(oldDate).setUpdatedAt(oldDate)
      ));

    underTest.restoreFromXml("<xml/>");

    verify(ruleOperations).updateRule(ruleCaptor.capture(), isNull(CharacteristicDto.class), isNull(String.class), isNull(String.class), isNull(String.class), eq(session));

    verify(ruleDao).selectEnabledAndNonManual(session);
    verify(session).commit();
  }

  @Test
  public void restore_from_xml_and_language() {
    when(characteristicsXMLImporter.importXML(anyString())).thenReturn(new DebtModel()
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY"));

    CharacteristicDto compiler = new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler").setParentId(1).setCreatedAt(oldDate);
    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability").setOrder(1).setCreatedAt(oldDate),
      compiler));

    when(rulesXMLImporter.importXML(anyString(), any(ValidationMessages.class))).thenReturn(newArrayList(new RuleDebt()
      .setRuleKey(RuleKey.of("squid", "UselessImportCheck")).setSubCharacteristicKey("COMPILER").setFunction(DebtRemediationFunction.Type.LINEAR.name()).setCoefficient("2h")));

    when(ruleDao.selectEnabledAndNonManual(session)).thenReturn(newArrayList(
      new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck").setLanguage("java")
        .setDefaultSubCharacteristicId(10)
        .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR.toString())
        .setDefaultRemediationCoefficient("2h"),
      // Should be ignored
      new RuleDto().setId(2).setRepositoryKey("checkstyle").setLanguage("java2")
        .setSubCharacteristicId(3)
        .setRemediationFunction(DebtRemediationFunction.Type.LINEAR.toString())
        .setRemediationCoefficient("2h")
      ));

    underTest.restoreFromXml("<xml/>", "java");

    verify(ruleOperations).updateRule(ruleCaptor.capture(), eq(compiler), eq("LINEAR"), eq("2h"), isNull(String.class), eq(session));

    verify(ruleDao).selectEnabledAndNonManual(session);
    verify(session).commit();
  }

  @Test
  public void restore_from_xml_and_language_disable_no_more_existing_characteristics() {
    when(characteristicsXMLImporter.importXML(anyString())).thenReturn(new DebtModel()
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1)));

    CharacteristicDto dto1 = new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability").setOrder(1);
    // No more existing characteristic
    CharacteristicDto dto2 = new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler").setParentId(1);
    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(dto1, dto2));

    underTest.restoreFromXml("<xml/>", "java");

    verify(debtModelOperations).delete(dto2, now, session);
    verify(session).commit();
  }

  @Test
  public void restore_from_xml_and_language_with_rule_not_in_xml() {
    when(characteristicsXMLImporter.importXML(anyString())).thenReturn(new DebtModel()
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY"));

    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability updated").setOrder(2).setCreatedAt(oldDate),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler updated").setParentId(1).setCreatedAt(oldDate)
      ));

    when(rulesXMLImporter.importXML(anyString(), any(ValidationMessages.class))).thenReturn(Collections.<RuleDebt>emptyList());
    when(ruleDao.selectEnabledAndNonManual(session)).thenReturn(newArrayList(
      // Rule does not exits in XML -> debt will be disabled
      new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck").setLanguage("java")
        .setDefaultSubCharacteristicId(2).setDefaultRemediationFunction("LINEAR").setDefaultRemediationCoefficient("2h")
        .setSubCharacteristicId(2)
        .setRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.toString())
        .setRemediationCoefficient("2h")
        .setRemediationOffset("15min")
      ));

    underTest.restoreFromXml("<xml/>", "java");

    verify(ruleOperations).updateRule(ruleCaptor.capture(), isNull(CharacteristicDto.class), isNull(String.class), isNull(String.class), isNull(String.class), eq(session));

    verify(ruleDao).selectEnabledAndNonManual(session);
    verify(session).commit();
  }

  @Test
  public void restore_from_xml_add_warning_message_when_rule_from_xml_is_not_found() {
    when(characteristicsXMLImporter.importXML(anyString())).thenReturn(new DebtModel()
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY"));

    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability").setOrder(1).setCreatedAt(oldDate),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler").setParentId(1).setCreatedAt(oldDate)));

    when(rulesXMLImporter.importXML(anyString(), any(ValidationMessages.class))).thenReturn(newArrayList(new RuleDebt()
      .setRuleKey(RuleKey.of("squid", "UselessImportCheck")).setSubCharacteristicKey("COMPILER").setFunction(DebtRemediationFunction.Type.LINEAR.name()).setCoefficient("2h")));

    when(ruleDao.selectEnabledAndNonManual(session)).thenReturn(Collections.<RuleDto>emptyList());

    assertThat(underTest.restoreFromXml("<xml/>").getWarnings()).hasSize(1);

    verifyZeroInteractions(ruleOperations);

    verify(ruleDao).selectEnabledAndNonManual(session);
    verify(session).commit();
  }

  @Test
  public void restore_from_xml_add_error_message_when_illegal_argument_exception() {
    when(characteristicsXMLImporter.importXML(anyString())).thenReturn(new DebtModel()
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY"));

    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability").setOrder(1).setCreatedAt(oldDate),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler").setParentId(1).setCreatedAt(oldDate)));

    when(rulesXMLImporter.importXML(anyString(), any(ValidationMessages.class))).thenReturn(newArrayList(new RuleDebt()
      .setRuleKey(RuleKey.of("squid", "UselessImportCheck")).setSubCharacteristicKey("COMPILER").setFunction(DebtRemediationFunction.Type.LINEAR.name()).setCoefficient("2h")));

    when(ruleDao.selectEnabledAndNonManual(session)).thenReturn(newArrayList(
      new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck")
        .setDefaultSubCharacteristicId(3).setDefaultRemediationFunction("LINEAR").setDefaultRemediationCoefficient("2h")
      // .setCreatedAt(oldDate).setUpdatedAt(oldDate)
      ));

    when(ruleOperations.updateRule(any(RuleDto.class), any(CharacteristicDto.class), anyString(), anyString(), anyString(), eq(session))).thenThrow(IllegalArgumentException.class);

    assertThat(underTest.restoreFromXml("<xml/>").getErrors()).hasSize(1);

    verify(ruleDao).selectEnabledAndNonManual(session);
    verify(session, never()).commit();
  }

}
