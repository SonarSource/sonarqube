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
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.debt.internal.DefaultDebtCharacteristic;
import org.sonar.api.server.rule.DebtRemediationFunction;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.rule.RuleDao;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.technicaldebt.TechnicalDebtModelRepository;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.core.technicaldebt.db.CharacteristicDto;
import org.sonar.server.rule.RuleRepositories;
import org.sonar.server.user.MockUserSession;

import java.io.Reader;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DebtModelRestoreTest {

  @Mock
  MyBatis myBatis;

  @Mock
  SqlSession session;

  @Mock
  TechnicalDebtModelRepository debtModelPluginRepository;

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
  RuleRepositories ruleRepositories;

  @Mock
  System2 system2;

  Date now = DateUtils.parseDate("2014-03-19");

  int currentId;

  DebtModel characteristics = new DebtModel();
  List<DebtRulesXMLImporter.RuleDebt> rules = newArrayList();

  DebtModelRestore debtModelRestore;

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
    when(characteristicsXMLImporter.importXML(eq(defaultModelReader))).thenReturn(characteristics);
    when(characteristicsXMLImporter.importXML(anyString())).thenReturn(characteristics);
    when(rulesXMLImporter.importXML(anyString(), any(ValidationMessages.class))).thenReturn(rules);

    debtModelRestore = new DebtModelRestore(myBatis, dao, ruleDao, debtModelOperations, debtModelPluginRepository, ruleRepositories, characteristicsXMLImporter, rulesXMLImporter,
      system2);
  }

  @Test
  public void create_characteristics_when_restoring_characteristics() throws Exception {
    debtModelRestore.restoreCharacteristics(
      new DebtModel()
        .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
        .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY"),
      Collections.<CharacteristicDto>emptyList(),
      now,
      session
    );

    ArgumentCaptor<CharacteristicDto> characteristicArgument = ArgumentCaptor.forClass(CharacteristicDto.class);
    verify(dao, times(2)).insert(characteristicArgument.capture(), eq(session));

    CharacteristicDto dto1 = characteristicArgument.getAllValues().get(0);
    assertThat(dto1.getId()).isEqualTo(10);
    assertThat(dto1.getKey()).isEqualTo("PORTABILITY");
    assertThat(dto1.getName()).isEqualTo("Portability");
    assertThat(dto1.getParentId()).isNull();
    assertThat(dto1.getOrder()).isEqualTo(1);
    assertThat(dto1.getCreatedAt()).isEqualTo(now);
    assertThat(dto1.getUpdatedAt()).isNull();

    CharacteristicDto dto2 = characteristicArgument.getAllValues().get(1);
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
    Date oldDate = DateUtils.parseDate("2014-01-01");

    debtModelRestore.restoreCharacteristics(
      new DebtModel()
        .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
        .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY"),
      newArrayList(
        new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability updated").setOrder(2).setCreatedAt(oldDate).setUpdatedAt(oldDate),
        new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler updated").setParentId(1).setCreatedAt(oldDate).setUpdatedAt(oldDate)
      ),
      now,
      session
    );

    ArgumentCaptor<CharacteristicDto> characteristicArgument = ArgumentCaptor.forClass(CharacteristicDto.class);
    verify(dao, times(2)).update(characteristicArgument.capture(), eq(session));

    CharacteristicDto dto1 = characteristicArgument.getAllValues().get(0);
    assertThat(dto1.getId()).isEqualTo(1);
    assertThat(dto1.getKey()).isEqualTo("PORTABILITY");
    assertThat(dto1.getName()).isEqualTo("Portability");
    assertThat(dto1.getParentId()).isNull();
    assertThat(dto1.getOrder()).isEqualTo(1);
    assertThat(dto1.getCreatedAt()).isEqualTo(oldDate);
    assertThat(dto1.getUpdatedAt()).isEqualTo(now);

    CharacteristicDto dto2 = characteristicArgument.getAllValues().get(1);
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

    debtModelRestore.restoreCharacteristics(new DebtModel(), newArrayList(dto1, dto2), now, session);

    verify(debtModelOperations).disableCharacteristic(dto1, now, session);
    verify(debtModelOperations).disableCharacteristic(dto2, now, session);
  }

  @Test
  public void restore_from_provided_model() throws Exception {
    Date oldDate = DateUtils.parseDate("2014-01-01");

    characteristics
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY");

    when(dao.selectEnabledCharacteristics()).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability updated").setOrder(2).setCreatedAt(oldDate),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler updated").setParentId(1).setCreatedAt(oldDate)
    ));

    when(ruleDao.selectEnablesAndNonManual(session)).thenReturn(newArrayList(
      new RuleDto().setRepositoryKey("squid").setCharacteristicId(2).setRemediationFunction("LINEAR_OFFSET").setRemediationFactor("2h").setRemediationOffset("15min")
        .setCreatedAt(oldDate).setUpdatedAt(oldDate)
    ));

    debtModelRestore.restore();

    verify(dao).selectEnabledCharacteristics();
    verify(dao, times(2)).update(any(CharacteristicDto.class), eq(session));
    verifyNoMoreInteractions(dao);

    verify(ruleDao).selectEnablesAndNonManual(session);
    ArgumentCaptor<RuleDto> ruleArgument = ArgumentCaptor.forClass(RuleDto.class);
    verify(ruleDao).update(ruleArgument.capture(), eq(session));
    verifyNoMoreInteractions(ruleDao);

    RuleDto rule = ruleArgument.getValue();
    assertThat(rule.getCharacteristicId()).isNull();
    assertThat(rule.getRemediationFunction()).isNull();
    assertThat(rule.getRemediationFactor()).isNull();
    assertThat(rule.getRemediationOffset()).isNull();
    assertThat(rule.getUpdatedAt()).isEqualTo(now);

    verify(session).commit();
  }

  @Test
  public void restore_from_language() throws Exception {
    Date oldDate = DateUtils.parseDate("2014-01-01");

    characteristics
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY");

    when(dao.selectEnabledCharacteristics()).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability updated").setOrder(2).setCreatedAt(oldDate),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler updated").setParentId(1).setCreatedAt(oldDate)
    ));

    when(ruleDao.selectEnablesAndNonManual(session)).thenReturn(newArrayList(
      new RuleDto().setId(1).setRepositoryKey("squid")
        .setCharacteristicId(2).setRemediationFunction("LINEAR_OFFSET").setRemediationFactor("2h").setRemediationOffset("15min")
        .setCreatedAt(oldDate).setUpdatedAt(oldDate),
      // Should be ignored
      new RuleDto().setId(2).setRepositoryKey("checkstyle")
        .setCharacteristicId(3).setRemediationFunction("LINEAR").setRemediationFactor("2h")
        .setCreatedAt(oldDate).setUpdatedAt(oldDate)
    ));

    RuleRepositories.Repository squid = mock(RuleRepositories.Repository.class);
    when(squid.getKey()).thenReturn("squid");
    when(ruleRepositories.repositoriesForLang("java")).thenReturn(newArrayList(squid));

    debtModelRestore.restore("java");

    verify(dao).selectEnabledCharacteristics();
    verify(dao, times(2)).update(any(CharacteristicDto.class), eq(session));
    verifyNoMoreInteractions(dao);

    verify(ruleDao).selectEnablesAndNonManual(session);
    ArgumentCaptor<RuleDto> ruleArgument = ArgumentCaptor.forClass(RuleDto.class);
    verify(ruleDao).update(ruleArgument.capture(), eq(session));
    verifyNoMoreInteractions(ruleDao);

    RuleDto rule = ruleArgument.getValue();
    assertThat(rule.getId()).isEqualTo(1);

    verify(session).commit();
  }

  @Test
  public void restore_from_xml_with_different_characteristic_and_same_function() throws Exception {
    Date oldDate = DateUtils.parseDate("2014-01-01");

    characteristics
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY");

    when(dao.selectEnabledCharacteristics()).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability").setOrder(1).setCreatedAt(oldDate),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler").setParentId(1).setCreatedAt(oldDate)));

    rules.add(new DebtRulesXMLImporter.RuleDebt()
      .setRuleKey(RuleKey.of("squid", "UselessImportCheck")).setCharacteristicKey("COMPILER").setFunction(DebtRemediationFunction.Type.LINEAR).setFactor("2h"));

    when(ruleDao.selectEnablesAndNonManual(session)).thenReturn(newArrayList(
      new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck")
        .setDefaultCharacteristicId(10).setDefaultRemediationFunction("LINEAR").setDefaultRemediationFactor("2h")
        .setCreatedAt(oldDate).setUpdatedAt(oldDate)
    ));

    debtModelRestore.restoreFromXml("<xml/>");

    verify(ruleDao).selectEnablesAndNonManual(session);
    ArgumentCaptor<RuleDto> ruleArgument = ArgumentCaptor.forClass(RuleDto.class);
    verify(ruleDao).update(ruleArgument.capture(), eq(session));
    verifyNoMoreInteractions(ruleDao);

    RuleDto rule = ruleArgument.getValue();
    assertThat(rule.getId()).isEqualTo(1);
    assertThat(rule.getCharacteristicId()).isEqualTo(2);
    assertThat(rule.getRemediationFunction()).isNull();
    assertThat(rule.getRemediationFactor()).isNull();
    assertThat(rule.getRemediationOffset()).isNull();
    assertThat(rule.getUpdatedAt()).isEqualTo(now);

    verify(session).commit();
  }

  @Test
  public void restore_from_xml_with_same_characteristic_and_different_function() throws Exception {
    Date oldDate = DateUtils.parseDate("2014-01-01");

    characteristics
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY");

    when(dao.selectEnabledCharacteristics()).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability").setOrder(1).setCreatedAt(oldDate),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler").setParentId(1).setCreatedAt(oldDate)));

    rules.add(new DebtRulesXMLImporter.RuleDebt()
      .setRuleKey(RuleKey.of("squid", "UselessImportCheck")).setCharacteristicKey("COMPILER").setFunction(DebtRemediationFunction.Type.LINEAR_OFFSET).setFactor("12h").setOffset("11min"));

    when(ruleDao.selectEnablesAndNonManual(session)).thenReturn(newArrayList(
      new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck")
        .setDefaultCharacteristicId(2).setDefaultRemediationFunction("LINEAR").setDefaultRemediationFactor("2h")
        .setCreatedAt(oldDate).setUpdatedAt(oldDate)
    ));

    debtModelRestore.restoreFromXml("<xml/>");

    verify(ruleDao).selectEnablesAndNonManual(session);
    ArgumentCaptor<RuleDto> ruleArgument = ArgumentCaptor.forClass(RuleDto.class);
    verify(ruleDao).update(ruleArgument.capture(), eq(session));
    verifyNoMoreInteractions(ruleDao);

    RuleDto rule = ruleArgument.getValue();
    assertThat(rule.getId()).isEqualTo(1);
    assertThat(rule.getCharacteristicId()).isNull();
    assertThat(rule.getRemediationFunction()).isEqualTo("LINEAR_OFFSET");
    assertThat(rule.getRemediationFactor()).isEqualTo("12h");
    assertThat(rule.getRemediationOffset()).isEqualTo("11min");
    assertThat(rule.getUpdatedAt()).isEqualTo(now);

    verify(session).commit();
  }

  @Test
  public void restore_from_xml_with_same_characteristic_and_same_function() throws Exception {
    Date oldDate = DateUtils.parseDate("2014-01-01");

    characteristics
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY");

    when(dao.selectEnabledCharacteristics()).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability").setOrder(1).setCreatedAt(oldDate),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler").setParentId(1).setCreatedAt(oldDate)));

    rules.add(new DebtRulesXMLImporter.RuleDebt()
      .setRuleKey(RuleKey.of("squid", "UselessImportCheck")).setCharacteristicKey("COMPILER").setFunction(DebtRemediationFunction.Type.LINEAR_OFFSET).setFactor("2h").setOffset("15min"));

    when(ruleDao.selectEnablesAndNonManual(session)).thenReturn(newArrayList(
      new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck")
        .setDefaultCharacteristicId(2).setDefaultRemediationFunction("LINEAR_OFFSET").setDefaultRemediationFactor("2h").setDefaultRemediationOffset("15min")
        .setCreatedAt(oldDate).setUpdatedAt(oldDate)
    ));

    debtModelRestore.restoreFromXml("<xml/>");

    verify(ruleDao).selectEnablesAndNonManual(session);
    ArgumentCaptor<RuleDto> ruleArgument = ArgumentCaptor.forClass(RuleDto.class);
    verify(ruleDao).update(ruleArgument.capture(), eq(session));
    verifyNoMoreInteractions(ruleDao);

    RuleDto rule = ruleArgument.getValue();
    assertThat(rule.getId()).isEqualTo(1);
    assertThat(rule.getCharacteristicId()).isNull();
    assertThat(rule.getRemediationFunction()).isNull();
    assertThat(rule.getRemediationFactor()).isNull();
    assertThat(rule.getRemediationOffset()).isNull();
    assertThat(rule.getUpdatedAt()).isEqualTo(now);

    verify(session).commit();
  }

  @Test
  public void restore_from_xml_disable_rule_debt_when_not_in_xml() throws Exception {
    Date oldDate = DateUtils.parseDate("2014-01-01");

    characteristics
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY");

    when(dao.selectEnabledCharacteristics()).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability").setOrder(1).setCreatedAt(oldDate),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler").setParentId(1).setCreatedAt(oldDate)));

    when(ruleDao.selectEnablesAndNonManual(session)).thenReturn(newArrayList(
      new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck")
        .setDefaultCharacteristicId(2).setDefaultRemediationFunction("LINEAR_OFFSET").setDefaultRemediationFactor("2h").setDefaultRemediationOffset("15min")
        .setCreatedAt(oldDate).setUpdatedAt(oldDate)
    ));

    debtModelRestore.restoreFromXml("<xml/>");

    verify(ruleDao).selectEnablesAndNonManual(session);
    ArgumentCaptor<RuleDto> ruleArgument = ArgumentCaptor.forClass(RuleDto.class);
    verify(ruleDao).update(ruleArgument.capture(), eq(session));
    verifyNoMoreInteractions(ruleDao);

    RuleDto rule = ruleArgument.getValue();
    assertThat(rule.getId()).isEqualTo(1);
    assertThat(rule.getCharacteristicId()).isEqualTo(-1);
    assertThat(rule.getRemediationFunction()).isNull();
    assertThat(rule.getRemediationFactor()).isNull();
    assertThat(rule.getRemediationOffset()).isNull();
    assertThat(rule.getUpdatedAt()).isEqualTo(now);

    verify(session).commit();
  }

  @Test
  public void restore_from_xml_and_language() throws Exception {
    Date oldDate = DateUtils.parseDate("2014-01-01");

    characteristics
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY");

    when(dao.selectEnabledCharacteristics()).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability").setOrder(1).setCreatedAt(oldDate),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler").setParentId(1).setCreatedAt(oldDate)));

    rules.add(new DebtRulesXMLImporter.RuleDebt()
      .setRuleKey(RuleKey.of("squid", "UselessImportCheck")).setCharacteristicKey("COMPILER").setFunction(DebtRemediationFunction.Type.LINEAR).setFactor("2h"));

    when(ruleDao.selectEnablesAndNonManual(session)).thenReturn(newArrayList(
      new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck")
        .setDefaultCharacteristicId(10).setDefaultRemediationFunction("LINEAR").setDefaultRemediationFactor("2h")
        .setCreatedAt(oldDate).setUpdatedAt(oldDate),
      // Should be ignored
      new RuleDto().setId(2).setRepositoryKey("checkstyle")
        .setCharacteristicId(3).setRemediationFunction("LINEAR").setRemediationFactor("2h")
        .setCreatedAt(oldDate).setUpdatedAt(oldDate)
    ));

    RuleRepositories.Repository squid = mock(RuleRepositories.Repository.class);
    when(squid.getKey()).thenReturn("squid");
    when(ruleRepositories.repositoriesForLang("java")).thenReturn(newArrayList(squid));

    debtModelRestore.restoreFromXml("<xml/>", "java");

    verify(ruleDao).selectEnablesAndNonManual(session);
    ArgumentCaptor<RuleDto> ruleArgument = ArgumentCaptor.forClass(RuleDto.class);
    verify(ruleDao).update(ruleArgument.capture(), eq(session));
    verifyNoMoreInteractions(ruleDao);

    RuleDto rule = ruleArgument.getValue();
    assertThat(rule.getId()).isEqualTo(1);

    verify(session).commit();
  }

  @Test
  public void add_warning_message_when_rule_from_xml_is_not_found() throws Exception {
    Date oldDate = DateUtils.parseDate("2014-01-01");

    characteristics
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(1))
      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("COMPILER").setName("Compiler"), "PORTABILITY");

    when(dao.selectEnabledCharacteristics()).thenReturn(newArrayList(
      new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability").setOrder(1).setCreatedAt(oldDate),
      new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler").setParentId(1).setCreatedAt(oldDate)));

    rules.add(new DebtRulesXMLImporter.RuleDebt()
      .setRuleKey(RuleKey.of("squid", "UselessImportCheck")).setCharacteristicKey("COMPILER").setFunction(DebtRemediationFunction.Type.LINEAR).setFactor("2h"));

    when(ruleDao.selectEnablesAndNonManual(session)).thenReturn(Collections.<RuleDto>emptyList());

    ValidationMessages validationMessages = debtModelRestore.restoreFromXml("<xml/>");

    assertThat(validationMessages.getWarnings()).hasSize(1);

    verify(ruleDao).selectEnablesAndNonManual(session);
    verifyNoMoreInteractions(ruleDao);

    verify(session).commit();
  }

}
