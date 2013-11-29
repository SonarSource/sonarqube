/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.core.technicaldebt;

import com.google.common.collect.Lists;
import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.technicaldebt.batch.internal.DefaultCharacteristic;
import org.sonar.api.technicaldebt.batch.internal.DefaultRequirement;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.api.utils.WorkUnit;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.core.technicaldebt.db.CharacteristicDto;

import java.io.Reader;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TechnicalDebtModelSynchronizerTest {

  @Mock
  MyBatis myBatis;

  @Mock
  SqlSession session;

  @Mock
  TechnicalDebtModelRepository technicalDebtModelRepository;

  @Mock
  TechnicalDebtRuleCache ruleCache;

  @Mock
  CharacteristicDao dao;

  @Mock
  TechnicalDebtXMLImporter xmlImporter;

  private DefaultTechnicalDebtModel defaultModel;

  private TechnicalDebtModelSynchronizer manager;

  @Before
  public void initAndMerge() throws Exception {
    when(myBatis.openSession()).thenReturn(session);

    defaultModel = new DefaultTechnicalDebtModel();
    Reader defaultModelReader = mock(Reader.class);
    when(technicalDebtModelRepository.createReaderForXMLFile("technical-debt")).thenReturn(defaultModelReader);
    when(xmlImporter.importXML(eq(defaultModelReader), any(ValidationMessages.class), eq(ruleCache))).thenReturn(defaultModel);

    manager = new TechnicalDebtModelSynchronizer(myBatis, dao, technicalDebtModelRepository, xmlImporter);
  }

  @Test
  public void create_default_model_on_first_execution_when_no_plugin() throws Exception {
    DefaultCharacteristic rootCharacteristic = new DefaultCharacteristic().setKey("PORTABILITY");
    new DefaultCharacteristic().setKey("COMPILER_RELATED_PORTABILITY").setParent(rootCharacteristic);
    defaultModel.addRootCharacteristic(rootCharacteristic);

    when(technicalDebtModelRepository.getContributingPluginList()).thenReturn(Collections.<String>emptyList());
    when(dao.selectEnabledCharacteristics()).thenReturn(Lists.<CharacteristicDto>newArrayList());

    manager.synchronize(ValidationMessages.create(), ruleCache);

    verify(dao).selectEnabledCharacteristics();
    ArgumentCaptor<CharacteristicDto> characteristicCaptor = ArgumentCaptor.forClass(CharacteristicDto.class);
    verify(dao, times(2)).insert(characteristicCaptor.capture(), eq(session));

    List<CharacteristicDto> result = characteristicCaptor.getAllValues();
    assertThat(result.get(0).getKey()).isEqualTo("PORTABILITY");
    assertThat(result.get(1).getKey()).isEqualTo("COMPILER_RELATED_PORTABILITY");
    verifyNoMoreInteractions(dao);
  }

  @Test
  public void create_model_with_requirements_from_plugin_on_first_execution() throws Exception {
    // Default model
    DefaultCharacteristic defaultRootCharacteristic = new DefaultCharacteristic().setKey("PORTABILITY");
    new DefaultCharacteristic().setKey("COMPILER_RELATED_PORTABILITY").setParent(defaultRootCharacteristic);
    defaultModel.addRootCharacteristic(defaultRootCharacteristic);

    // No db model
    when(dao.selectEnabledCharacteristics()).thenReturn(Lists.<CharacteristicDto>newArrayList());

    // Java model
    DefaultTechnicalDebtModel javaModel = new DefaultTechnicalDebtModel();
    DefaultCharacteristic javaRootCharacteristic = new DefaultCharacteristic().setKey("PORTABILITY");
    DefaultCharacteristic javaCharacteristic = new DefaultCharacteristic().setKey("COMPILER_RELATED_PORTABILITY").setParent(javaRootCharacteristic);
    javaModel.addRootCharacteristic(javaRootCharacteristic);

    Rule rule = Rule.create();
    rule.setId(10);
    RuleKey ruleKey = RuleKey.of("checkstyle", "import");
    when(ruleCache.getByRuleKey(ruleKey)).thenReturn(rule);
    new DefaultRequirement().setRuleKey(ruleKey)
      .setFunction("linear").setFactor(WorkUnit.create(30.0, WorkUnit.MINUTES)).setCharacteristic(javaCharacteristic);

    Reader javaModelReader = mock(Reader.class);
    when(xmlImporter.importXML(eq(javaModelReader), any(ValidationMessages.class), eq(ruleCache))).thenReturn(javaModel);
    when(technicalDebtModelRepository.createReaderForXMLFile("java")).thenReturn(javaModelReader);
    when(technicalDebtModelRepository.getContributingPluginList()).thenReturn(newArrayList("java"));

    manager.synchronize(ValidationMessages.create(), ruleCache);

    verify(dao).selectEnabledCharacteristics();
    ArgumentCaptor<CharacteristicDto> characteristicCaptor = ArgumentCaptor.forClass(CharacteristicDto.class);
    verify(dao, times(3)).insert(characteristicCaptor.capture(), eq(session));

    List<CharacteristicDto> result = characteristicCaptor.getAllValues();
    assertThat(result.get(0).getKey()).isEqualTo("PORTABILITY");
    assertThat(result.get(1).getKey()).isEqualTo("COMPILER_RELATED_PORTABILITY");
    assertThat(result.get(2).getRuleId()).isEqualTo(10);
    verifyNoMoreInteractions(dao);
  }

  @Test
  public void add_new_requirements_from_plugin() throws Exception {
    // Default model
    DefaultCharacteristic defaultRootCharacteristic = new DefaultCharacteristic().setKey("PORTABILITY");
    new DefaultCharacteristic().setKey("COMPILER_RELATED_PORTABILITY").setParent(defaultRootCharacteristic);
    defaultModel.addRootCharacteristic(defaultRootCharacteristic);

    // Db model
    CharacteristicDto dbRootCharacteristic = new CharacteristicDto().setId(1).setKey("PORTABILITY");
    CharacteristicDto dbCharacteristic = new CharacteristicDto().setId(2).setKey("COMPILER_RELATED_PORTABILITY").setParentId(1);
    CharacteristicDto requirement = new CharacteristicDto().setId(3)
      .setRuleId(10).setParentId(2).setFactorValue(30.0).setFactorUnit("mn");

    RuleKey ruleKey1 = RuleKey.of("checkstyle", "import");
    Rule rule1 = Rule.create();
    rule1.setId(10);
    when(ruleCache.getByRuleKey(ruleKey1)).thenReturn(rule1);
    when(ruleCache.exists(10)).thenReturn(true);
    when(dao.selectEnabledCharacteristics()).thenReturn(newArrayList(requirement, dbCharacteristic, dbRootCharacteristic));

    // Java model
    DefaultTechnicalDebtModel javaModel = new DefaultTechnicalDebtModel();
    DefaultCharacteristic javaRootCharacteristic = new DefaultCharacteristic().setKey("PORTABILITY");
    DefaultCharacteristic javaCharacteristic = new DefaultCharacteristic().setKey("COMPILER_RELATED_PORTABILITY").setParent(javaRootCharacteristic);
    javaModel.addRootCharacteristic(javaRootCharacteristic);

    RuleKey ruleKey2 = RuleKey.of("checkstyle", "export");
    Rule rule2 = Rule.create();
    rule2.setId(11);
    when(ruleCache.getByRuleKey(ruleKey2)).thenReturn(rule2);

    // New requirement
    new DefaultRequirement().setRuleKey(ruleKey2)
      .setFunction("linear").setFactor(WorkUnit.create(1.0, WorkUnit.HOURS)).setCharacteristic(javaCharacteristic);

    Reader javaModelReader = mock(Reader.class);
    when(technicalDebtModelRepository.createReaderForXMLFile("java")).thenReturn(javaModelReader);
    when(xmlImporter.importXML(eq(javaModelReader), any(ValidationMessages.class), eq(ruleCache))).thenReturn(javaModel);
    when(technicalDebtModelRepository.getContributingPluginList()).thenReturn(newArrayList("java"));

    manager.synchronize(ValidationMessages.create(), ruleCache);

    verify(dao).selectEnabledCharacteristics();
    ArgumentCaptor<CharacteristicDto> characteristicCaptor = ArgumentCaptor.forClass(CharacteristicDto.class);
    verify(dao).insert(characteristicCaptor.capture(), eq(session));
    assertThat(characteristicCaptor.getValue().getRuleId()).isEqualTo(11);
    verifyNoMoreInteractions(dao);
  }

  @Test
  public void disable_requirements_on_not_existing_rules() throws Exception {
    // Default model
    DefaultCharacteristic defaultRootCharacteristic = new DefaultCharacteristic().setKey("PORTABILITY");
    new DefaultCharacteristic().setKey("COMPILER_RELATED_PORTABILITY").setParent(defaultRootCharacteristic);
    defaultModel.addRootCharacteristic(defaultRootCharacteristic);

    // Db model
    CharacteristicDto dbRootCharacteristic = new CharacteristicDto().setId(1).setKey("PORTABILITY");
    CharacteristicDto dbCharacteristic = new CharacteristicDto().setId(2).setKey("COMPILER_RELATED_PORTABILITY").setParentId(1);
    // To be disabled as rule does not exists
    CharacteristicDto requirement = new CharacteristicDto().setId(3)
      .setRuleId(10).setParentId(2).setFactorValue(30.0).setFactorUnit("mn");

    when(ruleCache.exists(10)).thenReturn(false);

    when(dao.selectEnabledCharacteristics()).thenReturn(newArrayList(dbRootCharacteristic, dbCharacteristic, requirement));

    manager.synchronize(ValidationMessages.create(), ruleCache);

    verify(dao).selectEnabledCharacteristics();
    verify(dao).disable(eq(3), eq(session));
    verifyNoMoreInteractions(dao);
  }

  @Test
  public void fail_when_plugin_defines_characteristics_not_defined_in_default_model() throws Exception {
    try {
      // Default model
      DefaultCharacteristic defaultRootCharacteristic = new DefaultCharacteristic().setKey("PORTABILITY");
      new DefaultCharacteristic().setKey("COMPILER_RELATED_PORTABILITY").setParent(defaultRootCharacteristic);
      defaultModel.addRootCharacteristic(defaultRootCharacteristic);

      // Db model
      CharacteristicDto dbRootCharacteristic = new CharacteristicDto().setId(1).setKey("PORTABILITY");
      CharacteristicDto dbCharacteristic = new CharacteristicDto().setId(2).setKey("COMPILER_RELATED_PORTABILITY").setParentId(1);
      when(dao.selectEnabledCharacteristics()).thenReturn(newArrayList(dbRootCharacteristic, dbCharacteristic));

      // Java model
      DefaultTechnicalDebtModel javaModel = new DefaultTechnicalDebtModel();
      DefaultCharacteristic javaRootCharacteristic = new DefaultCharacteristic().setKey("PORTABILITY");
      new DefaultCharacteristic().setKey("NEW_CHARACTERISTIC").setParent(javaRootCharacteristic);
      javaModel.addRootCharacteristic(javaRootCharacteristic);

      Reader javaModelReader = mock(Reader.class);
      when(technicalDebtModelRepository.createReaderForXMLFile("java")).thenReturn(javaModelReader);
      when(xmlImporter.importXML(eq(javaModelReader), any(ValidationMessages.class), eq(ruleCache))).thenReturn(javaModel);
      when(technicalDebtModelRepository.getContributingPluginList()).thenReturn(newArrayList("java"));

      manager.synchronize(ValidationMessages.create(), ruleCache);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("The characteristic : NEW_CHARACTERISTIC cannot be used as it's not available in default characteristics.");
    } finally {
      verify(dao).selectEnabledCharacteristics();
      verifyNoMoreInteractions(dao);
    }
  }

}
