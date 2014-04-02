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

import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.debt.internal.DefaultDebtCharacteristic;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.rule.RuleDao;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.core.technicaldebt.db.CharacteristicDto;
import org.sonar.server.rule.RuleRegistry;
import org.sonar.server.user.MockUserSession;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.sonar.server.debt.DebtModelXMLExporter.DebtModel;
import static org.sonar.server.debt.DebtModelXMLExporter.RuleDebt;

@RunWith(MockitoJUnitRunner.class)
public class DebtModelBackupTest {

  @Mock
  MyBatis myBatis;

  @Mock
  SqlSession session;

  @Mock
  DebtModelPluginRepository debtModelPluginRepository;

  @Mock
  CharacteristicDao dao;

  @Mock
  RuleDao ruleDao;

  @Mock
  DebtModelOperations debtModelOperations;

  @Mock
  DebtCharacteristicsXMLImporter characteristicsXMLImporter;

  @Mock
  DebtRulesXMLImporter rulesXMLImporter;

  @Mock
  DebtModelXMLExporter debtModelXMLExporter;

  @Mock
  RuleRegistry ruleRegistry;

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

  DebtModelBackup debtModelBackup;

  @Before
  public void setUp() throws Exception {
    MockUserSession.set().setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    when(system2.now()).thenReturn(now.getTime());

    currentId = 10;
    // Associate an id when inserting an object to simulate the db id generator
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        CharacteristicDto dto = (CharacteristicDto) args[0];
        dto.setId(currentId++);
        return null;
      }
    }).when(dao).insert(any(CharacteristicDto.class), any(SqlSession.class));

    when(myBatis.openSession()).thenReturn(session);

    Reader defaultModelReader = mock(Reader.class);
    when(debtModelPluginRepository.createReaderForXMLFile("technical-debt")).thenReturn(defaultModelReader);

    debtModelBackup = new DebtModelBackup(myBatis, dao, ruleDao, debtModelOperations, debtModelPluginRepository, characteristicsXMLImporter, rulesXMLImporter,
      debtModelXMLExporter, ruleRegistry, system2);
  }

  @Test
  public void backup() throws Exception {
    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability updated").setOrder(2),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler updated").setParentId(1)
    ));

    when(ruleDao.selectEnablesAndNonManual(session)).thenReturn(newArrayList(
      // Rule with overridden debt values
      new RuleDto().setRepositoryKey("squid").setRuleKey("UselessImportCheck").setSubCharacteristicId(2).setRemediationFunction("LINEAR_OFFSET").setRemediationCoefficient("2h").setRemediationOffset("15min"),

      // Rule with default debt values
      new RuleDto().setRepositoryKey("squid").setRuleKey("AvoidNPE").setDefaultSubCharacteristicId(2).setDefaultRemediationFunction("LINEAR").setDefaultRemediationCoefficient("2h")
    ));

    debtModelBackup.backup();

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
    assertThat(rule.function().name()).isEqualTo("LINEAR_OFFSET");
    assertThat(rule.coefficient()).isEqualTo("2h");
    assertThat(rule.offset()).isEqualTo("15min");

    rule = rules.get(1);
    assertThat(rule.ruleKey().repository()).isEqualTo("squid");
    assertThat(rule.ruleKey().rule()).isEqualTo("AvoidNPE");
    assertThat(rule.subCharacteristicKey()).isEqualTo("COMPILER");
    assertThat(rule.function().name()).isEqualTo("LINEAR");
    assertThat(rule.coefficient()).isEqualTo("2h");
    assertThat(rule.offset()).isNull();
  }

  @Test
  public void backup_with_disabled_rules() throws Exception {
    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability updated").setOrder(2),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler updated").setParentId(1)
    ));

    when(ruleDao.selectEnablesAndNonManual(session)).thenReturn(newArrayList(
      // Debt disabled
      new RuleDto().setRepositoryKey("squid").setRuleKey("UselessImportCheck").setSubCharacteristicId(RuleDto.DISABLED_CHARACTERISTIC_ID),

      // Not debt
      new RuleDto().setRepositoryKey("squid").setRuleKey("AvoidNPE")
    ));

    debtModelBackup.backup();

    verify(debtModelXMLExporter).export(any(DebtModel.class), ruleDebtListCaptor.capture());

    assertThat(ruleDebtListCaptor.getValue()).isEmpty();
  }

  @Test
  public void backup_from_language() throws Exception {
    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability updated").setOrder(2),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler updated").setParentId(1)
    ));

    when(ruleDao.selectEnablesAndNonManual(session)).thenReturn(newArrayList(
      new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck").setLanguage("java")
        .setSubCharacteristicId(2).setRemediationFunction("CONSTANT_ISSUE").setRemediationOffset("15min")
        .setCreatedAt(oldDate).setUpdatedAt(oldDate),
      // Should be ignored
      new RuleDto().setId(2).setRepositoryKey("checkstyle").setLanguage("java2")
        .setSubCharacteristicId(3).setRemediationFunction("LINEAR").setRemediationCoefficient("2h")
        .setCreatedAt(oldDate).setUpdatedAt(oldDate)
    ));

    debtModelBackup.backup("java");

    verify(debtModelXMLExporter).export(any(DebtModel.class), ruleDebtListCaptor.capture());

    List<RuleDebt> rules = ruleDebtListCaptor.getValue();
    assertThat(rules).hasSize(1);

    RuleDebt rule = rules.get(0);
    assertThat(rule.ruleKey().repository()).isEqualTo("squid");
    assertThat(rule.ruleKey().rule()).isEqualTo("UselessImportCheck");
    assertThat(rule.subCharacteristicKey()).isEqualTo("COMPILER");
    assertThat(rule.function().name()).isEqualTo("CONSTANT_ISSUE");
    assertThat(rule.coefficient()).isNull();
    assertThat(rule.offset()).isEqualTo("15min");
  }

  @Test
  public void create_characteristics_when_restoring_characteristics() throws Exception {
    when(dao.selectEnabledCharacteristics(session)).thenReturn(Collections.<CharacteristicDto>emptyList());

    debtModelBackup.restoreCharacteristics(
      new DebtModel()
        .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
        .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY"),
      true,
      now,
      session
    );

    verify(dao, times(2)).insert(characteristicCaptor.capture(), eq(session));

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
  public void update_characteristics_when_restoring_characteristics() throws Exception {
    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability updated").setOrder(2).setCreatedAt(oldDate).setUpdatedAt(oldDate),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler updated").setParentId(1).setCreatedAt(oldDate).setUpdatedAt(oldDate)
    ));

    debtModelBackup.restoreCharacteristics(
      new DebtModel()
        .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
        .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY"),
      true,
      now,
      session
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
  public void disable_no_more_existing_characteristics_when_restoring_characteristics() throws Exception {
    CharacteristicDto dto1 = new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability").setOrder(1);
    CharacteristicDto dto2 = new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler").setParentId(1);

    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(dto1, dto2));

    debtModelBackup.restoreCharacteristics(new DebtModel(), true, now, session);

    verify(debtModelOperations).delete(dto1, now, session);
    verify(debtModelOperations).delete(dto2, now, session);
  }

  @Test
  public void reset_from_provided_model() throws Exception {
    when(characteristicsXMLImporter.importXML(any(Reader.class))).thenReturn(new DebtModel()
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY"));

    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability updated").setOrder(2).setCreatedAt(oldDate),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler updated").setParentId(1).setCreatedAt(oldDate)
    ));

    when(ruleDao.selectEnablesAndNonManual(session)).thenReturn(newArrayList(
      new RuleDto().setRepositoryKey("squid").setSubCharacteristicId(2).setRemediationFunction("LINEAR_OFFSET").setRemediationCoefficient("2h").setRemediationOffset("15min")
        .setCreatedAt(oldDate).setUpdatedAt(oldDate)
    ));

    debtModelBackup.reset();

    verify(dao).selectEnabledCharacteristics(session);
    verify(dao, times(2)).update(any(CharacteristicDto.class), eq(session));
    verifyNoMoreInteractions(dao);

    verify(ruleDao).selectEnablesAndNonManual(session);
    verify(ruleDao).update(ruleCaptor.capture(), eq(session));
    verifyNoMoreInteractions(ruleDao);
    verify(ruleRegistry).reindex(ruleCaptor.getAllValues(), session);

    verify(session).commit();

    RuleDto rule = ruleCaptor.getValue();
    assertThat(rule.getSubCharacteristicId()).isNull();
    assertThat(rule.getRemediationFunction()).isNull();
    assertThat(rule.getRemediationCoefficient()).isNull();
    assertThat(rule.getRemediationOffset()).isNull();
    assertThat(rule.getUpdatedAt()).isEqualTo(now);
  }

  @Test
  public void restore_from_xml_with_different_characteristic_and_same_function() throws Exception {
    when(characteristicsXMLImporter.importXML(anyString())).thenReturn(new DebtModel()
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY"));

    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability").setOrder(1).setCreatedAt(oldDate),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler").setParentId(1).setCreatedAt(oldDate)));

    when(rulesXMLImporter.importXML(anyString(), any(ValidationMessages.class))).thenReturn(newArrayList(new RuleDebt()
      .setRuleKey(RuleKey.of("squid", "UselessImportCheck")).setSubCharacteristicKey("COMPILER").setFunction(DebtRemediationFunction.Type.LINEAR).setCoefficient("2h")));

    when(ruleDao.selectEnablesAndNonManual(session)).thenReturn(newArrayList(
      new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck")
        .setDefaultSubCharacteristicId(10).setDefaultRemediationFunction("LINEAR").setDefaultRemediationCoefficient("2h")
        .setCreatedAt(oldDate).setUpdatedAt(oldDate)
    ));

    debtModelBackup.restoreFromXml("<xml/>");

    verify(ruleDao).selectEnablesAndNonManual(session);
    verify(ruleDao).update(ruleCaptor.capture(), eq(session));
    verifyNoMoreInteractions(ruleDao);
    verify(ruleRegistry).reindex(ruleCaptor.getAllValues(), session);
    verify(session).commit();

    RuleDto rule = ruleCaptor.getValue();
    assertThat(rule.getId()).isEqualTo(1);
    assertThat(rule.getSubCharacteristicId()).isEqualTo(2);
    assertThat(rule.getRemediationFunction()).isNull();
    assertThat(rule.getRemediationCoefficient()).isNull();
    assertThat(rule.getRemediationOffset()).isNull();
    assertThat(rule.getUpdatedAt()).isEqualTo(now);
  }

  @Test
  public void restore_from_xml_with_same_characteristic_and_different_function() throws Exception {
    when(characteristicsXMLImporter.importXML(anyString())).thenReturn(new DebtModel()
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY"));

    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability").setOrder(1).setCreatedAt(oldDate),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler").setParentId(1).setCreatedAt(oldDate)));

    when(rulesXMLImporter.importXML(anyString(), any(ValidationMessages.class))).thenReturn(newArrayList(new RuleDebt()
      .setRuleKey(RuleKey.of("squid", "UselessImportCheck")).setSubCharacteristicKey("COMPILER").setFunction(DebtRemediationFunction.Type.LINEAR_OFFSET).setCoefficient("12h").setOffset("11min")));

    when(ruleDao.selectEnablesAndNonManual(session)).thenReturn(newArrayList(
      new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck")
        .setDefaultSubCharacteristicId(2).setDefaultRemediationFunction("LINEAR").setDefaultRemediationCoefficient("2h")
        .setCreatedAt(oldDate).setUpdatedAt(oldDate)
    ));

    debtModelBackup.restoreFromXml("<xml/>");

    verify(ruleDao).selectEnablesAndNonManual(session);
    verify(ruleDao).update(ruleCaptor.capture(), eq(session));
    verifyNoMoreInteractions(ruleDao);
    verify(ruleRegistry).reindex(ruleCaptor.getAllValues(), session);
    verify(session).commit();

    RuleDto rule = ruleCaptor.getValue();
    assertThat(rule.getId()).isEqualTo(1);
    assertThat(rule.getSubCharacteristicId()).isNull();
    assertThat(rule.getRemediationFunction()).isEqualTo("LINEAR_OFFSET");
    assertThat(rule.getRemediationCoefficient()).isEqualTo("12h");
    assertThat(rule.getRemediationOffset()).isEqualTo("11min");
    assertThat(rule.getUpdatedAt()).isEqualTo(now);
  }

  @Test
  public void restore_from_xml_with_same_characteristic_and_same_function() throws Exception {
    when(characteristicsXMLImporter.importXML(anyString())).thenReturn(new DebtModel()
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY"));

    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability").setOrder(1).setCreatedAt(oldDate),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler").setParentId(1).setCreatedAt(oldDate)));

    when(rulesXMLImporter.importXML(anyString(), any(ValidationMessages.class))).thenReturn(newArrayList(new RuleDebt()
      .setRuleKey(RuleKey.of("squid", "UselessImportCheck")).setSubCharacteristicKey("COMPILER").setFunction(DebtRemediationFunction.Type.LINEAR_OFFSET).setCoefficient("2h").setOffset("15min")));

    when(ruleDao.selectEnablesAndNonManual(session)).thenReturn(newArrayList(
      new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck")
        .setDefaultSubCharacteristicId(2).setDefaultRemediationFunction("LINEAR_OFFSET").setDefaultRemediationCoefficient("2h").setDefaultRemediationOffset("15min")
        .setCreatedAt(oldDate).setUpdatedAt(oldDate)
    ));

    debtModelBackup.restoreFromXml("<xml/>");

    verify(ruleDao).selectEnablesAndNonManual(session);
    verify(ruleDao).update(ruleCaptor.capture(), eq(session));
    verifyNoMoreInteractions(ruleDao);
    verify(ruleRegistry).reindex(ruleCaptor.getAllValues(), session);
    verify(session).commit();

    RuleDto rule = ruleCaptor.getValue();
    assertThat(rule.getId()).isEqualTo(1);
    assertThat(rule.getSubCharacteristicId()).isNull();
    assertThat(rule.getRemediationFunction()).isNull();
    assertThat(rule.getRemediationCoefficient()).isNull();
    assertThat(rule.getRemediationOffset()).isNull();
    assertThat(rule.getUpdatedAt()).isEqualTo(now);
  }

  @Test
  public void restore_from_xml_disable_rule_debt_when_not_in_xml_and_rule_have_default_debt_values() throws Exception {
    when(characteristicsXMLImporter.importXML(anyString())).thenReturn(new DebtModel()
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY"));

    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability").setOrder(1).setCreatedAt(oldDate),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler").setParentId(1).setCreatedAt(oldDate)));

    when(ruleDao.selectEnablesAndNonManual(session)).thenReturn(newArrayList(
      new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck")
        .setDefaultSubCharacteristicId(2).setDefaultRemediationFunction("LINEAR_OFFSET").setDefaultRemediationCoefficient("2h").setDefaultRemediationOffset("15min")
        .setCreatedAt(oldDate).setUpdatedAt(oldDate)
    ));

    debtModelBackup.restoreFromXml("<xml/>");

    verify(ruleDao).selectEnablesAndNonManual(session);
    verify(ruleDao).update(ruleCaptor.capture(), eq(session));
    verifyNoMoreInteractions(ruleDao);
    verify(ruleRegistry).reindex(ruleCaptor.getAllValues(), session);
    verify(session).commit();

    RuleDto rule = ruleCaptor.getValue();
    assertThat(rule.getId()).isEqualTo(1);
    assertThat(rule.getSubCharacteristicId()).isEqualTo(-1);
    assertThat(rule.getRemediationFunction()).isNull();
    assertThat(rule.getRemediationCoefficient()).isNull();
    assertThat(rule.getRemediationOffset()).isNull();
    assertThat(rule.getUpdatedAt()).isEqualTo(now);
  }

  @Test
  public void restore_from_xml_set_no_rule_debt_when_not_in_xml_and_rule_has_no_default_debt_values() throws Exception {
    when(characteristicsXMLImporter.importXML(anyString())).thenReturn(new DebtModel()
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY"));

    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability").setOrder(1).setCreatedAt(oldDate),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler").setParentId(1).setCreatedAt(oldDate)));

    when(ruleDao.selectEnablesAndNonManual(session)).thenReturn(newArrayList(
      new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck")
        .setCreatedAt(oldDate).setUpdatedAt(oldDate)
    ));

    debtModelBackup.restoreFromXml("<xml/>");

    verify(ruleDao).selectEnablesAndNonManual(session);
    verify(ruleDao).update(ruleCaptor.capture(), eq(session));
    verifyNoMoreInteractions(ruleDao);
    verify(ruleRegistry).reindex(ruleCaptor.getAllValues(), session);
    verify(session).commit();

    RuleDto rule = ruleCaptor.getValue();
    assertThat(rule.getId()).isEqualTo(1);
    // As rule has no debt value, characteristic is set to null
    assertThat(rule.getSubCharacteristicId()).isNull();
    assertThat(rule.getRemediationFunction()).isNull();
    assertThat(rule.getRemediationCoefficient()).isNull();
    assertThat(rule.getRemediationOffset()).isNull();
    assertThat(rule.getUpdatedAt()).isEqualTo(now);
  }

  @Test
  public void restore_from_xml_and_language() throws Exception {
    when(characteristicsXMLImporter.importXML(anyString())).thenReturn(new DebtModel()
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY"));

    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability").setOrder(1).setCreatedAt(oldDate),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler").setParentId(1).setCreatedAt(oldDate)));

    when(rulesXMLImporter.importXML(anyString(), any(ValidationMessages.class))).thenReturn(newArrayList(new RuleDebt()
      .setRuleKey(RuleKey.of("squid", "UselessImportCheck")).setSubCharacteristicKey("COMPILER").setFunction(DebtRemediationFunction.Type.LINEAR).setCoefficient("2h")));

    when(ruleDao.selectEnablesAndNonManual(session)).thenReturn(newArrayList(
      new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck").setLanguage("java")
        .setDefaultSubCharacteristicId(10).setDefaultRemediationFunction("LINEAR").setDefaultRemediationCoefficient("2h")
        .setCreatedAt(oldDate).setUpdatedAt(oldDate),
      // Should be ignored
      new RuleDto().setId(2).setRepositoryKey("checkstyle").setLanguage("java2")
        .setSubCharacteristicId(3).setRemediationFunction("LINEAR").setRemediationCoefficient("2h")
        .setCreatedAt(oldDate).setUpdatedAt(oldDate)
    ));

    debtModelBackup.restoreFromXml("<xml/>", "java");

    verify(ruleDao).selectEnablesAndNonManual(session);
    verify(ruleDao).update(ruleCaptor.capture(), eq(session));
    verifyNoMoreInteractions(ruleDao);
    verify(ruleRegistry).reindex(ruleCaptor.getAllValues(), session);
    verify(session).commit();

    RuleDto rule = ruleCaptor.getValue();
    assertThat(rule.getId()).isEqualTo(1);
  }

  @Test
  public void restore_from_xml_and_language_do_not_disable_no_more_existing_characteristics() throws Exception {
    when(characteristicsXMLImporter.importXML(anyString())).thenReturn(new DebtModel()
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
        // No more existing characteristic
      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY"));

    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(
        new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability").setOrder(1))
    );

    debtModelBackup.restoreFromXml("<xml/>", "java");

    verify(debtModelOperations, never()).delete(any(CharacteristicDto.class), eq(now), eq(session));
    verify(session).commit();
  }

  @Test
  public void restore_from_xml_and_language_with_rule_not_in_xml_and_linked_on_disabled_default_characteristic() throws Exception {
    when(characteristicsXMLImporter.importXML(anyString())).thenReturn(new DebtModel()
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY"));

    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability updated").setOrder(2).setCreatedAt(oldDate),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler updated").setParentId(1).setCreatedAt(oldDate)
    ));

    when(rulesXMLImporter.importXML(anyString(), any(ValidationMessages.class))).thenReturn(Collections.<RuleDebt>emptyList());
    when(ruleDao.selectEnablesAndNonManual(session)).thenReturn(newArrayList(
      // Linked on a default disabled characteristic -> Rule debt should be disabled
      new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck").setLanguage("java")
        .setDefaultSubCharacteristicId(3).setDefaultRemediationFunction("LINEAR").setDefaultRemediationCoefficient("2h")
        .setSubCharacteristicId(2).setRemediationFunction("LINEAR_OFFSET").setRemediationCoefficient("2h").setRemediationOffset("15min")
        .setCreatedAt(oldDate).setUpdatedAt(oldDate)
    ));

    debtModelBackup.restoreFromXml("<xml/>", "java");

    verify(ruleDao).selectEnablesAndNonManual(session);
    verify(ruleDao).update(ruleCaptor.capture(), eq(session));
    verifyNoMoreInteractions(ruleDao);
    verify(ruleRegistry).reindex(ruleCaptor.getAllValues(), session);
    verify(session).commit();

    RuleDto rule = ruleCaptor.getValue();
    assertThat(rule.getId()).isEqualTo(1);
    assertThat(rule.getSubCharacteristicId()).isEqualTo(-1);
    assertThat(rule.getRemediationFunction()).isNull();
    assertThat(rule.getRemediationCoefficient()).isNull();
    assertThat(rule.getRemediationOffset()).isNull();
    assertThat(rule.getUpdatedAt()).isEqualTo(now);
  }

  @Test
  public void restore_from_xml_and_language_with_rule_linked_on_disabled_characteristic2() throws Exception {
    when(characteristicsXMLImporter.importXML(anyString())).thenReturn(new DebtModel()
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY"));

    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability updated").setOrder(2).setCreatedAt(oldDate),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler updated").setParentId(1).setCreatedAt(oldDate)
    ));

    when(ruleDao.selectEnablesAndNonManual(session)).thenReturn(newArrayList(
      new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck").setLanguage("java")
        .setDefaultSubCharacteristicId(3).setDefaultRemediationFunction("LINEAR").setDefaultRemediationCoefficient("2h")
        .setSubCharacteristicId(2).setRemediationFunction("LINEAR_OFFSET").setRemediationCoefficient("2h").setRemediationOffset("15min")
        .setCreatedAt(oldDate).setUpdatedAt(oldDate)
    ));

    when(rulesXMLImporter.importXML(anyString(), any(ValidationMessages.class))).thenReturn(newArrayList(new RuleDebt()
      // Linked on a default disabled characteristic -> Rule debt should be disabled
      .setRuleKey(RuleKey.of("squid", "UselessImportCheck")).setSubCharacteristicKey("HARDWARE").setFunction(DebtRemediationFunction.Type.LINEAR).setCoefficient("2h")));

    debtModelBackup.restoreFromXml("<xml/>", "java");

    verify(ruleDao).selectEnablesAndNonManual(session);
    verify(ruleDao).update(ruleCaptor.capture(), eq(session));
    verifyNoMoreInteractions(ruleDao);
    verify(ruleRegistry).reindex(ruleCaptor.getAllValues(), session);
    verify(session).commit();

    RuleDto rule = ruleCaptor.getValue();
    assertThat(rule.getId()).isEqualTo(1);
    assertThat(rule.getSubCharacteristicId()).isEqualTo(-1);
    assertThat(rule.getRemediationFunction()).isNull();
    assertThat(rule.getRemediationCoefficient()).isNull();
    assertThat(rule.getRemediationOffset()).isNull();
    assertThat(rule.getUpdatedAt()).isEqualTo(now);
  }

  @Test
  public void add_warning_message_when_rule_from_xml_is_not_found() throws Exception {
    when(characteristicsXMLImporter.importXML(anyString())).thenReturn(new DebtModel()
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY"));

    when(dao.selectEnabledCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability").setOrder(1).setCreatedAt(oldDate),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler").setParentId(1).setCreatedAt(oldDate)));

    when(rulesXMLImporter.importXML(anyString(), any(ValidationMessages.class))).thenReturn(newArrayList(new RuleDebt()
      .setRuleKey(RuleKey.of("squid", "UselessImportCheck")).setSubCharacteristicKey("COMPILER").setFunction(DebtRemediationFunction.Type.LINEAR).setCoefficient("2h")));

    when(ruleDao.selectEnablesAndNonManual(session)).thenReturn(Collections.<RuleDto>emptyList());

    assertThat(debtModelBackup.restoreFromXml("<xml/>").getWarnings()).hasSize(1);

    verify(ruleDao).selectEnablesAndNonManual(session);
    verifyNoMoreInteractions(ruleDao);
    verify(ruleRegistry).reindex(ruleCaptor.getAllValues(), session);
    verify(session).commit();
  }

}
